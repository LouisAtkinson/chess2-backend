package com.chess.dto.response;

import com.chess.entity.*;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Responses {

    @Data @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UserResponse user;
    }

    @Data @Builder
    public static class UserResponse {
        private UUID id;
        private String username;
        private String email;
        private String avatarUrl;
        private boolean emailNotificationsEnabled;
        private OffsetDateTime createdAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .emailNotificationsEnabled(user.isEmailNotificationsEnabled())
                .createdAt(user.getCreatedAt())
                .build();
        }
    }

    @Data @Builder
    public static class PublicUserResponse {
        private UUID id;
        private String username;
        private String avatarUrl;

        public static PublicUserResponse from(User user) {
            if (user == null) return null;
            return PublicUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .build();
        }
    }

    @Data @Builder
    public static class FriendResponse {
        private UUID id;
        private PublicUserResponse user;
        private Friend.Status status;
        private OffsetDateTime createdAt;

        public static FriendResponse from(Friend friend, UUID currentUserId) {
            // Show the OTHER person in the friendship
            User other = friend.getRequester().getId().equals(currentUserId)
                ? friend.getAddressee()
                : friend.getRequester();
            return FriendResponse.builder()
                .id(friend.getId())
                .user(PublicUserResponse.from(other))
                .status(friend.getStatus())
                .createdAt(friend.getCreatedAt())
                .build();
        }
    }

    @Data @Builder
    public static class PieceResponse {
        private UUID id;
        private String name;
        private String svgKey;
        private List<Map<String, Object>> movementRules;
        private List<Map<String, Object>> captureRules;
        private PublicUserResponse owner;
        private boolean isPublic;
        private boolean isStandard;
        private OffsetDateTime createdAt;

        public static PieceResponse from(Piece piece) {
            return PieceResponse.builder()
                .id(piece.getId())
                .name(piece.getName())
                .svgKey(piece.getSvgKey())
                .movementRules(piece.getMovementRules())
                .captureRules(piece.getCaptureRules())
                .owner(PublicUserResponse.from(piece.getOwner()))
                .isPublic(piece.isPublic())
                .isStandard(piece.isStandard())
                .createdAt(piece.getCreatedAt())
                .build();
        }
    }

    @Data @Builder
    public static class GameResponse {
        private UUID id;
        private PublicUserResponse whitePlayer;
        private PublicUserResponse blackPlayer;
        private Game.Status status;
        private Game.Result result;
        private Game.ResultReason resultReason;
        private Game.GameType gameType;
        private Game.GameMode mode;
        private Map<String, Object> boardState;
        private String turn;
        private Map<String, Object> variantConfig;
        private List<Map<String, Object>> moveHistory;
        private int halfMoveClock;
        private int fullMoveNumber;
        private Long whiteTimeRemainingMs;
        private Long blackTimeRemainingMs;
        private OffsetDateTime lastMoveAt;
        private OffsetDateTime createdAt;

        public static GameResponse from(Game game) {
            return GameResponse.builder()
                .id(game.getId())
                .whitePlayer(PublicUserResponse.from(game.getWhitePlayer()))
                .blackPlayer(PublicUserResponse.from(game.getBlackPlayer()))
                .status(game.getStatus())
                .result(game.getResult())
                .resultReason(game.getResultReason())
                .gameType(game.getGameType())
                .mode(game.getMode())
                .boardState(game.getBoardState())
                .turn(game.getTurn())
                .variantConfig(game.getVariantConfig())
                .moveHistory(game.getMoveHistory())
                .halfMoveClock(game.getHalfMoveClock())
                .fullMoveNumber(game.getFullMoveNumber())
                .whiteTimeRemainingMs(game.getWhiteTimeRemainingMs())
                .blackTimeRemainingMs(game.getBlackTimeRemainingMs())
                .lastMoveAt(game.getLastMoveAt())
                .createdAt(game.getCreatedAt())
                .build();
        }
    }

    @Data @Builder
    public static class GameSummaryResponse {
        private UUID id;
        private PublicUserResponse whitePlayer;
        private PublicUserResponse blackPlayer;
        private Game.Status status;
        private Game.Result result;
        private Game.GameType gameType;
        private Game.GameMode mode;
        private OffsetDateTime createdAt;

        public static GameSummaryResponse from(Game game) {
            return GameSummaryResponse.builder()
                .id(game.getId())
                .whitePlayer(PublicUserResponse.from(game.getWhitePlayer()))
                .blackPlayer(PublicUserResponse.from(game.getBlackPlayer()))
                .status(game.getStatus())
                .result(game.getResult())
                .gameType(game.getGameType())
                .mode(game.getMode())
                .createdAt(game.getCreatedAt())
                .build();
        }
    }

    @Data @Builder
    public static class ChatMessageResponse {
        private UUID id;
        private UUID gameId;
        private PublicUserResponse sender;
        private String message;
        private OffsetDateTime createdAt;

        public static ChatMessageResponse from(GameChatMessage msg) {
            return ChatMessageResponse.builder()
                .id(msg.getId())
                .gameId(msg.getGame().getId())
                .sender(PublicUserResponse.from(msg.getSender()))
                .message(msg.getMessage())
                .createdAt(msg.getCreatedAt())
                .build();
        }
    }

    @Data @Builder
    public static class MessageResponse {
        private String message;

        public static MessageResponse of(String message) {
            return MessageResponse.builder().message(message).build();
        }
    }

    @Data @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
