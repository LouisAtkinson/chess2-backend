package com.chess.controller;

import com.chess.dto.request.GameRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.security.UserPrincipal;
import com.chess.service.GameService;
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
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(gameService.createGame(principal, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResponse> getGame(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(gameService.getGame(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<GameSummaryResponse>> getActiveGames(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(gameService.getActiveGames(principal));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<GameSummaryResponse>> getUserGames(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(gameService.getUserGames(userId, pageable));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<GameResponse> makeMove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody MakeMoveRequest request) {
        return ResponseEntity.ok(gameService.applyMove(principal, id, request));
    }

    @PostMapping("/{id}/resign")
    public ResponseEntity<GameResponse> resign(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(gameService.resign(principal, id));
    }

    @GetMapping("/{id}/legal-moves")
    public ResponseEntity<List<String>> getLegalMoves(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String square) {
        return ResponseEntity.ok(gameService.getLegalMoves(id, square));
    }

    @GetMapping("/{id}/chat")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(gameService.getChatHistory(principal, id));
    }
}
