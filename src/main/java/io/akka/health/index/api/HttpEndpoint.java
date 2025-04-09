package io.akka.health.index.api;

import io.akka.health.index.application.RagIndexingWorkflow;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@akka.javasdk.annotations.http.HttpEndpoint("/api/index")
public class HttpEndpoint {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ComponentClient componentClient;

  public HttpEndpoint(ComponentClient componentClient) {

    this.componentClient = componentClient;
  }

  @Post("/start")
  public CompletionStage<HttpResponse> startIndexation() {
    return componentClient.forWorkflow("indexing")
      .method(RagIndexingWorkflow::start)
      .invokeAsync()
      .thenApply(__ -> HttpResponses.accepted());
  }

  @Post("/abort")
  public CompletionStage<HttpResponse> abortIndexation() {
    return componentClient.forWorkflow("indexing")
      .method(RagIndexingWorkflow::abort)
      .invokeAsync()
      .thenApply(__ -> HttpResponses.accepted());
  }
}
