package io.akka.health;

import akka.javasdk.http.HttpClientProvider;
import fitbit.FitbitClient;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.common.KeyUtils;
import akka.javasdk.DependencyProvider;
import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import akka.javasdk.client.ComponentClient;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Setup
public class Bootstrap implements ServiceSetup {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final MongoClient mongoClient;
  private final ComponentClient componentClient;
  private final FitbitClient fitbitClient;

  public Bootstrap(ComponentClient componentClient, HttpClientProvider httpClientProvider, com.typesafe.config.Config config) {

    if (!KeyUtils.hasValidKeys()) {
      throw new IllegalStateException(
        "No API keys found. When running locally, make sure you have a " + ".env file located under " +
          "src/main/resources/ (see src/main/resources/.env.example). When running in production, " +
          "make sure you have OPENAI_API_KEY and MONGODB_ATLAS_URI defined as environment variable.");
    }

    this.componentClient = componentClient;
    this.mongoClient = MongoClients.create(KeyUtils.readMongoDbUri());
    this.fitbitClient = new FitbitClient(httpClientProvider.httpClientFor("https://api.fitbit.com"));

    // Retrieve your Tokens manually via https://dev.fitbit.com/build/reference/web-api/troubleshooting-guide/oauth2-tutorial/?clientEncodedId=23Q95R&redirectUri=https://janikdotzel.com/&applicationType=SERVER
    // FitBits API does not support client credentials flow, so you need to use the authorization code flow that requires user interaction.
    fitbitClient.setTokens(
            "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyM1E5NVIiLCJzdWIiOiI3RlI3WDIiLCJpc3MiOiJGaXRiaXQiLCJ0eXAiOiJhY2Nlc3NfdG9rZW4iLCJzY29wZXMiOiJyc29jIHJlY2cgcnNldCByaXJuIHJveHkgcm51dCBycHJvIHJzbGUgcmNmIHJhY3QgcmxvYyBycmVzIHJ3ZWkgcmhyIHJ0ZW0iLCJleHAiOjE3NDYyMDkzOTEsImlhdCI6MTc0NjE4MDU5MX0.5eY9PKTApPqRS7RLZ9vpBYZEN5poSLCPfubn8X3bvnQ",
            "faa0dddb8891c4cf76a5806dde80ebb51e8f9d441c2a98bca900f43cd1223b5a",
            28800);
  }

  @Override
  public DependencyProvider createDependencyProvider() {
    return new DependencyProvider() {
      @Override
      public <T> T getDependency(Class<T> cls) {
        if (cls.equals(HealthAgent.class)) {
          return (T) new HealthAgent(componentClient, mongoClient, fitbitClient);
        }

        if (cls.equals(MongoClient.class)) {
          return (T) mongoClient;
        }

        if (cls.equals(FitbitClient.class)) {
            return (T) fitbitClient;
        }
        return null;
      }
    };
  }
}
