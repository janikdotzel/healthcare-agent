package io.akka.health.index.api;

import io.akka.health.index.application.MedicalRecordEntity;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import io.akka.health.index.application.SensorEntity;
import io.akka.health.index.domain.MedicalRecord;
import io.akka.health.index.domain.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@akka.javasdk.annotations.http.HttpEndpoint("/index")
public class HttpEndpoint {

  public record IndexSensorRequest(String userId, SensorData data) {}
  public record IndexMedicalRecordRequest(String userId, MedicalRecord data) {}

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public HttpEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/index-sensor")
  public CompletionStage<HttpResponse> indexSensorData(IndexSensorRequest request) {
    return componentClient.forEventSourcedEntity(request.userId)
            .method(SensorEntity::addData)
            .invokeAsync(request.data)
            .thenApply(__ -> HttpResponses.accepted());
  }

  @Post("/index-medical-record")
  public CompletionStage<HttpResponse> indexMedicalRecord(IndexMedicalRecordRequest request) {
    return componentClient.forEventSourcedEntity(request.userId)
            .method(MedicalRecordEntity::addData)
            .invokeAsync(request.data)
            .thenApply(__ -> HttpResponses.accepted());
  }
}
