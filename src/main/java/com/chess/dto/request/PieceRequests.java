package com.chess.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class PieceRequests {

    @Data
    public static class CreatePieceRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "SVG key is required")
        private String svgKey;

        @NotEmpty(message = "At least one movement rule is required")
        private List<Map<String, Object>> movementRules;

        private List<Map<String, Object>> captureRules; // null = same as movement

        private boolean isPublic = false;
    }

    @Data
    public static class UpdatePieceRequest {
        @Size(max = 100)
        private String name;
        private String svgKey;
        private List<Map<String, Object>> movementRules;
        private List<Map<String, Object>> captureRules;
        private Boolean isPublic;
    }
}
