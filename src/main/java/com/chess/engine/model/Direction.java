package com.chess.engine.model;

import java.util.Map;

// dx/dy are from White's perspective (y increases toward Black's side).
public enum Direction {
    N(0, 1), NE(1, 1), E(1, 0), SE(1, -1),
    S(0, -1), SW(-1, -1), W(-1, 0), NW(-1, 1);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    private static final Map<String, Direction> BY_NAME = Map.of(
        "N", N, "NE", NE, "E", E, "SE", SE,
        "S", S, "SW", SW, "W", W, "NW", NW
    );

    public static Direction fromString(String s) {
        Direction d = BY_NAME.get(s.toUpperCase());
        if (d == null) throw new IllegalArgumentException("Unknown direction: " + s);
        return d;
    }

    // flip direction for black pieces
    public Direction flip() {
        return switch (this) {
            case N -> S; case S -> N;
            case NE -> SE; case SE -> NE;
            case NW -> SW; case SW -> NW;
            case E -> E; case W -> W;
        };
    }
}
