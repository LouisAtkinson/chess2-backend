package com.chess.service;

import com.chess.engine.GameEngineService;
import com.chess.entity.Game;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.GameMoveRepository;
import com.chess.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComputerGameService {

    private final StockfishService stockfishService;
    private final GameEngineService engineService;
    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;

    private static final int COMPUTER_DEPTH = 12;

    @Async
    @Transactional
    public void makeComputerMove(UUID gameId, java.util.function.Consumer<Game> onComplete) {
        try {
            Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found"));

            if (game.getStatus() != Game.Status.ACTIVE) return;
            if (game.getGameType() != Game.GameType.COMPUTER) return;

            var state = engineService.buildStateForFen(game);
            String fen = engineService.getFenForState(state);

            log.info("Computer move requested for game {} with FEN: {}", gameId, fen);

            Optional<String> bestMove = stockfishService.getBestMove(fen, COMPUTER_DEPTH);
            if (bestMove.isEmpty()) {
                log.warn("Stockfish returned no move for game {}", gameId);
                return;
            }

            // parse UCI move
            String uciMove = bestMove.get();
            String from = uciMove.substring(0, 2);
            String to   = uciMove.substring(2, 4);
            String promotionChar = uciMove.length() == 5 ? uciMove.substring(4) : null;

            UUID promotionPieceId = resolvePromotionPieceId(promotionChar);

            List<String> positionHistory = gameMoveRepository
                .findByGameIdOrderByMoveNumberAsc(gameId).stream()
                .map(m -> {
                    Object key = m.getBoardStateAfter().get("positionKey");
                    return key != null ? key.toString() : "";
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

            // apply move
            var result = engineService.processMove(game, from, to, promotionPieceId, positionHistory);

            gameMoveRepository.save(result.persistedMove());

            // update game entity
            game.setBoardState(result.newBoardStateJson());
            game.setTurn(result.newState().getTurn().lower());
            game.setHalfMoveClock(result.newState().getHalfMoveClock());
            game.setFullMoveNumber(result.newState().getFullMoveNumber());
            game.setStatus(result.newStatus());
            game.setResult(result.result());
            game.setResultReason(result.resultReason());
            game.setLastMoveAt(OffsetDateTime.now());

            Map<String, Object> moveEntry = new LinkedHashMap<>();
            moveEntry.put("from", from);
            moveEntry.put("to", to);
            moveEntry.put("san", result.appliedMove().getSanNotation());
            moveEntry.put("color", result.appliedMove().getPiece().getColor().lower());
            moveEntry.put("isCapture", result.appliedMove().isCapture());
            moveEntry.put("isCheck", result.appliedMove().isCheck());
            List<Map<String, Object>> history = new ArrayList<>(game.getMoveHistory());
            history.add(moveEntry);
            game.setMoveHistory(history);

            game = gameRepository.save(game);
            log.info("Computer played {} → {} in game {}", from, to, gameId);

            onComplete.accept(game);

        } catch (Exception e) {
            log.error("Computer move failed for game {}: {}", gameId, e.getMessage(), e);
        }
    }

    private UUID resolvePromotionPieceId(String promotionChar) {
        if (promotionChar == null) return null;
        return switch (promotionChar.toLowerCase()) {
            case "q" -> UUID.fromString("00000000-0000-0000-0000-000000000002"); // queen
            case "r" -> UUID.fromString("00000000-0000-0000-0000-000000000003"); // rook
            case "b" -> UUID.fromString("00000000-0000-0000-0000-000000000004"); // bishop
            case "n" -> UUID.fromString("00000000-0000-0000-0000-000000000005"); // knight
            default  -> UUID.fromString("00000000-0000-0000-0000-000000000002"); // default queen
        };
    }
}
