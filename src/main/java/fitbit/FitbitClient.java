package fitbit;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import akka.http.javadsl.unmarshalling.Unmarshaller;
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

/**
 * Client for interacting with the Fitbit API using OAuth2 authentication.
 */
public class FitbitClient {
    private static final String AUTH_URL = "https://www.fitbit.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.fitbit.com/oauth2/token";
    private static final String API_BASE_URL = "https://api.fitbit.com";
    private static final String REDIRECT_URI = "https://janikdotzel.com/";
    private static final String SCOPE = "heartrate activity sleep weight";

    private final ActorSystem system;
    private final Http http;
    private final ObjectMapper objectMapper;
    private final FitbitParser parser;
    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private String refreshToken;
    private long expiresAt;

    public FitbitClient(ActorSystem system) {
        this.system = system;
        this.http = Http.get(system);
        this.objectMapper = new ObjectMapper();
        this.parser = new FitbitParser();
        this.clientId = KeyUtils.readFitbitClientId();
        this.clientSecret = KeyUtils.readFitbitClientSecret();

        if (!KeyUtils.hasFitbitKeys()) {
            throw new IllegalStateException(
                    "Fitbit API keys not found. Make sure FITBIT_CLIENT_ID and FITBIT_CLIENT_SECRET are defined as environment variables or in the .env file.");
        }
    }

    /**
     * Generates the authorization URL for the OAuth2 flow.
     * 
     * @return The authorization URL to redirect the user to.
     */
    public String getAuthorizationUrl() {
        return AUTH_URL + "?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                "&expires_in=604800"; // 7 days
    }

    /**
     * Exchanges an authorization code for an access token.
     * 
     * @param code The authorization code received from the redirect.
     * @return A CompletionStage that completes when the token exchange is done.
     */
    public CompletionStage<TokenResponse> exchangeCodeForToken(String code) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("code", code);
        formData.put("redirect_uri", REDIRECT_URI);

        String formDataString = formData.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        HttpRequest request = HttpRequest.POST(TOKEN_URL)
                .withEntity(ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED, formDataString)
                .addHeader(HttpHeader.parse("Authorization", authHeader));

        return http.singleRequest(request)
                .thenCompose(response -> {
                    if (response.status().isSuccess()) {
                        return response.entity().toStrict(10000, system)
                                .thenApply(strict -> strict.getData().utf8String())
                                .thenApply(this::parseTokenResponse);
                    } else {
                        return response.entity().toStrict(10000, system)
                                .thenApply(strict -> strict.getData().utf8String())
                                .thenCompose(body -> {
                                    CompletableFuture<TokenResponse> future = new CompletableFuture<>();
                                    future.completeExceptionally(new RuntimeException(
                                            "Failed to exchange code for token: " + response.status() + " - " + body));
                                    return future;
                                });
                    }
                });
    }

    /**
     * Refreshes an expired access token using the refresh token.
     * 
     * @return A CompletionStage that completes when the token refresh is done.
     */
    public CompletionStage<TokenResponse> refreshAccessToken() {
        if (refreshToken == null) {
            CompletableFuture<TokenResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("No refresh token available"));
            return future;
        }

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "refresh_token");
        formData.put("refresh_token", refreshToken);

        String formDataString = formData.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        HttpRequest request = HttpRequest.POST(TOKEN_URL)
                .withEntity(ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED, formDataString)
                .addHeader(HttpHeader.parse("Authorization", authHeader));

        return http.singleRequest(request)
                .thenCompose(response -> {
                    if (response.status().isSuccess()) {
                        return response.entity().toStrict(10000, system)
                                .thenApply(strict -> strict.getData().utf8String())
                                .thenApply(this::parseTokenResponse);
                    } else {
                        return response.entity().toStrict(10000, system)
                                .thenApply(strict -> strict.getData().utf8String())
                                .thenCompose(body -> {
                                    CompletableFuture<TokenResponse> future = new CompletableFuture<>();
                                    future.completeExceptionally(new RuntimeException(
                                            "Failed to refresh token: " + response.status() + " - " + body));
                                    return future;
                                });
                    }
                });
    }

    /**
     * Gets heart rate data for a specific date.
     * 
     * @param date The date to get heart rate data for.
     * @return A CompletionStage that completes with the heart rate data.
     */
    public CompletionStage<HeartRateData> getHeartRateByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/activities/heart/date/" + dateStr + "/1d.json";

            HttpRequest request = HttpRequest.GET(url)
                    .addHeader(HttpHeader.parse("Authorization", "Bearer " + accessToken));

            return http.singleRequest(request)
                    .thenCompose(response -> {
                        if (response.status().isSuccess()) {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenApply(json -> {
                                        try {
                                            return parser.parseHeartRateData(json);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed to parse heart rate data", e);
                                        }
                                    });
                        } else {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenCompose(body -> {
                                        CompletableFuture<HeartRateData> future = new CompletableFuture<>();
                                        future.completeExceptionally(new RuntimeException(
                                                "Failed to get heart rate data: " + response.status() + " - " + body));
                                        return future;
                                    });
                        }
                    });
        });
    }

    /**
     * Gets Active Zone Minutes time series data for a specific date.
     * 
     * @param date The date to get Active Zone Minutes data for.
     * @return A CompletionStage that completes with the Active Zone Minutes data.
     */
    public CompletionStage<ActiveZoneMinutesData> getActiveZoneMinutesByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/activities/active-zone-minutes/date/" + dateStr + "/1d.json";

            HttpRequest request = HttpRequest.GET(url)
                    .addHeader(HttpHeader.parse("Authorization", "Bearer " + accessToken));

            return http.singleRequest(request)
                    .thenCompose(response -> {
                        if (response.status().isSuccess()) {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenApply(json -> {
                                        try {
                                            return parser.parseActiveZoneMinutesData(json);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed to parse Active Zone Minutes data", e);
                                        }
                                    });
                        } else {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenCompose(body -> {
                                        CompletableFuture<ActiveZoneMinutesData> future = new CompletableFuture<>();
                                        future.completeExceptionally(new RuntimeException(
                                                "Failed to get Active Zone Minutes data: " + response.status() + " - " + body));
                                        return future;
                                    });
                        }
                    });
        });
    }

    /**
     * Gets sleep log data for a specific date.
     * 
     * @param date The date to get sleep log data for.
     * @return A CompletionStage that completes with the sleep log data.
     */
    public CompletionStage<SleepLogData> getSleepLogByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1.2/user/-/sleep/date/" + dateStr + ".json";

            HttpRequest request = HttpRequest.GET(url)
                    .addHeader(HttpHeader.parse("Authorization", "Bearer " + accessToken));

            return http.singleRequest(request)
                    .thenCompose(response -> {
                        if (response.status().isSuccess()) {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenApply(json -> {
                                        try {
                                            return parser.parseSleepLogData(json);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed to parse sleep log data", e);
                                        }
                                    });
                        } else {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenCompose(body -> {
                                        CompletableFuture<SleepLogData> future = new CompletableFuture<>();
                                        future.completeExceptionally(new RuntimeException(
                                                "Failed to get sleep log data: " + response.status() + " - " + body));
                                        return future;
                                    });
                        }
                    });
        });
    }

    /**
     * Gets weight log data for a specific date.
     * 
     * @param date The date to get weight log data for.
     * @return A CompletionStage that completes with the weight log data.
     */
    public CompletionStage<WeightLogData> getWeightLogByDate(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/body/log/weight/date/" + dateStr + ".json";

            HttpRequest request = HttpRequest.GET(url)
                    .addHeader(HttpHeader.parse("Authorization", "Bearer " + accessToken));

            return http.singleRequest(request)
                    .thenCompose(response -> {
                        if (response.status().isSuccess()) {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenApply(json -> {
                                        try {
                                            return parser.parseWeightLogData(json);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed to parse weight log data", e);
                                        }
                                    });
                        } else {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenCompose(body -> {
                                        CompletableFuture<WeightLogData> future = new CompletableFuture<>();
                                        future.completeExceptionally(new RuntimeException(
                                                "Failed to get weight log data: " + response.status() + " - " + body));
                                        return future;
                                    });
                        }
                    });
        });
    }

    /**
     * Gets daily activity summary for a specific date.
     * 
     * @param date The date to get activity summary for.
     * @return A CompletionStage that completes with the activity summary data.
     */
    public CompletionStage<DailyActivitySummary> getDailyActivitySummary(LocalDate date) {
        return ensureValidToken().thenCompose(valid -> {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = API_BASE_URL + "/1/user/-/activities/date/" + dateStr + ".json";

            HttpRequest request = HttpRequest.GET(url)
                    .addHeader(HttpHeader.parse("Authorization", "Bearer " + accessToken));

            return http.singleRequest(request)
                    .thenCompose(response -> {
                        if (response.status().isSuccess()) {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenApply(json -> {
                                        try {
                                            return parser.parseDailyActivitySummary(json);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed to parse daily activity summary", e);
                                        }
                                    });
                        } else {
                            return response.entity().toStrict(10000, system)
                                    .thenApply(strict -> strict.getData().utf8String())
                                    .thenCompose(body -> {
                                        CompletableFuture<DailyActivitySummary> future = new CompletableFuture<>();
                                        future.completeExceptionally(new RuntimeException(
                                                "Failed to get daily activity summary: " + response.status() + " - " + body));
                                        return future;
                                    });
                        }
                    });
        });
    }

    /**
     * Ensures that the access token is valid, refreshing it if necessary.
     * 
     * @return A CompletionStage that completes with true if the token is valid.
     */
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

    /**
     * Parses the token response from the Fitbit API.
     * 
     * @param json The JSON response from the token endpoint.
     * @return The parsed TokenResponse.
     */
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

    /**
     * Sets the tokens manually (useful for testing or when tokens are stored externally).
     * 
     * @param accessToken  The access token.
     * @param refreshToken The refresh token.
     * @param expiresIn    The number of seconds until the token expires.
     */
    public void setTokens(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
    }

    /**
     * Response object for the token endpoint.
     */
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
