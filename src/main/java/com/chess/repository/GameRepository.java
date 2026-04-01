package com.chess.repository;

import com.chess.entity.Game;
import com.chess.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    @Query("""
        SELECT g FROM Game g
        WHERE g.whitePlayer = :user OR g.blackPlayer = :user
        ORDER BY g.createdAt DESC
        """)
    Page<Game> findByPlayer(@Param("user") User user, Pageable pageable);

    @Query("""
        SELECT g FROM Game g
        WHERE (g.whitePlayer = :user OR g.blackPlayer = :user)
        AND g.status = 'ACTIVE'
        """)
    List<Game> findActiveGamesByPlayer(@Param("user") User user);

    List<Game> findByStatus(Game.Status status);
}
