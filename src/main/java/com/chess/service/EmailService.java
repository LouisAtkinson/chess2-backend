package com.chess.service;

import com.chess.entity.Game;
import com.chess.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendYourTurnNotification(User recipient, Game game) {
        if (!mailEnabled || !recipient.isEmailNotificationsEnabled()) {
            return;
        }

        try {
            String opponentName = game.getWhitePlayer().getId().equals(recipient.getId())
                ? game.getBlackPlayer().getUsername()
                : game.getWhitePlayer().getUsername();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.getEmail());
            message.setSubject("It's your turn in your game vs " + opponentName);
            message.setText(String.format("""
                Hi %s,
                
                It's your turn in your correspondence game against %s.
                
                Play now: %s/game/%s
                """,
                recipient.getUsername(),
                opponentName,
                frontendUrl,
                game.getId()
            ));

            mailSender.send(message);
            log.info("Turn notification sent to {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send turn notification to {}: {}", recipient.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendGameInviteNotification(User recipient, User inviter, Game game) {
        if (!mailEnabled || !recipient.isEmailNotificationsEnabled()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.getEmail());
            message.setSubject(inviter.getUsername() + " challenged you to a game!");
            message.setText(String.format("""
                Hi %s,
                
                %s has invited you to play a correspondence chess game.
                
                Accept and play: %s/game/%s
                """,
                recipient.getUsername(),
                inviter.getUsername(),
                frontendUrl,
                game.getId()
            ));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send invite notification: {}", e.getMessage());
        }
    }
}
