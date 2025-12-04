package com.socialapp.notificationservice.controller;

import com.socialapp.notificationservice.dto.RegisterPushTokenRequest;
import com.socialapp.notificationservice.dto.UnregisterPushTokenRequest;
import com.socialapp.notificationservice.dto.UpdateNotificationSettingsRequest;
import com.socialapp.notificationservice.entity.NotificationSettings;
import com.socialapp.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/push-token/register")
    public ResponseEntity<String> registerPushToken(@RequestBody @Valid RegisterPushTokenRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.registerPushToken(request, userId);
        return ResponseEntity.ok("Push token registered successfully");
    }

    @PostMapping("/push-token/unregister")
    public ResponseEntity<String> unregisterPushToken(@RequestBody @Valid UnregisterPushTokenRequest request) {
        notificationService.unregisterPushToken(request);
        return ResponseEntity.ok("Push token unregistered successfully");
    }

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettings> getSettings(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(notificationService.getSettings(userId));
    }

    @PutMapping("/settings")
    public ResponseEntity<String> updateSettings(@RequestBody UpdateNotificationSettingsRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.updateSettings(request, userId);
        return ResponseEntity.ok("Settings updated successfully");
    }
}
