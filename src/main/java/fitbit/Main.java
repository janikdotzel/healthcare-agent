package fitbit;

import akka.actor.ActorSystem;
import fitbit.model.HeartRateData;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Main class that demonstrates the Fitbit API OAuth2 flow and heart rate data retrieval.
 */
public class Main {

    public static void main(String[] args) {
        // Create an ActorSystem
        ActorSystem system = ActorSystem.create("fitbit-example");

        try {
            // Initialize the Fitbit client
            FitbitClient fitbitClient = new FitbitClient(system);

            // Step 1: Generate the authorization URL
            String authUrl = fitbitClient.getAuthorizationUrl();
            System.out.println("Please open the following URL in your browser:");
            System.out.println(authUrl);
            System.out.println("\nAfter authorizing the application, you will be redirected to a callback URL.");
            System.out.println("Copy the 'code' parameter from the URL and paste it here:");

            // Read the authorization code from the console
            Scanner scanner = new Scanner(System.in);
            String authCode = scanner.nextLine().trim();

            // Step 2: Exchange the authorization code for an access token
            CompletionStage<FitbitClient.TokenResponse> tokenFuture = fitbitClient.exchangeCodeForToken(authCode);
            FitbitClient.TokenResponse tokenResponse = tokenFuture.toCompletableFuture().get();

            System.out.println("Successfully obtained access token!");
            System.out.println("Access Token: " + tokenResponse.getAccessToken());
            System.out.println("Refresh Token: " + tokenResponse.getRefreshToken());
            System.out.println("Expires In: " + tokenResponse.getExpiresIn() + " seconds");

            // Step 3: Get heart rate data for today
            LocalDate today = LocalDate.now();
            CompletionStage<HeartRateData> heartRateFuture = fitbitClient.getHeartRateByDate(today);
            HeartRateData heartRateData = heartRateFuture.toCompletableFuture().get();

            System.out.println("\nHeart Rate Data for " + today + ":");
            System.out.println("Activities Heart: " + heartRateData.activitiesHeart());

            if (!heartRateData.activitiesHeart().isEmpty() && 
                heartRateData.activitiesHeart().get(0).value() != null && 
                heartRateData.activitiesHeart().get(0).value().restingHeartRate() != null) {
                System.out.println("Resting Heart Rate: " + heartRateData.activitiesHeart().get(0).value().restingHeartRate());
            }

            if (heartRateData.activitiesHeartIntraday() != null && 
                heartRateData.activitiesHeartIntraday().dataset() != null) {
                System.out.println("Intraday Data Points: " + heartRateData.activitiesHeartIntraday().dataset().size());
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Terminate the ActorSystem
            system.terminate();
        }
    }
}
