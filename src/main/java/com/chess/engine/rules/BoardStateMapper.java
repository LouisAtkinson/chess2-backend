package com.chess.engine.rules;

import com.chess.engine.model.*;
import com.chess.entity.Piece;
import com.chess.repository.PieceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;


// converts between JSONB board state stored in DB and engine's BoardState object
@Component
@RequiredArgsConstructor
public class BoardStateMapper {

    private final PieceRepository pieceRepository;

    // standard piece UUIDs, matching schema.sql seed
    private static final Map<String, String> STANDARD_CODE_TO_UUID = Map.of(
        "WK", "00000000-0000-0000-0000-000000000001",
        "BK", "00000000-0000-0000-0000-000000000001",
        "WQ", "00000000-0000-0000-0000-000000000002",
        "BQ", "00000000-0000-0000-0000-000000000002",
        "WR", "00000000-0000-0000-0000-000000000003",
        "BR", "00000000-0000-0000-0000-000000000003",
        "WB", "00000000-0000-0000-0000-000000000004",
        "BB", "00000000-0000-0000-0000-000000000004",
        "WN", "00000000-0000-0000-0000-000000000005",
        "BN", "00000000-0000-0000-0000-000000000005"
    );
    private static final Map<String, String> PAWN_CODE_TO_UUID = Map.of(
        "WP", "00000000-0000-0000-0000-000000000006",
        "BP", "00000000-0000-0000-0000-000000000006"
    );

    @SuppressWarnings("unchecked")
    public BoardState fromDb(Map<String, Object> boardStateJson,
                              Map<String, Object> variantConfig,
                              String turnStr,
                              int halfMoveClock,
                              int fullMoveNumber,
                              List<String> positionHistory) {

        // load piece definitions (standard + any custom from variant)
        Map<String, Piece> pieceCache = buildPieceCache(variantConfig);

        List<Barrier> barriers = parseBarriers(variantConfig);

        Map<String, Object> squaresJson = (Map<String, Object>) boardStateJson.getOrDefault("squares", Map.of());
        Map<String, BoardPiece> squares = new HashMap<>();
        for (Map.Entry<String, Object> entry : squaresJson.entrySet()) {
            String algebraic = entry.getKey();
            String code = entry.getValue().toString();
            BoardPiece bp = resolvePiece(code, pieceCache);
            if (bp != null) squares.put(algebraic, bp);
        }

        Map<String, Object> castling = (Map<String, Object>)
            boardStateJson.getOrDefault("castlingRights", Map.of());
        boolean wk = boolFrom(castling, "whiteKingside", true);
        boolean wq = boolFrom(castling, "whiteQueenside", true);
        boolean bk = boolFrom(castling, "blackKingside", true);
        boolean bq = boolFrom(castling, "blackQueenside", true);

        String ep = (String) boardStateJson.get("enPassantTarget");

        return new BoardState(
            squares,
            PieceColor.fromString(turnStr),
            wk, wq, bk, bq,
            ep,
            halfMoveClock,
            fullMoveNumber,
            barriers,
            positionHistory != null ? positionHistory : new ArrayList<>()
        );
    }

    // convert a BoardState back to the JSONB map for DB storage.
    public Map<String, Object> toDb(BoardState state) {
        Map<String, String> squaresJson = new LinkedHashMap<>();
        for (Map.Entry<String, BoardPiece> entry : state.getSquares().entrySet()) {
            squaresJson.put(entry.getKey(), pieceToCode(entry.getValue()));
        }

        Map<String, Object> castling = new LinkedHashMap<>();
        castling.put("whiteKingside", state.isWhiteKingsideCastle());
        castling.put("whiteQueenside", state.isWhiteQueensideCastle());
        castling.put("blackKingside", state.isBlackKingsideCastle());
        castling.put("blackQueenside", state.isBlackQueensideCastle());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("squares", squaresJson);
        result.put("castlingRights", castling);
        result.put("enPassantTarget", state.getEnPassantTarget());
        return result;
    }

    private BoardPiece resolvePiece(String code, Map<String, Piece> cache) {
        // try standard code first
        String uuid = STANDARD_CODE_TO_UUID.getOrDefault(code,
            PAWN_CODE_TO_UUID.getOrDefault(code, code)); // fall back to treating code as UUID

        Piece def = cache.get(uuid);
        if (def == null) return null;

        PieceColor color = code.startsWith("W") || code.startsWith("w")
            ? PieceColor.WHITE : PieceColor.BLACK;

        if (code.length() > 4 && code.contains(":")) {
            String[] parts = code.split(":");
            color = "w".equalsIgnoreCase(parts[0]) ? PieceColor.WHITE : PieceColor.BLACK;
        }

        return BoardPiece.builder()
            .definitionId(def.getId())
            .instanceId(code)
            .name(def.getName())
            .svgKey(def.getSvgKey())
            .color(color)
            .isKing(def.getName().equalsIgnoreCase("king"))
            .isStandard(def.isStandard())
            .movementRules(def.getMovementRules())
            .captureRules(def.getCaptureRules())
            .hasMoved(false)
            .build();
    }

    private String pieceToCode(BoardPiece piece) {
        String prefix = piece.getColor() == PieceColor.WHITE ? "W" : "B";
        if (piece.isStandard()) {
            return switch (piece.getName().toLowerCase()) {
                case "king"   -> prefix + "K";
                case "queen"  -> prefix + "Q";
                case "rook"   -> prefix + "R";
                case "bishop" -> prefix + "B";
                case "knight" -> prefix + "N";
                case "pawn"   -> prefix + "P";
                default       -> piece.getInstanceId();
            };
        }
        return piece.getInstanceId(); // custom piece retains its code
    }

    private Map<String, Piece> buildPieceCache(Map<String, Object> variantConfig) {
        Map<String, Piece> cache = new HashMap<>();

        pieceRepository.findByIsStandardTrue()
            .forEach(p -> cache.put(p.getId().toString(), p));

        return cache;
    }

    @SuppressWarnings("unchecked")
    private List<Barrier> parseBarriers(Map<String, Object> variantConfig) {
        List<Barrier> barriers = new ArrayList<>();
        if (variantConfig == null) return barriers;

        Object rawBarriers = variantConfig.get("barriers");
        if (!(rawBarriers instanceof List<?> list)) return barriers;

        for (Object item : list) {
            if (item instanceof Map<?, ?> b) {
                Object fromObj = b.get("from");
                Object toObj   = b.get("to");
                if (fromObj instanceof List<?> fromList && toObj instanceof List<?> toList) {
                    int x1 = ((Number) fromList.get(0)).intValue();
                    int y1 = ((Number) fromList.get(1)).intValue();
                    int x2 = ((Number) toList.get(0)).intValue();
                    int y2 = ((Number) toList.get(1)).intValue();
                    if (Square.isValid(x1, y1) && Square.isValid(x2, y2)) {
                        barriers.add(Barrier.of(Square.of(x1, y1), Square.of(x2, y2)));
                    }
                }
            }
        }
        return barriers;
    }

    private boolean boolFrom(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }
}
