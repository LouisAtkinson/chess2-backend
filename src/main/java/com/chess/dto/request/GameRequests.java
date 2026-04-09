package com.chess.dto.request;

import com.chess.entity.Game;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameRequests {

    @Data
    public static class CreateGameRequest {
        @NotNull
        private Game.GameType gameType;

        @NotNull
        private Game.GameMode mode;

        private UUID opponentId; // null for open challenge
        private UUID variantConfigId; // null for standard chess
        private Long timeLimitMs; // null for no time control
    }

    @Data
    public static class MakeMoveRequest {
        @NotBlank
        private String from; // eg "e2"

        @NotBlank
        private String to; // eg "e4"

        private UUID promotionPieceId; // null unless promotion
    }

    @Data
    public static class ResignRequest {

    }

    @Data
    public static class DrawOfferRequest {
        private boolean accept; // true = accept, false = decline
    }
}
