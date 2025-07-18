package io.akka.health.ingest;

import akka.Done;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.http.HttpResponses;
import akka.javasdk.http.StrictResponse;
import com.mongodb.client.MongoClients;
import io.akka.health.common.KeyUtils;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.ingest.api.IngestionEndpoint;
import io.akka.health.ingest.application.SensorEntity;
import io.akka.health.ingest.application.SensorView;
import io.akka.health.ingest.domain.Index;
import io.akka.health.ingest.domain.MedicalRecord;
import io.akka.health.ingest.domain.SensorData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import akka.javasdk.testkit.TestKitSupport;
import java.time.Duration;
import java.util.concurrent.TimeUnit;



/**
 * This is a skeleton for implementing integration tests for an Akka application built with the Akka SDK.
 *
 * It interacts with the components of the application using a componentClient
 * (already configured and provided automatically through injection).
 */
public class IntegrationTest extends TestKitSupport {

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

  @Test
  public void testSensorEndpoint() {
    String userId = "user-1";
    SensorData sensorData = new SensorData(userId, "smartwatch", "heart rate", "75 bpm");
    IngestionEndpoint.IngestSensorRequest request = new IngestionEndpoint.IngestSensorRequest(userId, sensorData);
    HttpResponse response = await(
            httpClient
                    .POST("/ingest/sensor")
                    .withRequestBody(request)
                    .invokeAsync()
                    .thenApply(StrictResponse::httpResponse));

    Assertions.assertEquals(HttpResponses.accepted().status(), response.status());
  }

    @Test
    public void testMedicalRecordEndpoint() {
      String patientId = "patient-1";
      MedicalRecord medicalRecord = new MedicalRecord(
              patientId,
              "Severe lower back pain",
              "Pinched nerve",
              "Ibuprofen and massage therapy",
              "Has an office job. Sits for long hours. Doesn't do any exercise.");
      IngestionEndpoint.IngestMedicalRecordRequest request = new IngestionEndpoint.IngestMedicalRecordRequest(patientId, medicalRecord);
      HttpResponse response = await(
              httpClient
                      .POST("/ingest/medical-record")
                      .withRequestBody(request)
                      .invokeAsync()
                      .thenApply(StrictResponse::httpResponse));

      Assertions.assertEquals(HttpResponses.accepted().status(), response.status());
    }

  @Test
  public void testSensorView() {
    // First, add some sensor data
    String userId = "user-view-test";
    SensorData sensorData = new SensorData(userId, "smartwatch", "heart rate", "75 bpm");

    // Add the data using SensorEntity
    Done addResponse = await(
            componentClient
                    .forEventSourcedEntity(userId)
                    .method(SensorEntity::addData)
                    .invokeAsync(sensorData));
    Assertions.assertNotNull(addResponse);

    // Wait a bit to allow the view to be updated (views are eventually consistent)
    try {
        TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    // Query the data using SensorView with retries
    SensorView.AllSensorData viewResponse = null;
    int maxRetries = 3;
    boolean dataFound = false;

    for (int i = 0; i < maxRetries && !dataFound; i++) {
        viewResponse = await(
                componentClient
                        .forView()
                        .method(SensorView::getSensorDataByByUser)
                        .invokeAsync(userId));

        // Check if we got data
        if (viewResponse != null && !viewResponse.data().isEmpty()) {
            dataFound = true;
        } else {
            // Wait before retrying
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Verify the response
    Assertions.assertNotNull(viewResponse, "View response should not be null");
    Assertions.assertTrue(dataFound, "Sensor data should be found after retries");

    if (dataFound) {
        // Verify the data matches what we added
        SensorData retrievedData = viewResponse.data().getFirst();
        Assertions.assertEquals(userId, retrievedData.userId());
        Assertions.assertEquals(sensorData.source(), retrievedData.source());
        Assertions.assertEquals(sensorData.description(), retrievedData.description());
        Assertions.assertEquals(sensorData.value(), retrievedData.value());
    }
  }

  @Test
  public void testIndexing() {
    var mongoDbconfig = new MongoDbUtils.MongoDbConfig(
            MongoClients.create(KeyUtils.readMongoDbUri()),
            "health",
            "medicalrecord",
            "medicalrecord-ingest");
    Index index = new Index(mongoDbconfig);

    MedicalRecord medicalRecord = new MedicalRecord(
            "user-1",
            "Severe lower back pain",
            "Pinched nerve",
            "Ibuprofen and massage therapy",
            "Has an office job. Sits for long hours. Doesn't do any exercise.");
    Done response = await(index.indexMedicalRecord(medicalRecord));

    Assertions.assertNotNull(response);
  }
}
