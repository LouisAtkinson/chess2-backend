package com.chess.engine.rules;

import com.chess.engine.model.Direction;
import com.chess.engine.model.MovementRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * parses raw JSONB movement rule maps into strongly-typed MovementRule objects
 *
 * expected JSONB shapes:
 *
 * SLIDE:
 * {
 *   "type": "SLIDE",
 *   "directions": ["N","NE",...],
 *   "maxDistance": 7,          // optional, defaults to 7
 *   "canCapture": true,
 *   "mustCapture": false,
 *   "firstMoveOnly": false,    // optional
 *   "firstMoveDouble": false   // optional — pawn double push
 * }
 *
 * JUMP:
 * {
 *   "type": "JUMP",
 *   "offsets": [[1,2],[2,1],...],
 *   "canCapture": true,
 *   "mustCapture": false,
 *   "firstMoveOnly": false
 * }
 */
@Component
public class MovementRuleParser {

    public List<MovementRule> parse(List<Map<String, Object>> rawRules) {
        if (rawRules == null) return List.of();
        List<MovementRule> result = new ArrayList<>();
        for (Map<String, Object> raw : rawRules) {
            result.add(parseRule(raw));
        }
        return result;
    }

    private MovementRule parseRule(Map<String, Object> raw) {
        String type = str(raw, "type").toUpperCase();
        boolean canCapture = bool(raw, "canCapture", true);
        boolean mustCapture = bool(raw, "mustCapture", false);
        boolean firstMoveOnly = bool(raw, "firstMoveOnly", false);
        boolean firstMoveDouble = bool(raw, "firstMoveDouble", false);

        return switch (type) {
            case "SLIDE" -> parseSlide(raw, canCapture, mustCapture, firstMoveOnly, firstMoveDouble);
            case "JUMP"  -> parseJump(raw, canCapture, mustCapture, firstMoveOnly);
            default -> throw new IllegalArgumentException("Unknown rule type: " + type);
        };
    }

    private MovementRule parseSlide(Map<String, Object> raw,
                                    boolean canCapture, boolean mustCapture,
                                    boolean firstMoveOnly, boolean firstMoveDouble) {
        @SuppressWarnings("unchecked")
        List<String> dirStrings = (List<String>) raw.get("directions");
        List<Direction> directions = new ArrayList<>();
        if (dirStrings != null) {
            for (String d : dirStrings) directions.add(Direction.fromString(d));
        }

        int maxDistance = intVal(raw, "maxDistance", 7);

        return MovementRule.builder()
            .type(MovementRule.RuleType.SLIDE)
            .directions(directions)
            .maxDistance(maxDistance)
            .canCapture(canCapture)
            .mustCapture(mustCapture)
            .firstMoveOnly(firstMoveOnly)
            .firstMoveDouble(firstMoveDouble)
            .offsets(List.of())
            .build();
    }

    private MovementRule parseJump(Map<String, Object> raw,
                                   boolean canCapture, boolean mustCapture,
                                   boolean firstMoveOnly) {
        @SuppressWarnings("unchecked")
        List<List<Number>> rawOffsets = (List<List<Number>>) raw.get("offsets");
        List<int[]> offsets = new ArrayList<>();
        if (rawOffsets != null) {
            for (List<Number> pair : rawOffsets) {
                offsets.add(new int[]{pair.get(0).intValue(), pair.get(1).intValue()});
            }
        }

        return MovementRule.builder()
            .type(MovementRule.RuleType.JUMP)
            .offsets(offsets)
            .canCapture(canCapture)
            .mustCapture(mustCapture)
            .firstMoveOnly(firstMoveOnly)
            .firstMoveDouble(false)
            .directions(List.of())
            .maxDistance(0)
            .build();
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) throw new IllegalArgumentException("Missing required field: " + key);
        return v.toString();
    }

    private boolean bool(Map<String, Object> map, String key, boolean defaultVal) {
        Object v = map.get(key);
        if (v == null) return defaultVal;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    private int intVal(Map<String, Object> map, String key, int defaultVal) {
        Object v = map.get(key);
        if (v == null) return defaultVal;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }
}
