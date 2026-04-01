package com.chess.repository;

import com.chess.entity.Friend;
import com.chess.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRepository extends JpaRepository<Friend, UUID> {

    Optional<Friend> findByRequesterAndAddressee(User requester, User addressee);

    @Query("""
        SELECT f FROM Friend f
        WHERE (f.requester = :user OR f.addressee = :user)
        AND f.status = 'ACCEPTED'
        """)
    List<Friend> findAcceptedFriendships(@Param("user") User user);

    @Query("""
        SELECT f FROM Friend f
        WHERE f.addressee = :user AND f.status = 'PENDING'
        """)
    List<Friend> findPendingRequests(@Param("user") User user);

    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
        FROM Friend f
        WHERE ((f.requester = :u1 AND f.addressee = :u2)
            OR (f.requester = :u2 AND f.addressee = :u1))
        AND f.status = 'ACCEPTED'
        """)
    boolean areFriends(@Param("u1") User u1, @Param("u2") User u2);

    @Query("""
        SELECT f FROM Friend f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
        AND (f.requester.id = :otherId OR f.addressee.id = :otherId)
        """)
    Optional<Friend> findFriendshipBetween(@Param("userId") UUID userId, @Param("otherId") UUID otherId);
}
