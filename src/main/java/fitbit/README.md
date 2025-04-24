# Fitbit API Client

This package provides a client for interacting with the Fitbit API using OAuth2 authentication. It allows you to:

1. Request authorization from a user to access their data
2. Query the user's heart rate data by date
3. Refresh expired access tokens with the corresponding refresh token

## Setup

### Environment Variables

The client requires the following environment variables to be set:

- `FITBIT_CLIENT_ID`: Your Fitbit application's client ID
- `FITBIT_CLIENT_SECRET`: Your Fitbit application's client secret

You can set these variables in your environment or add them to the `.env` file in the `src/main/resources` directory.

### Creating a Fitbit Application

1. Go to [dev.fitbit.com](https://dev.fitbit.com/) and log in
2. Create a new application with the following settings:
   - Application Type: Personal
   - Callback URL: http://localhost:8080/fitbit/callback
   - Default Access Type: Read-Only

After creating the application, you'll receive a client ID and client secret that you can use with this client.

## Usage

See the `Example.java` file for a complete example of how to use the client. Here's a summary:

```java
// Create an ActorSystem
ActorSystem system = ActorSystem.create("fitbit-example");

// Initialize the Fitbit client
FitbitClient fitbitClient = new FitbitClient(system);

// Step 1: Generate the authorization URL
String authUrl = fitbitClient.getAuthorizationUrl();
// Direct the user to this URL to authorize your application

// Step 2: Exchange the authorization code for an access token
CompletionStage<FitbitClient.TokenResponse> tokenFuture = 
    fitbitClient.exchangeCodeForToken(authorizationCode);
FitbitClient.TokenResponse tokenResponse = tokenFuture.toCompletableFuture().get();

// Step 3: Get heart rate data for a specific date
LocalDate date = LocalDate.now();
CompletionStage<String> heartRateFuture = fitbitClient.getHeartRateByDate(date);
String heartRateData = heartRateFuture.toCompletableFuture().get();
```

## OAuth2 Flow

The OAuth2 flow implemented by this client works as follows:

1. The client generates an authorization URL that the user must visit in their browser
2. The user logs in to Fitbit and authorizes your application
3. Fitbit redirects the user to your callback URL with an authorization code
4. Your application exchanges this code for an access token and refresh token
5. The access token is used to make API requests
6. When the access token expires, the refresh token is used to obtain a new access token

## API Endpoints

Currently, the client supports the following Fitbit API endpoints:

- Heart Rate by Date: `/1/user/-/activities/heart/date/{date}/1d.json`

You can extend the client to support additional endpoints as needed.