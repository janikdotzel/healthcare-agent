package io.akka.health.agent.application;

import akka.javasdk.client.ComponentClient;
import dev.langchain4j.agent.tool.Tool;
import io.akka.health.ingest.application.SensorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorTool {

    private final ComponentClient componentClient;
    private final static Logger logger = LoggerFactory.getLogger(SensorTool.class);

    SensorTool(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @Tool("Get all Sensor Data for a specific user")
    SensorView.AllSensorData getSensorData(String userId) {
        logger.info("Getting all sensor data for user {}", userId);
        return componentClient.forView()
                .method(SensorView::getSensorDataByByUser)
                .invokeAsync(userId)
                .toCompletableFuture()
                //TODO remove blocking call

                // We don't want blocking calls, but langchain4j doesn't support an async tool call...
                .join();
    }
}

