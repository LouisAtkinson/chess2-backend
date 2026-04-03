package com.chess.controller;

import com.chess.dto.request.UserRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.security.UserPrincipal;
import com.chess.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(friendService.getFriends(principal));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendResponse>> getPendingRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(friendService.getPendingRequests(principal));
    }

    @PostMapping("/request")
    public ResponseEntity<MessageResponse> sendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FriendRequest request) {
        return ResponseEntity.ok(friendService.sendRequest(principal, request.getUserId()));
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<MessageResponse> accept(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(friendService.respondToRequest(principal, requestId, true));
    }

    @PostMapping("/{requestId}/decline")
    public ResponseEntity<MessageResponse> decline(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(friendService.respondToRequest(principal, requestId, false));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> removeFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(friendService.removeFriend(principal, userId));
    }
}
