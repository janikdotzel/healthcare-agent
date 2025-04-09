package io.akka.health.agent.application;

import akka.Done;
import io.akka.health.agent.domain.SessionEvent;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.akka.health.agent.application.SessionEntity.MessageType.ASSISTANT;
import static io.akka.health.agent.application.SessionEntity.MessageType.USER;

@ComponentId("session-entity")
public class SessionEntity extends EventSourcedEntity<SessionEntity.State, SessionEvent> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public record Exchange(
          String userId,
          String sessionId,
          String userQuestion,
          int queryTokensCount,
          String assistantResponse,
          int responseTokensCount) {}

  enum MessageType {
    ASSISTANT,
    USER
  }

  public record Message(String content, MessageType type) {}
  public record Messages(List<Message> messages) {}

  public record State(List<Message> messages, int totalTokenUsage) {
    public static State empty() {
      return new State(new ArrayList<>(), 0);
    }

    public State addMessage(Message message) {
      messages.add(message);
      return new State(messages, totalTokenUsage);
    }

    public State addTokenUsage(int usage) {
      return new State(messages, totalTokenUsage + usage);
    }
  }

  public Effect<Done> addExchange(Exchange exchange) {
    var now = Instant.now();

    var userEvent = new SessionEvent.UserMessageAdded(
        exchange.userId,
        exchange.sessionId,
        exchange.userQuestion,
        exchange.queryTokensCount,
        now);

    var assistantEvent = new SessionEvent.AssistantMessageAdded(
      exchange.userId,
      exchange.sessionId,
      exchange.assistantResponse,
      exchange.responseTokensCount,
      now);

    return effects()
        .persist(userEvent, assistantEvent)
        .thenReply(__ -> Done.getInstance());
  }

  public Effect<Messages> getHistory() {
    return effects().reply(new Messages(currentState().messages));
  }

  @Override
  public State emptyState() {
    return State.empty();
  }

  @Override
  public State applyEvent(SessionEvent event) {
    return switch (event) {
      case SessionEvent.UserMessageAdded msg ->
        currentState()
          .addMessage(new Message(msg.query(), USER))
          .addTokenUsage(msg.tokensUsed());

      case SessionEvent.AssistantMessageAdded msg ->
        currentState()
          .addMessage(new Message(msg.response(), ASSISTANT))
          .addTokenUsage(msg.tokensUsed());
    };
  }

}
