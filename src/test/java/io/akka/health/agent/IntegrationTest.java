package io.akka.health.agent;

import akka.Done;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.javasdk.http.StrictResponse;
import akka.javasdk.testkit.TestKitSupport;
import akka.stream.javadsl.Sink;
import com.mongodb.client.MongoClients;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.common.KeyUtils;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.ingest.api.IngestionEndpoint;
import io.akka.health.ingest.application.MedicalRecordEntity;
import io.akka.health.ingest.application.SensorEntity;
import io.akka.health.ingest.domain.Index;
import io.akka.health.ingest.domain.MedicalRecord;
import io.akka.health.ingest.domain.SensorData;
import io.akka.health.ui.application.SessionEntity;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This is a skeleton for implementing integration tests for an Akka application built with the Akka SDK.
 * <p>
 * It interacts with the components of the application using a componentClient
 * (already configured and provided automatically through injection).
 */
public class IntegrationTest extends TestKitSupport {

    private final static Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

    @Test
    public void testThatHealthAgentUsesRAG() {
        var userId = "user-1";
        var sessionId = "session-1";
        var compositeId = userId + ":" + sessionId;
        var agent = new HealthAgent(componentClient, MongoClients.create(KeyUtils.readMongoDbUri()));

        // Check that the agent used RAG by asking a question that requires it
        var question = "What is the reason for the patient's visit?";
        var streamResponse = agent.ask(userId, sessionId, question);
        var answer = await(
                streamResponse.runFold("", (acc, partial) -> acc + partial.content(), testKit.getMaterializer()));
        logger.info("Question: {}", question);
        logger.info("Answer: {}", answer);

        // Assert that the answer includes "back pain"
        Assertions.assertTrue(answer.contains("back"));
        Assertions.assertTrue(answer.contains("pain"));

        // Fetch the Session to ensure the messages have been stored
        Awaitility.await()
                .atMost(30, TimeUnit.of(SECONDS))
                .untilAsserted(() -> {
                    var session = getSession(componentClient, compositeId);

                    // Assert that the question and the answer are saved in the session
                    Assertions.assertTrue(session.messages().stream().anyMatch(message -> Objects.equals(message.content(), question)));
                    Assertions.assertTrue(session.messages().stream().anyMatch(message -> Objects.equals(message.content(), answer)));
                });
    }

    @Test
    public void testThatHealthAgentUsesToolCalling() {
        // TODO Assert that the AI queries sensor data via a tool call
    }

    @Test
    public void testAgentEndpoint() {
        // TODO
    }

    private SessionEntity.Messages getSession(ComponentClient componentClient, String sessionId) {
        var session = await(componentClient
                .forEventSourcedEntity(sessionId)
                .method(SessionEntity::getHistory)
                .invokeAsync());
        return session;
    }
}
