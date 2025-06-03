package io.akka.health.agent.api;

import akka.http.javadsl.model.*;
import akka.javasdk.client.ComponentClient;
import io.akka.health.agent.application.HealthAgent;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.http.HttpResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
@HttpEndpoint("/agent")
public class AgentEndpoint {

    private static final Logger log = LoggerFactory.getLogger(AgentEndpoint.class);
    private final ComponentClient componentClient;

    public record AskRequest(String userId, String sessionId, String question) {}

    public AgentEndpoint(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @Post("/ask")
    public HttpResponse ask(AskRequest request) {
        log.info("Received request: {}", request);
        var sessionId = request.userId + "-" + request.sessionId;
        var responseStream = componentClient
                .forAgent()
                .inSession(sessionId)
                .tokenStream(HealthAgent::ask)
                .source(request.question);

        return HttpResponses.serverSentEvents(responseStream);
    }
}
