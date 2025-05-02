package io.akka.health;

import akka.javasdk.http.HttpClient;
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
  private final HttpClient httpClient;
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
    // If it is a dns name prefixed with "http://" or "https://" it will connect to services available on the public internet.
    this.httpClient = httpClientProvider.httpClientFor("https://akka.io/");
    this.fitbitClient = new FitbitClient(httpClient);
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
            return (T) new FitbitClient(httpClient);
        }
        return null;
      }
    };
  }
}
