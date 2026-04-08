package com.chess.engine;

import com.chess.engine.model.*;
import com.chess.engine.notation.FenGenerator;
import com.chess.engine.notation.SanGenerator;
import com.chess.engine.rules.*;
import com.chess.engine.validator.MoveValidator;
import com.chess.entity.Game;
import com.chess.entity.GameMove;
import com.chess.entity.Piece;
import com.chess.exception.GlobalExceptionHandler.InvalidMoveException;
import com.chess.repository.GameMoveRepository;
import com.chess.repository.PieceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameEngineService {

    private final BoardStateMapper boardStateMapper;
    private final MoveValidator moveValidator;
    private final MoveApplier moveApplier;
    private final CheckDetector checkDetector;
    private final FenGenerator fenGenerator;
    private final SanGenerator sanGenerator;
    private final GameMoveRepository gameMoveRepository;
    private final PieceRepository pieceRepository;

    public record MoveResult(
        Move appliedMove,
        BoardState newState,
        Map<String, Object> newBoardStateJson,
        Game.Status newStatus,
        Game.Result result,
        Game.ResultReason resultReason,
        String fen,
        GameMove persistedMove
    ) {}

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * validate and apply a player's move on the given game entity
     * returns a MoveResult containing everything needed to update the DB and
     * broadcast via websocket
     *
     * @param game             the current game entity (not yet mutated)
     * @param fromAlg          source square in algebraic notation (e.g. "e2")
     * @param toAlg            target square in algebraic notation (e.g. "e4")
     * @param promotionPieceId optional UUID of piece to promote to (nullable)
     * @param positionHistory  list of past FEN position keys for repetition detection
     */
    public MoveResult processMove(Game game,
                                   String fromAlg, String toAlg,
                                   UUID promotionPieceId,
                                   List<String> positionHistory) {

        BoardState state = boardStateMapper.fromDb(
            game.getBoardState(),
            game.getVariantConfig(),
            game.getTurn(),
            game.getHalfMoveClock(),
            game.getFullMoveNumber(),
            positionHistory
        );

        Square from, to;
        try {
            from = Square.fromAlgebraic(fromAlg);
            to   = Square.fromAlgebraic(toAlg);
        } catch (IllegalArgumentException e) {
            throw new InvalidMoveException("Invalid square: " + e.getMessage());
        }

        BoardPiece piece = state.pieceAt(from)
            .orElseThrow(() -> new InvalidMoveException("No piece at " + fromAlg));

        PieceColor currentTurn = state.getTurn();
        if (piece.getColor() != currentTurn) {
            throw new InvalidMoveException("That is not your piece");
        }

        Move validatedMove = moveValidator.validateMove(from, to, state);
        if (validatedMove == null) {
            throw new InvalidMoveException("Illegal move: " + fromAlg + " → " + toAlg);
        }

        BoardPiece promotionBoardPiece = null;
        if (validatedMove.isPromotion() && promotionPieceId != null) {
            promotionBoardPiece = resolvePromotionPiece(promotionPieceId, piece.getColor());
        } else if (validatedMove.isPromotion()) {
            // default promotion to queen
            promotionBoardPiece = resolveDefaultQueen(piece.getColor());
        }

        BoardState newState = moveApplier.apply(validatedMove, state, promotionBoardPiece);

        String fen = fenGenerator.generate(newState);
        String positionKey = fenGenerator.generatePositionKey(newState);

        boolean isCheck     = checkDetector.isInCheck(newState.getTurn(), newState);
        boolean isCheckmate = moveValidator.isCheckmate(newState);
        boolean isStalemate = moveValidator.isStalemate(newState);
        boolean isFifty     = moveValidator.isFiftyMoveRule(newState);
        boolean isInsuff    = moveValidator.isInsufficientMaterial(newState);

        long repetitionCount = positionHistory.stream()
            .filter(pk -> pk.equals(positionKey)).count() + 1;
        boolean isThreefold = repetitionCount >= 3;

        Move annotatedMove = validatedMove.withPostMoveFlags(isCheck, isCheckmate, isStalemate,
            sanGenerator.generate(validatedMove, state, newState));

        Game.Status newStatus = Game.Status.ACTIVE;
        Game.Result result = null;
        Game.ResultReason reason = null;

        if (isCheckmate) {
            newStatus = Game.Status.CHECKMATE;
            result = (currentTurn == PieceColor.WHITE) ? Game.Result.WHITE_WINS : Game.Result.BLACK_WINS;
            reason = Game.ResultReason.CHECKMATE;
        } else if (isStalemate) {
            newStatus = Game.Status.STALEMATE;
            result = Game.Result.DRAW;
            reason = Game.ResultReason.STALEMATE;
        } else if (isFifty) {
            newStatus = Game.Status.DRAW;
            result = Game.Result.DRAW;
            reason = Game.ResultReason.FIFTY_MOVE;
        } else if (isThreefold) {
            newStatus = Game.Status.DRAW;
            result = Game.Result.DRAW;
            reason = Game.ResultReason.THREEFOLD_REPETITION;
        } else if (isInsuff) {
            newStatus = Game.Status.DRAW;
            result = Game.Result.DRAW;
            reason = Game.ResultReason.STALEMATE;
        }

        long moveNumber = gameMoveRepository.countByGameId(game.getId()) + 1;
        GameMove gameMove = GameMove.builder()
            .game(game)
            .moveNumber((int) moveNumber)
            .color(currentTurn.lower())
            .fromSquare(fromAlg)
            .toSquare(toAlg)
            .isCapture(annotatedMove.isCapture())
            .isCheck(isCheck)
            .isCheckmate(isCheckmate)
            .isCastleKingside(annotatedMove.isCastleKingside())
            .isCastleQueenside(annotatedMove.isCastleQueenside())
            .sanNotation(annotatedMove.getSanNotation())
            .boardStateAfter(boardStateMapper.toDb(newState))
            .build();

        Map<String, Object> newBoardStateJson = boardStateMapper.toDb(newState);

        newState.getPositionHistory().add(positionKey);

        return new MoveResult(
            annotatedMove,
            newState,
            newBoardStateJson,
            newStatus,
            result,
            reason,
            fen,
            gameMove
        );
    }

    // get all legal moves for piece, used by front end to highlight squares
    public List<String> getLegalMoveSquares(Game game, String fromAlg) {
        BoardState state = boardStateMapper.fromDb(
            game.getBoardState(),
            game.getVariantConfig(),
            game.getTurn(),
            game.getHalfMoveClock(),
            game.getFullMoveNumber(),
            List.of()
        );

        try {
            Square from = Square.fromAlgebraic(fromAlg);
            return moveValidator.legalMovesFrom(from, state).stream()
                .map(m -> m.getTo().toAlgebraic())
                .toList();
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    private BoardPiece resolvePromotionPiece(UUID pieceId, PieceColor color) {
        Piece def = pieceRepository.findById(pieceId)
            .orElseThrow(() -> new InvalidMoveException("Promotion piece not found"));
        return pieceEntityToBoardPiece(def, color);
    }

    private BoardPiece resolveDefaultQueen(PieceColor color) {
        UUID queenId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Piece def = pieceRepository.findById(queenId)
            .orElseThrow(() -> new InvalidMoveException("Queen piece definition not found"));
        return pieceEntityToBoardPiece(def, color);
    }

    private BoardPiece pieceEntityToBoardPiece(Piece def, PieceColor color) {
        return BoardPiece.builder()
            .definitionId(def.getId())
            .instanceId((color == PieceColor.WHITE ? "W" : "B") + def.getName().substring(0, 1).toUpperCase())
            .name(def.getName())
            .svgKey(def.getSvgKey())
            .color(color)
            .isKing(def.getName().equalsIgnoreCase("king"))
            .isStandard(def.isStandard())
            .movementRules(def.getMovementRules())
            .captureRules(def.getCaptureRules())
            .hasMoved(true)
            .build();
    }

    public com.chess.engine.model.BoardState buildStateForFen(com.chess.entity.Game game) {
        return boardStateMapper.fromDb(
            game.getBoardState(),
            game.getVariantConfig(),
            game.getTurn(),
            game.getHalfMoveClock(),
            game.getFullMoveNumber(),
            java.util.List.of()
        );
    }

    public String getFenForState(com.chess.engine.model.BoardState state) {
        return fenGenerator.generate(state);
    }

}