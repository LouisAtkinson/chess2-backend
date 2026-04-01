package com.chess.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pieces")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Piece {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "svg_key", nullable = false, length = 100)
    private String svgKey;

    @Type(JsonBinaryType.class)
    @Column(name = "movement_rules", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> movementRules = List.of();

    @Type(JsonBinaryType.class)
    @Column(name = "capture_rules", columnDefinition = "jsonb")
    private List<Map<String, Object>> captureRules;  // null = same as movement

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Column(name = "is_standard", nullable = false)
    @Builder.Default
    private boolean isStandard = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
