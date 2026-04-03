package com.chess.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

public class UserRequests {

    @Data
    public static class UpdateProfileRequest {
        private String avatarUrl;
        private Boolean emailNotificationsEnabled;
    }

    @Data
    public static class FriendRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;
    }
}
