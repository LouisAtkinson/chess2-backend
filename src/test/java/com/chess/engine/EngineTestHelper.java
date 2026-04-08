package com.chess.engine;

import com.chess.engine.model.*;

import java.util.*;

public class EngineTestHelper {
    public static List<Map<String, Object>> kingRules() {
        return List.of(Map.of(
            "type", "SLIDE",
            "directions", List.of("N","NE","E","SE","S","SW","W","NW"),
            "maxDistance", 1,
            "canCapture", true,
            "mustCapture", false
        ));
    }

    public static List<Map<String, Object>> queenRules() {
        return List.of(Map.of(
            "type", "SLIDE",
            "directions", List.of("N","NE","E","SE","S","SW","W","NW"),
            "maxDistance", 7,
            "canCapture", true,
            "mustCapture", false
        ));
    }

    public static List<Map<String, Object>> rookRules() {
        return List.of(Map.of(
            "type", "SLIDE",
            "directions", List.of("N","E","S","W"),
            "maxDistance", 7,
            "canCapture", true,
            "mustCapture", false
        ));
    }

    public static List<Map<String, Object>> bishopRules() {
        return List.of(Map.of(
            "type", "SLIDE",
            "directions", List.of("NE","SE","SW","NW"),
            "maxDistance", 7,
            "canCapture", true,
            "mustCapture", false
        ));
    }

    public static List<Map<String, Object>> knightRules() {
        return List.of(Map.of(
            "type", "JUMP",
            "offsets", List.of(
                List.of(1, 2), List.of(2, 1), List.of(2, -1), List.of(1, -2),
                List.of(-1, -2), List.of(-2, -1), List.of(-2, 1), List.of(-1, 2)
            ),
            "canCapture", true,
            "mustCapture", false
        ));
    }

    public static List<Map<String, Object>> pawnMoveRules() {
        return List.of(
            Map.of("type", "SLIDE", "directions", List.of("N"),
                   "maxDistance", 1, "canCapture", false, "mustCapture", false,
                   "firstMoveDouble", true),
            Map.of("type", "SLIDE", "directions", List.of("NE","NW"),
                   "maxDistance", 1, "canCapture", true, "mustCapture", true)
        );
    }

    public static BoardPiece king(PieceColor color) {
        return BoardPiece.builder()
            .definitionId(UUID.randomUUID()).instanceId(color == PieceColor.WHITE ? "WK" : "BK")
            .name("King").svgKey("standard_king").color(color)
            .isKing(true).isStandard(true)
            .movementRules(kingRules()).captureRules(null).hasMoved(false).build();
    }

    public static BoardPiece queen(PieceColor color) {
        return BoardPiece.builder()
            .definitionId(UUID.randomUUID()).instanceId(color == PieceColor.WHITE ? "WQ" : "BQ")
            .name("Queen").svgKey("standard_queen").color(color)
            .isKing(false).isStandard(true)
            .movementRules(queenRules()).captureRules(null).hasMoved(false).build();
    }

    public static BoardPiece rook(PieceColor color) {
        return BoardPiece.builder()
            .definitionId(UUID.randomUUID()).instanceId(color == PieceColor.WHITE ? "WR" : "BR")
            .name("Rook").svgKey("standard_rook").color(color)
            .isKing(false).isStandard(true)
            .movementRules(rookRules()).captureRules(null).hasMoved(false).build();
    }

    public static BoardPiece bishop(PieceColor color) {
        return BoardPiece.builder()
            .definitionId(UUID.randomUUID()).instanceId(color == PieceColor.WHITE ? "WB" : "BB")
            .name("Bishop").svgKey("standard_bishop").color(color)
            .isKing(false).isStandard(true)
            .movementRules(bishopRules()).captureRules(null).hasMoved(false).build();
    }

    public static BoardPiece knight(PieceColor color) {
        return BoardPiece.builder()
            .definitionId(UUID.randomUUID()).instanceId(color == PieceColor.WHITE ? "WN" : "BN")
            .name("Knight").svgKey("standard_knight").color(color)
            .isKing(false).isStandard(true)
            .movementRules(knightRules()).captureRules(null).hasMoved(false).build();
    }

    public static BoardPiece pawn(PieceColor color) {
        return BoardPiece.builder()
            .definitionId(UUID.randomUUID()).instanceId(color == PieceColor.WHITE ? "WP" : "BP")
            .name("Pawn").svgKey("standard_pawn").color(color)
            .isKing(false).isStandard(true)
            .movementRules(pawnMoveRules()).captureRules(null).hasMoved(false).build();
    }

    public static BoardPiece movedPawn(PieceColor color) {
        return pawn(color).withHasMoved(true);
    }

    // empty board with just two kings
    public static BoardState emptyBoard(PieceColor turn) {
        Map<String, BoardPiece> squares = new HashMap<>();
        return new BoardState(squares, turn,
            false, false, false, false,
            null, 0, 1, List.of(), List.of());
    }

    // standard starting position
    public static BoardState standardStart() {
        Map<String, BoardPiece> sq = new HashMap<>();

        // white pieces
        sq.put("a1", rook(PieceColor.WHITE));   sq.put("h1", rook(PieceColor.WHITE));
        sq.put("b1", knight(PieceColor.WHITE)); sq.put("g1", knight(PieceColor.WHITE));
        sq.put("c1", bishop(PieceColor.WHITE)); sq.put("f1", bishop(PieceColor.WHITE));
        sq.put("d1", queen(PieceColor.WHITE));  sq.put("e1", king(PieceColor.WHITE));
        for (char f = 'a'; f <= 'h'; f++) sq.put(f + "2", pawn(PieceColor.WHITE));

        // black pieces
        sq.put("a8", rook(PieceColor.BLACK));   sq.put("h8", rook(PieceColor.BLACK));
        sq.put("b8", knight(PieceColor.BLACK)); sq.put("g8", knight(PieceColor.BLACK));
        sq.put("c8", bishop(PieceColor.BLACK)); sq.put("f8", bishop(PieceColor.BLACK));
        sq.put("d8", queen(PieceColor.BLACK));  sq.put("e8", king(PieceColor.BLACK));
        for (char f = 'a'; f <= 'h'; f++) sq.put(f + "7", pawn(PieceColor.BLACK));

        return new BoardState(sq, PieceColor.WHITE,
            true, true, true, true,
            null, 0, 1, List.of(), new ArrayList<>());
    }
}
