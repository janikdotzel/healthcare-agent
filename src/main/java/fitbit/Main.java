package fitbit;

import akka.javasdk.http.HttpClientProvider;
import io.akka.health.agent.application.FitbitTool;
import akka.javasdk.http.HttpClient;

import java.time.LocalDate;
import java.util.Scanner;


/**
 * Main class that demonstrates the Fitbit API OAuth2 flow and heart rate data retrieval.
 */
public class Main {

    public static void main(String[] args) {

        // Initialize the Fitbit client
        HttpClient httpClient = null;
        FitbitClient fitbitClient = new FitbitClient(httpClient);

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
        FitbitClient.TokenResponse tokenResponse = fitbitClient.exchangeCodeForAccessToken(authCode);

        System.out.println("Successfully obtained access token!");
        System.out.println("Access Token: " + tokenResponse.getAccessToken());
        System.out.println("Refresh Token: " + tokenResponse.getRefreshToken());
        System.out.println("Expires In: " + tokenResponse.getExpiresIn() + " seconds");

        // ----------------------------------------------------------------------------------------

//        FitbitTool checker = new FitbitTool(fitbitClient);
//
//        // Check when was the last time the resting heart rate was above 65
//        System.out.println("\nChecking when was the last time the resting heart rate was above 65...");
//
//        // Get the date for 10th april 2025
//        LocalDate currentDate = LocalDate.of(2025, 4, 6);
////            LocalDate currentDate = LocalDate.now();
//        LocalDate foundDate = null;
//        boolean dateFound = false;
//
//        // Check the last 30 days
//        for (int i = 0; i < 30 && !dateFound; i++) {
//            LocalDate dateToCheck = currentDate.minusDays(i);
//
//            try {
//                // Check if resting heart rate was 65 or higher on this date
//                Integer heartRate = checker.restingHeartRate(dateToCheck);
//                System.out.println("Checking date: " + dateToCheck + " - Resting heart rate was " + heartRate);
//                if (heartRate >= 65) {
//                    foundDate = dateToCheck;
//                    dateFound = true;
//                }
//            } catch (Exception e) {
//                System.err.println("Error checking heart rate for date " + dateToCheck + ": " + e.getMessage());
//            }
//        }
//
//        if (dateFound) {
//            System.out.println("The last time the resting heart rate was above 65 was on: " + foundDate);
//        } else {
//            System.out.println("No days found with resting heart rate above 65 in the last 30 days.");
//        }
    }
}
