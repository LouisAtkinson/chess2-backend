package com.chess.engine.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode(of = {"instanceId", "color"})
public class BoardPiece {
    private final UUID definitionId;
    private final String instanceId;

    private final String name;
    private final String svgKey;
    private final PieceColor color;
    private final boolean isKing;
    private final boolean isStandard;

    private final List<Map<String, Object>> movementRules;
    private final List<Map<String, Object>> captureRules;

    private final boolean hasMoved;

    public List<Map<String, Object>> effectiveCaptureRules() {
        return captureRules != null ? captureRules : movementRules;
    }

    public BoardPiece withHasMoved(boolean moved) {
        return BoardPiece.builder()
            .definitionId(definitionId)
            .instanceId(instanceId)
            .name(name)
            .svgKey(svgKey)
            .color(color)
            .isKing(isKing)
            .isStandard(isStandard)
            .movementRules(movementRules)
            .captureRules(captureRules)
            .hasMoved(moved)
            .build();
    }
}
