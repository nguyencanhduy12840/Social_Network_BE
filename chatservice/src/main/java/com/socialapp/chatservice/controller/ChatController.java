package com.socialapp.chatservice.controller;

import com.socialapp.chatservice.dto.request.CreateChatRequest;
import com.socialapp.chatservice.dto.request.MarkAsReadRequest;
import com.socialapp.chatservice.dto.request.SendMessageRequest;
import com.socialapp.chatservice.dto.response.ChatResponse;
import com.socialapp.chatservice.dto.response.MessageResponse;
import com.socialapp.chatservice.service.ChatService;
import com.socialapp.chatservice.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 1. Tạo hoặc lấy conversation với một người
     * POST /api/v1/chat
     */
    @PostMapping
    public ResponseEntity<ChatResponse> createOrGetChat(
            @Valid @RequestBody CreateChatRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        ChatResponse chat = chatService.createOrGetChat(currentUserId, request);
        return ResponseEntity.ok(chat);
    }

    /**
     * 2. Lấy danh sách conversation của user
     * GET /api/v1/chat?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<ChatResponse>> getChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        Page<ChatResponse> chats = chatService.getChats(currentUserId, page, size);
        return ResponseEntity.ok(chats);
    }

    /**
     * 3. Lấy chi tiết một conversation
     * GET /api/v1/chat/{chatId}
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(@PathVariable String chatId) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        ChatResponse chat = chatService.getChatById(currentUserId, chatId);
        return ResponseEntity.ok(chat);
    }

    /**
     * 4. Gửi tin nhắn
     * POST /api/v1/chat/messages
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        MessageResponse message = chatService.sendMessage(currentUserId, request);
        return ResponseEntity.ok(message);
    }

    /**
     * 5. Lấy danh sách tin nhắn trong một conversation
     * GET /api/v1/chat/{chatId}/messages?page=0&size=50
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        Page<MessageResponse> messages = chatService.getMessages(currentUserId, chatId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * 6. Đánh dấu tin nhắn đã đọc
     * PUT /api/v1/chat/messages/read
     */
    @PutMapping("/messages/read")
    public ResponseEntity<String> markAsRead(
            @Valid @RequestBody MarkAsReadRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        chatService.markAsRead(currentUserId, request);
        return ResponseEntity.ok("Đánh dấu đã đọc thành công");
    }

    /**
     * 7. Xóa tin nhắn
     * DELETE /api/v1/chat/messages/{messageId}
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable String messageId) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        chatService.deleteMessage(currentUserId, messageId);
        return ResponseEntity.ok("Xóa tin nhắn thành công");
    }
}
