package io.akka;

import akka.Done;
import io.akka.health.ingest.application.SensorEntity;
import io.akka.health.ingest.domain.SensorData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import akka.javasdk.testkit.TestKitSupport;



/**
 * This is a skeleton for implementing integration tests for an Akka application built with the Akka SDK.
 *
 * It interacts with the components of the application using a componentClient
 * (already configured and provided automatically through injection).
 */
public class IntegrationTest extends TestKitSupport {


  // ----------------- Ingestion Tests -----------------

  // SensorEntity Tests
  @Test
  public void testSensorEntity() {
    String userId = "user-1";
    SensorData sensorData = new SensorData(userId, "smartwatch", "heart rate", "75 bpm");
    Done response = await(
            componentClient
                    .forEventSourcedEntity(userId)
                    .method(SensorEntity::addData)
                    .invokeAsync(sensorData));

    Assertions.assertNotNull(response);
    SensorEntity.State sensorState = await(
            componentClient
                    .forEventSourcedEntity(userId)
                    .method(SensorEntity::getState)
                    .invokeAsync());

    Assertions.assertEquals(sensorData.source(), sensorState.data().getFirst().source());
    Assertions.assertEquals(sensorData.description(), sensorState.data().getFirst().description());
    Assertions.assertEquals(sensorData.value(), sensorState.data().getFirst().value());
  }


    // MedicalRecordEntity Tests

    // Index Tests


  // ----------------- Agent Tests -----------------

    // Ask gets a successful reply
      // Assert that previous messages from the session are used
      // Assert that the AI queries sensor data via a tool call
      // Assert that medical records are augmented via RAG
      // Assert that the AI has been called




  @Test
  public void test() throws Exception {
    // implement your integration tests here by calling your
    // components by using the `componentClient`
  }
}
