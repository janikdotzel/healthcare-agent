package io.akka.health.agent.application;

import akka.Done;
import akka.javasdk.client.ComponentClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import io.akka.health.ui.application.SessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractAgent {

    private final static Logger logger = LoggerFactory.getLogger(AbstractAgent.class);

    private final ComponentClient componentClient;

    public AbstractAgent(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    protected Done addExchangeToSession(String sessionId, SessionEntity.Exchange conversation) {
        return componentClient
                .forEventSourcedEntity(sessionId)
                .method(SessionEntity::addExchange)
                .invoke(conversation);
    }

    protected List<ChatMessage> fetchSessionHistory(String sessionId) {
        var history = componentClient
                .forEventSourcedEntity(sessionId)
                .method(SessionEntity::getHistory)
                .invoke();

        return history.messages().stream().map(this::toChatMessage).toList();
    }

    private ChatMessage toChatMessage(SessionEntity.Message msg) {
        return switch (msg.type()) {
            case ASSISTANT -> new AiMessage(msg.content());
            case USER -> new UserMessage(msg.content());
        };
    }
}
