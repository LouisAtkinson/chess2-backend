package com.chess.service;

import com.chess.dto.request.GameRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.engine.GameEngineService;
import com.chess.entity.*;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.*;
import com.chess.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final GameChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final VariantConfigRepository variantConfigRepository;
    private final EmailService emailService;
    private final GameEngineService engineService;

    private static final Map<String, Object> STANDARD_STARTING_BOARD = buildStandardBoard();

    private static Map<String, Object> buildStandardBoard() {
        // piece UUIDs match seed data
        Map<String, String> squares = new LinkedHashMap<>();
        // black pieces
        squares.put("a8", "BR"); squares.put("b8", "BN"); squares.put("c8", "BB");
        squares.put("d8", "BQ"); squares.put("e8", "BK"); squares.put("f8", "BB");
        squares.put("g8", "BN"); squares.put("h8", "BR");
        for (char c = 'a'; c <= 'h'; c++) squares.put(c + "7", "BP");
        // white pieces
        for (char c = 'a'; c <= 'h'; c++) squares.put(c + "2", "WP");
        squares.put("a1", "WR"); squares.put("b1", "WN"); squares.put("c1", "WB");
        squares.put("d1", "WQ"); squares.put("e1", "WK"); squares.put("f1", "WB");
        squares.put("g1", "WN"); squares.put("h1", "WR");

        Map<String, Object> board = new LinkedHashMap<>();
        board.put("squares", squares);
        board.put("enPassantTarget", null);
        board.put("castlingRights", Map.of(
            "whiteKingside", true, "whiteQueenside", true,
            "blackKingside", true, "blackQueenside", true
        ));
        return board;
    }

    @Transactional
    public GameResponse createGame(UserPrincipal principal, CreateGameRequest request) {
        User creator = userService.getUser(principal);

        // assign colours randomly for standard games
        boolean creatorIsWhite = new Random().nextBoolean();
        User whitePlayer = creatorIsWhite ? creator : null;
        User blackPlayer = creatorIsWhite ? null : creator;

        User opponent = null;
        if (request.getOpponentId() != null) {
            opponent = userService.getUser(request.getOpponentId());
            if (creatorIsWhite) {
                blackPlayer = opponent;
            } else {
                whitePlayer = opponent;
            }
        }

        Map<String, Object> variantConfig = Map.of();
        Map<String, Object> boardState = STANDARD_STARTING_BOARD;

        if (request.getVariantConfigId() != null) {
            VariantConfig vc = variantConfigRepository.findById(request.getVariantConfigId())
                .orElseThrow(() -> new NotFoundException("Variant config not found"));
            variantConfig = Map.of(
                "name", vc.getName(),
                "pieces", vc.getPieces(),
                "barriers", vc.getBarriers()
            );
            boardState = vc.getStartingPosition();
        }

        Game game = Game.builder()
            .whitePlayer(whitePlayer)
            .blackPlayer(blackPlayer)
            .status(opponent != null ? Game.Status.ACTIVE : Game.Status.WAITING)
            .gameType(request.getGameType())
            .mode(request.getMode())
            .boardState(new HashMap<>(boardState))
            .variantConfig(new HashMap<>(variantConfig))
            .moveHistory(new ArrayList<>())
            .whiteTimeRemainingMs(request.getTimeLimitMs())
            .blackTimeRemainingMs(request.getTimeLimitMs())
            .build();

        game = gameRepository.save(game);

        // notify opponent if async mode and opponent set
        if (opponent != null && request.getMode() == Game.GameMode.ASYNC) {
            emailService.sendGameInviteNotification(opponent, creator, game);
        }

        return GameResponse.from(game);
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(UUID gameId) {
        Game game = findGame(gameId);
        return GameResponse.from(game);
    }

    @Transactional(readOnly = true)
    public PageResponse<GameSummaryResponse> getUserGames(UUID userId, Pageable pageable) {
        User user = userService.getUser(userId);
        Page<Game> page = gameRepository.findByPlayer(user, pageable);
        return PageResponse.<GameSummaryResponse>builder()
            .content(page.getContent().stream().map(GameSummaryResponse::from).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public List<GameSummaryResponse> getActiveGames(UserPrincipal principal) {
        User user = userService.getUser(principal);
        return gameRepository.findActiveGamesByPlayer(user).stream()
            .map(GameSummaryResponse::from)
            .toList();
    }

    @Transactional
    public GameResponse applyMove(UserPrincipal principal, UUID gameId, MakeMoveRequest request) {
        Game game = findGame(gameId);
        validatePlayerTurn(principal, game);

        // build position history from past moves for threefold repetition detection
        List<String> positionHistory = gameMoveRepository
            .findByGameIdOrderByMoveNumberAsc(gameId).stream()
            .map(m -> {
                Object key = m.getBoardStateAfter().get("positionKey");
                return key != null ? key.toString() : "";
            })
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toList());

        // run full engine pipeline
        GameEngineService.MoveResult engineResult = engineService.processMove(
            game,
            request.getFrom(),
            request.getTo(),
            request.getPromotionPieceId(),
            positionHistory
        );

        gameMoveRepository.save(engineResult.persistedMove());

        // update the game entity with new board state
        game.setBoardState(engineResult.newBoardStateJson());
        game.setTurn(engineResult.newState().getTurn().lower());
        game.setHalfMoveClock(engineResult.newState().getHalfMoveClock());
        game.setFullMoveNumber(engineResult.newState().getFullMoveNumber());
        game.setStatus(engineResult.newStatus());
        game.setResult(engineResult.result());
        game.setResultReason(engineResult.resultReason());
        game.setLastMoveAt(OffsetDateTime.now());

        // append to move history list in game entity
        Map<String, Object> moveEntry = new LinkedHashMap<>();
        moveEntry.put("from", request.getFrom());
        moveEntry.put("to", request.getTo());
        moveEntry.put("san", engineResult.appliedMove().getSanNotation());
        moveEntry.put("color", engineResult.appliedMove().getPiece().getColor().lower());
        moveEntry.put("isCapture", engineResult.appliedMove().isCapture());
        moveEntry.put("isCheck", engineResult.appliedMove().isCheck());
        List<Map<String, Object>> history = new ArrayList<>(game.getMoveHistory());
        history.add(moveEntry);
        game.setMoveHistory(history);

        game = gameRepository.save(game);

        // email notification for async games
        if (game.getMode() == Game.GameMode.ASYNC && game.getStatus() == Game.Status.ACTIVE) {
            User nextPlayer = "white".equals(game.getTurn())
                ? game.getWhitePlayer() : game.getBlackPlayer();
            if (nextPlayer != null) {
                emailService.sendYourTurnNotification(nextPlayer, game);
            }
        }

        return GameResponse.from(game);
    }

    @Transactional(readOnly = true)
    public List<String> getLegalMoves(UUID gameId, String square) {
        Game game = findGame(gameId);
        return engineService.getLegalMoveSquares(game, square);
    }

    @Transactional
    public GameResponse resign(UserPrincipal principal, UUID gameId) {
        Game game = findGame(gameId);
        boolean isWhite = isWhitePlayer(principal, game);

        game.setStatus(Game.Status.RESIGNED);
        game.setResult(isWhite ? Game.Result.BLACK_WINS : Game.Result.WHITE_WINS);
        game.setResultReason(Game.ResultReason.RESIGNATION);
        gameRepository.save(game);

        return GameResponse.from(game);
    }

    @Transactional
    public ChatMessageResponse sendChatMessage(UserPrincipal principal, UUID gameId, String message) {
        Game game = findGame(gameId);
        User sender = userService.getUser(principal);

        if (!isParticipant(principal, game)) {
            throw new ForbiddenException("You are not a participant in this game");
        }

        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }

        if (message.length() > 500) {
            throw new BadRequestException("Message too long (max 500 characters)");
        }

        GameChatMessage chatMessage = GameChatMessage.builder()
            .game(game)
            .sender(sender)
            .message(message.trim())
            .build();

        return ChatMessageResponse.from(chatMessageRepository.save(chatMessage));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(UserPrincipal principal, UUID gameId) {
        Game game = findGame(gameId);

        if (!isParticipant(principal, game)) {
            throw new ForbiddenException("You are not a participant in this game");
        }

        return chatMessageRepository.findByGameIdOrderByCreatedAtAsc(gameId).stream()
            .map(ChatMessageResponse::from)
            .toList();
    }

    public Game findGame(UUID gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new NotFoundException("Game not found"));
    }

    private void validatePlayerTurn(UserPrincipal principal, Game game) {
        if (game.getStatus() != Game.Status.ACTIVE) {
            throw new BadRequestException("Game is not active");
        }
        boolean isWhiteTurn = "white".equals(game.getTurn());
        boolean isWhitePlayer = isWhitePlayer(principal, game);
        boolean isBlackPlayer = isBlackPlayer(principal, game);

        if (isWhiteTurn && !isWhitePlayer) {
            throw new BadRequestException("It is not your turn");
        }
        if (!isWhiteTurn && !isBlackPlayer) {
            throw new BadRequestException("It is not your turn");
        }
    }

    private boolean isWhitePlayer(UserPrincipal principal, Game game) {
        return game.getWhitePlayer() != null
            && game.getWhitePlayer().getId().equals(principal.getId());
    }

    private boolean isBlackPlayer(UserPrincipal principal, Game game) {
        return game.getBlackPlayer() != null
            && game.getBlackPlayer().getId().equals(principal.getId());
    }

    public boolean isParticipant(UserPrincipal principal, Game game) {
        return isWhitePlayer(principal, game) || isBlackPlayer(principal, game);
    }
}
