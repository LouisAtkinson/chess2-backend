package com.chess.controller;

import com.chess.dto.request.UserRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.security.UserPrincipal;
import com.chess.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicUserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getPublicProfile(id));
    }
}
