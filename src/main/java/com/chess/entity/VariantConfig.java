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
@Table(name = "variant_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VariantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> pieces = List.of();

    @Type(JsonBinaryType.class)
    @Column(name = "starting_position", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> startingPosition = Map.of();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> barriers = List.of();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
