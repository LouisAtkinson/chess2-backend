package com.chess.service;

import com.chess.dto.request.PieceRequests.*;
import com.chess.dto.response.Responses.*;
import com.chess.entity.Piece;
import com.chess.entity.User;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.PieceRepository;
import com.chess.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PieceService {

    private final PieceRepository pieceRepository;
    private final UserService userService;

    @Transactional
    public PieceResponse createPiece(UserPrincipal principal, CreatePieceRequest request) {
        User owner = userService.getUser(principal);
        Piece piece = Piece.builder()
            .name(request.getName())
            .svgKey(request.getSvgKey())
            .movementRules(request.getMovementRules())
            .captureRules(request.getCaptureRules())
            .owner(owner)
            .isPublic(request.isPublic())
            .isStandard(false)
            .build();
        return PieceResponse.from(pieceRepository.save(piece));
    }

    @Transactional(readOnly = true)
    public PieceResponse getPiece(UUID id) {
        Piece piece = pieceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Piece not found"));
        return PieceResponse.from(piece);
    }

    @Transactional(readOnly = true)
    public List<PieceResponse> getMyPieces(UserPrincipal principal) {
        User owner = userService.getUser(principal);
        return pieceRepository.findByOwner(owner).stream()
            .map(PieceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PieceResponse> getStandardPieces() {
        return pieceRepository.findByIsStandardTrue().stream()
            .map(PieceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PieceResponse> getPublicPieces(String query, Pageable pageable) {
        Page<Piece> page = (query != null && !query.isBlank())
            ? pieceRepository.searchPublic(query, pageable)
            : pieceRepository.findByIsPublicTrue(pageable);

        return PageResponse.<PieceResponse>builder()
            .content(page.getContent().stream().map(PieceResponse::from).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }

    @Transactional
    public PieceResponse updatePiece(UserPrincipal principal, UUID id, UpdatePieceRequest request) {
        Piece piece = pieceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Piece not found"));

        if (piece.isStandard() || !piece.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Cannot modify this piece");
        }

        if (request.getName() != null) piece.setName(request.getName());
        if (request.getSvgKey() != null) piece.setSvgKey(request.getSvgKey());
        if (request.getMovementRules() != null) piece.setMovementRules(request.getMovementRules());
        if (request.getCaptureRules() != null) piece.setCaptureRules(request.getCaptureRules());
        if (request.getIsPublic() != null) piece.setPublic(request.getIsPublic());

        return PieceResponse.from(pieceRepository.save(piece));
    }

    @Transactional
    public MessageResponse deletePiece(UserPrincipal principal, UUID id) {
        Piece piece = pieceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Piece not found"));

        if (piece.isStandard() || !piece.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Cannot delete this piece");
        }

        pieceRepository.delete(piece);
        return MessageResponse.of("Piece deleted");
    }

    @Transactional
    public MessageResponse savePieceToLibrary(UserPrincipal principal, UUID pieceId) {
        // verify piece exists and is public or owned by user
        Piece piece = pieceRepository.findById(pieceId)
            .orElseThrow(() -> new NotFoundException("Piece not found"));

        if (!piece.isPublic() && !piece.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Cannot save this piece");
        }

        return MessageResponse.of("Piece saved to library");
    }

    @Transactional(readOnly = true)
    public List<PieceResponse> getSavedPieces(UserPrincipal principal) {
        return pieceRepository.findSavedByUser(principal.getId()).stream()
            .map(PieceResponse::from)
            .toList();
    }
}
