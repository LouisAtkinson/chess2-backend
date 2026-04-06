package com.chess.engine.rules;

import com.chess.engine.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MoveApplier {

    // apply move to a copy of the state and return the new state
    public BoardState apply(Move move, BoardState state, BoardPiece promotionPiece) {
        BoardState next = state.copy();

        Square from = move.getFrom();
        Square to = move.getTo();
        BoardPiece piece = move.getPiece();

        if (move.isCastleKingside()) {
            applyCastle(next, piece, from, to, 7, 5);
            return finalise(next, move, piece);
        }
        if (move.isCastleQueenside()) {
            applyCastle(next, piece, from, to, 0, 3);
            return finalise(next, move, piece);
        }

        if (move.getMoveType() == Move.MoveType.EN_PASSANT) {
            next.remove(from);
            next.place(to, piece.withHasMoved(true));
            Square capturedPawnSq = Square.of(to.getX(), from.getY());
            next.remove(capturedPawnSq);
            next.setEnPassantTarget(null);
            next.setHalfMoveClock(0);
            return finalise(next, move, piece);
        }

        next.remove(from);

        BoardPiece landingPiece;
        if (move.isPromotion() && promotionPiece != null) {
            landingPiece = promotionPiece;
        } else {
            landingPiece = piece.withHasMoved(true);
        }

        next.place(to, landingPiece);

        boolean isPawn = isPawnLike(piece);
        int rankDiff = Math.abs(to.getY() - from.getY());
        if (isPawn && rankDiff == 2) {
            int epY = (from.getY() + to.getY()) / 2;
            next.setEnPassantTarget(Square.of(from.getX(), epY).toAlgebraic());
        } else {
            next.setEnPassantTarget(null);
        }

        if (move.isCapture() || isPawn) {
            next.setHalfMoveClock(0);
        } else {
            next.setHalfMoveClock(next.getHalfMoveClock() + 1);
        }

        return finalise(next, move, piece);
    }

    private void applyCastle(BoardState state, BoardPiece king,
                              Square kingFrom, Square kingTo,
                              int rookFromFile, int rookToFile) {
        int rank = kingFrom.getY();
        Square rookFrom = Square.of(rookFromFile, rank);
        Square rookTo = Square.of(rookToFile, rank);

        BoardPiece rook = state.pieceAt(rookFrom).orElseThrow();

        state.remove(kingFrom);
        state.remove(rookFrom);
        state.place(kingTo, king.withHasMoved(true));
        state.place(rookTo, rook.withHasMoved(true));
    }

    private BoardState finalise(BoardState state, Move move, BoardPiece piece) {
        if (piece.isKing()) {
            state.revokeCastlingRightsFor(piece.getColor());
        }
        revokeRookCastlingRight(state, move.getFrom(), piece.getColor());
        revokeRookCastlingRight(state, move.getTo(), piece.getColor().opposite());

        if (state.getTurn() == PieceColor.BLACK) {
            state.setFullMoveNumber(state.getFullMoveNumber() + 1);
        }
        state.setTurn(state.getTurn().opposite());

        return state;
    }

    private void revokeRookCastlingRight(BoardState state, Square sq, PieceColor color) {
        int rank = color == PieceColor.WHITE ? 0 : 7;
        if (sq.getY() != rank) return;
        if (sq.getX() == 7) { // kingside rook
            if (color == PieceColor.WHITE) state.setWhiteKingsideCastle(false);
            else state.setBlackKingsideCastle(false);
        } else if (sq.getX() == 0) { // queenside rook
            if (color == PieceColor.WHITE) state.setWhiteQueensideCastle(false);
            else state.setBlackQueensideCastle(false);
        }
    }

    private boolean isPawnLike(BoardPiece piece) {
        return piece.getMovementRules().stream().anyMatch(r -> {
            Object fmd = r.get("firstMoveDouble");
            return Boolean.TRUE.equals(fmd) || "true".equals(String.valueOf(fmd));
        });
    }
}
