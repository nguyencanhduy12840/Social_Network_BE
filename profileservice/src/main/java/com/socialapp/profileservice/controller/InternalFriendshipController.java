package com.socialapp.profileservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.service.FriendshipService;

@RestController
public class InternalFriendshipController {
    private final FriendshipService friendshipService;
    public InternalFriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping("/internal/friendships/{userId}")
    public List<UserProfile> getFriends(@PathVariable String userId) {
        List<UserProfile> friends = friendshipService.getFriends(userId, 0, 20);
        return friends;
    }

    @GetMapping("/internal/friendships/isFriend/{userId}/{friendId}")
    public Boolean isFriend(@PathVariable String userId, @PathVariable String friendId) {
        return friendshipService.isFriend(userId, friendId);
    }
}
