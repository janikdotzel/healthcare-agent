package io.akka.health.agent;

import akka.actor.ActorSystem;
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
