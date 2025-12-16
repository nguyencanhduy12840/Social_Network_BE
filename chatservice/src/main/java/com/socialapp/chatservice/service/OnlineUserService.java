package com.socialapp.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OnlineUserService {

    // Set lưu trữ các userId đang online
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public void userConnected(String userId) {
        onlineUsers.add(userId);
        log.info("User connected: {}", userId);
    }

    public void userDisconnected(String userId) {
        onlineUsers.remove(userId);
        log.info("User disconnected: {}", userId);
    }

    public boolean isUserOnline(String userId) {
        return onlineUsers.contains(userId);
    }
}
