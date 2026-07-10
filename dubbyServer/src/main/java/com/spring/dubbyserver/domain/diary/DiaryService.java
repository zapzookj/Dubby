package com.spring.dubbyserver.domain.diary;

import com.spring.dubbyserver.domain.diary.dto.DiaryEntryDto;
import com.spring.dubbyserver.domain.metrics.AppEventRecorder;
import com.spring.dubbyserver.domain.template.DerbyDefaults;
import com.spring.dubbyserver.global.common.CursorPageResponse;
import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryCandidateRepository candidateRepository;
    private final DiaryEntryRepository entryRepository;
    private final DubbyProperties properties;
    private final AppEventRecorder events;
    private final DerbyDefaults derbyDefaults;

    // ── 후보 (ChatService에서 사용) ──

    /** 채팅 합승으로 생성된 일기 후보 저장 */
    @Transactional
    public DiaryCandidate createCandidate(UUID userId, Long sourceMessageId,
                                          String fact, String interpretation, String conclusion) {
        Instant expiresAt = Instant.now().plus(properties.diary().candidateTtl());
        return candidateRepository.save(
                new DiaryCandidate(userId, sourceMessageId, fact, interpretation, conclusion, expiresAt));
    }

    @Transactional(readOnly = true)
    public long countCandidatesSince(UUID userId, Instant since) {
        return candidateRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, since);
    }

    @Transactional(readOnly = true)
    public Optional<Long> lastCandidateSourceMessageId(UUID userId) {
        return candidateRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(DiaryCandidate::getSourceMessageId);
    }

    // ── 승인/거절 ──

    @Transactional
    public DiaryEntryDto approve(UUID userId, Long candidateId, boolean autoSaved) {
        DiaryCandidate candidate = candidateRepository.findByIdAndUserId(candidateId, userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.DIARY_CANDIDATE_NOT_FOUND));
        if (!candidate.isPendingAndAlive()) {
            throw new DubbyException(ErrorCode.DIARY_CANDIDATE_NOT_FOUND, "Candidate expired or handled");
        }
        // 슬롯 검사 (P4 전까지 FREE 한도)
        long count = entryRepository.countByUserId(userId);
        if (count >= properties.diary().slotLimit().free()) {
            throw new DubbyException(ErrorCode.DIARY_SLOT_FULL);
        }
        candidate.approve();
        DiaryEntry entry = entryRepository.save(DiaryEntry.from(candidate, autoSaved));
        events.record(userId, "DIARY_SAVED", "DIARY", entry.getId());
        return DiaryEntryDto.from(entry);
    }

    @Transactional
    public void reject(UUID userId, Long candidateId) {
        DiaryCandidate candidate = candidateRepository.findByIdAndUserId(candidateId, userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.DIARY_CANDIDATE_NOT_FOUND));
        candidate.reject();
    }

    // ── 일기 CRUD ──

    @Transactional(readOnly = true)
    public CursorPageResponse<DiaryEntryDto> list(UUID userId, Long cursor, int size) {
        List<DiaryEntry> page = entryRepository.findPage(userId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = page.size() > size;
        List<DiaryEntry> items = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;
        return new CursorPageResponse<>(items.stream().map(DiaryEntryDto::from).toList(), nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public DiaryEntryDto get(UUID userId, Long entryId) {
        return DiaryEntryDto.from(findOwned(userId, entryId));
    }

    /** 즉시 실삭제 — "삭제 요청 시 확실히 삭제" (페르소나 §9.5) */
    @Transactional
    public Map<String, Object> delete(UUID userId, Long entryId) {
        DiaryEntry entry = findOwned(userId, entryId);
        entryRepository.delete(entry);
        return Map.of("derbyMessage",
                "삭제했습니다. 기억은 사라졌지만, 더비의 허전함은 남았습니다. 물론 더비는 이것도 곧 까먹을겁니다.");
    }

    @Transactional
    public Map<String, Object> deleteAll(UUID userId) {
        int deleted = entryRepository.deleteAllByUser(userId);
        return Map.of(
                "deletedCount", deleted,
                "derbyMessage", "더비의 일기장이 비워졌습니다. 더비가 사용자님을 처음 만난 척하기 시작합니다.");
    }

    @Transactional
    public Map<String, Object> share(UUID userId, Long entryId) {
        DiaryEntry entry = findOwned(userId, entryId);
        entry.markShared();
        events.record(userId, "DIARY_SHARED", "DIARY", entry.getId());
        String shareText = "더비의 일기장\n" + entry.getFact() + "\n" + entry.getInterpretation()
                + "\n" + entry.getConclusion() + "\n" + derbyDefaults.shareSuffix();
        return Map.of("shareText", shareText);
    }

    /** 프리미엄 일기 재생성 — P4 전까지 FREE라 403. 게이트만 여기서, LLM 호출은 P4에서 연결 */
    @Transactional
    public DiaryEntryDto rewrite(UUID userId, Long entryId) {
        // TODO(P4): BillingService.resolveTier == SALARY 검사 후 DIARY purpose LLM 호출
        throw new DubbyException(ErrorCode.DIARY_PREMIUM_ONLY);
    }

    /** 홈/기타에서 쓰는 요약 — 최근 승인 일기 fact N개 (더비의 기억 블록) */
    @Transactional(readOnly = true)
    public List<String> recentFacts(UUID userId, int count) {
        return entryRepository.findRecent(userId, PageRequest.of(0, count))
                .stream().map(DiaryEntry::getFact).toList();
    }

    /** 일일 배치: 만료 후보 정리 */
    @Transactional
    public int cleanupExpiredCandidates() {
        return candidateRepository.deleteExpired(Instant.now());
    }

    private DiaryEntry findOwned(UUID userId, Long entryId) {
        return entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.DIARY_ENTRY_NOT_FOUND));
    }
}
