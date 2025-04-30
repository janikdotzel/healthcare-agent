package io.akka.health.agent.application;

import akka.javasdk.client.ComponentClient;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.common.OpenAiUtils;
import java.util.List;

public interface RagAssistant {
    TokenStream chat(String message);

    static RagAssistant create(
            String sessionId,
            String userId,
            List<ChatMessage> messages,
            String systemMessage,
            MongoDbUtils.MongoDbConfig mongoDbConfig,
            ComponentClient componentClient) {

        // Set up a langchain retriever to search for relevant medical records
        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(MongoDbUtils.embeddingStore(mongoDbConfig))
                .embeddingModel(OpenAiUtils.embeddingModel())
                .maxResults(10)
                .minScore(0.1)
                // Currently the patientId must equal the userId
                .filter(MetadataFilterBuilder.metadataKey("patientId").isEqualTo(userId))
                .build();
        var retrievalAugmenter = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        // Create the chat memory and fill it with the messages
        var chatMemoryStore = new InMemoryChatMemoryStore();
        chatMemoryStore.updateMessages(sessionId, messages);
        var chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(2000)
                .chatMemoryStore(chatMemoryStore)
                .build();

        // Create the langchain assistant
        return AiServices.builder(RagAssistant.class)
                .systemMessageProvider(__ -> systemMessage)
                .streamingChatLanguageModel(OpenAiUtils.streamingChatModel())
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmenter)
                .tools(new SensorTool(componentClient))
                .build();
    }
}
