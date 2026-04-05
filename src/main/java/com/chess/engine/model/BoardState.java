package com.chess.engine.model;

import lombok.*;

import java.util.*;

@Getter
@Setter
public class BoardState {

    // map from algebraic square (eg 'e4') to BoardPiece
    private final Map<String, BoardPiece> squares;

    private PieceColor turn;

    // castling rights
    private boolean whiteKingsideCastle;
    private boolean whiteQueensideCastle;
    private boolean blackKingsideCastle;
    private boolean blackQueensideCastle;

    private String enPassantTarget;

    private int halfMoveClock;
    private int fullMoveNumber;

    private final List<Barrier> barriers;

    private final List<String> positionHistory;

    public BoardState(
            Map<String, BoardPiece> squares,
            PieceColor turn,
            boolean whiteKingsideCastle,
            boolean whiteQueensideCastle,
            boolean blackKingsideCastle,
            boolean blackQueensideCastle,
            String enPassantTarget,
            int halfMoveClock,
            int fullMoveNumber,
            List<Barrier> barriers,
            List<String> positionHistory) {
        this.squares = new HashMap<>(squares);
        this.turn = turn;
        this.whiteKingsideCastle = whiteKingsideCastle;
        this.whiteQueensideCastle = whiteQueensideCastle;
        this.blackKingsideCastle = blackKingsideCastle;
        this.blackQueensideCastle = blackQueensideCastle;
        this.enPassantTarget = enPassantTarget;
        this.halfMoveClock = halfMoveClock;
        this.fullMoveNumber = fullMoveNumber;
        this.barriers = new ArrayList<>(barriers);
        this.positionHistory = new ArrayList<>(positionHistory);
    }

    public BoardState copy() {
        return new BoardState(
            new HashMap<>(squares),
            turn,
            whiteKingsideCastle, whiteQueensideCastle,
            blackKingsideCastle, blackQueensideCastle,
            enPassantTarget,
            halfMoveClock, fullMoveNumber,
            barriers,
            positionHistory
        );
    }

    public Optional<BoardPiece> pieceAt(Square sq) {
        return Optional.ofNullable(squares.get(sq.toAlgebraic()));
    }

    public Optional<BoardPiece> pieceAt(String alg) {
        return Optional.ofNullable(squares.get(alg));
    }

    public boolean isEmpty(Square sq) {
        return !squares.containsKey(sq.toAlgebraic());
    }

    public boolean isEmpty(String alg) {
        return !squares.containsKey(alg);
    }

    public void place(Square sq, BoardPiece piece) {
        squares.put(sq.toAlgebraic(), piece);
    }

    public void remove(Square sq) {
        squares.remove(sq.toAlgebraic());
    }

    public void remove(String alg) {
        squares.remove(alg);
    }

    public Optional<Square> findKing(PieceColor color) {
        return squares.entrySet().stream()
            .filter(e -> e.getValue().isKing() && e.getValue().getColor() == color)
            .map(e -> Square.fromAlgebraic(e.getKey()))
            .findFirst();
    }

    public boolean isBlockedByBarrier(Square from, Square to) {
        return barriers.stream().anyMatch(b -> b.blocks(from, to));
    }

    public void revokeCastlingRightsFor(PieceColor color) {
        if (color == PieceColor.WHITE) {
            whiteKingsideCastle = false;
            whiteQueensideCastle = false;
        } else {
            blackKingsideCastle = false;
            blackQueensideCastle = false;
        }
    }

    public boolean kingsideCastleRight(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKingsideCastle : blackKingsideCastle;
    }

    public boolean queensideCastleRight(PieceColor color) {
        return color == PieceColor.WHITE ? whiteQueensideCastle : blackQueensideCastle;
    }

    public Map<Square, BoardPiece> allPiecesOf(PieceColor color) {
        Map<Square, BoardPiece> result = new HashMap<>();
        for (var entry : squares.entrySet()) {
            if (entry.getValue().getColor() == color) {
                result.put(Square.fromAlgebraic(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }
}
