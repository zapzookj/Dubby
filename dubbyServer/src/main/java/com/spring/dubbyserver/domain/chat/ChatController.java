package com.spring.dubbyserver.domain.chat;

import com.spring.dubbyserver.domain.chat.dto.ChatMessageDto;
import com.spring.dubbyserver.domain.chat.dto.ChatQuotaDto;
import com.spring.dubbyserver.domain.chat.dto.ChatSendResponse;
import com.spring.dubbyserver.global.common.CursorPageResponse;
import com.spring.dubbyserver.global.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    public record SendRequest(@NotBlank String clientMessageId, @NotBlank String content) {}

    @PostMapping("/messages")
    public ChatSendResponse send(@AuthenticationPrincipal AuthUser authUser,
                                 @Valid @RequestBody SendRequest request) {
        return chatService.send(authUser.id(), request.clientMessageId(), request.content());
    }

    @GetMapping("/messages")
    public CursorPageResponse<ChatMessageDto> messages(@AuthenticationPrincipal AuthUser authUser,
                                                       @RequestParam(required = false) Long cursor,
                                                       @RequestParam(defaultValue = "30") int size) {
        return chatService.messages(authUser.id(), cursor, Math.min(size, 50));
    }

    @GetMapping("/quota")
    public ChatQuotaDto quota(@AuthenticationPrincipal AuthUser authUser) {
        return chatService.quota(authUser.id());
    }

    @PostMapping("/messages/{messageId}/save")
    public Map<String, Object> save(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long messageId) {
        return chatService.saveMessage(authUser.id(), messageId);
    }
}
