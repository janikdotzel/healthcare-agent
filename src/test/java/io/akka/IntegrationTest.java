package io.akka;

import akka.Done;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.http.HttpResponses;
import akka.javasdk.http.StrictResponse;
import com.mongodb.client.MongoClients;
import io.akka.health.common.KeyUtils;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.ingest.api.IngestionEndpoint;
import io.akka.health.ingest.application.MedicalRecordEntity;
import io.akka.health.ingest.application.SensorEntity;
import io.akka.health.ingest.domain.Index;
import io.akka.health.ingest.domain.MedicalRecord;
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

  // TODO split it up into different test classes in different packages

  // ----------------- Ingestion Tests -----------------

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
  public void testMedicalRecordEntity() {
    String patientId = "patient-1";
    MedicalRecord medicalRecord = new MedicalRecord(
            patientId,
            "Severe lower back pain",
            "Pinched nerve",
            "Ibuprofen and massage therapy",
            "Has an office job. Sits for long hours. Doesn't do any exercise.");
    Done response = await(
            componentClient
                    .forEventSourcedEntity(patientId)
                    .method(MedicalRecordEntity::addData)
                    .invokeAsync(medicalRecord));

    Assertions.assertNotNull(response);
    MedicalRecordEntity.State state = await(
            componentClient
                    .forEventSourcedEntity(patientId)
                    .method(MedicalRecordEntity::getState)
                    .invokeAsync());

    Assertions.assertEquals(medicalRecord.reasonForVisit(), state.data().getFirst().reasonForVisit());
    Assertions.assertEquals(medicalRecord.diagnosis(), state.data().getFirst().diagnosis());
    Assertions.assertEquals(medicalRecord.prescribedMedication(), state.data().getFirst().prescribedMedication());
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
  public void testIndexing() {
    var mongoDbconfig = new MongoDbUtils.MongoDbConfig(
            MongoClients.create(KeyUtils.readMongoDbUri()),
            "health",
            "medicalrecord",
            "medicalrecord-ingest");
    Index index = new Index(mongoDbconfig);

    MedicalRecord medicalRecord = new MedicalRecord(
            "patient-1",
            "Severe lower back pain",
            "Pinched nerve",
            "Ibuprofen and massage therapy",
            "Has an office job. Sits for long hours. Doesn't do any exercise.");
    Done response = await(index.indexMedicalRecord(medicalRecord));

    Assertions.assertNotNull(response);
  }


  // ----------------- Agent Tests -----------------

    // Ask gets a successful reply
      // Assert that previous messages from the session are used
      // Assert that the AI queries sensor data via a tool call
      // Assert that medical records are augmented via RAG
      // Assert that the AI has been called


}
