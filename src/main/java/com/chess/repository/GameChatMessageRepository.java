package com.chess.repository;

import com.chess.entity.GameChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameChatMessageRepository extends JpaRepository<GameChatMessage, UUID> {
    List<GameChatMessage> findByGameIdOrderByCreatedAtAsc(UUID gameId);
}
