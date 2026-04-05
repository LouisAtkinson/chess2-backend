package com.chess.engine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class Square {

    private final int x; // files
    private final int y; // ranks

    private Square(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Square of(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IllegalArgumentException("Square out of bounds: " + x + "," + y);
        }
        return new Square(x, y);
    }

    public static boolean isValid(int x, int y) {
        return x >= 0 && x <= 7 && y >= 0 && y <= 7;
    }

    // parse algebraic notation eg "e4" to Square(4, 3)
    public static Square fromAlgebraic(String alg) {
        if (alg == null || alg.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + alg);
        }
        int x = alg.charAt(0) - 'a';
        int y = alg.charAt(1) - '1';
        return Square.of(x, y);
    }

    // convert to algebraic notation eg Square(4,3) → "e4"
    public String toAlgebraic() {
        return "" + (char) ('a' + x) + (char) ('1' + y);
    }

    public Square offset(int dx, int dy) {
        return Square.of(x + dx, y + dy);
    }

    public boolean canOffset(int dx, int dy) {
        return isValid(x + dx, y + dy);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
