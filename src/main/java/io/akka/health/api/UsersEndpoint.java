package io.akka.health.api;

import io.akka.health.application.ConversationHistoryView;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.client.ComponentClient;

import java.util.concurrent.CompletionStage;

/**
 * Endpoint to fetch user's sessions using the ConversationHistoryView.
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/api")
public class UsersEndpoint {


  private final ComponentClient componentClient;

  public UsersEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Get("/users/{userId}/sessions/")
  public CompletionStage<ConversationHistoryView.ConversationHistory> getSession(String userId) {

    return componentClient.forView()
        .method(ConversationHistoryView::getMessagesBySession)
        .invokeAsync(userId);
  }

}
