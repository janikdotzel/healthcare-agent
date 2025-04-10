package io.akka.health.ui.domain;

import java.time.Instant;
import akka.javasdk.annotations.TypeName;

public sealed interface SessionEvent {

  @TypeName("user-message-added")
  public record UserMessageAdded(String userId, String sessionId, String query, int tokensUsed, Instant timeStamp) implements SessionEvent {}

  @TypeName("assistant-message-added")
  public record AssistantMessageAdded(String userId, String sessionId, String response, int tokensUsed, Instant timeStamp) implements SessionEvent {}
}
