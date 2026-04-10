package com.chess.controller;

import com.chess.dto.response.Responses.*;
import com.chess.entity.VariantConfig;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.VariantConfigRepository;
import com.chess.security.UserPrincipal;
import com.chess.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/variant-configs")
@RequiredArgsConstructor
public class VariantConfigController {

    private final VariantConfigRepository variantConfigRepository;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {

        var owner = userService.getUser(principal);

        @SuppressWarnings("unchecked")
        var pieces = (List<Map<String, Object>>) body.getOrDefault("pieces", List.of());
        @SuppressWarnings("unchecked")
        var startingPosition = (Map<String, Object>) body.getOrDefault("startingPosition", Map.of());
        @SuppressWarnings("unchecked")
        var barriers = (List<Map<String, Object>>) body.getOrDefault("barriers", List.of());

        var vc = VariantConfig.builder()
            .name(body.getOrDefault("name", "Untitled").toString())
            .description(body.get("description") != null ? body.get("description").toString() : null)
            .owner(owner)
            .isPublic(Boolean.TRUE.equals(body.get("isPublic")))
            .pieces(pieces)
            .startingPosition(startingPosition)
            .barriers(barriers)
            .build();

        var saved = variantConfigRepository.save(vc);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", saved.getId().toString(),
            "name", saved.getName()
        ));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Map<String, Object>>> getMine(
            @AuthenticationPrincipal UserPrincipal principal) {
        var owner = userService.getUser(principal);
        var configs = variantConfigRepository.findByOwner(owner);
        return ResponseEntity.ok(configs.stream().map(this::toMap).toList());
    }

    @GetMapping("/public")
    public ResponseEntity<List<Map<String, Object>>> getPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<VariantConfig> result = variantConfigRepository.findByIsPublicTrue(pageable);
        return ResponseEntity.ok(result.getContent().stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable UUID id) {
        var vc = variantConfigRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Variant config not found"));
        return ResponseEntity.ok(toFullMap(vc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        var vc = variantConfigRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Variant config not found"));
        if (!vc.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Cannot delete this variant");
        }
        variantConfigRepository.delete(vc);
        return ResponseEntity.ok(MessageResponse.of("Deleted"));
    }

    private Map<String, Object> toMap(VariantConfig vc) {
        return Map.of(
            "id", vc.getId().toString(),
            "name", vc.getName(),
            "description", vc.getDescription() != null ? vc.getDescription() : "",
            "isPublic", vc.isPublic(),
            "createdAt", vc.getCreatedAt().toString()
        );
    }

    private Map<String, Object> toFullMap(VariantConfig vc) {
        return Map.of(
            "id", vc.getId().toString(),
            "name", vc.getName(),
            "description", vc.getDescription() != null ? vc.getDescription() : "",
            "isPublic", vc.isPublic(),
            "pieces", vc.getPieces(),
            "startingPosition", vc.getStartingPosition(),
            "barriers", vc.getBarriers(),
            "createdAt", vc.getCreatedAt().toString()
        );
    }
}
