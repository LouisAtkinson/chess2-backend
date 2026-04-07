package com.chess.engine.notation;

import com.chess.engine.model.*;
import org.springframework.stereotype.Component;

@Component
public class FenGenerator {

    public String generate(BoardState state) {
        StringBuilder sb = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                Square sq = Square.of(file, rank);
                var piece = state.pieceAt(sq);
                if (piece.isEmpty()) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    sb.append(pieceChar(piece.get()));
                }
            }
            if (emptyCount > 0) sb.append(emptyCount);
            if (rank > 0) sb.append('/');
        }

        sb.append(' ').append(state.getTurn() == PieceColor.WHITE ? 'w' : 'b');

        sb.append(' ');
        StringBuilder castling = new StringBuilder();
        if (state.isWhiteKingsideCastle()) castling.append('K');
        if (state.isWhiteQueensideCastle()) castling.append('Q');
        if (state.isBlackKingsideCastle()) castling.append('k');
        if (state.isBlackQueensideCastle()) castling.append('q');
        sb.append(castling.isEmpty() ? "-" : castling);

        sb.append(' ');
        sb.append(state.getEnPassantTarget() != null ? state.getEnPassantTarget() : "-");

        sb.append(' ').append(state.getHalfMoveClock());

        sb.append(' ').append(state.getFullMoveNumber());

        return sb.toString();
    }

    public String generatePositionKey(BoardState state) {
        String full = generate(state);
        String[] parts = full.split(" ");
        return parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
    }

    private char pieceChar(BoardPiece piece) {
        char c = switch (piece.getName().toLowerCase()) {
            case "king"   -> 'k';
            case "queen"  -> 'q';
            case "rook"   -> 'r';
            case "bishop" -> 'b';
            case "knight" -> 'n';
            case "pawn"   -> 'p';
            default -> '?'; // custom piece
        };
        return piece.getColor() == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }
}
