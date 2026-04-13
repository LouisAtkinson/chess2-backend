package com.chess.websocket;

import com.chess.dto.request.GameRequests.MakeMoveRequest;
import com.chess.service.ComputerGameService;
import com.chess.dto.response.Responses.*;
import com.chess.engine.GameEngineService;
import com.chess.entity.Game;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.GameMoveRepository;
import com.chess.repository.GameRepository;
import com.chess.security.UserPrincipal;
import com.chess.service.GameService;
import com.chess.websocket.WsMessages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    private final GameEngineService engineService;
    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final ComputerGameService computerGameService;

    @MessageMapping("/game/{gameId}/join")
    public void joinGame(
            @DestinationVariable UUID gameId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            GameResponse game = gameService.getGame(gameId);
            broadcastToGame(gameId, WsEvent.of(EventType.PLAYER_JOINED, game));
        } catch (Exception e) {
            sendError(principal, gameId, e.getMessage());
        }
    }

    @MessageMapping("/game/{gameId}/move")
    public void makeMove(
            @DestinationVariable UUID gameId,
            @Payload WsMoveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            MakeMoveRequest moveReq = new MakeMoveRequest();
            moveReq.setFrom(request.getFrom());
            moveReq.setTo(request.getTo());
            moveReq.setPromotionPieceId(request.getPromotionPieceId());

            GameResponse updatedGame = gameService.applyMove(principal, gameId, moveReq);

            // build the move payload from the last move in history
            var history = updatedGame.getMoveHistory();
            var lastMove = history.isEmpty() ? null : history.get(history.size() - 1);

            MovePayload payload = MovePayload.builder()
                .from(request.getFrom())
                .to(request.getTo())
                .isCapture(lastMove != null && Boolean.TRUE.equals(lastMove.get("isCapture")))
                .isCheck(lastMove != null && Boolean.TRUE.equals(lastMove.get("isCheck")))
                .isCheckmate(updatedGame.getStatus() == Game.Status.CHECKMATE)
                .sanNotation(lastMove != null ? (String) lastMove.get("san") : null)
                .boardState(updatedGame.getBoardState())
                .turn(updatedGame.getTurn())
                .moveNumber(history.size())
                .build();

            broadcastToGame(gameId, WsEvent.of(EventType.OPPONENT_MOVE, payload));

            // if vs computer and still active, trigger computer move asynchronously
            if (updatedGame.getStatus() == Game.Status.ACTIVE
                    && updatedGame.getGameType() == Game.GameType.COMPUTER) {
                final UUID gId = gameId;
                computerGameService.makeComputerMove(gId, computerGame -> {
                    GameResponse computerResp = com.chess.dto.response.Responses.GameResponse.from(computerGame);
                    var history = computerResp.getMoveHistory();
                    var lastComputerMove = history.isEmpty() ? null : history.get(history.size() - 1);
                    MovePayload computerPayload = MovePayload.builder()
                        .from(lastComputerMove != null ? (String) lastComputerMove.get("from") : "")
                        .to(lastComputerMove != null ? (String) lastComputerMove.get("to") : "")
                        .isCapture(lastComputerMove != null && Boolean.TRUE.equals(lastComputerMove.get("isCapture")))
                        .isCheck(lastComputerMove != null && Boolean.TRUE.equals(lastComputerMove.get("isCheck")))
                        .isCheckmate(computerGame.getStatus() == Game.Status.CHECKMATE)
                        .sanNotation(lastComputerMove != null ? (String) lastComputerMove.get("san") : "")
                        .boardState(computerResp.getBoardState())
                        .turn(computerResp.getTurn())
                        .moveNumber(history.size())
                        .build();
                    broadcastToGame(gId, WsEvent.of(EventType.OPPONENT_MOVE, computerPayload));
                    if (computerGame.getStatus() != Game.Status.ACTIVE && computerGame.getStatus() != Game.Status.WAITING) {
                        broadcastToGame(gId, WsEvent.of(EventType.GAME_END,
                            GameEndPayload.builder()
                                .status(computerGame.getStatus().name())
                                .result(computerGame.getResult() != null ? computerGame.getResult().name() : null)
                                .reason(computerGame.getResultReason() != null ? computerGame.getResultReason().name() : null)
                                .build()));
                    }
                });
            }

            // if game ended, also send a GAME_END event
            if (updatedGame.getStatus() != Game.Status.ACTIVE
                    && updatedGame.getStatus() != Game.Status.WAITING) {
                GameEndPayload endPayload = GameEndPayload.builder()
                    .status(updatedGame.getStatus().name())
                    .result(updatedGame.getResult() != null ? updatedGame.getResult().name() : null)
                    .reason(updatedGame.getResultReason() != null ? updatedGame.getResultReason().name() : null)
                    .build();
                broadcastToGame(gameId, WsEvent.of(EventType.GAME_END, endPayload));
            }

        } catch (InvalidMoveException e) {
            sendError(principal, gameId, e.getMessage());
        } catch (BadRequestException e) {
            sendError(principal, gameId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing move in game {}", gameId, e);
            sendError(principal, gameId, "An error occurred processing your move");
        }
    }

    @MessageMapping("/game/{gameId}/chat")
    public void sendChat(
            @DestinationVariable UUID gameId,
            @Payload WsChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            ChatMessageResponse saved = gameService.sendChatMessage(
                principal, gameId, request.getMessage());
            broadcastToGame(gameId, WsEvent.of(EventType.CHAT_MESSAGE, saved));
        } catch (ForbiddenException | BadRequestException e) {
            sendError(principal, gameId, e.getMessage());
        } catch (Exception e) {
            log.error("Error sending chat in game {}", gameId, e);
            sendError(principal, gameId, "Failed to send message");
        }
    }

    @MessageMapping("/game/{gameId}/draw-offer")
    public void handleDrawOffer(
            @DestinationVariable UUID gameId,
            @Payload WsDrawOfferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Game game = gameService.findGame(gameId);
            if (!gameService.isParticipant(principal, game)) {
                throw new ForbiddenException("Not a participant");
            }

            if (request.isAccept()) {
                // accept a standing draw offer, end the game
                game.setStatus(Game.Status.DRAW);
                game.setResult(Game.Result.DRAW);
                game.setResultReason(Game.ResultReason.AGREEMENT);
                gameRepository.save(game);

                broadcastToGame(gameId, WsEvent.of(EventType.GAME_END,
                    GameEndPayload.builder()
                        .status("DRAW").result("DRAW").reason("AGREEMENT").build()));
            } else {
                // extend a draw offer to the opponent
                broadcastToGame(gameId, WsEvent.of(EventType.DRAW_OFFER, principal.getUsername()));
            }
        } catch (ForbiddenException e) {
            sendError(principal, gameId, e.getMessage());
        } catch (Exception e) {
            log.error("Error handling draw offer in game {}", gameId, e);
            sendError(principal, gameId, "Failed to process draw offer");
        }
    }

    @MessageMapping("/game/{gameId}/resign")
    public void resign(
            @DestinationVariable UUID gameId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            GameResponse game = gameService.resign(principal, gameId);
            broadcastToGame(gameId, WsEvent.of(EventType.GAME_END,
                GameEndPayload.builder()
                    .status(game.getStatus().name())
                    .result(game.getResult() != null ? game.getResult().name() : null)
                    .reason("RESIGNATION")
                    .build()));
        } catch (Exception e) {
            sendError(principal, gameId, e.getMessage());
        }
    }

    public void broadcastToGame(UUID gameId, WsEvent event) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, event);
    }

    private void sendError(UserPrincipal principal, UUID gameId, String message) {
        messagingTemplate.convertAndSendToUser(
            principal.getUsername(),
            "/queue/game/" + gameId + "/errors",
            WsEvent.of(EventType.ERROR, message)
        );
    }
}
