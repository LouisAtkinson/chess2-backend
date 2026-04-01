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
@Table(name = "games")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Game {

    public enum Status {
        WAITING, ACTIVE, CHECKMATE, STALEMATE, DRAW, RESIGNED, ABANDONED
    }

    public enum Result {
        WHITE_WINS, BLACK_WINS, DRAW
    }

    public enum ResultReason {
        CHECKMATE, STALEMATE, RESIGNATION, TIMEOUT, AGREEMENT, FIFTY_MOVE, THREEFOLD_REPETITION
    }

    public enum GameType {
        STANDARD, CUSTOM, COMPUTER
    }

    public enum GameMode {
        REALTIME, ASYNC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Result result;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_reason", length = 50)
    private ResultReason resultReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 20)
    @Builder.Default
    private GameType gameType = GameType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GameMode mode = GameMode.REALTIME;

    @Type(JsonBinaryType.class)
    @Column(name = "board_state", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> boardState = Map.of();

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String turn = "white";

    @Type(JsonBinaryType.class)
    @Column(name = "variant_config", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> variantConfig = Map.of();

    @Type(JsonBinaryType.class)
    @Column(name = "move_history", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> moveHistory = List.of();

    @Column(name = "half_move_clock", nullable = false)
    @Builder.Default
    private int halfMoveClock = 0;

    @Column(name = "full_move_number", nullable = false)
    @Builder.Default
    private int fullMoveNumber = 1;

    @Column(name = "white_time_remaining_ms")
    private Long whiteTimeRemainingMs;

    @Column(name = "black_time_remaining_ms")
    private Long blackTimeRemainingMs;

    @Column(name = "last_move_at")
    private OffsetDateTime lastMoveAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
