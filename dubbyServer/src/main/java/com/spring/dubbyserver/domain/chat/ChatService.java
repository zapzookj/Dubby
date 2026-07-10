package com.spring.dubbyserver.domain.chat;

import com.spring.dubbyserver.domain.billing.BillingService;
import com.spring.dubbyserver.domain.chat.dto.ChatMessageDto;
import com.spring.dubbyserver.domain.chat.dto.ChatQuotaDto;
import com.spring.dubbyserver.domain.chat.dto.ChatSendResponse;
import com.spring.dubbyserver.domain.diary.DiaryCandidate;
import com.spring.dubbyserver.domain.diary.DiaryService;
import com.spring.dubbyserver.domain.metrics.AppEventRecorder;
import com.spring.dubbyserver.domain.user.User;
import com.spring.dubbyserver.domain.user.UserService;
import com.spring.dubbyserver.global.common.CursorPageResponse;
import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DerbyCopy;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import com.spring.dubbyserver.infra.llm.*;
import com.spring.dubbyserver.infra.llm.LlmClient.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/** 채팅 오케스트레이션 — derby_llm_pipeline_v1.md §1.3 시퀀스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatQuotaService quotaService;
    private final BillingService billingService;
    private final DiaryService diaryService;
    private final UserService userService;
    private final SafetyFilter safetyFilter;
    private final PromptFactory promptFactory;
    private final LlmClient llmClient;
    private final OutputValidator outputValidator;
    private final LlmBudgetService budgetService;
    private final LlmUsageLogService usageLog;
    private final AppEventRecorder events;
    private final DubbyProperties properties;
    private final ObjectMapper objectMapper;

    /** 사용자당 동시 1건 가드 */
    private final ConcurrentHashMap<UUID, Boolean> inFlight = new ConcurrentHashMap<>();

    private static final Pattern PRECISE_PATTERN = Pattern.compile("(정확|진짜로|제대로|똑바로)");
    private static final Pattern DIARY_SELF_PATTERN =
            Pattern.compile("(나는|난 |내가 |제가 |저는 |내 취미|내 직업)");

    // ═══ 전송 ═══

    public ChatSendResponse send(UUID userId, String clientMessageId, String content) {
        // 1. 입력 검증
        if (content == null || content.isBlank() || clientMessageId == null || clientMessageId.isBlank()) {
            throw new DubbyException(ErrorCode.COMMON_INVALID_REQUEST, "content/clientMessageId required");
        }
        if (content.length() > properties.chat().maxContentLength()) {
            throw new DubbyException(ErrorCode.CHAT_CONTENT_TOO_LONG);
        }

        User user = userService.getActiveUser(userId);
        ZoneId zone = ZoneId.of(user.getTimezone());
        LocalDate localDate = LocalDate.now(zone);
        int limit = billingService.chatDailyLimit(billingService.resolveTier(userId));

        // 2. 멱등 재생
        var existing = messageRepository.findByUserIdAndClientMsgId(userId, clientMessageId);
        if (existing.isPresent()) {
            return replay(userId, existing.get(), localDate, limit);
        }

        // 3. 안전 사전 필터 — LLM 미호출·쿼터 미차감·대화 미저장
        SafetyFilter.Category preHit = safetyFilter.preCheck(content);
        if (preHit != null) {
            int used = quotaService.used(userId, localDate);
            return ChatSendResponse.safety(SafetyNotices.of(preHit),
                    new ChatQuotaDto(null, limit, used, limit - used, null, null));
        }

        // 4. 동시성 가드
        if (inFlight.putIfAbsent(userId, Boolean.TRUE) != null) {
            throw new DubbyException(ErrorCode.CHAT_CONCURRENT_REQUEST);
        }
        try {
            return sendInternal(user, zone, localDate, limit, clientMessageId, content);
        } finally {
            inFlight.remove(userId);
        }
    }

    private ChatSendResponse sendInternal(User user, ZoneId zone, LocalDate localDate, int limit,
                                          String clientMessageId, String content) {
        UUID userId = user.getId();

        // 5. 쿼터 원자적 선차감
        var consumed = quotaService.tryConsume(userId, localDate, limit);
        if (consumed.isEmpty()) {
            events.record(userId, "CHAT_LIMIT_HIT", null, null);
            throw new ChatLimitExceededException(nextMidnight(zone), "SALARY");
        }
        int used = consumed.get();

        try {
            // 6. 글로벌 예산
            if (budgetService.isGlobalBudgetExhausted()) {
                quotaService.refund(userId, localDate);
                throw new DubbyException(ErrorCode.LLM_BUDGET_EXHAUSTED);
            }

            // 7. 오해 강도 + 정밀 모드
            int level = decideMisreadLevel(userId, content);
            LlmPurpose purpose = isPrecise(content) ? LlmPurpose.CHAT_PRECISE : LlmPurpose.CHAT;

            // 8. 일기 게이트
            boolean diaryGate = diaryGateAllows(userId, zone, content);

            // 9. 컨텍스트 조립 → 10. LLM 호출 → 12. 검증 (1회 교정 재시도)
            List<Message> messages = buildMessages(user, zone, level, diaryGate, content);
            LlmOutcome outcome = callWithValidation(userId, purpose, messages, level);

            // 11. 모델 자기신고 안전 — reply 폐기, 쿼터 환불, user 메시지만 flagged 저장
            if (outcome.safetyCategory != null) {
                quotaService.refund(userId, localDate);
                saveFlaggedUserMessage(userId, content, clientMessageId);
                int usedNow = quotaService.used(userId, localDate);
                return ChatSendResponse.safety(SafetyNotices.of(outcome.safetyCategory),
                        new ChatQuotaDto(null, limit, usedNow, limit - usedNow, null, null));
            }

            // 13. 저장
            ChatMessage userMsg = messageRepository.save(
                    ChatMessage.user(userId, content, clientMessageId, false));
            ChatMessage derbyMsg = messageRepository.save(
                    ChatMessage.derby(userId, outcome.reply, outcome.model, level,
                            outcome.promptTokens, outcome.completionTokens));
            events.record(userId, "CHAT_SENT", null, userMsg.getId(), Map.of("level", level));

            // 일기 후보 (실패해도 채팅은 성공 — 부가 기능)
            DiaryCandidate candidate = null;
            if (outcome.diary != null) {
                try {
                    boolean auto = Boolean.TRUE.equals(user.getPrefs().get("autoDiary"));
                    candidate = diaryService.createCandidate(userId, userMsg.getId(),
                            outcome.diary.path("fact").asString(""),
                            outcome.diary.path("interpretation").asString(""),
                            outcome.diary.path("conclusion").asString(""));
                    if (auto) {
                        diaryService.approve(userId, candidate.getId(), true);
                    }
                } catch (Exception e) {
                    log.warn("일기 후보 저장 실패 (무시): {}", e.getMessage());
                    candidate = null;
                }
            }

            return ChatSendResponse.derby(
                    ChatMessageDto.from(userMsg), ChatMessageDto.from(derbyMsg),
                    candidate != null
                            ? new ChatSendResponse.DiaryCandidatePreview(candidate.getId(),
                                    candidate.getFact() + " " + candidate.getInterpretation())
                            : null,
                    new ChatQuotaDto(null, limit, used, limit - used, null, null));
        } catch (DubbyException e) {
            // LLM 실패 시 환불 (스펙 §10)
            if (e.getErrorCode() == ErrorCode.LLM_UPSTREAM_ERROR || e.getErrorCode() == ErrorCode.LLM_TIMEOUT) {
                quotaService.refund(userId, localDate);
            }
            throw e;
        }
    }

    @Transactional
    void saveFlaggedUserMessage(UUID userId, String content, String clientMessageId) {
        messageRepository.save(ChatMessage.user(userId, content, clientMessageId, true));
    }

    // ═══ LLM 호출 + 출력 계약 파싱 ═══

    private record LlmOutcome(String reply, JsonNode diary, SafetyFilter.Category safetyCategory,
                              String model, int promptTokens, int completionTokens) {}

    private LlmOutcome callWithValidation(UUID userId, LlmPurpose purpose, List<Message> messages, int level) {
        LlmClient.LlmResult result = llmClient.complete(purpose, messages, true);
        LlmOutcome outcome = parse(result);

        boolean validationFailed = false;
        if (outcome.safetyCategory == null) {
            var verdict = outputValidator.validate(outcome.reply);
            if (!verdict.valid()) {
                validationFailed = true;
                // 1회 교정 재시도
                List<Message> retryMessages = new ArrayList<>(messages);
                retryMessages.add(Message.system(promptFactory.load("ooc_retry_suffix")));
                LlmClient.LlmResult retry = llmClient.complete(purpose, retryMessages, true);
                LlmOutcome retryOutcome = parse(retry);
                var retryVerdict = outputValidator.validate(retryOutcome.reply);
                logUsage(userId, purpose, retry, level, retryOutcome.safetyCategory, !retryVerdict.valid());
                if (!retryVerdict.valid() && retryOutcome.safetyCategory == null) {
                    throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "OOC validation failed twice");
                }
                outcome = new LlmOutcome(
                        retryVerdict.valid() ? retryVerdict.sanitizedReply() : retryOutcome.reply,
                        retryOutcome.diary, retryOutcome.safetyCategory,
                        retryOutcome.model, retryOutcome.promptTokens, retryOutcome.completionTokens);
            } else {
                outcome = new LlmOutcome(verdict.sanitizedReply(), outcome.diary, null,
                        outcome.model, outcome.promptTokens, outcome.completionTokens);
            }
        }
        logUsage(userId, purpose, result, level, outcome.safetyCategory, validationFailed);
        return outcome;
    }

    /** 출력 계약 { reply, misreadType, diary, safety } 파싱 (코드펜스 등 관용 처리) */
    private LlmOutcome parse(LlmClient.LlmResult result) {
        String raw = result.content().trim();
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "Non-JSON LLM output");
        }
        try {
            JsonNode root = objectMapper.readTree(raw.substring(start, end + 1));
            String reply = root.path("reply").asString("");
            JsonNode diary = root.path("diary");
            SafetyFilter.Category safety = SafetyFilter.parseModelReport(root.path("safety").asString(null));
            return new LlmOutcome(reply.replace("\\n", "\n"),
                    diary.isObject() ? diary : null, safety,
                    result.model(), result.promptTokens(), result.completionTokens());
        } catch (DubbyException e) {
            throw e;
        } catch (Exception e) {
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "LLM output parse failed");
        }
    }

    private void logUsage(UUID userId, LlmPurpose purpose, LlmClient.LlmResult result,
                          int level, SafetyFilter.Category safety, boolean validationFailed) {
        usageLog.log(new LlmUsageLogService.Entry(userId, purpose, result.model(),
                promptFactory.promptVersion(), level, result.promptTokens(), result.completionTokens(),
                result.latencyMs(), result.finishReason(),
                safety != null ? safety.name() : null, validationFailed, false));
    }

    // ═══ 컨텍스트 조립 (derby_llm_pipeline_v1.md §4) ═══

    private List<Message> buildMessages(User user, ZoneId zone, int level, boolean diaryGate, String content) {
        List<Message> messages = new ArrayList<>();

        StringBuilder system = new StringBuilder(promptFactory.load("chat_system"));
        system.append("\n\n").append(promptFactory.load("chat_level_lv" + level));
        if (diaryGate) {
            system.append("\n\n").append(promptFactory.load("chat_diary_block"));
        }
        if (user.getNickname() != null) {
            system.append("\n\n[호칭] 사용자를 \"").append(user.getNickname()).append("님\"이라고 부른다.");
        }
        List<String> facts = diaryService.recentFacts(user.getId(), 2);
        if (!facts.isEmpty()) {
            system.append("\n\n[더비가 이미 아는 사용자님 정보]\n- ").append(String.join("\n- ", facts));
        }
        messages.add(Message.system(system.toString()));

        // 히스토리: 오늘(로컬 자정 이후)의 최근 N개, 오래된 것부터. "더비는 하루 지나면 까먹는다"
        Instant since = localMidnight(zone);
        List<ChatMessage> history = messageRepository.findHistorySince(user.getId(), since,
                PageRequest.of(0, properties.chat().historyMaxMessages()));
        int charBudget = (int) (properties.llm().inputTokenHardCap() / 0.7); // 한국어 1자 ≈ 0.7 token 근사
        List<Message> historyMessages = new ArrayList<>();
        for (ChatMessage m : history) { // 최신순 → 예산 내에서 수집 후 뒤집기
            String text = m.getRole() == ChatMessage.Role.USER
                    ? truncate(m.getContent(), 500) : truncate(m.getContent(), 700);
            if (charBudget - text.length() < 0) break;
            charBudget -= text.length();
            historyMessages.add(m.getRole() == ChatMessage.Role.USER
                    ? Message.user(text) : Message.assistant(text));
        }
        for (int i = historyMessages.size() - 1; i >= 0; i--) {
            messages.add(historyMessages.get(i));
        }

        messages.add(Message.user(content));
        return messages;
    }

    // ═══ 오해 강도 / 정밀 모드 / 일기 게이트 ═══

    private int decideMisreadLevel(UUID userId, String content) {
        if (isPrecise(content)) return 1;
        // 직전 응답이 Lv.2였으면 Lv.1 강제 (연속 사고 → 답답함 방지)
        var lastDerby = messageRepository.findFirstByUserIdAndRoleOrderByIdDesc(userId, ChatMessage.Role.DERBY);
        if (lastDerby.isPresent() && lastDerby.get().getMisreadLevel() != null
                && lastDerby.get().getMisreadLevel() == 2) {
            return 1;
        }
        Map<String, Integer> weights = properties.llm().misreadLevelWeights();
        int lv1 = weights.getOrDefault("lv1", 60);
        int lv2 = weights.getOrDefault("lv2", 40);
        return ThreadLocalRandom.current().nextInt(lv1 + lv2) < lv1 ? 1 : 2;
    }

    private boolean isPrecise(String content) {
        return content.contains("번역") || content.contains("요약") || PRECISE_PATTERN.matcher(content).find();
    }

    private boolean diaryGateAllows(UUID userId, ZoneId zone, String content) {
        if (!DIARY_SELF_PATTERN.matcher(content).find()) return false;
        if (safetyFilter.isDiarySensitive(content)) return false;
        // 일일 상한
        if (diaryService.countCandidatesSince(userId, localMidnight(zone))
                >= properties.diary().dailyCandidateMax()) return false;
        // 빈도: 마지막 후보 생성 이후 사용자 메시지 minGap개 경과
        var lastSourceId = diaryService.lastCandidateSourceMessageId(userId);
        if (lastSourceId.isPresent() && lastSourceId.get() != null) {
            long userMsgsAfter = messageRepository
                    .findHistorySince(userId, Instant.EPOCH, PageRequest.of(0, 50)).stream()
                    .filter(m -> m.getRole() == ChatMessage.Role.USER && m.getId() > lastSourceId.get())
                    .count();
            return userMsgsAfter >= properties.diary().candidateMinGapMessages();
        }
        return true;
    }

    // ═══ 조회/저장/멱등 재생 ═══

    private ChatSendResponse replay(UUID userId, ChatMessage userMsg, LocalDate localDate, int limit) {
        int used = quotaService.used(userId, localDate);
        ChatQuotaDto quota = new ChatQuotaDto(null, limit, used, Math.max(0, limit - used), null, null);
        var derbyMsg = messageRepository.findFirstByUserIdAndRoleAndIdGreaterThanOrderByIdAsc(
                userId, ChatMessage.Role.DERBY, userMsg.getId());
        return derbyMsg
                .map(d -> ChatSendResponse.derby(ChatMessageDto.from(userMsg), ChatMessageDto.from(d), null, quota))
                .orElseThrow(() -> new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "No stored reply for replay"));
    }

    @Transactional(readOnly = true)
    public ChatQuotaDto quota(UUID userId) {
        User user = userService.getActiveUser(userId);
        ZoneId zone = ZoneId.of(user.getTimezone());
        LocalDate localDate = LocalDate.now(zone);
        BillingService.Tier tier = billingService.resolveTier(userId);
        int limit = billingService.chatDailyLimit(tier);
        int used = quotaService.used(userId, localDate);
        return new ChatQuotaDto(tier.name(), limit, used, Math.max(0, limit - used),
                nextMidnight(zone), DerbyCopy.pick(ErrorCode.CHAT_LIMIT_EXCEEDED));
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<ChatMessageDto> messages(UUID userId, Long cursor, int size) {
        List<ChatMessage> page = messageRepository.findPage(userId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = page.size() > size;
        List<ChatMessage> items = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;
        return new CursorPageResponse<>(items.stream().map(ChatMessageDto::from).toList(), nextCursor, hasNext);
    }

    @Transactional
    public Map<String, Object> saveMessage(UUID userId, Long messageId) {
        ChatMessage message = messageRepository.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        message.markSaved();
        events.record(userId, "CHAT_SAVED", null, messageId);
        return Map.of("messageId", messageId, "saved", true,
                "derbyMessage", "저장했습니다. 이 발언은 이제 공식 기록입니다.");
    }

    // ═══ 유틸 ═══

    private static Instant localMidnight(ZoneId zone) {
        return LocalDate.now(zone).atStartOfDay(zone).toInstant();
    }

    private static String nextMidnight(ZoneId zone) {
        return ZonedDateTime.of(LocalDate.now(zone).plusDays(1).atStartOfDay(), zone)
                .toOffsetDateTime().toString();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
