package com.chess.engine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class Barrier {

    private final Square from;
    private final Square to;

    private Barrier(Square from, Square to) {
        this.from = from;
        this.to = to;
    }

    public static Barrier of(Square from, Square to) {
        return new Barrier(from, to);
    }

    // does this barrier block the segment between sq1 and sq2?
    public boolean blocks(Square sq1, Square sq2) {
        return (from.equals(sq1) && to.equals(sq2))
            || (from.equals(sq2) && to.equals(sq1));
    }

    @Override
    public String toString() {
        return from + "—" + to;
    }
}
