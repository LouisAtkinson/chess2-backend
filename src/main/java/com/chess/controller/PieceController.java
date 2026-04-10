package com.chess.controller;

import com.chess.dto.request.PieceRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.security.UserPrincipal;
import com.chess.service.PieceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pieces")
@RequiredArgsConstructor
public class PieceController {

    private final PieceService pieceService;

    @PostMapping
    public ResponseEntity<PieceResponse> createPiece(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePieceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pieceService.createPiece(principal, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PieceResponse> getPiece(@PathVariable UUID id) {
        return ResponseEntity.ok(pieceService.getPiece(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<PieceResponse>> getMyPieces(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pieceService.getMyPieces(principal));
    }

    @GetMapping("/standard")
    public ResponseEntity<List<PieceResponse>> getStandardPieces() {
        return ResponseEntity.ok(pieceService.getStandardPieces());
    }

    @GetMapping("/public")
    public ResponseEntity<PageResponse<PieceResponse>> getPublicPieces(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(pieceService.getPublicPieces(query, pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PieceResponse> updatePiece(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePieceRequest request) {
        return ResponseEntity.ok(pieceService.updatePiece(principal, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePiece(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(pieceService.deletePiece(principal, id));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<MessageResponse> savePiece(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(pieceService.savePieceToLibrary(principal, id));
    }

    @GetMapping("/saved")
    public ResponseEntity<List<PieceResponse>> getSavedPieces(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pieceService.getSavedPieces(principal));
    }
}
