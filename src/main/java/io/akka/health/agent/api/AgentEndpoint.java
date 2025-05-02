package io.akka.health.agent.api;

import akka.http.javadsl.model.*;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.http.HttpClient;
import akka.javasdk.http.HttpClientProvider;
import fitbit.FitbitClient;
import io.akka.health.agent.application.FitbitTool;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.agent.domain.StreamedResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.http.HttpResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Scanner;


@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
@HttpEndpoint("/agent")
public class AgentEndpoint {

  private static final Logger log = LoggerFactory.getLogger(AgentEndpoint.class);

  public record AskRequest(String userId, String sessionId, String question) {}

  private final HealthAgent agent;
  private final FitbitClient fitbitClient;

  public AgentEndpoint(HealthAgent agent, FitbitClient fitbitClient) {
    this.agent = agent;
    this.fitbitClient = fitbitClient;
  }

  @Post("/ask")
  public HttpResponse ask(AskRequest request) {
    log.info("Received request: {}", request);
    var response = agent
        .ask(request.userId, request.sessionId, request.question)
        .map(StreamedResponse::content);

    return HttpResponses.serverSentEvents(response);
  }
}
