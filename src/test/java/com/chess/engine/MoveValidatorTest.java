package com.chess.engine;

import com.chess.engine.model.*;
import com.chess.engine.rules.*;
import com.chess.engine.validator.MoveValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static com.chess.engine.EngineTestHelper.*;
import static org.assertj.core.api.Assertions.*;

class MoveValidatorTest {

    private MovementRuleParser ruleParser;
    private BarrierChecker barrierChecker;
    private CandidateMoveGenerator candidateGen;
    private MoveApplier moveApplier;
    private CheckDetector checkDetector;
    private MoveValidator validator;

    @BeforeEach
    void setup() {
        ruleParser      = new MovementRuleParser();
        barrierChecker  = new BarrierChecker();
        candidateGen    = new CandidateMoveGenerator(ruleParser, barrierChecker);
        moveApplier     = new MoveApplier();
        checkDetector   = new CheckDetector(candidateGen);
        validator       = new MoveValidator(candidateGen, checkDetector, moveApplier);
    }

    @Test
    @DisplayName("White pawn can move one square forward")
    void whitePawnSinglePush() {
        BoardState state = standardStart();
        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e2"), state);
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("e3")));
    }

    @Test
    @DisplayName("White pawn can move two squares from starting rank")
    void whitePawnDoublePush() {
        BoardState state = standardStart();
        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e2"), state);
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("e4")));
    }

    @Test
    @DisplayName("Pawn cannot move two squares after it has moved")
    void movedPawnNoDoublePush() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("e4"), movedPawn(PieceColor.WHITE));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e4"), state);
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("e5")));
        assertThat(moves).noneMatch(m -> m.getTo().equals(Square.fromAlgebraic("e6")));
    }

    @Test
    @DisplayName("Pawn cannot move forward into occupied square")
    void pawnBlockedByPiece() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("e2"), pawn(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e3"), pawn(PieceColor.BLACK));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e2"), state);
        assertThat(moves).isEmpty();
    }

    @Test
    @DisplayName("Pawn captures diagonally")
    void pawnCapture() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("e4"), movedPawn(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("d5"), pawn(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("f5"), pawn(PieceColor.BLACK));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e4"), state);
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("d5")) && m.isCapture());
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("f5")) && m.isCapture());
    }

    @Test
    @DisplayName("Knight can jump over pieces")
    void knightJumpsOverPieces() {
        BoardState state = standardStart();
        // b1 knight should be able to jump to a3 and c3 even with pawns on rank 2
        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("b1"), state);
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("a3")));
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("c3")));
    }

    @Test
    @DisplayName("Knight has 8 moves from centre of empty board")
    void knightCentreMovement() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("d4"), knight(PieceColor.WHITE));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("d4"), state);
        assertThat(moves).hasSize(8);
    }

    @Test
    @DisplayName("Detects king in check")
    void detectsCheck() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("e5"), rook(PieceColor.BLACK));

        assertThat(checkDetector.isInCheck(PieceColor.WHITE, state)).isTrue();
    }

    @Test
    @DisplayName("Move that leaves own king in check is illegal")
    void moveLeavingKingInCheckIsIllegal() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        // rook pins white bishop on e-file
        state.place(Square.fromAlgebraic("e4"), bishop(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e7"), rook(PieceColor.BLACK));

        // bishop cannot move - would expose king
        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e4"), state);
        assertThat(moves).isEmpty();
    }

    @Test
    @DisplayName("Fool's mate is detected as checkmate")
    void foolsMate() {
        // set up fool's mate position
        BoardState state = standardStart();

        // 1. f3
        applyMove(state, "f2", "f3");
        state.setTurn(PieceColor.BLACK);
        // 1... e5
        applyMove(state, "e7", "e5");
        state.setTurn(PieceColor.WHITE);
        // 2. g4
        applyMove(state, "g2", "g4");
        state.setTurn(PieceColor.BLACK);
        // 2... Qh4#
        applyMove(state, "d8", "h4");
        state.setTurn(PieceColor.WHITE);

        assertThat(checkDetector.isInCheck(PieceColor.WHITE, state)).isTrue();
        assertThat(validator.isCheckmate(state)).isTrue();
    }

    @Test
    @DisplayName("Stalemate is detected correctly")
    void stalemateDetected() {
        // king trapped in corner, no legal moves + no check
        BoardState state = emptyBoard(PieceColor.BLACK);
        state.place(Square.fromAlgebraic("h8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("f7"), queen(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("g6"), king(PieceColor.WHITE));

        assertThat(checkDetector.isInCheck(PieceColor.BLACK, state)).isFalse();
        assertThat(validator.isStalemate(state)).isTrue();
    }

    @Test
    @DisplayName("White can castle kingside when path clear")
    void whiteCastlesKingside() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("h1"), rook(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.setWhiteKingsideCastle(true);

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e1"), state);
        assertThat(moves).anyMatch(Move::isCastleKingside);
    }

    @Test
    @DisplayName("Cannot castle through check")
    void cannotCastleThroughCheck() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("h1"), rook(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        // enemy rook controls f1
        state.place(Square.fromAlgebraic("f8"), rook(PieceColor.BLACK));
        state.setWhiteKingsideCastle(true);

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e1"), state);
        assertThat(moves).noneMatch(Move::isCastleKingside);
    }

    @Test
    @DisplayName("Cannot castle while in check")
    void cannotCastleWhileInCheck() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("h1"), rook(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        // enemy rook attacks e1 (king's current square)
        state.place(Square.fromAlgebraic("e5"), rook(PieceColor.BLACK));
        state.setWhiteKingsideCastle(true);

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e1"), state);
        assertThat(moves).noneMatch(Move::isCastleKingside);
    }

    @Test
    @DisplayName("Sliding piece cannot cross a barrier")
    void sliderBlockedByBarrier() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("a1"), rook(PieceColor.WHITE));

        // barrier between a1 and a2
        state.getBarriers().add(Barrier.of(
            Square.fromAlgebraic("a1"), Square.fromAlgebraic("a2")));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("a1"), state);
        // rook can move along rank (E) but not up the file
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("b1")));
        assertThat(moves).noneMatch(m -> m.getTo().equals(Square.fromAlgebraic("a2")));
        assertThat(moves).noneMatch(m -> m.getTo().equals(Square.fromAlgebraic("a8")));
    }

    @Test
    @DisplayName("Knight jump blocked when all L-paths cross barriers")
    void knightBlockedByBarriers() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("d4"), knight(PieceColor.WHITE));

        state.getBarriers().add(Barrier.of(
            Square.fromAlgebraic("d4"), Square.fromAlgebraic("d5")));
        state.getBarriers().add(Barrier.of(
            Square.fromAlgebraic("d4"), Square.fromAlgebraic("e4")));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("d4"), state);
        assertThat(moves).noneMatch(m -> m.getTo().equals(Square.fromAlgebraic("e6")));
    }

    @Test
    @DisplayName("Knight jump allowed when one L-path is clear")
    void knightAllowedWithOnePathClear() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("d4"), knight(PieceColor.WHITE));

        state.getBarriers().add(Barrier.of(
            Square.fromAlgebraic("d4"), Square.fromAlgebraic("d5")));

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("d4"), state);
        assertThat(moves).anyMatch(m -> m.getTo().equals(Square.fromAlgebraic("e6")));
    }

    @Test
    @DisplayName("En passant capture is legal")
    void enPassantLegal() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        state.place(Square.fromAlgebraic("e5"), movedPawn(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("d5"), movedPawn(PieceColor.BLACK));
        state.setEnPassantTarget("d6");

        List<Move> moves = validator.legalMovesFrom(Square.fromAlgebraic("e5"), state);
        assertThat(moves).anyMatch(m ->
            m.getTo().equals(Square.fromAlgebraic("d6"))
            && m.getMoveType() == Move.MoveType.EN_PASSANT);
    }

    @Test
    @DisplayName("King vs king is insufficient material")
    void kingVsKingInsufficient() {
        BoardState state = emptyBoard(PieceColor.WHITE);
        state.place(Square.fromAlgebraic("e1"), king(PieceColor.WHITE));
        state.place(Square.fromAlgebraic("e8"), king(PieceColor.BLACK));
        assertThat(validator.isInsufficientMaterial(state)).isTrue();
    }

    // for moving pieces without validation, for setup
    private void applyMove(BoardState state, String from, String to) {
        Square f = Square.fromAlgebraic(from);
        Square t = Square.fromAlgebraic(to);
        BoardPiece piece = state.pieceAt(f).orElseThrow();
        state.remove(f);
        state.place(t, piece.withHasMoved(true));
    }
}
