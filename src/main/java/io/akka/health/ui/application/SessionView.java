
package io.akka.health.ui.application;

import io.akka.health.agent.domain.SessionEvent;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ComponentId("session-view")
public class SessionView extends View {

  private final static Logger logger = LoggerFactory.getLogger(SessionView.class);

  public record Sessions(List<Session> sessions) {}
  public record Message(String message, String origin, long timestamp) {}
  public record Session(String userId, String sessionId, long creationDate, List<Message> messages) {
    public Session addMessage(Message message) {
      messages.add(message);
      return this;
    }
  }
  public record GetSession(String userId, String sessionId) {}

  @Query("SELECT collect(*) as sessions FROM view_chat_log WHERE userId = :userId ORDER by creationDate DESC")
  public QueryEffect<Sessions> getSessionsByUser(String userId) {
    return queryResult();
  }

  @Query("SELECT collect(*) as sessions FROM view_chat_log WHERE userId = :userId AND sessionId = :sessionId ORDER by creationDate DESC")
  public QueryEffect<Sessions> getSessionByUserAndSessionId(GetSession request) {
      String userId = request.userId();
      String sessionId = request.sessionId();
      return queryResult();
  }

  @Consume.FromEventSourcedEntity(SessionEntity.class)
  public static class ChatMessageUpdater extends TableUpdater<Session> {

    public Effect<Session> onEvent(SessionEvent event) {
      return switch (event) {
        case SessionEvent.AssistantMessageAdded added -> assistantMessage(added);
        case SessionEvent.UserMessageAdded added -> userMessage(added);
      };
    }

    private Effect<Session> assistantMessage(SessionEvent.AssistantMessageAdded added) {
      Message newMessage = new Message(added.response(), "assistant", added.timeStamp().toEpochMilli());
      var rowState = rowStateOrNew(added.userId(), added.sessionId());
      return effects().updateRow(rowState.addMessage(newMessage));
    }

    private Effect<Session> userMessage(SessionEvent.UserMessageAdded added) {
      Message newMessage = new Message(added.query(), "user", added.timeStamp().toEpochMilli());
      var rowState = rowStateOrNew(added.userId(), added.sessionId());
      return effects().updateRow(rowState.addMessage(newMessage));
    }

    private Session rowStateOrNew(String userId, String sessionId) {
      if (rowState() != null) return rowState();
      else
        return new Session(
          userId,
          sessionId,
          Instant.now().toEpochMilli(),
          new ArrayList<>());
    }
  }
}
