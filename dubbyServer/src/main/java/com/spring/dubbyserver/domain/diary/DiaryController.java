package com.spring.dubbyserver.domain.diary;

import com.spring.dubbyserver.domain.diary.dto.DiaryEntryDto;
import com.spring.dubbyserver.global.common.CursorPageResponse;
import com.spring.dubbyserver.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping("/candidates/{candidateId}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public DiaryEntryDto approve(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long candidateId) {
        return diaryService.approve(authUser.id(), candidateId, false);
    }

    @PostMapping("/candidates/{candidateId}/reject")
    public Map<String, Object> reject(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long candidateId) {
        diaryService.reject(authUser.id(), candidateId);
        return Map.of("rejected", true, "derbyMessage", "찢었습니다. 더비는 방금 그 기억을 못 본 척하기로 했습니다.");
    }

    @GetMapping("/entries")
    public CursorPageResponse<DiaryEntryDto> list(@AuthenticationPrincipal AuthUser authUser,
                                                  @RequestParam(required = false) Long cursor,
                                                  @RequestParam(defaultValue = "20") int size) {
        return diaryService.list(authUser.id(), cursor, Math.min(size, 50));
    }

    @GetMapping("/entries/{entryId}")
    public DiaryEntryDto get(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long entryId) {
        return diaryService.get(authUser.id(), entryId);
    }

    @DeleteMapping("/entries/{entryId}")
    public Map<String, Object> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long entryId) {
        return diaryService.delete(authUser.id(), entryId);
    }

    @DeleteMapping("/entries")
    public Map<String, Object> deleteAll(@AuthenticationPrincipal AuthUser authUser) {
        return diaryService.deleteAll(authUser.id());
    }

    @PostMapping("/entries/{entryId}/share")
    public Map<String, Object> share(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long entryId) {
        return diaryService.share(authUser.id(), entryId);
    }

    @PostMapping("/entries/{entryId}/rewrite")
    public DiaryEntryDto rewrite(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long entryId) {
        return diaryService.rewrite(authUser.id(), entryId);
    }
}
