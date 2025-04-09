package io.akka.health.ingest.api;

import io.akka.health.ingest.application.MedicalRecordEntity;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import io.akka.health.ingest.application.SensorEntity;
import io.akka.health.ingest.domain.MedicalRecord;
import io.akka.health.ingest.domain.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@akka.javasdk.annotations.http.HttpEndpoint("/ingest")
public class IngestionEndpoint {

  public record IndexSensorRequest(String userId, SensorData data) {}
  public record IndexMedicalRecordRequest(String userId, MedicalRecord data) {}

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public IngestionEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/sensor")
  public CompletionStage<HttpResponse> ingestSensorData(IndexSensorRequest request) {
    return componentClient.forEventSourcedEntity(request.userId)
            .method(SensorEntity::addData)
            .invokeAsync(request.data)
            .thenApply(__ -> HttpResponses.accepted());
  }

  @Post("/medical-record")
  public CompletionStage<HttpResponse> ingestMedicalRecord(IndexMedicalRecordRequest request) {
    return componentClient.forEventSourcedEntity(request.userId)
            .method(MedicalRecordEntity::addData)
            .invokeAsync(request.data)
            .thenApply(__ -> HttpResponses.accepted());
  }
}
