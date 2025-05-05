package io.akka.health.agent.api;

import akka.http.javadsl.model.*;
import akka.javasdk.client.ComponentClient;
import com.mongodb.client.MongoClient;
import io.akka.fitbit.FitbitClient;
import io.akka.health.agent.application.HealthAgent;
import io.akka.health.agent.domain.StreamResponse;
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

    public record AskRequest(String userId, String sessionId, String question) {
    }

    private final ComponentClient componentClient;
    private final MongoClient mongoClient;
    private final FitbitClient fitbitClient;


    public AgentEndpoint(ComponentClient componentClient, MongoClient mongoClient, FitbitClient fitbitClient) {
        this.componentClient = componentClient;
        this.mongoClient = mongoClient;
        this.fitbitClient = fitbitClient;
    }

    @Post("/ask")
    public HttpResponse ask(AskRequest request) {
        log.info("Received request: {}", request);

        var agent = new HealthAgent(componentClient, mongoClient, fitbitClient, request.userId, request.sessionId);
        var response = agent
                .ask(request.question)
                .map(StreamResponse::content);

        return HttpResponses.serverSentEvents(response);
    }
}
