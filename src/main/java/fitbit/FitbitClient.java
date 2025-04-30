package fitbit;

import akka.javasdk.http.HttpClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import fitbit.model.*;
import fitbit.parser.FitbitParser;
import io.akka.health.common.KeyUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FitbitClient {
    private static final String AUTH_URL = "https://www.fitbit.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.fitbit.com/oauth2/token";
    private static final String API_BASE_URL = "https://api.fitbit.com";
    private static final String REDIRECT_URI = "https://janikdotzel.com/";
    private static final String SCOPE = "heartrate activity sleep weight";

    private final ObjectMapper objectMapper;
    private final FitbitParser parser;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;

    private String accessToken;
    private String refreshToken;
    private long expiresAt;

    public FitbitClient(HttpClient httpClient) {
        this.objectMapper = new ObjectMapper();
        this.parser = new FitbitParser();
        this.clientId = KeyUtils.readFitbitClientId();
        this.clientSecret = KeyUtils.readFitbitClientSecret();
        this.httpClient = httpClient;

        if (!KeyUtils.hasFitbitKeys()) {
            throw new IllegalStateException(
                    "Fitbit API keys not found. Make sure FITBIT_CLIENT_ID and FITBIT_CLIENT_SECRET are defined as environment variables or in the .env file.");
        }
    }

    public String getAuthorizationUrl() {
        return AUTH_URL + "?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&expires_in=604800"; // 7 days
    }

    public TokenResponse exchangeCodeForAccessToken(String code) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("code", code);
        formData.put("redirect_uri", REDIRECT_URI);

        String formDataString = formData.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        var response = httpClient
                .POST(TOKEN_URL)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", authHeader)
                .withRequestBody(formDataString)
                .invoke();

        if (response.status().intValue() == 200) {
            try {
                return parseTokenResponse(response.body().utf8String());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse token response", e);
            }
        } else {
            throw new RuntimeException("Failed to exchange code for token: " + response.status() + " - " + response.body().utf8String());
        }
    }

    public TokenResponse refreshAccessToken() {

        if (refreshToken == null) throw new IllegalStateException("No refresh token available");

        String authHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "refresh_token");
        formData.put("refresh_token", refreshToken);

        String formDataString = formData.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        var response = httpClient
                .POST(TOKEN_URL)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", authHeader)
                .withRequestBody(formDataString)
                .invoke();

        if (response.status().intValue() == 200) {
            try {
                return parseTokenResponse(response.body().utf8String());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse token response", e);
            }
        } else {
            throw new RuntimeException("Failed to refresh token: " + response.status() + " - " + response.body().utf8String());
        }
    }

    public CompletionStage<HeartRateData> getHeartRateByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/activities/heart/date/" + dateStr + "/1d.json";

            // For now, we'll continue to use the java.net.http.HttpClient
            // This is a temporary solution until we can figure out how to properly use the akka.javasdk.http.HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            CompletableFuture<HeartRateData> future = new CompletableFuture<>();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                HeartRateData data = parser.parseHeartRateData(response.body());
                                future.complete(data);
                            } catch (Exception e) {
                                future.completeExceptionally(new RuntimeException("Failed to parse heart rate data", e));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException(
                                    "Failed to get heart rate data: " + response.statusCode() + " - " + response.body()));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(new RuntimeException("Failed to send request", ex));
                        return null;
                    });

            return future;
        });
    }

    public CompletionStage<ActiveZoneMinutesData> getActiveZoneMinutesByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/activities/active-zone-minutes/date/" + dateStr + "/1d.json";

            // For now, we'll continue to use the java.net.http.HttpClient
            // This is a temporary solution until we can figure out how to properly use the akka.javasdk.http.HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            CompletableFuture<ActiveZoneMinutesData> future = new CompletableFuture<>();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                ActiveZoneMinutesData data = parser.parseActiveZoneMinutesData(response.body());
                                future.complete(data);
                            } catch (Exception e) {
                                future.completeExceptionally(new RuntimeException("Failed to parse Active Zone Minutes data", e));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException(
                                    "Failed to get Active Zone Minutes data: " + response.statusCode() + " - " + response.body()));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(new RuntimeException("Failed to send request", ex));
                        return null;
                    });

            return future;
        });
    }

    public CompletionStage<SleepLogData> getSleepLogByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1.2/user/-/sleep/date/" + dateStr + ".json";

            // For now, we'll continue to use the java.net.http.HttpClient
            // This is a temporary solution until we can figure out how to properly use the akka.javasdk.http.HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            CompletableFuture<SleepLogData> future = new CompletableFuture<>();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                SleepLogData data = parser.parseSleepLogData(response.body());
                                future.complete(data);
                            } catch (Exception e) {
                                future.completeExceptionally(new RuntimeException("Failed to parse sleep log data", e));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException(
                                    "Failed to get sleep log data: " + response.statusCode() + " - " + response.body()));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(new RuntimeException("Failed to send request", ex));
                        return null;
                    });

            return future;
        });
    }

    public CompletionStage<WeightLogData> getWeightLogByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/body/log/weight/date/" + dateStr + ".json";

            // For now, we'll continue to use the java.net.http.HttpClient
            // This is a temporary solution until we can figure out how to properly use the akka.javasdk.http.HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            CompletableFuture<WeightLogData> future = new CompletableFuture<>();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                WeightLogData data = parser.parseWeightLogData(response.body());
                                future.complete(data);
                            } catch (Exception e) {
                                future.completeExceptionally(new RuntimeException("Failed to parse weight log data", e));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException(
                                    "Failed to get weight log data: " + response.statusCode() + " - " + response.body()));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(new RuntimeException("Failed to send request", ex));
                        return null;
                    });

            return future;
        });
    }

    public CompletionStage<DailyActivitySummary> getDailyActivitySummary(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/activities/date/" + dateStr + ".json";

            // For now, we'll continue to use the java.net.http.HttpClient
            // This is a temporary solution until we can figure out how to properly use the akka.javasdk.http.HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            CompletableFuture<DailyActivitySummary> future = new CompletableFuture<>();

            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                DailyActivitySummary data = parser.parseDailyActivitySummary(response.body());
                                future.complete(data);
                            } catch (Exception e) {
                                future.completeExceptionally(new RuntimeException("Failed to parse daily activity summary", e));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException(
                                    "Failed to get daily activity summary: " + response.statusCode() + " - " + response.body()));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(new RuntimeException("Failed to send request", ex));
                        return null;
                    });

            return future;
        });
    }


    private CompletionStage<Boolean> ensureValidToken() {
        if (accessToken == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("No access token available"));
            return future;
        }

        if (System.currentTimeMillis() < expiresAt) {
            return CompletableFuture.completedFuture(true);
        }

        return refreshAccessToken().thenApply(response -> true);
    }

    private TokenResponse parseTokenResponse(String json) {
        try {
            TokenResponse response = objectMapper.readValue(json, TokenResponse.class);
            this.accessToken = response.accessToken;
            this.refreshToken = response.refreshToken;
            this.expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }

    public void setTokens(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
    }

    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private long expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("user_id")
        private String userId;

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public String getTokenType() {
            return tokenType;
        }

        public String getScope() {
            return scope;
        }

        public String getUserId() {
            return userId;
        }
    }
}
