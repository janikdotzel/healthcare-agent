package io.akka.health.agent.application;

import akka.javasdk.client.ComponentClient;
import dev.langchain4j.agent.tool.Tool;
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
import io.akka.health.ingest.application.SensorView;
import io.akka.health.ingest.domain.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface RagAssistant {
    TokenStream chat(String message);

    class Sensor {
        private final ComponentClient componentClient;
        private final static Logger logger = LoggerFactory.getLogger(Sensor.class);

        Sensor(ComponentClient componentClient) {
            this.componentClient = componentClient;
        }

        @Tool("Get all Sensor Data for a specific user")
        SensorView.AllSensorData getSensorData(String userId) {
            logger.info("Getting all sensor data for user {}", userId);
            return componentClient.forView()
                    .method(SensorView::getSensorDataByByUser)
                    .invokeAsync(userId)
                    .toCompletableFuture()
                    // We don't want blocking calls, but langchain4j doesn't support an async tool call...
                    .join();
        }
    }

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
                .tools(new Sensor(componentClient))
                .build();
    }
}
