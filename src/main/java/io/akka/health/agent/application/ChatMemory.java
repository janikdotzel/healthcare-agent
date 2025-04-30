package io.akka.health.agent.application;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChatMemory {

    private final static Logger logger = LoggerFactory.getLogger(ChatMemory.class);

    public ChatMemory() {}

    public MessageWindowChatMemory getChatMemory(String sessionId, List<ChatMessage> messages) {
        logger.info("Creating chat memory store for sessionId: {}", sessionId);

        var chatMemoryStore = new InMemoryChatMemoryStore();
        chatMemoryStore.updateMessages(sessionId, messages);

        return MessageWindowChatMemory.builder()
                .maxMessages(2000)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
