package com.chess.service;

import com.chess.dto.request.UserRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.entity.User;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.UserRepository;
import com.chess.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User getUser(UserPrincipal principal) {
        return getUser(principal.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UserPrincipal principal) {
        return UserResponse.from(getUser(principal));
    }

    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfile(UUID userId) {
        return PublicUserResponse.from(getUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = getUser(principal);
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        return UserResponse.from(userRepository.save(user));
    }
}
