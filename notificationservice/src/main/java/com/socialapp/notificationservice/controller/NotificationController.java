package com.socialapp.notificationservice.controller;

import com.socialapp.notificationservice.dto.PagedNotificationResponse;
import com.socialapp.notificationservice.dto.RegisterPushTokenRequest;
import com.socialapp.notificationservice.dto.UnregisterPushTokenRequest;
import com.socialapp.notificationservice.dto.UpdateNotificationSettingsRequest;
import com.socialapp.notificationservice.entity.Notification;
import com.socialapp.notificationservice.entity.NotificationSettings;
import com.socialapp.notificationservice.service.NotificationService;
import com.socialapp.notificationservice.util.SecurityUtil;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ==========================================
    // Notification Management APIs
    // ==========================================

    @GetMapping
    public ResponseEntity<PagedNotificationResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));

        Page<Notification> notificationsPage = notificationService.getNotifications(userId, page, size);

        PagedNotificationResponse response = PagedNotificationResponse.builder()
                .notifications(notificationsPage.getContent())
                .currentPage(notificationsPage.getNumber())
                .totalPages(notificationsPage.getTotalPages())
                .totalElements(notificationsPage.getTotalElements())
                .pageSize(notificationsPage.getSize())
                .hasNext(notificationsPage.hasNext())
                .hasPrevious(notificationsPage.hasPrevious())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<String> markAllAsRead() {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("Mark all as read successfully");
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable String id) {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok("Mark as read successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable String id) {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok("Delete notification successfully");
    }

    @GetMapping("/unread")
    public ResponseEntity<Long> getUnreadCount() {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    // ==========================================
    // Push Token Management APIs
    // ==========================================

    @PostMapping("/push-token/register")
    public ResponseEntity<String> registerPushToken(@RequestBody @Valid RegisterPushTokenRequest request) {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        notificationService.registerPushToken(request, userId);
        return ResponseEntity.ok("Register push token successfully");
    }

    @PostMapping("/push-token/unregister")
    public ResponseEntity<String> unregisterPushToken(@RequestBody @Valid UnregisterPushTokenRequest request) {
        notificationService.unregisterPushToken(request);
        return ResponseEntity.ok("Unregister push token successfully");
    }

    // ==========================================
    // Settings Management APIs
    // ==========================================

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettings> getSettings() {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        return ResponseEntity.ok(notificationService.getSettings(userId));
    }

    @PutMapping("/settings")
    public ResponseEntity<String> updateSettings(@RequestBody UpdateNotificationSettingsRequest request) {
        String userId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập"));
        notificationService.updateSettings(request, userId);
        return ResponseEntity.ok("Update settings successfully");
    }
}
