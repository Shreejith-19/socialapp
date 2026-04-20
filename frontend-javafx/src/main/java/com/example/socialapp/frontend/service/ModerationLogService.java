package com.example.socialapp.frontend.service;

import com.example.socialapp.frontend.model.ModerationLogDTO;
import com.example.socialapp.frontend.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Moderation Log Service.
 * Client service for fetching moderation logs from the backend.
 */
@Slf4j
public class ModerationLogService {
    private static final String BASE_URL = "http://localhost:8080/api/v1";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class,
            (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                LocalDateTime.parse(json.getAsString()))
        .create();

    private static ModerationLogService instance;
    private final SessionManager sessionManager;

    private ModerationLogService() {
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * Get singleton instance.
     */
    public static ModerationLogService getInstance() {
        if (instance == null) {
            instance = new ModerationLogService();
        }
        return instance;
    }

    /**
     * Get all moderation logs.
     * Only accessible to ADMIN users.
     *
     * @return Optional containing list of ModerationLogDTO
     */
    public Optional<List<ModerationLogDTO>> getAllLogs() {
        String authToken = sessionManager.getAuthToken().orElse(null);
        if (authToken == null) {
            log.warn("Cannot fetch moderation logs - no auth token set");
            return Optional.empty();
        }

        try {
            URL url = URI.create(BASE_URL + "/admin/logs").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                List<ModerationLogDTO> logs = GSON.fromJson(response,
                    new TypeToken<List<ModerationLogDTO>>(){}.getType());
                log.info("Fetched {} moderation logs", logs != null ? logs.size() : 0);
                return Optional.ofNullable(logs);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Unauthorized access to moderation logs (code: {})", responseCode);
                return Optional.empty();
            } else {
                log.warn("Get moderation logs failed with status code: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error fetching moderation logs", e);
            return Optional.empty();
        }
    }

    /**
     * Read response from HTTP connection.
     */
    private String readResponse(HttpURLConnection conn) throws java.io.IOException {
        java.io.InputStream is = conn.getResponseCode() >= 400 ?
            conn.getErrorStream() : conn.getInputStream();

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
