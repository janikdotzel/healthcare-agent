package fitbit;

import akka.actor.ActorSystem;
import fitbit.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Test implementation of FitbitClient that returns predefined test data.
 * Used for testing FitbitTool without making actual API calls.
 */
public class MockFitbitClient extends FitbitClient {

    private final Map<LocalDate, HeartRateData> heartRateDataMap = new HashMap<>();
    private final Map<LocalDate, ActiveZoneMinutesData> activeZoneMinutesDataMap = new HashMap<>();
    private final Map<LocalDate, SleepLogData> sleepLogDataMap = new HashMap<>();
    private final Map<LocalDate, DailyActivitySummary> dailyActivitySummaryMap = new HashMap<>();

    public MockFitbitClient(ActorSystem system) {
        super(null);
    }

    /**
     * Sets test heart rate data for a specific date.
     */
    public void setHeartRateData(LocalDate date, HeartRateData data) {
        heartRateDataMap.put(date, data);
    }

    /**
     * Sets test heart rate data with a specific resting heart rate.
     */
    public void setHeartRateData(LocalDate date, int restingHeartRate) {
        HeartRateData.HeartRateValue value = new HeartRateData.HeartRateValue(
            new ArrayList<>(),
            new ArrayList<>(),
            restingHeartRate
        );

        HeartRateData.DailyHeartRate dailyHeartRate = new HeartRateData.DailyHeartRate(
            date,
            value
        );

        HeartRateData data = new HeartRateData(
            List.of(dailyHeartRate),
            null
        );

        heartRateDataMap.put(date, data);
    }

    /**
     * Sets test heart rate data with specific heart rate values.
     */
    public void setHeartRateDataWithIntraday(LocalDate date, List<Integer> heartRateValues) {
        List<HeartRateData.HeartRateDataPoint> dataPoints = new ArrayList<>();
        for (int i = 0; i < heartRateValues.size(); i++) {
            dataPoints.add(new HeartRateData.HeartRateDataPoint(
                String.format("%02d:%02d:00", 8 + i, 0),
                heartRateValues.get(i)
            ));
        }

        HeartRateData.IntradayHeartRate intradayHeartRate = new HeartRateData.IntradayHeartRate(
            dataPoints,
            1,
            "1min"
        );

        HeartRateData.HeartRateValue value = new HeartRateData.HeartRateValue(
            new ArrayList<>(),
            new ArrayList<>(),
            60
        );

        HeartRateData.DailyHeartRate dailyHeartRate = new HeartRateData.DailyHeartRate(
            date,
            value
        );

        HeartRateData data = new HeartRateData(
            List.of(dailyHeartRate),
            intradayHeartRate
        );

        heartRateDataMap.put(date, data);
    }

    /**
     * Sets test active zone minutes data for a specific date.
     */
    public void setActiveZoneMinutesData(LocalDate date, ActiveZoneMinutesData data) {
        activeZoneMinutesDataMap.put(date, data);
    }

    /**
     * Sets test active zone minutes data with specific values.
     */
    public void setActiveZoneMinutesData(LocalDate date, int cardioMinutes, int peakMinutes, int fatBurnMinutes, int totalActiveMinutes) {
        ActiveZoneMinutesData.ActiveZoneMinutesValue value = new ActiveZoneMinutesData.ActiveZoneMinutesValue(
            totalActiveMinutes,
            fatBurnMinutes,
            cardioMinutes,
            peakMinutes
        );

        ActiveZoneMinutesData.DailyActiveZoneMinutes dailyActiveZoneMinutes = new ActiveZoneMinutesData.DailyActiveZoneMinutes(
            date,
            value
        );

        ActiveZoneMinutesData data = new ActiveZoneMinutesData(
            List.of(dailyActiveZoneMinutes),
            null
        );

        activeZoneMinutesDataMap.put(date, data);
    }

    /**
     * Sets test sleep log data for a specific date.
     */
    public void setSleepLogData(LocalDate date, SleepLogData data) {
        sleepLogDataMap.put(date, data);
    }

    /**
     * Sets test sleep log data with specific minutes asleep.
     */
    public void setSleepLogData(LocalDate date, int minutesAsleep) {
        SleepLogData.SleepSummary summary = new SleepLogData.SleepSummary(
            null,
            minutesAsleep,
            1,
            minutesAsleep + 30
        );

        SleepLogData data = new SleepLogData(
            new ArrayList<>(),
            summary
        );

        sleepLogDataMap.put(date, data);
    }

    /**
     * Sets test sleep log data with specific REM minutes.
     */
    public void setSleepLogDataWithRem(LocalDate date, int remMinutes) {
        SleepLogData.SleepLevelSummaryItem remSummary = new SleepLogData.SleepLevelSummaryItem(
            5,
            remMinutes,
            90
        );

        SleepLogData.SleepLevelSummary levelSummary = new SleepLogData.SleepLevelSummary(
            null,
            null,
            remSummary,
            null
        );

        SleepLogData.SleepLevels levels = new SleepLogData.SleepLevels(
            new ArrayList<>(),
            new ArrayList<>(),
            levelSummary
        );

        SleepLogData.Sleep sleep = new SleepLogData.Sleep(
            date,
            480L * 60 * 1000, // 8 hours in milliseconds
            90,
            LocalDateTime.of(2025, 4, 24, 8, 0),
            0,
            true,
            levels,
            12345L,
            10,
            480, // 8 hours in minutes
            30,
            15,
            LocalDateTime.of(2025, 4, 24, 0, 0),
            525, // 8 hours 45 minutes
            "stages"
        );

        SleepLogData data = new SleepLogData(
            List.of(sleep),
            null
        );

        sleepLogDataMap.put(date, data);
    }

    /**
     * Sets test daily activity summary for a specific date.
     */
    public void setDailyActivitySummary(LocalDate date, DailyActivitySummary data) {
        dailyActivitySummaryMap.put(date, data);
    }

    /**
     * Sets test daily activity summary with specific steps.
     */
    public void setDailyActivitySummaryWithSteps(LocalDate date, int steps) {
        DailyActivitySummary.Summary summary = new DailyActivitySummary.Summary(
            80,
            400,
            1500,
            2000,
            new ArrayList<>(),
            10.0,
            30,
            10,
            new ArrayList<>(),
            120,
            200,
            60,
            1000,
            steps,
            45
        );

        DailyActivitySummary.Goals goals = new DailyActivitySummary.Goals(
            30,
            2000,
            8.0,
            10,
            10000
        );

        DailyActivitySummary data = new DailyActivitySummary(
            new ArrayList<>(),
            goals,
            summary
        );

        dailyActivitySummaryMap.put(date, data);
    }

    /**
     * Sets test daily activity summary with sport activities.
     */
    public void setDailyActivitySummaryWithSportActivity(LocalDate date, boolean hasSportActivity) {
        List<DailyActivitySummary.Activity> activities = new ArrayList<>();

        if (hasSportActivity) {
            activities.add(new DailyActivitySummary.Activity(
                1234L,
                9L,
                "Sport",
                300,
                "Running",
                3600L, // 1 hour in seconds
                true,
                true,
                false,
                LocalDateTime.of(2025, 4, 24, 18, 0),
                5678L,
                "Running",
                date,
                "17:00:00",
                5000
            ));
        }

        DailyActivitySummary.Summary summary = new DailyActivitySummary.Summary(
            80,
            400,
            1500,
            2000,
            new ArrayList<>(),
            10.0,
            30,
            10,
            new ArrayList<>(),
            120,
            200,
            60,
            1000,
            10500,
            45
        );

        DailyActivitySummary.Goals goals = new DailyActivitySummary.Goals(
            30,
            2000,
            8.0,
            10,
            10000
        );

        DailyActivitySummary data = new DailyActivitySummary(
            activities,
            goals,
            summary
        );

        dailyActivitySummaryMap.put(date, data);
    }

    @Override
    public HeartRateData getHeartRateByDate(LocalDate date) {
        return heartRateDataMap.getOrDefault(date, createDefaultHeartRateData(date));
    }

    @Override
    public ActiveZoneMinutesData getActiveZoneMinutesByDate(LocalDate date) {
        return activeZoneMinutesDataMap.getOrDefault(date, createDefaultActiveZoneMinutesData(date));
    }

    @Override
    public SleepLogData getSleepLogByDate(LocalDate date) {
        return sleepLogDataMap.getOrDefault(date, createDefaultSleepLogData(date));
    }

    @Override
    public DailyActivitySummary getDailyActivitySummary(LocalDate date) {
        return dailyActivitySummaryMap.getOrDefault(date, createDefaultDailyActivitySummary(date));
    }

    @Override
    public TokenResponse getAccessTokenWithClientCredentials() {
        // Mock implementation that returns a valid token response
        // This allows the FitbitTool to authenticate without user interaction
        return new TokenResponse() {
            @Override
            public String getAccessToken() {
                return "mock-access-token";
            }

            @Override
            public String getRefreshToken() {
                return "mock-refresh-token";
            }

            @Override
            public long getExpiresIn() {
                return 3600; // 1 hour
            }

            @Override
            public String getTokenType() {
                return "Bearer";
            }

            @Override
            public String getScope() {
                return "heartrate activity sleep weight";
            }

            @Override
            public String getUserId() {
                return "mock-user-id";
            }
        };
    }

    // Helper methods to create default data

    private HeartRateData createDefaultHeartRateData(LocalDate date) {
        return new HeartRateData(new ArrayList<>(), null);
    }

    private ActiveZoneMinutesData createDefaultActiveZoneMinutesData(LocalDate date) {
        return new ActiveZoneMinutesData(new ArrayList<>(), null);
    }

    private SleepLogData createDefaultSleepLogData(LocalDate date) {
        return new SleepLogData(new ArrayList<>(), null);
    }

    private DailyActivitySummary createDefaultDailyActivitySummary(LocalDate date) {
        return new DailyActivitySummary(new ArrayList<>(), null, null);
    }
}
