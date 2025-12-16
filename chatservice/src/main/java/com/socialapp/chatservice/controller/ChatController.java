package com.socialapp.chatservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.chatservice.dto.request.CreateChatRequest;
import com.socialapp.chatservice.dto.request.SendMessageRequest;
import com.socialapp.chatservice.dto.response.ChatResponse;
import com.socialapp.chatservice.dto.response.MessageResponse;
import com.socialapp.chatservice.service.ChatService;
import com.socialapp.chatservice.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createOrGetChat(
            @Valid @RequestBody CreateChatRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        ChatResponse chat = chatService.createOrGetChat(currentUserId, request);
        return ResponseEntity.ok(chat);
    }

    @GetMapping
    public ResponseEntity<Page<ChatResponse>> getChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        Page<ChatResponse> chats = chatService.getChats(currentUserId, page, size);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(@PathVariable String chatId) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        ChatResponse chat = chatService.getChatById(currentUserId, chatId);
        return ResponseEntity.ok(chat);
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<String> deleteChat(@PathVariable String chatId) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        chatService.deleteChat(currentUserId, chatId);
        return ResponseEntity.ok("Xóa đoạn chat thành công");
    }

    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestPart("message") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile file) throws JsonProcessingException {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        ObjectMapper mapper = new ObjectMapper();
        SendMessageRequest request = mapper.readValue(requestJson, SendMessageRequest.class);

        MessageResponse message = chatService.sendMessage(currentUserId, request, file);
        return ResponseEntity.ok(message);
    }

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

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable String messageId) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        chatService.deleteMessage(currentUserId, messageId);
        return ResponseEntity.ok("Xóa tin nhắn thành công");
    }

    @PutMapping("/{chatId}")
    public ResponseEntity<String> markAsRead(@PathVariable String chatId) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        chatService.markAsRead(currentUserId, chatId);
        return ResponseEntity.ok("Đánh dấu đã đọc thành công");
    }

    // @GetMapping("/unread-count")
    // public ResponseEntity<Long> getUnreadCount() {
    //     String currentUserId = SecurityUtil.getCurrentUserLogin()
    //             .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

    //     return ResponseEntity.ok(chatService.getGlobalUnreadCount(currentUserId));
    // }

    // @GetMapping("/users/{userId}/online")
    // public ResponseEntity<Boolean> isUserOnline(@PathVariable String userId) {
    // return ResponseEntity.ok(chatService.isUserOnline(userId));
    // }
}
