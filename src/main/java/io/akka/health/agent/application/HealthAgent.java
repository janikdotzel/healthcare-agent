package io.akka.health.agent.application;

import akka.Done;
import akka.NotUsed;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import io.akka.health.common.AkkaStreamUtils;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.common.OpenAiUtils;
import io.akka.health.common.StreamedResponse;
import akka.javasdk.client.ComponentClient;
import akka.stream.javadsl.Source;
import com.mongodb.client.MongoClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import io.akka.health.ui.application.SessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * This services works as an interface to the AI.
 * It returns the AI response as a stream and can be used to stream out a response using for example SSE in an
 * UiEndpoint.
 *
 * The service is configured as a RAG agent that uses the OpenAI API to generate responses based on the Akka SDK documentation.
 * It uses a MongoDB Atlas ingest to retrieve relevant documentation sections for the user's question.
 *
 * Moreover, the whole RAG setup is done through LangChain4j APIs.
 *
 * The chat memory is preserved on a SessionEntity. At start of each new exchange, the existing chat memory is
 * retrieved and include into the chat context. Once the exchange finished, the latest pair of messages (user message
 * and AI message) are saved to the SessionEntity.
 *
 */
public class HealthAgent {

  private final static Logger logger = LoggerFactory.getLogger(HealthAgent.class);
  private final ComponentClient componentClient;
  private final MongoDbUtils.MongoDbConfig mongoDbConfig;
  private final String systemMessage = """
    You are a medical assistant that helps the doctor to answer questions about the patient.
    Given the following medical records, answer the question.
    """;

  public HealthAgent(ComponentClient componentClient, MongoClient mongoClient) {
    this.componentClient = componentClient;
    this.mongoDbConfig = new MongoDbUtils.MongoDbConfig(
        mongoClient,
        "health",
        "medicalrecord",
        "medicalrecord-index");
  }

    /**
     * This method is the main entry point for the agent.
     * It takes a userId, sessionId and a question and returns a stream of responses.
     *
     * @param userId    The user id
     * @param sessionId The session id
     * @param question  The question to ask
     * @return A stream of responses
     */
  public Source<StreamedResponse, NotUsed> ask(String userId, String sessionId, String question) {

    // we want the SessionEntity id to be unique for each user session,
    // therefore we use a composite key of userId and sessionId
    var compositeEntityId = userId + ":" + sessionId;

    // Fetch chat messages by looking up the session
    var sessionHistoryFuture = fetchSessionHistory(compositeEntityId);

    // Assemble the langchain assistant
    var assistantFuture = sessionHistoryFuture.thenApply(messages -> {
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

      // TODO: Make Sensor Data available through a tool call

      // Create the chat memory and fill it with the messages
      var chatMemoryStore = new InMemoryChatMemoryStore();
      chatMemoryStore.updateMessages(compositeEntityId, messages);
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
              .build();
    });

    // Build an akka source
    return Source.completionStage(assistantFuture)
      // Call the llm and get the response streamed back
      .flatMapConcat(assistant -> AkkaStreamUtils.toAkkaSource(assistant.chat(question)))
        .mapAsync(1, res -> {
          if (res.finished()) { // is the last message?
            logger.debug("Exchange finished. Total input tokens {}, total output tokens {}", res.inputTokens(), res.outputTokens());

            // when we have a finished response, we save the exchange to the SessionEntity
            var exchange = new SessionEntity.Exchange(
              userId,
              sessionId,
              question, res.inputTokens(),
              res.content(), res.outputTokens());

            logger.info("Complete Response: {}", res.content());

            // since the full response has already been streamed,
            // the last message can be transformed to an empty message
            return addExchangeToSession(compositeEntityId, exchange)
                    .thenApply(__ -> StreamedResponse.empty());
          }
          else {
            logger.debug("partial message '{}'", res.content());
            // other messages are streamed out to the caller
            // (those are the responseTokensCount emitted by the llm)
            return CompletableFuture.completedFuture(res);
          }
        });
  }

  private CompletionStage<Done> addExchangeToSession(String compositeEntityId, SessionEntity.Exchange conversation) {
    return componentClient
            .forEventSourcedEntity(compositeEntityId)
            .method(SessionEntity::addExchange)
            .invokeAsync(conversation);
  }

  private CompletionStage<List<ChatMessage>> fetchSessionHistory(String sessionId) {
    return componentClient
            .forEventSourcedEntity(sessionId)
            .method(SessionEntity::getHistory).invokeAsync()
            .thenApply(messages -> messages.messages().stream()
                    .map(this::toChatMessage).toList());
  }

  private ChatMessage toChatMessage(SessionEntity.Message msg) {
    return switch (msg.type()) {
      case ASSISTANT -> new AiMessage(msg.content());
      case USER -> new UserMessage(msg.content());
    };
  }
}
