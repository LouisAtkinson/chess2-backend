package com.chess.repository;

import com.chess.entity.GameMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameMoveRepository extends JpaRepository<GameMove, UUID> {
    List<GameMove> findByGameIdOrderByMoveNumberAsc(UUID gameId);
    long countByGameId(UUID gameId);
}
