package io.akka.health.agent.api;

import akka.http.javadsl.model.*;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.agent.domain.StreamedResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.http.HttpResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/agent")
public class AgentEndpoint {

  private static final Logger log = LoggerFactory.getLogger(AgentEndpoint.class);

  public record AskRequest(String userId, String sessionId, String question) {}

  private final HealthAgent agent;

  public AgentEndpoint(HealthAgent agent) {
    this.agent = agent;
  }

  /**
   * This method runs the search and streams the response to the UI.
   */
  @Post("/ask")
  public HttpResponse ask(AskRequest request) {
    log.info("Received request: {}", request);
    var response = agent
        .ask(request.userId, request.sessionId, request.question)
        .map(StreamedResponse::content);

    return HttpResponses.serverSentEvents(response);
  }
}
