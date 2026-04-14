package com.chess.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockfishService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.stockfish.api-url}")
    private String stockfishApiUrl;

    public Optional<String> getBestMove(String fen, int depth) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(stockfishApiUrl)
                .queryParam("fen", fen)
                .queryParam("depth", Math.min(depth, 15))
                .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return Optional.empty();

            JsonNode node = objectMapper.readTree(response);
            if (node.has("success") && node.get("success").asBoolean()) {
                String bestmove = node.get("bestmove").asText();
                String[] parts = bestmove.split(" ");
                if (parts.length >= 2) {
                    return Optional.of(parts[1]);
                }
            }
            log.warn("Stockfish API returned unsuccessful response: {}", response);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Stockfish API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Integer> getEvaluation(String fen, int depth) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(stockfishApiUrl)
                .queryParam("fen", fen)
                .queryParam("depth", Math.min(depth, 15))
                .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) return Optional.empty();

            JsonNode node = objectMapper.readTree(response);
            if (node.has("success") && node.get("success").asBoolean()) {
                if (node.has("evaluation")) {
                    return Optional.of((int)(node.get("evaluation").asDouble() * 100));
                }
                if (node.has("mate")) {
                    int mate = node.get("mate").asInt();
                    return Optional.of(mate > 0 ? 100000 : -100000);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Stockfish evaluation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
