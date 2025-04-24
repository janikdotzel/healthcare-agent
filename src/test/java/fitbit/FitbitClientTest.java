package fitbit;

import akka.actor.ActorSystem;
import jnr.constants.platform.Local;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDate;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test class for FitbitClient that uses a provided access token.
 */
public class FitbitClientTest {

    private static ActorSystem system;
    private static FitbitClient fitbitClient;

    // If expired, you need to create a new one using the Main class
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyM1E5NVIiLCJzdWIiOiI3RlI3WDIiLCJpc3MiOiJGaXRiaXQiLCJ0eXAiOiJhY2Nlc3NfdG9rZW4iLCJzY29wZXMiOiJyd2VpIHJociByYWN0IHJzbGUiLCJleHAiOjE3NDU1MzgzMjMsImlhdCI6MTc0NTUwOTUyM30.oFuUSV_B_cPkebMKk0cwx59bVYcsjDPtcOtCHa9MnEw";

    @BeforeAll
    public static void setup() {
        // Create an ActorSystem for testing
        system = ActorSystem.create("fitbit-test");

        // Initialize the FitbitClient
        fitbitClient = new FitbitClient(system);

        // Set the access token manually
        // Note: We're setting a null refresh token and a long expiry time since we only need the access token for this test
        fitbitClient.setTokens(ACCESS_TOKEN, null, 3600);
    }

    @AfterAll
    public static void teardown() {
        // Terminate the ActorSystem after tests
        if (system != null) {
            system.terminate();
        }
    }

    @Test
    public void testGetHeartRateData() throws ExecutionException, InterruptedException, TimeoutException {
        // Get heart rate data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        CompletionStage<String> heartRateFuture = fitbitClient.getHeartRateByDate(testDate);

        // Wait for the result with a timeout
        String heartRateData = heartRateFuture.toCompletableFuture().get(30, TimeUnit.SECONDS);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Heart Rate Data Response: " + heartRateData);

        // Verify that we got a non-null, non-empty response
        Assertions.assertNotNull(heartRateData, "Heart rate data should not be null");
        Assertions.assertFalse(heartRateData.isEmpty(), "Heart rate data should not be empty");

        // Verify that the response contains expected JSON structure elements
        Assertions.assertTrue(heartRateData.contains("activities-heart"),
                "Response should contain 'activities-heart' field");

        // Verify that the restingHeartRate is 60
        Assertions.assertTrue(heartRateData.contains("""
                        "restingHeartRate": 60
                        """),
                "'restingHeartRate' should be 60");
    }

    @Test
    public void testGetActiveZoneMinutesByDate() throws ExecutionException, InterruptedException, TimeoutException {
        // Get Active Zone Minutes data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        CompletionStage<String> azmFuture = fitbitClient.getActiveZoneMinutesByDate(testDate);

        // Wait for the result with a timeout
        String azmData = azmFuture.toCompletableFuture().get(30, TimeUnit.SECONDS);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Active Zone Minutes Data Response: " + azmData);

        // Verify that we got a non-null, non-empty response
        Assertions.assertNotNull(azmData, "Active Zone Minutes data should not be null");
        Assertions.assertFalse(azmData.isEmpty(), "Active Zone Minutes data should not be empty");

        // Verify that the response contains expected JSON structure elements
        Assertions.assertTrue(azmData.contains("activities-active-zone-minutes"),
                "Response should contain 'activities-active-zone-minutes' field");

    }

    @Test
    public void testGetSleepLogByDate() throws ExecutionException, InterruptedException, TimeoutException {
        // Get sleep log data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        CompletionStage<String> sleepFuture = fitbitClient.getSleepLogByDate(testDate);

        // Wait for the result with a timeout
        String sleepData = sleepFuture.toCompletableFuture().get(30, TimeUnit.SECONDS);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Sleep Log Data Response: " + sleepData);

        // Verify that we got a non-null, non-empty response
        Assertions.assertNotNull(sleepData, "Sleep log data should not be null");
        Assertions.assertFalse(sleepData.isEmpty(), "Sleep log data should not be empty");

        // Verify that the response contains expected JSON structure elements
        Assertions.assertTrue(sleepData.contains("sleep"),
                "Response should contain 'sleep' field");

    }

    @Test
    public void testGetWeightLogByDate() throws ExecutionException, InterruptedException, TimeoutException {
        // Get weight log data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        CompletionStage<String> weightFuture = fitbitClient.getWeightLogByDate(testDate);

        // Wait for the result with a timeout
        String weightData = weightFuture.toCompletableFuture().get(30, TimeUnit.SECONDS);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Weight Log Data Response: " + weightData);

        // Verify that we got a non-null, non-empty response
        Assertions.assertNotNull(weightData, "Weight log data should not be null");
        Assertions.assertFalse(weightData.isEmpty(), "Weight log data should not be empty");

        // Verify that the response contains expected JSON structure elements
        Assertions.assertTrue(weightData.contains("weight"),
                "Response should contain 'weight' field");

    }

    @Test
    public void testGetDailyActivitySummary() throws ExecutionException, InterruptedException, TimeoutException {
        // Get daily activity summary for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        CompletionStage<String> activityFuture = fitbitClient.getDailyActivitySummary(testDate);

        // Wait for the result with a timeout
        String activityData = activityFuture.toCompletableFuture().get(30, TimeUnit.SECONDS);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Daily Activity Summary Response: " + activityData);

        // Verify that we got a non-null, non-empty response
        Assertions.assertNotNull(activityData, "Daily activity summary should not be null");
        Assertions.assertFalse(activityData.isEmpty(), "Daily activity summary should not be empty");

        // Verify that the response contains expected JSON structure elements
        Assertions.assertTrue(activityData.contains("activities"),
                "Response should contain 'activities' field");
        Assertions.assertTrue(activityData.contains("summary"),
                "Response should contain 'summary' field");

    }
}
