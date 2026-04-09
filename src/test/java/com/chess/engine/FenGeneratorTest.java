package com.chess.engine;

import com.chess.engine.model.PieceColor;
import com.chess.engine.notation.FenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.chess.engine.EngineTestHelper.*;
import static org.assertj.core.api.Assertions.*;

class FenGeneratorTest {

    private FenGenerator fenGenerator;

    @BeforeEach
    void setup() {
        fenGenerator = new FenGenerator();
    }

    @Test
    @DisplayName("Standard starting position produces correct FEN")
    void standardStartingFen() {
        var state = standardStart();
        String fen = fenGenerator.generate(state);
        assertThat(fen).startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
        assertThat(fen).contains("w KQkq - 0 1");
    }

    @Test
    @DisplayName("FEN updates correctly after e4 opening")
    void fenAfterE4() {
        var state = standardStart();
        // simulate e2-e4
        var pawn = state.pieceAt("e2").get();
        state.remove("e2");
        state.place(com.chess.engine.model.Square.fromAlgebraic("e4"), pawn.withHasMoved(true));
        state.setEnPassantTarget("e3");
        state.setTurn(PieceColor.BLACK);

        String fen = fenGenerator.generate(state);
        assertThat(fen).startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR");
        assertThat(fen).contains("b KQkq e3");
    }

    @Test
    @DisplayName("Position key strips clock fields for repetition detection")
    void positionKeyStripsClocks() {
        var state = standardStart();
        state.setHalfMoveClock(10);
        state.setFullMoveNumber(15);

        String key = fenGenerator.generatePositionKey(state);
        String[] parts = key.split(" ");
        assertThat(parts).hasSize(4);
    }
}
