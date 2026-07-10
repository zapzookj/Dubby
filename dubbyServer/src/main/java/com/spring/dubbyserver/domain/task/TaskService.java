package com.spring.dubbyserver.domain.task;

import com.spring.dubbyserver.domain.metrics.AppEventRecorder;
import com.spring.dubbyserver.domain.task.DailyTaskAssignment.Reaction;
import com.spring.dubbyserver.domain.task.dto.SavedTaskItem;
import com.spring.dubbyserver.domain.task.dto.TaskItem;
import com.spring.dubbyserver.domain.task.dto.TasksTodayResponse;
import com.spring.dubbyserver.domain.template.DerbyDefaults;
import com.spring.dubbyserver.domain.template.Template;
import com.spring.dubbyserver.domain.template.TemplatePicker;
import com.spring.dubbyserver.domain.template.TemplateRepository;
import com.spring.dubbyserver.domain.user.User;
import com.spring.dubbyserver.domain.user.UserService;
import com.spring.dubbyserver.global.common.CursorPageResponse;
import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final DailyTaskAssignmentRepository assignmentRepository;
    private final TemplateRepository templateRepository;
    private final TemplatePicker templatePicker;
    private final DerbyDefaults derbyDefaults;
    private final UserService userService;
    private final DubbyProperties properties;
    private final AppEventRecorder events;

    /** 오늘 배정 조회 — 없으면 lazy 배정 (스펙 §9) */
    @Transactional
    public TasksTodayResponse getToday(UUID userId) {
        User user = userService.getActiveUser(userId);
        LocalDate localDate = LocalDate.now(ZoneId.of(user.getTimezone()));

        List<DailyTaskAssignment> assignments =
                assignmentRepository.findByUserIdAndAssignedDateOrderBySlotAsc(userId, localDate);

        if (assignments.isEmpty()) {
            assignments = assign(user, localDate);
        }

        return new TasksTodayResponse(localDate.toString(),
                assignments.stream().map(this::toItem).toList());
    }

    private List<DailyTaskAssignment> assign(User user, LocalDate localDate) {
        int count = properties.task().dailyCount();
        // TODO(P4): BillingService.resolveTier로 프리미엄 판정. P1은 무료만
        List<TemplatePicker.Candidate> picked =
                templatePicker.pickDailyTasks(user.getId(), localDate, false, user.getLocale(), count);

        try {
            int slot = 1;
            for (TemplatePicker.Candidate c : picked) {
                Template template = templateRepository.getReferenceById(c.templateId());
                DailyTaskAssignment assignment =
                        new DailyTaskAssignment(user.getId(), template, localDate, slot++);
                assignmentRepository.save(assignment);
            }
            assignmentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경쟁 (더블 탭): 먼저 배정한 쪽 결과를 사용
        }

        List<DailyTaskAssignment> saved =
                assignmentRepository.findByUserIdAndAssignedDateOrderBySlotAsc(user.getId(), localDate);
        for (DailyTaskAssignment a : saved) {
            events.record(user.getId(), "TASK_ASSIGNED", "TEMPLATE", a.getTemplate().getId());
        }
        return saved;
    }

    /** 반응 — 덮어쓰기 허용, RETRY만 retry-max 제한 (스펙 §5.3) */
    @Transactional
    public Map<String, Object> react(UUID userId, Long assignmentId, String reactionRaw) {
        DailyTaskAssignment assignment = findOwned(userId, assignmentId);

        Reaction reaction;
        try {
            reaction = Reaction.valueOf(reactionRaw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DubbyException(ErrorCode.TASK_INVALID_REACTION);
        }

        if (reaction == Reaction.RETRY && assignment.getRetryCount() >= properties.task().retryMax()) {
            throw new DubbyException(ErrorCode.TASK_RETRY_EXHAUSTED, derbyDefaults.retryExhausted());
        }

        assignment.react(reaction);
        events.record(userId, "TASK_REACTION", "TEMPLATE", assignment.getTemplate().getId(),
                Map.of("type", reaction.name(), "templateCode", assignment.getTemplate().getCode()));

        return Map.of(
                "assignmentId", assignment.getId(),
                "reaction", reaction.name(),
                "followUpMessage", followUpMessage(assignment.getTemplate(), reaction));
    }

    /** 후속 문구: 템플릿 content.buttons[key].responses 랜덤 → 없으면 기본 풀 */
    private String followUpMessage(Template template, Reaction reaction) {
        List<String> responses = template.buttonResponses(reaction.name());
        if (responses != null && !responses.isEmpty()) {
            return responses.get(ThreadLocalRandom.current().nextInt(responses.size()));
        }
        return derbyDefaults.defaultResponse(reaction.name());
    }

    @Transactional
    public Map<String, Object> save(UUID userId, Long assignmentId, boolean saved) {
        DailyTaskAssignment assignment = findOwned(userId, assignmentId);
        assignment.setSaved(saved);
        if (saved) {
            events.record(userId, "TASK_SAVED", "TEMPLATE", assignment.getTemplate().getId());
        }
        return Map.of(
                "assignmentId", assignment.getId(),
                "saved", saved,
                "derbyMessage", saved ? derbyDefaults.saveConfirm() : derbyDefaults.unsaveConfirm());
    }

    @Transactional
    public Map<String, Object> share(UUID userId, Long assignmentId) {
        DailyTaskAssignment assignment = findOwned(userId, assignmentId);
        assignment.markShared();
        events.record(userId, "TASK_SHARED", "TEMPLATE", assignment.getTemplate().getId());

        Template t = assignment.getTemplate();
        String shareText = t.contentString("shareText");
        if (shareText == null) {
            shareText = t.contentString("title") + "\n결론: " + t.contentString("conclusion");
        }
        return Map.of("shareText", shareText + "\n" + derbyDefaults.shareSuffix());
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<SavedTaskItem> savedList(UUID userId, Long cursor, int size) {
        List<DailyTaskAssignment> page =
                assignmentRepository.findSavedPage(userId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = page.size() > size;
        List<DailyTaskAssignment> items = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;
        return new CursorPageResponse<>(
                items.stream().map(a -> new SavedTaskItem(
                        a.getId(),
                        a.getTemplate().getCode(),
                        a.getTemplate().contentString("title"),
                        a.getTemplate().contentString("conclusion"),
                        a.getTemplate().contentString("note"),
                        a.getAssignedDate().toString())).toList(),
                nextCursor, hasNext);
    }

    private DailyTaskAssignment findOwned(UUID userId, Long assignmentId) {
        return assignmentRepository.findByIdAndUserId(assignmentId, userId)
                .orElseThrow(() -> new DubbyException(ErrorCode.TASK_ASSIGNMENT_NOT_FOUND));
    }

    private TaskItem toItem(DailyTaskAssignment a) {
        Template t = a.getTemplate();
        List<TaskItem.Button> buttons = derbyDefaults.buttonLabels().entrySet().stream()
                .map(e -> new TaskItem.Button(e.getKey(), e.getValue()))
                .toList();
        return new TaskItem(
                a.getId(), t.getCode(),
                t.contentString("title"), t.contentString("conclusion"), t.contentString("note"),
                buttons,
                a.getReaction() != null ? a.getReaction().name() : null,
                a.getRetryCount(), a.isSaved());
    }
}
