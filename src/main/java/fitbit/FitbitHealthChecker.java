package fitbit;

import fitbit.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Class that provides health check methods based on Fitbit data.
 * Uses FitbitClient to retrieve data and performs specific health checks.
 */
public class FitbitHealthChecker {
    private final FitbitClient fitbitClient;

    /**
     * Constructor for FitbitHealthChecker.
     *
     * @param fitbitClient The FitbitClient to use for retrieving data
     */
    public FitbitHealthChecker(FitbitClient fitbitClient) {
        this.fitbitClient = fitbitClient;
    }

    /**
     * Gets the resting heart rate if it is above the threshold.
     *
     * @param date The date to check
     * @param threshold The threshold in bpm (default is 65)
     * @return CompletionStage with Optional containing the resting heart rate if above threshold, empty otherwise
     */
    public CompletionStage<Optional<Integer>> isRestingHeartRateAboveThreshold(LocalDate date, int threshold) {
        return fitbitClient.getHeartRateByDate(date)
                .thenApply(heartRateData -> {
                    if (heartRateData.activitiesHeart().isEmpty()) {
                        return Optional.empty();
                    }

                    HeartRateData.HeartRateValue value = heartRateData.activitiesHeart().get(0).value();
                    Integer restingHeartRate = value.restingHeartRate();

                    if (restingHeartRate != null && restingHeartRate > threshold) {
                        return Optional.of(restingHeartRate);
                    } else {
                        return Optional.empty();
                    }
                });
    }

    /**
     * Gets the heart rate value that exceeded the safe range the most.
     *
     * @param date The date to check
     * @param minThreshold The minimum safe threshold in bpm (default is 40)
     * @param maxThreshold The maximum safe threshold in bpm (default is 170)
     * @return CompletionStage with Optional containing the heart rate value that exceeded the safe range the most, empty if all values are within range
     */
    public CompletionStage<Optional<Integer>> isHeartRateOutsideSafeRange(LocalDate date, int minThreshold, int maxThreshold) {
        return fitbitClient.getHeartRateByDate(date)
                .thenApply(heartRateData -> {
                    if (heartRateData.activitiesHeartIntraday() == null || 
                        heartRateData.activitiesHeartIntraday().dataset() == null || 
                        heartRateData.activitiesHeartIntraday().dataset().isEmpty()) {
                        return Optional.empty();
                    }

                    List<HeartRateData.HeartRateDataPoint> dataPoints = heartRateData.activitiesHeartIntraday().dataset();

                    Integer maxDeviation = null;
                    Integer mostExtremeValue = null;

                    for (HeartRateData.HeartRateDataPoint point : dataPoints) {
                        int value = point.value();
                        int deviation = 0;

                        if (value < minThreshold) {
                            deviation = minThreshold - value;
                        } else if (value > maxThreshold) {
                            deviation = value - maxThreshold;
                        }

                        if (deviation > 0 && (maxDeviation == null || deviation > maxDeviation)) {
                            maxDeviation = deviation;
                            mostExtremeValue = value;
                        }
                    }

                    return mostExtremeValue != null ? Optional.of(mostExtremeValue) : Optional.empty();
                });
    }


    /**
     * Gets the total active minutes for the user in a week.
     *
     * @param startDate The start date of the week
     * @param endDate The end date of the week
     * @return CompletionStage with integer representing total active minutes in the week
     */
    public CompletionStage<Integer> getActiveMinutesInWeek(LocalDate startDate, LocalDate endDate) {
        List<CompletionStage<ActiveZoneMinutesData>> futures = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            futures.add(fitbitClient.getActiveZoneMinutesByDate(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int totalActiveMinutes = 0;

                    for (CompletionStage<ActiveZoneMinutesData> future : futures) {
                        ActiveZoneMinutesData data = ((CompletableFuture<ActiveZoneMinutesData>) future).join();

                        if (data.activitiesActiveZoneMinutes() != null && !data.activitiesActiveZoneMinutes().isEmpty()) {
                            ActiveZoneMinutesData.ActiveZoneMinutesValue value = data.activitiesActiveZoneMinutes().get(0).value();

                            if (value.activeZoneMinutes() != null) {
                                totalActiveMinutes += value.activeZoneMinutes();
                            }
                        }
                    }

                    return totalActiveMinutes;
                });
    }

    /**
     * Gets the number of hours the user slept on a particular day.
     *
     * @param date The date to check
     * @return CompletionStage with double representing hours slept on that day
     */
    public CompletionStage<Double> getSleepHoursForDay(LocalDate date) {
        return fitbitClient.getSleepLogByDate(date)
                .thenApply(sleepLogData -> {
                    if (sleepLogData.summary() != null && sleepLogData.summary().totalMinutesAsleep() != null) {
                        return sleepLogData.summary().totalMinutesAsleep() / 60.0;
                    } else {
                        return 0.0;
                    }
                });
    }

    /**
     * Gets the amount of REM sleep in minutes for a specific date.
     *
     * @param date The date to check
     * @return CompletionStage with integer representing minutes of REM sleep
     */
    public CompletionStage<Integer> getRemSleepMinutes(LocalDate date) {
        return fitbitClient.getSleepLogByDate(date)
                .thenApply(sleepLogData -> {
                    if (sleepLogData.sleep() == null || sleepLogData.sleep().isEmpty()) {
                        return 0;
                    }

                    int totalRemMinutes = 0;

                    for (SleepLogData.Sleep sleep : sleepLogData.sleep()) {
                        if (sleep.levels() != null && sleep.levels().summary() != null && 
                            sleep.levels().summary().rem() != null && sleep.levels().summary().rem().minutes() != null) {
                            totalRemMinutes += sleep.levels().summary().rem().minutes();
                        }
                    }

                    return totalRemMinutes;
                });
    }

    /**
     * Gets all sport activities (sport, gym, aerobic) for the specified week.
     *
     * @param startDate The start date of the week
     * @param endDate The end date of the week
     * @return CompletionStage with list of sport activities
     */
    public CompletionStage<List<DailyActivitySummary.Activity>> getSportActivitiesInWeek(LocalDate startDate, LocalDate endDate) {
        List<CompletionStage<DailyActivitySummary>> futures = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            futures.add(fitbitClient.getDailyActivitySummary(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<DailyActivitySummary.Activity> sportActivities = new ArrayList<>();

                    for (CompletionStage<DailyActivitySummary> future : futures) {
                        DailyActivitySummary data = ((CompletableFuture<DailyActivitySummary>) future).join();

                        if (data.activities() != null) {
                            // Collect activities that are sports or intensive (like gym or aerobic)
                            List<DailyActivitySummary.Activity> intensiveActivities = data.activities().stream()
                                    .filter(activity -> {
                                        String name = activity.name() != null ? activity.name().toLowerCase() : "";
                                        String parentName = activity.activityParentName() != null ? activity.activityParentName().toLowerCase() : "";

                                        return name.contains("sport") || name.contains("gym") || name.contains("aerobic") ||
                                               parentName.contains("sport") || parentName.contains("gym") || parentName.contains("aerobic");
                                    })
                                    .collect(Collectors.toList());

                            sportActivities.addAll(intensiveActivities);
                        }
                    }

                    return sportActivities;
                });
    }

    /**
     * Gets the number of steps walked on a specific day.
     *
     * @param date The date to check
     * @return CompletionStage with integer representing steps walked on that day
     */
    public CompletionStage<Integer> getStepsForDay(LocalDate date) {
        return fitbitClient.getDailyActivitySummary(date)
                .thenApply(data -> {
                    if (data.summary() != null && data.summary().steps() != null) {
                        return data.summary().steps();
                    } else {
                        return 0;
                    }
                });
    }
}
