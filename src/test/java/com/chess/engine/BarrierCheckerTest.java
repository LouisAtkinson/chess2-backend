package com.chess.engine;

import com.chess.engine.model.Barrier;
import com.chess.engine.model.Square;
import com.chess.engine.rules.BarrierChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BarrierCheckerTest {

    private BarrierChecker checker;

    @BeforeEach
    void setup() {
        checker = new BarrierChecker();
    }

    @Test
    @DisplayName("Step is blocked when barrier exists between squares")
    void stepBlockedByBarrier() {
        var a1 = Square.fromAlgebraic("a1");
        var a2 = Square.fromAlgebraic("a2");
        var barriers = List.of(Barrier.of(a1, a2));
        assertThat(checker.isStepBlocked(a1, a2, barriers)).isTrue();
    }

    @Test
    @DisplayName("Barrier is bidirectional")
    void barrierIsBidirectional() {
        var a1 = Square.fromAlgebraic("a1");
        var a2 = Square.fromAlgebraic("a2");
        var barriers = List.of(Barrier.of(a1, a2));
        assertThat(checker.isStepBlocked(a2, a1, barriers)).isTrue();
    }

    @Test
    @DisplayName("Step is clear when no matching barrier exists")
    void stepClearWithNoBarrier() {
        var a1 = Square.fromAlgebraic("a1");
        var a2 = Square.fromAlgebraic("a2");
        var b1 = Square.fromAlgebraic("b1");
        var barriers = List.of(Barrier.of(b1, a2)); // different squares
        assertThat(checker.isStepBlocked(a1, a2, barriers)).isFalse();
    }

    @Test
    @DisplayName("Knight jump blocked when both L-paths cross barriers")
    void knightBothPathsBlocked() {
        var d4 = Square.fromAlgebraic("d4");
        var e6 = Square.fromAlgebraic("e6");
        var e4 = Square.fromAlgebraic("e4");
        var d5 = Square.fromAlgebraic("d5");
        var barriers = List.of(Barrier.of(d4, e4), Barrier.of(d4, d5));
        assertThat(checker.isJumpBlocked(d4, e6, barriers)).isTrue();
    }

    @Test
    @DisplayName("Knight jump allowed when at least one L-path is clear")
    void knightOnePathClear() {
        var d4 = Square.fromAlgebraic("d4");
        var e6 = Square.fromAlgebraic("e6");
        var d5 = Square.fromAlgebraic("d5");
        // only block one path
        var barriers = List.of(Barrier.of(d4, d5));
        assertThat(checker.isJumpBlocked(d4, e6, barriers)).isFalse();
    }

    @Test
    @DisplayName("No barriers means never blocked")
    void noBarriersNeverBlocked() {
        var a1 = Square.fromAlgebraic("a1");
        var h8 = Square.fromAlgebraic("h8");
        assertThat(checker.isJumpBlocked(a1, h8, List.of())).isFalse();
    }
}
