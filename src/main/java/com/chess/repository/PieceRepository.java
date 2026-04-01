package com.chess.repository;

import com.chess.entity.Piece;
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
public interface PieceRepository extends JpaRepository<Piece, UUID> {

    List<Piece> findByOwner(User owner);

    List<Piece> findByIsStandardTrue();

    Page<Piece> findByIsPublicTrue(Pageable pageable);

    @Query("""
        SELECT p FROM Piece p
        WHERE p.isPublic = TRUE
        AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    Page<Piece> searchPublic(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT p FROM Piece p
        JOIN user_saved_pieces usp ON usp.piece_id = p.id
        WHERE usp.user_id = :userId
        """)
    List<Piece> findSavedByUser(@Param("userId") UUID userId);

    boolean existsByIdAndOwner(UUID id, User owner);
}
