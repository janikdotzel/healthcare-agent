package io.akka.fitbit;

import akka.actor.ActorSystem;
import akka.javasdk.testkit.TestKitSupport;
import io.akka.health.fitbit.FitbitClient;
import io.akka.health.fitbit.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDate;

/**
 * Test class for FitbitClient that uses a provided access token.
 */
public class FitbitClientTest extends TestKitSupport {

    private static ActorSystem system;
    private final FitbitClient fitbitClient = new FitbitClient(httpClient);

    private static final String ACCESS_TOKEN = "";

    @Test
    public void testGetHeartRateData() {
        fitbitClient.setTokens(ACCESS_TOKEN, null, 3600);

        // Get heart rate data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        HeartRateData heartRateData = fitbitClient.getHeartRateByDate(testDate);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Heart Rate Data Response: " + heartRateData);

        // Verify that we got a non-null response
        Assertions.assertNotNull(heartRateData, "Heart rate data should not be null");

        // Verify that the activities heart list is not null or empty
        Assertions.assertNotNull(heartRateData.activitiesHeart(), "Activities heart should not be null");
        Assertions.assertFalse(heartRateData.activitiesHeart().isEmpty(), "Activities heart should not be empty");

        // Verify that the first activities heart entry has the expected date
        Assertions.assertEquals(testDate, heartRateData.activitiesHeart().getFirst().dateTime(),
                "Date should match the requested date");

        // Verify that the resting heart rate is 60
        Assertions.assertEquals(60, heartRateData.activitiesHeart().getFirst().value().restingHeartRate(),
                "Resting heart rate should be 60");
    }

    @Test
    public void testGetActiveZoneMinutesByDate() {
        fitbitClient.setTokens(ACCESS_TOKEN, null, 3600);

        // Get Active Zone Minutes data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        ActiveZoneMinutesData azmData = fitbitClient.getActiveZoneMinutesByDate(testDate);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Active Zone Minutes Data Response: " + azmData);

        // Verify that we got a non-null response
        Assertions.assertNotNull(azmData, "Active Zone Minutes data should not be null");

        // Verify that the activities active zone minutes list is not null or empty
        Assertions.assertNotNull(azmData.activitiesActiveZoneMinutes(), "Activities active zone minutes should not be null");
        Assertions.assertFalse(azmData.activitiesActiveZoneMinutes().isEmpty(), "Activities active zone minutes should not be empty");

        // Verify that the first activities active zone minutes entry has the expected date
        Assertions.assertEquals(testDate, azmData.activitiesActiveZoneMinutes().getFirst().dateTime(),
                "Date should match the requested date");
    }

    @Test
    public void testGetSleepLogByDate() {
        fitbitClient.setTokens(ACCESS_TOKEN, null, 3600);

        // Get sleep log data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        SleepLogData sleepData = fitbitClient.getSleepLogByDate(testDate);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Sleep Log Data Response: " + sleepData);

        // Verify that we got a non-null response
        Assertions.assertNotNull(sleepData, "Sleep log data should not be null");

        // Verify that the sleep list is not null or empty
        Assertions.assertNotNull(sleepData.sleep(), "Sleep list should not be null");
        Assertions.assertFalse(sleepData.sleep().isEmpty(), "Sleep list should not be empty");

        // Verify that the first sleep entry has the expected date
        Assertions.assertEquals(testDate, sleepData.sleep().get(0).dateOfSleep(), 
                "Date should match the requested date");
    }

    @Test
    public void testGetWeightLogByDate() {
        fitbitClient.setTokens(ACCESS_TOKEN, null, 3600);

        // Get weight log data for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        WeightLogData weightData = fitbitClient.getWeightLogByDate(testDate);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Weight Log Data Response: " + weightData);

        // Verify that we got a non-null response
        Assertions.assertNotNull(weightData, "Weight log data should not be null");

        // Verify that the weight list is not null
        Assertions.assertNotNull(weightData.weight(), "Weight list should not be null");

        // Note: The weight list might be empty if there are no weight entries for the specified date
        // If the weight list is not empty, verify that the first weight entry has the expected date
        if (!weightData.weight().isEmpty()) {
            Assertions.assertEquals(testDate, weightData.weight().get(0).date(), 
                    "Date should match the requested date");
        } else {
            System.out.println("[DEBUG_LOG] No weight entries found for date: " + testDate);
        }
    }

    @Test
    public void testGetDailyActivitySummary() {
        fitbitClient.setTokens(ACCESS_TOKEN, null, 3600);

        // Get daily activity summary for the 24th of april 2025
        LocalDate testDate = LocalDate.of(2025, 4, 24);
        DailyActivitySummary activityData = fitbitClient.getDailyActivitySummary(testDate);

        // Log the response for debugging
        System.out.println("[DEBUG_LOG] Daily Activity Summary Response: " + activityData);

        // Verify that we got a non-null response
        Assertions.assertNotNull(activityData, "Daily activity summary should not be null");

        // Verify that the summary is not null
        Assertions.assertNotNull(activityData.summary(), "Summary should not be null");

        // Verify that the goals are not null
        Assertions.assertNotNull(activityData.goals(), "Goals should not be null");

        // Verify that the activities list is not null
        Assertions.assertNotNull(activityData.activities(), "Activities should not be null");
    }
}
