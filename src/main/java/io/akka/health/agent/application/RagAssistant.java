package io.akka.health.agent.application;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.common.OpenAiUtils;

import java.util.List;

public interface RagAssistant {
    TokenStream chat(String message);

    static RagAssistant create(String sessionId, List<ChatMessage> messages, String systemMessage, MongoDbUtils.MongoDbConfig mongoDbConfig) {
        var chatLanguageModel = OpenAiUtils.streamingChatModel();

        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(MongoDbUtils.embeddingStore(mongoDbConfig))
                .embeddingModel(OpenAiUtils.embeddingModel())
                .maxResults(10)
                .minScore(0.1)
                .build();

        var retrievalAugmenter = DefaultRetrievalAugmentor.builder()
                        .contentRetriever(contentRetriever)
                        .build();

        var chatMemoryStore = new InMemoryChatMemoryStore();
        chatMemoryStore.updateMessages(sessionId, messages);

        var chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(2000)
                .chatMemoryStore(chatMemoryStore)
                .build();

        return AiServices.builder(RagAssistant.class)
                .systemMessageProvider(__ -> systemMessage)
                .streamingChatLanguageModel(chatLanguageModel)
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmenter)
                .build();
    }
}
