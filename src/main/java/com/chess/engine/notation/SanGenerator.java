package com.chess.engine.notation;

import com.chess.engine.model.*;
import com.chess.engine.rules.CandidateMoveGenerator;
import com.chess.engine.rules.CheckDetector;
import com.chess.engine.rules.MoveApplier;
import com.chess.engine.validator.MoveValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SanGenerator {

    private final CandidateMoveGenerator candidateGenerator;
    private final CheckDetector checkDetector;
    private final MoveValidator moveValidator;

    public String generate(Move move, BoardState stateBefore, BoardState stateAfter) {
        if (move.isCastleKingside())  return "O-O"  + checkSuffix(stateAfter);
        if (move.isCastleQueenside()) return "O-O-O" + checkSuffix(stateAfter);

        BoardPiece piece = move.getPiece();
        StringBuilder san = new StringBuilder();

        boolean isPawn = isPawnLike(piece);

        if (!isPawn) {
            san.append(pieceSymbol(piece));
        }

        if (!isPawn) {
            String disambig = disambiguate(move, stateBefore);
            san.append(disambig);
        } else if (move.isCapture()) {
            san.append((char) ('a' + move.getFrom().getX()));
        }

        if (move.isCapture()) {
            san.append('x');
        }

        san.append(move.getTo().toAlgebraic());

        if (move.isPromotion() && move.getPromotionPieceId() != null) {
            san.append("=Q");
        }

        san.append(checkSuffix(stateAfter));

        return san.toString();
    }

    private String checkSuffix(BoardState stateAfter) {
        PieceColor nextTurn = stateAfter.getTurn();
        if (moveValidator.isCheckmate(stateAfter)) return "#";
        if (checkDetector.isInCheck(nextTurn, stateAfter)) return "+";
        return "";
    }

    private String disambiguate(Move move, BoardState state) {
        List<Move> allMoves = moveValidator.allLegalMoves(state);
        long sameTarget = allMoves.stream()
            .filter(m -> !m.getFrom().equals(move.getFrom()))
            .filter(m -> m.getTo().equals(move.getTo()))
            .filter(m -> m.getPiece().getName().equals(move.getPiece().getName()))
            .count();

        if (sameTarget == 0) return "";

        long sameFile = allMoves.stream()
            .filter(m -> !m.getFrom().equals(move.getFrom()))
            .filter(m -> m.getTo().equals(move.getTo()))
            .filter(m -> m.getPiece().getName().equals(move.getPiece().getName()))
            .filter(m -> m.getFrom().getX() == move.getFrom().getX())
            .count();

        if (sameFile == 0) {
            return String.valueOf((char) ('a' + move.getFrom().getX()));
        }

        long sameRank = allMoves.stream()
            .filter(m -> !m.getFrom().equals(move.getFrom()))
            .filter(m -> m.getTo().equals(move.getTo()))
            .filter(m -> m.getPiece().getName().equals(move.getPiece().getName()))
            .filter(m -> m.getFrom().getY() == move.getFrom().getY())
            .count();

        if (sameRank == 0) {
            return String.valueOf((char) ('1' + move.getFrom().getY()));
        }

        return move.getFrom().toAlgebraic();
    }

    private String pieceSymbol(BoardPiece piece) {
        return switch (piece.getName().toLowerCase()) {
            case "king"   -> "K";
            case "queen"  -> "Q";
            case "rook"   -> "R";
            case "bishop" -> "B";
            case "knight" -> "N";
            default       -> piece.getName().substring(0, 1).toUpperCase();
        };
    }

    private boolean isPawnLike(BoardPiece piece) {
        return piece.getMovementRules().stream().anyMatch(r -> {
            Object fmd = r.get("firstMoveDouble");
            return Boolean.TRUE.equals(fmd) || "true".equals(String.valueOf(fmd));
        });
    }
}
