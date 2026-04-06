package com.chess.engine.validator;

import com.chess.engine.model.*;
import com.chess.engine.rules.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * validates moves through by:
 * 1. generate pseudo-legal candidates moves
 * 2. filter moves that leave own king in check
 * 3. validate castling doesn't pass through check
 * 4. confirm requested move is in the legal set
 */
@Component
@RequiredArgsConstructor
public class MoveValidator {

    private final CandidateMoveGenerator candidateGenerator;
    private final CheckDetector checkDetector;
    private final MoveApplier moveApplier;

    public List<Move> legalMovesFrom(Square from, BoardState state) {
        List<Move> candidates = candidateGenerator.generateCandidates(from, state);

        return candidates.stream()
            .filter(m -> isFullyLegal(m, state))
            .collect(Collectors.toList());
    }

    public List<Move> allLegalMoves(BoardState state) {
        return state.allPiecesOf(state.getTurn()).entrySet().stream()
            .flatMap(e -> legalMovesFrom(e.getKey(), state).stream())
            .collect(Collectors.toList());
    }

    public Move validateMove(Square from, Square to, BoardState state) {
        return legalMovesFrom(from, state).stream()
            .filter(m -> m.getTo().equals(to))
            .findFirst()
            .orElse(null);
    }

    public boolean isCheckmate(BoardState state) {
        return checkDetector.isInCheck(state.getTurn(), state)
            && allLegalMoves(state).isEmpty();
    }

    public boolean isStalemate(BoardState state) {
        return !checkDetector.isInCheck(state.getTurn(), state)
            && allLegalMoves(state).isEmpty();
    }

    public boolean isFiftyMoveRule(BoardState state) {
        return state.getHalfMoveClock() >= 100; // 100 half-moves = 50 full moves
    }

    public boolean isInsufficientMaterial(BoardState state) {
        var whites = state.allPiecesOf(PieceColor.WHITE);
        var blacks = state.allPiecesOf(PieceColor.BLACK);

        // only kings remain
        if (whites.size() == 1 && blacks.size() == 1) return true;

        // king + bishop or king + knight vs lone king
        if (whites.size() == 1 && blacks.size() == 2) {
            return blacks.values().stream()
                .anyMatch(p -> isMinorPiece(p) && !p.isKing());
        }
        if (blacks.size() == 1 && whites.size() == 2) {
            return whites.values().stream()
                .anyMatch(p -> isMinorPiece(p) && !p.isKing());
        }

        return false;
    }

    private boolean isFullyLegal(Move move, BoardState state) {
        if (checkDetector.wouldLeaveKingInCheck(move, state, moveApplier)) return false;

        if (move.isCastleKingside() || move.isCastleQueenside()) {
            if (checkDetector.isInCheck(move.getPiece().getColor(), state)) return false;
            if (!castlingPathSafe(move, state)) return false;
        }

        return true;
    }

    private boolean castlingPathSafe(Move move, BoardState state) {
        PieceColor color = move.getPiece().getColor();
        int rank = color == PieceColor.WHITE ? 0 : 7;
        PieceColor opponent = color.opposite();

        if (move.isCastleKingside()) {
            Square fFile = Square.of(5, rank);
            return !checkDetector.isSquareAttackedBy(fFile, opponent, state);
        } else {
            Square dFile = Square.of(3, rank);
            return !checkDetector.isSquareAttackedBy(dFile, opponent, state);
        }
    }

    private boolean isMinorPiece(BoardPiece p) {
        String svg = p.getSvgKey();
        return svg != null && (svg.contains("bishop") || svg.contains("knight"));
    }
}
