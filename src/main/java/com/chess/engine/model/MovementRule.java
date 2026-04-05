package com.chess.engine.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MovementRule {

    public enum RuleType {
        SLIDE, JUMP
    }

    private final RuleType type;
    private final List<Direction> directions;

    // max squares to slide - 0 means unlimited (use 7 for board limit)
    private final int maxDistance;

    // dx/dy pairs for jump offsets
    private final List<int[]> offsets;

    private final boolean canCapture;
    private final boolean mustCapture;
    private final boolean firstMoveOnly;
    private final boolean firstMoveDouble;  // pawn double-push on first move
}
