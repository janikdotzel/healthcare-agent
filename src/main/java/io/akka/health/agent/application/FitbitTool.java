package io.akka.health.agent.application;

import dev.langchain4j.agent.tool.Tool;
import fitbit.FitbitClient;
import fitbit.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class FitbitTool {

    private final FitbitClient fitbitClient;

    public FitbitTool(FitbitClient fitbitClient) {
        this.fitbitClient = fitbitClient;
    }

    @Tool("Get resting heart rate for a specific date")
    public CompletionStage<Integer> restingHeartRate(LocalDate date) {
        return fitbitClient.getHeartRateByDate(date)
                .thenApply(heartRateData -> {
                    if (heartRateData.activitiesHeart().isEmpty() || heartRateData.activitiesHeart().getFirst().value().restingHeartRate() == null) {
                        return -1;
                    }

                    HeartRateData.HeartRateValue value = heartRateData.activitiesHeart().getFirst().value();
                    return value.restingHeartRate();
                });
    }

    @Tool("Check if heart rate (in bpm) exceeded the range for a specific date. If exceeded, it returns the value that exceeded the range the most.")
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

    @Tool("Get total active minutes fora specific date range (usually one week).")
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

    @Tool("Get amount of sleep hours for a specific date.")
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

    @Tool("Get amount of REM sleep in minutes for a specific date.")
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

    @Tool("Get all sport activities (sport, gym, aerobic) for a specific date range (usually one week).")
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

    @Tool("Get number of steps walked for a specific date.")
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
