package com.chess.repository;

import com.chess.entity.VariantConfig;
import com.chess.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VariantConfigRepository extends JpaRepository<VariantConfig, UUID> {
    List<VariantConfig> findByOwner(User owner);
    Page<VariantConfig> findByIsPublicTrue(Pageable pageable);
}
