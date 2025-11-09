package com.socialapp.profileservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialapp.profileservice.dto.request.FriendActionRequest;
import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.service.FriendshipService;

@RestController
@RequestMapping("/friendships")
public class FriendshipController {

    private final FriendshipService friendService;
    public FriendshipController(FriendshipService friendService) {
        this.friendService = friendService;
    }
    @GetMapping("/{userId}")
    public List<UserProfile> getFriends(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return friendService.getFriends(userId, page, size);
    }

    @PostMapping("/request")
    public ResponseEntity<String> sendRequest(@RequestBody FriendActionRequest request) {
        String response = friendService.sendFriendRequest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accept")
    public ResponseEntity<String> acceptRequest(@RequestBody FriendActionRequest request) {
        String response = friendService.acceptFriendRequest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject")
    public ResponseEntity<String> rejectRequest(@RequestBody FriendActionRequest request) {
        String response = friendService.rejectFriendRequest(request);
        return ResponseEntity.ok(response);
    }
}
