package io.akka.health.agent;

import akka.javasdk.agent.SessionMessage;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.impl.agent.SessionMemoryClient;
import akka.javasdk.testkit.TestKitSupport;
import com.mongodb.client.MongoClients;
import io.akka.fitbit.FitbitClient;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.common.KeyUtils;
import io.akka.health.ui.application.SessionEntity;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;


/**
 * This is a skeleton for implementing integration tests for an Akka application built with the Akka SDK.
 * <p>
 * It interacts with the components of the application using a componentClient
 * (already configured and provided automatically through injection).
 */
public class IntegrationTest extends TestKitSupport {

    private final static Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

//    @Test
//    public void testThatHealthAgentUsesRAG() {
//        var userId = "user-1";
//        var sessionId = "session-1";
//        var compositeId = userId + "-" + sessionId;
//
//        // Check that the agent used RAG by asking a question that requires it
//        var question = "What is the reason for the patient's visit?";
//        var streamResponse = componentClient
//                .forAgent()
//                .inSession(compositeId)
//                .tokenStream(HealthAgent::ask)
//                .source(question);
//        var answer = await(
//                streamResponse.runFold("", (acc, partial) -> acc + partial, testKit.getMaterializer()),
//                Duration.ofSeconds(30));
//        logger.info("Question: {}", question);
//        logger.info("Answer: {}", answer);
//
//        // Assert that the answer includes "back pain"
//        Assertions.assertTrue(answer.contains("back"));
//        Assertions.assertTrue(answer.contains("pain"));
//
//        // Fetch the Session to ensure the messages have been stored
//        Awaitility.await()
//                .atMost(30, TimeUnit.of(SECONDS))
//                .untilAsserted(() -> {
//                    var messages = getSession(componentClient, compositeId);
//
//                    Assertions.assertFalse(messages.isEmpty(), "Session should not be empty");
//
//                    Assertions.assertInstanceOf(SessionMessage.UserMessage.class, messages.getFirst(), "First message should be of type UserMessage");
//                    SessionMessage.UserMessage userMessage = (SessionMessage.UserMessage) messages.getFirst();
//                    Assertions.assertTrue(userMessage.text().contains(question), "First message should contain the question");
//
//                    Assertions.assertInstanceOf(SessionMessage.AiMessage.class, messages.getLast(), "Second message should be of type AiMessage");
//                    SessionMessage.AiMessage aiMessage = (SessionMessage.AiMessage) messages.getLast();
//                    Assertions.assertTrue(aiMessage.text().contains(answer), "Last message should contain the answer");
//                });
//    }

    private List<SessionMessage> getSession(ComponentClient componentClient, String sessionId) {
        SessionMemoryClient sessionMemoryClient = new SessionMemoryClient(
                componentClient,
                new SessionMemoryClient.MemorySettings(true, false, Optional.of(Integer.MAX_VALUE)));

        var history = sessionMemoryClient.getHistory(sessionId);
        return history.messages();
    }
}
