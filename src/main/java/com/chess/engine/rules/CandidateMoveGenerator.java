package com.chess.engine.rules;

import com.chess.engine.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// generate all legal moves for a piece
@Component
@RequiredArgsConstructor
public class CandidateMoveGenerator {

    private final MovementRuleParser ruleParser;
    private final BarrierChecker barrierChecker;

    public List<Move> generateCandidates(Square from, BoardState state) {
        BoardPiece piece = state.pieceAt(from).orElse(null);
        if (piece == null) return List.of();

        List<Move> candidates = new ArrayList<>();

        List<MovementRule> moveRules = ruleParser.parse(piece.getMovementRules());
        List<MovementRule> captureRules = ruleParser.parse(piece.effectiveCaptureRules());

        for (MovementRule rule : moveRules) {
            if (rule.isMustCapture()) continue; // handled via captureRules
            if (rule.isFirstMoveOnly() && piece.isHasMoved()) continue;
            candidates.addAll(applyRule(from, piece, rule, false, state));
        }

        // capture-only rules (eg pawn diagonal)
        for (MovementRule rule : captureRules) {
            if (rule.isFirstMoveOnly() && piece.isHasMoved()) continue;
            if (rule.isMustCapture()) {
                candidates.addAll(applyRule(from, piece, rule, true, state));
            } else if (!moveRules.contains(rule)) {
                candidates.addAll(applyRule(from, piece, rule, false, state));
            }
        }

        // en passant
        if (state.getEnPassantTarget() != null) {
            candidates.addAll(generateEnPassant(from, piece, state));
        }

        // castling
        if (piece.isKing() && !piece.isHasMoved()) {
            candidates.addAll(generateCastling(from, piece, state));
        }

        return candidates;
    }

    private List<Move> applyRule(Square from, BoardPiece piece,
                                  MovementRule rule, boolean captureOnly,
                                  BoardState state) {
        return switch (rule.getType()) {
            case SLIDE -> generateSlide(from, piece, rule, captureOnly, state);
            case JUMP  -> generateJump(from, piece, rule, captureOnly, state);
        };
    }

    private List<Move> generateSlide(Square from, BoardPiece piece,
                                      MovementRule rule, boolean captureOnly,
                                      BoardState state) {
        List<Move> moves = new ArrayList<>();
        int maxDist = rule.getMaxDistance() == 0 ? 7 : rule.getMaxDistance();

        for (Direction dir : rule.getDirections()) {
            Direction effective = (piece.getColor() == PieceColor.BLACK) ? dir.flip() : dir;

            Square prev = from;
            for (int dist = 1; dist <= maxDist; dist++) {
                if (!prev.canOffset(effective.dx, effective.dy)) break;
                Square target = prev.offset(effective.dx, effective.dy);

                if (barrierChecker.isStepBlocked(prev, target, state.getBarriers())) break;

                var occupant = state.pieceAt(target);

                if (occupant.isPresent()) {
                    BoardPiece occ = occupant.get();
                    if (occ.getColor() != piece.getColor() && rule.isCanCapture() && !captureOnly) {
                        moves.add(buildMove(from, target, piece, occ, Move.MoveType.CAPTURE));
                    } else if (occ.getColor() != piece.getColor() && captureOnly) {
                        moves.add(buildMove(from, target, piece, occ, Move.MoveType.CAPTURE));
                    }
                    break; // sliding stops at any occupied square
                }

                if (!captureOnly && !rule.isMustCapture()) {
                    moves.add(buildMove(from, target, piece, null, Move.MoveType.NORMAL));
                }

                // pawn double push on first move
                if (rule.isFirstMoveDouble() && !piece.isHasMoved() && dist == 1) {
                    maxDist = Math.max(maxDist, 2);
                }

                prev = target;
            }
        }
        return moves;
    }

    private List<Move> generateJump(Square from, BoardPiece piece,
                                     MovementRule rule, boolean captureOnly,
                                     BoardState state) {
        List<Move> moves = new ArrayList<>();

        for (int[] offset : rule.getOffsets()) {
            int dx = offset[0];
            int dy = offset[1];

            if (piece.getColor() == PieceColor.BLACK) dy = -dy;

            if (!from.canOffset(dx, dy)) continue;
            Square target = from.offset(dx, dy);

            if (barrierChecker.isJumpBlocked(from, target, state.getBarriers())) continue;

            var occupant = state.pieceAt(target);

            if (occupant.isPresent()) {
                BoardPiece occ = occupant.get();
                if (occ.getColor() != piece.getColor() && rule.isCanCapture()) {
                    moves.add(buildMove(from, target, piece, occ, Move.MoveType.CAPTURE));
                }
            } else if (!captureOnly && !rule.isMustCapture()) {
                moves.add(buildMove(from, target, piece, null, Move.MoveType.NORMAL));
            }
        }
        return moves;
    }

    private List<Move> generateEnPassant(Square from, BoardPiece piece, BoardState state) {
        List<Move> moves = new ArrayList<>();

        boolean hasDiagonalCapture = ruleParser.parse(piece.effectiveCaptureRules()).stream()
            .anyMatch(r -> r.getType() == MovementRule.RuleType.SLIDE
                && r.isMustCapture()
                && r.getMaxDistance() == 1);

        if (!hasDiagonalCapture) return moves;

        Square epTarget = Square.fromAlgebraic(state.getEnPassantTarget());

        // check if piece is adjacent diagonally
        int dy = piece.getColor() == PieceColor.WHITE ? 1 : -1;
        if (Math.abs(from.getX() - epTarget.getX()) == 1
                && epTarget.getY() - from.getY() == dy) {

            if (barrierChecker.isJumpBlocked(from, epTarget, state.getBarriers())) {
                return moves;
            }

            Square capturedPawnSquare = Square.of(epTarget.getX(), from.getY());
            BoardPiece capturedPawn = state.pieceAt(capturedPawnSquare).orElse(null);

            if (capturedPawn != null && capturedPawn.getColor() != piece.getColor()) {
                moves.add(buildMove(from, epTarget, piece, capturedPawn, Move.MoveType.EN_PASSANT));
            }
        }
        return moves;
    }

    private List<Move> generateCastling(Square kingSquare, BoardPiece king, BoardState state) {
        List<Move> moves = new ArrayList<>();
        PieceColor color = king.getColor();
        int rank = color == PieceColor.WHITE ? 0 : 7;

        // kingside
        if (state.kingsideCastleRight(color)) {
            Square rookSq = Square.of(7, rank);
            if (state.pieceAt(rookSq).map(p -> !p.isHasMoved()).orElse(false)) {
                // check squares between empty/unbarriered
                Square f = Square.of(5, rank);
                Square g = Square.of(6, rank);
                if (state.isEmpty(f) && state.isEmpty(g)
                        && !barrierChecker.isStepBlocked(kingSquare, f, state.getBarriers())
                        && !barrierChecker.isStepBlocked(f, g, state.getBarriers())) {
                    moves.add(buildMove(kingSquare, g, king, null, Move.MoveType.CASTLE_KINGSIDE));
                }
            }
        }

        // queenside
        if (state.queensideCastleRight(color)) {
            Square rookSq = Square.of(0, rank);
            if (state.pieceAt(rookSq).map(p -> !p.isHasMoved()).orElse(false)) {
                Square b = Square.of(1, rank);
                Square c = Square.of(2, rank);
                Square d = Square.of(3, rank);
                if (state.isEmpty(b) && state.isEmpty(c) && state.isEmpty(d)
                        && !barrierChecker.isStepBlocked(kingSquare, d, state.getBarriers())
                        && !barrierChecker.isStepBlocked(d, c, state.getBarriers())) {
                    moves.add(buildMove(kingSquare, c, king, null, Move.MoveType.CASTLE_QUEENSIDE));
                }
            }
        }

        return moves;
    }

    private Move buildMove(Square from, Square to, BoardPiece piece,
                           BoardPiece captured, Move.MoveType type) {
        return Move.builder()
            .from(from).to(to)
            .piece(piece)
            .capturedPiece(captured)
            .moveType(type)
            .build();
    }
}
