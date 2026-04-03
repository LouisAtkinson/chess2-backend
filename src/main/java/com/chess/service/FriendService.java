package com.chess.service;

import com.chess.dto.response.Responses.*;
import com.chess.entity.Friend;
import com.chess.entity.User;
import com.chess.exception.GlobalExceptionHandler.*;
import com.chess.repository.FriendRepository;
import com.chess.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserService userService;

    @Transactional
    public MessageResponse sendRequest(UserPrincipal principal, UUID targetId) {
        if (principal.getId().equals(targetId)) {
            throw new BadRequestException("Cannot friend yourself");
        }
        User requester = userService.getUser(principal);
        User addressee = userService.getUser(targetId);

        friendRepository.findFriendshipBetween(requester.getId(), addressee.getId())
            .ifPresent(f -> {
                throw new ConflictException("Friend request already exists");
            });

        Friend friend = Friend.builder()
            .requester(requester)
            .addressee(addressee)
            .status(Friend.Status.PENDING)
            .build();
        friendRepository.save(friend);

        return MessageResponse.of("Friend request sent");
    }

    @Transactional
    public MessageResponse respondToRequest(UserPrincipal principal, UUID requestId, boolean accept) {
        Friend friend = friendRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Friend request not found"));

        if (!friend.getAddressee().getId().equals(principal.getId())) {
            throw new ForbiddenException("Not your friend request");
        }
        if (friend.getStatus() != Friend.Status.PENDING) {
            throw new BadRequestException("Request already handled");
        }

        friend.setStatus(accept ? Friend.Status.ACCEPTED : Friend.Status.BLOCKED);
        friendRepository.save(friend);

        return MessageResponse.of(accept ? "Friend request accepted" : "Friend request declined");
    }

    @Transactional
    public MessageResponse removeFriend(UserPrincipal principal, UUID otherId) {
        Friend friend = friendRepository.findFriendshipBetween(principal.getId(), otherId)
            .orElseThrow(() -> new NotFoundException("Friendship not found"));
        friendRepository.delete(friend);
        return MessageResponse.of("Friend removed");
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(UserPrincipal principal) {
        User user = userService.getUser(principal);
        return friendRepository.findAcceptedFriendships(user).stream()
            .map(f -> FriendResponse.from(f, principal.getId()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getPendingRequests(UserPrincipal principal) {
        User user = userService.getUser(principal);
        return friendRepository.findPendingRequests(user).stream()
            .map(f -> FriendResponse.from(f, principal.getId()))
            .toList();
    }
}
