package com.chess.engine.rules;

import com.chess.engine.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckDetector {

    private final CandidateMoveGenerator candidateGenerator;

    public boolean isInCheck(PieceColor color, BoardState state) {
        return state.findKing(color)
            .map(kingSquare -> isSquareAttackedBy(kingSquare, color.opposite(), state))
            .orElse(false);
    }

    public boolean isSquareAttackedBy(Square square, PieceColor attackerColor, BoardState state) {
        Map<Square, BoardPiece> attackers = state.allPiecesOf(attackerColor);

        for (Map.Entry<Square, BoardPiece> entry : attackers.entrySet()) {
            Square attackerSquare = entry.getKey();
            List<Move> candidates = candidateGenerator.generateCandidates(attackerSquare, state);

            boolean attacks = candidates.stream().anyMatch(m ->
                m.getTo().equals(square) && m.isCapture()
                    || (m.getTo().equals(square) && m.getMoveType() == Move.MoveType.NORMAL)
            );

            if (attacks) return true;
        }
        return false;
    }

    // for checking if move would leave king in check
    public boolean wouldLeaveKingInCheck(Move move, BoardState state, MoveApplier applier) {
        BoardState simulated = applier.apply(move, state, null);
        PieceColor mover = move.getPiece().getColor();
        return isInCheck(mover, simulated);
    }
}
