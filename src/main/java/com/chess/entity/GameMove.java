package com.chess.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_moves")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private User player;

    @Column(nullable = false, length = 5)
    private String color;

    @Column(name = "from_square", nullable = false, length = 5)
    private String fromSquare;

    @Column(name = "to_square", nullable = false, length = 5)
    private String toSquare;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_id")
    private Piece piece;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_piece_id")
    private Piece promotionPiece;

    @Column(name = "is_capture", nullable = false)
    @Builder.Default
    private boolean isCapture = false;

    @Column(name = "is_check", nullable = false)
    @Builder.Default
    private boolean isCheck = false;

    @Column(name = "is_checkmate", nullable = false)
    @Builder.Default
    private boolean isCheckmate = false;

    @Column(name = "is_castle_kingside", nullable = false)
    @Builder.Default
    private boolean isCastleKingside = false;

    @Column(name = "is_castle_queenside", nullable = false)
    @Builder.Default
    private boolean isCastleQueenside = false;

    @Column(name = "san_notation", length = 20)
    private String sanNotation;

    @Type(JsonBinaryType.class)
    @Column(name = "board_state_after", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> boardStateAfter = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
