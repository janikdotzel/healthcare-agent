package io.akka.health.agent;

import akka.actor.ActorSystem;
import fitbit.FitbitClient;
import fitbit.MockFitbitClient;
import fitbit.model.*;
import io.akka.health.agent.application.FitbitTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test class for FitbitTool.
 */
public class FitbitToolTest {

    private static ActorSystem system;
    private MockFitbitClient mockFitbitClient;
    private FitbitTool healthChecker;

    @BeforeAll
    public static void setupClass() {
        // Create an ActorSystem for testing
        system = ActorSystem.create("fitbit-health-checker-test");
    }

    @AfterAll
    public static void teardownClass() {
        // Terminate the ActorSystem after tests
        if (system != null) {
            system.terminate();
        }
    }

    @BeforeEach
    public void setup() {
        // Create a test FitbitClient
        mockFitbitClient = new MockFitbitClient(system);

        // Create the FitbitTool with the test client
        healthChecker = new FitbitTool(mockFitbitClient);
    }

    @Test
    public void testInitializeClientCredentials() {
        // This test verifies that the FitbitTool can successfully initialize the FitbitClient
        // with client credentials. The initialization happens in the FitbitTool constructor,
        // which calls initializeClientCredentials(), which in turn calls
        // fitbitClient.getAccessTokenWithClientCredentials().

        // Since we're using a MockFitbitClient that returns a valid token response,
        // no exception should be thrown during initialization.

        // Create a new FitbitTool instance to trigger the initialization process
        FitbitTool newTool = new FitbitTool(mockFitbitClient);

        // If we got here without an exception, the test passes
        // We can also verify that the tool can retrieve data from the Fitbit API
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        mockFitbitClient.setHeartRateData(testDate, 70);

        Integer heartRate = newTool.restingHeartRate(testDate);
        Assertions.assertEquals(70, heartRate, "Heart rate should be 70");
    }

    @Test
    public void testGetAccessTokenWithClientCredentials() {
        // This test directly verifies that the getAccessTokenWithClientCredentials method
        // of the MockFitbitClient returns a valid token response with the expected values.

        // Call the method under test
        FitbitClient.TokenResponse tokenResponse = mockFitbitClient.getAccessTokenWithClientCredentials();

        // Verify that we got a non-null response
        Assertions.assertNotNull(tokenResponse, "Token response should not be null");

        // Verify that the access token is not null and has the expected value
        Assertions.assertNotNull(tokenResponse.getAccessToken(), "Access token should not be null");
        Assertions.assertEquals("mock-access-token", tokenResponse.getAccessToken(), "Access token should match expected value");

        // Verify that the refresh token is not null and has the expected value
        Assertions.assertNotNull(tokenResponse.getRefreshToken(), "Refresh token should not be null");
        Assertions.assertEquals("mock-refresh-token", tokenResponse.getRefreshToken(), "Refresh token should match expected value");

        // Verify that the expires in is correct
        Assertions.assertEquals(3600, tokenResponse.getExpiresIn(), "Expires in should be 3600 seconds (1 hour)");

        // Verify that the token type is correct
        Assertions.assertEquals("Bearer", tokenResponse.getTokenType(), "Token type should be Bearer");

        // Verify that the scope is correct
        Assertions.assertEquals("heartrate activity sleep weight", tokenResponse.getScope(), "Scope should match expected value");
    }

    @Test
    public void testRestingHeartRate_True() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setHeartRateData(testDate, 70); // Resting heart rate above threshold (65)

        // Call the method under test
        Integer heartRate = healthChecker.restingHeartRate(testDate);

        // Verify the result
        Assertions.assertEquals(70, heartRate, "Heart rate should be 70");
    }

    @Test
    public void testRestingHeartRate_False() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setHeartRateData(testDate, 60); // Resting heart rate below threshold (65)

        // Call the method under test
        Integer heartRate = healthChecker.restingHeartRate(testDate);

        // Verify the result
        Assertions.assertEquals(60, heartRate, "Heart rate should be 60");
    }

    @Test
    public void testGetSleepHoursForDay() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setSleepLogData(testDate, 480); // 8 hours (480 minutes) of sleep

        // Call the method under test
        Double hoursSlept = healthChecker.getSleepHoursForDay(testDate);

        // Verify the result
        Assertions.assertEquals(8.0, hoursSlept, 0.01, "Should have slept 8 hours");
    }

    @Test
    public void testGetStepsForDay() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setDailyActivitySummaryWithSteps(testDate, 10500);

        // Call the method under test
        Integer steps = healthChecker.getStepsForDay(testDate);

        // Verify the result
        Assertions.assertEquals(10500, steps, "Should have 10500 steps");
    }

    @Test
    public void testIsHeartRateOutsideSafeRange_TooHigh() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setHeartRateDataWithIntraday(testDate, List.of(60, 75, 180)); // One value above max threshold (170)

        // Call the method under test
        Optional<Integer> heartRate = healthChecker.isHeartRateOutsideSafeRange(testDate, 40, 170);

        // Verify the result
        Assertions.assertTrue(heartRate.isPresent(), "Heart rate should be present");
        Assertions.assertEquals(180, heartRate.get(), "Heart rate should be 180");
    }

    @Test
    public void testIsHeartRateOutsideSafeRange_TooLow() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setHeartRateDataWithIntraday(testDate, List.of(60, 35, 75)); // One value below min threshold (40)

        // Call the method under test
        Optional<Integer> heartRate = healthChecker.isHeartRateOutsideSafeRange(testDate, 40, 170);

        // Verify the result
        Assertions.assertTrue(heartRate.isPresent(), "Heart rate should be present");
        Assertions.assertEquals(35, heartRate.get(), "Heart rate should be 35");
    }


    @Test
    public void testGetActiveMinutesInWeek() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate startDate = LocalDate.of(2025, 4, 20);
        LocalDate endDate = LocalDate.of(2025, 4, 26);

        // Set test data in the test client for each day
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            // Create data with 20 active minutes each day (140 total)
            mockFitbitClient.setActiveZoneMinutesData(date, 10, 5, 5, 20);
        }

        // Call the method under test
        Integer activeMinutes = healthChecker.getActiveMinutesInWeek(startDate, endDate);

        // Verify the result
        Assertions.assertEquals(140, activeMinutes, "Should have 140 active minutes in the week");
    }

    @Test
    public void testGetRemSleepMinutes() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate testDate = LocalDate.of(2025, 4, 24);

        // Set test data in the test client
        mockFitbitClient.setSleepLogDataWithRem(testDate, 60); // 1 hour (60 minutes) of REM sleep

        // Call the method under test
        Integer remMinutes = healthChecker.getRemSleepMinutes(testDate);

        // Verify the result
        Assertions.assertEquals(60, remMinutes, "Should have 60 minutes of REM sleep");
    }

    @Test
    public void testGetSportActivitiesInWeek() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup test data
        LocalDate startDate = LocalDate.of(2025, 4, 20);
        LocalDate endDate = LocalDate.of(2025, 4, 26);

        // Set test data in the test client for each day
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            // Create data with sport activities on 2 days
            boolean hasSportActivity = i == 1 || i == 3; // Day 2 and 4 have sport activities
            mockFitbitClient.setDailyActivitySummaryWithSportActivity(date, hasSportActivity);
        }

        // Call the method under test
        List<DailyActivitySummary.Activity> sportActivities = healthChecker.getSportActivitiesInWeek(startDate, endDate);

        // Verify the result
        Assertions.assertEquals(2, sportActivities.size(), "Should have 2 sport activities");
    }
}
