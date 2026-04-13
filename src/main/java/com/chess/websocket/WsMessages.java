package com.chess.websocket;

import com.chess.dto.response.Responses.*;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class WsMessages {

    @Data
    public static class WsMoveRequest {
        private String from;
        private String to;
        private UUID promotionPieceId;
    }

    @Data
    public static class WsChatRequest {
        private String message;
    }

    @Data
    public static class WsDrawOfferRequest {
        private boolean accept; // true = offer draw; false = decline draw offer
    }

    public enum EventType {
        GAME_STATE_UPDATE,
        OPPONENT_MOVE,
        GAME_END,
        CHAT_MESSAGE,
        DRAW_OFFER,
        DRAW_DECLINED,
        PLAYER_JOINED,
        PLAYER_DISCONNECTED,
        ERROR
    }

    @Data @Builder
    public static class WsEvent {
        private EventType type;
        private Object payload;
        private OffsetDateTime timestamp;

        public static WsEvent of(EventType type, Object payload) {
            return WsEvent.builder()
                .type(type)
                .payload(payload)
                .timestamp(OffsetDateTime.now())
                .build();
        }
    }

    @Data @Builder
    public static class MovePayload {
        private String from;
        private String to;
        private String piece;
        private boolean isCapture;
        private boolean isCheck;
        private boolean isCheckmate;
        private boolean isCastleKingside;
        private boolean isCastleQueenside;
        private String sanNotation;
        private Map<String, Object> boardState;
        private String turn;
        private int moveNumber;
    }

    @Data @Builder
    public static class GameEndPayload {
        private String status;
        private String result;
        private String reason;
    }
}
