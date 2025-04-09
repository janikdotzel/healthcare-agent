package io.akka.health.ui.api;

import io.akka.health.ui.application.SessionView;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;

import java.util.concurrent.CompletionStage;

/**
 * Endpoint to fetch user's sessions using the SessionView.
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/session")
public class SessionEndpoint {

  private final ComponentClient componentClient;

  public SessionEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/{userId}/")
  public CompletionStage<SessionView.Sessions> getSessions(String userId) {

    return componentClient.forView()
        .method(SessionView::getSessionsByUser)
        .invokeAsync(userId);
  }

  @Get("/{userId}/{sessionId}/")
  public CompletionStage<SessionView.Sessions> getSession(String userId, String sessionId) {
    return componentClient.forView()
        .method(SessionView::getSessionByUserAndSessionId)
        .invokeAsync(new SessionView.GetSession(userId, sessionId));
  }

}
