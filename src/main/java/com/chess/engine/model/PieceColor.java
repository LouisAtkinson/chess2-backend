package com.chess.engine.model;

public enum PieceColor {
    WHITE, BLACK;

    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String lower() {
        return this == WHITE ? "white" : "black";
    }

    // direction multiplier - white moves "up" (positive y), black moves "down"
    public int direction() {
        return this == WHITE ? 1 : -1;
    }

    public static PieceColor fromString(String s) {
        return "white".equalsIgnoreCase(s) ? WHITE : BLACK;
    }
}
