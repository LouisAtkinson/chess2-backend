package com.chess.engine.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode(of = {"from", "to", "promotionPieceId"})
public class Move {

    public enum MoveType {
        NORMAL,
        CAPTURE,
        EN_PASSANT,
        CASTLE_KINGSIDE,
        CASTLE_QUEENSIDE,
        PROMOTION,
        PROMOTION_CAPTURE
    }

    private final Square from;
    private final Square to;
    private final BoardPiece piece;
    private final BoardPiece capturedPiece; // null if not a capture
    private final MoveType moveType;

    private final UUID promotionPieceId;

    private final boolean isCheck;
    private final boolean isCheckmate;
    private final boolean isStalemate;
    private final String sanNotation;

    public boolean isCapture() {
        return capturedPiece != null
            || moveType == MoveType.CAPTURE
            || moveType == MoveType.EN_PASSANT
            || moveType == MoveType.PROMOTION_CAPTURE;
    }

    public boolean isCastleKingside() {
        return moveType == MoveType.CASTLE_KINGSIDE;
    }

    public boolean isCastleQueenside() {
        return moveType == MoveType.CASTLE_QUEENSIDE;
    }

    public boolean isPromotion() {
        return moveType == MoveType.PROMOTION || moveType == MoveType.PROMOTION_CAPTURE;
    }

    public Move withPostMoveFlags(boolean check, boolean checkmate, boolean stalemate, String san) {
        return Move.builder()
            .from(from).to(to).piece(piece).capturedPiece(capturedPiece)
            .moveType(moveType).promotionPieceId(promotionPieceId)
            .isCheck(check).isCheckmate(checkmate).isStalemate(stalemate)
            .sanNotation(san)
            .build();
    }
}
