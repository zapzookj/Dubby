package com.spring.dubbyserver.domain.task;

import com.spring.dubbyserver.domain.task.dto.SavedTaskItem;
import com.spring.dubbyserver.domain.task.dto.TasksTodayResponse;
import com.spring.dubbyserver.global.common.CursorPageResponse;
import com.spring.dubbyserver.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/today")
    public TasksTodayResponse today(@AuthenticationPrincipal AuthUser authUser) {
        return taskService.getToday(authUser.id());
    }

    @PostMapping("/{assignmentId}/reaction")
    public Map<String, Object> react(@AuthenticationPrincipal AuthUser authUser,
                                     @PathVariable Long assignmentId,
                                     @RequestBody Map<String, String> body) {
        return taskService.react(authUser.id(), assignmentId, body.get("reaction"));
    }

    @PostMapping("/{assignmentId}/save")
    public Map<String, Object> save(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long assignmentId,
                                    @RequestBody(required = false) Map<String, Boolean> body) {
        boolean saved = body == null || body.getOrDefault("saved", true);
        return taskService.save(authUser.id(), assignmentId, saved);
    }

    @PostMapping("/{assignmentId}/share")
    public Map<String, Object> share(@AuthenticationPrincipal AuthUser authUser,
                                     @PathVariable Long assignmentId) {
        return taskService.share(authUser.id(), assignmentId);
    }

    @GetMapping("/saved")
    public CursorPageResponse<SavedTaskItem> saved(@AuthenticationPrincipal AuthUser authUser,
                                                   @RequestParam(required = false) Long cursor,
                                                   @RequestParam(defaultValue = "20") int size) {
        return taskService.savedList(authUser.id(), cursor, Math.min(size, 50));
    }
}
