package io.akka.health.agent;

import akka.javasdk.testkit.TestKitSupport;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.agent.model.HealthAgentRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        var compositeId = userId + "-" + sessionId;

        // Check that the agent used RAG by asking a question that requires it
        var question = "What is the reason for the patient's visit?";
        var answer = componentClient
                .forAgent()
                .inSession(compositeId)
                .method(HealthAgent::ask)
                .invoke(new HealthAgentRequest(question, userId));
        logger.info("Question: {}", question);
        logger.info("Answer: {}", answer);

        // Assert that the answer includes "back pain"
        Assertions.assertTrue(answer.contains("back"));
        Assertions.assertTrue(answer.contains("pain"));
    }
}
