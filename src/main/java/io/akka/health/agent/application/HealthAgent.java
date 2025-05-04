package io.akka.health.agent.application;

import akka.NotUsed;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import io.akka.fitbit.FitbitClient;
import io.akka.health.common.AkkaStreamUtils;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.agent.domain.StreamResponse;
import akka.javasdk.client.ComponentClient;
import akka.stream.javadsl.Source;
import com.mongodb.client.MongoClient;
import dev.langchain4j.data.message.ChatMessage;
import io.akka.health.common.OpenAiUtils;
import io.akka.health.ui.application.SessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HealthAgent extends AbstractAgent {

  private final static Logger logger = LoggerFactory.getLogger(HealthAgent.class);
  private final ComponentClient componentClient;
  private final MongoDbUtils.MongoDbConfig mongoDbConfig;
  private final FitbitClient fitbitClient;
  private final String systemMessage = """
    You are a personal health assistant that helps the user to stay healthy.
    You have access to the user's health data that is observed through fitness trackers and made available through Fitbit.
    You have access to the user's medical records.
    Answer the question in a concise way.
    """;

  public HealthAgent(ComponentClient componentClient, MongoClient mongoClient, FitbitClient fitbitClient) {
    super(componentClient);
    this.componentClient = componentClient;
    this.mongoDbConfig = new MongoDbUtils.MongoDbConfig(
        mongoClient,
        "health",
        "medicalrecord",
        "medicalrecord-index");
    this.fitbitClient = fitbitClient;
  }

  public Source<StreamResponse, NotUsed> ask(String userId, String sessionId, String question) {

    // we want the SessionEntity id to be unique for each user session,
    // therefore we use a composite key of userId and sessionId
    var compositeEntityId = userId + ":" + sessionId;
    var sessionHistory = fetchSessionHistory(compositeEntityId);
    var assistant = buildAiService(compositeEntityId, userId, sessionHistory);

    // Call the llm and get the response as a streaming source
    var source = AkkaStreamUtils.toAkkaSource(assistant.chat(question));
    return source.map(res -> {
          if (res.finished()) { // is the last message?
            logger.debug("Exchange finished. Total input tokens {}, total output tokens {}", res.inputTokens(), res.outputTokens());

            // when we have a finished response, we save the exchange to the SessionEntity
            var exchange = new SessionEntity.Exchange(
              userId,
              sessionId,
              question, res.inputTokens(),
              res.content(), res.outputTokens());

            logger.info("Complete Response: {}", res.content());

            // since the full response has already been streamed, the last message can be transformed to an empty message
            addExchangeToSession(compositeEntityId, exchange);
            return StreamResponse.empty();
          }
          else {
            logger.debug("partial message '{}'", res.content());
            // other messages are streamed out to the called (those are the responseTokensCount emitted by the llm)
            return res;
          }
        });
  }

  private interface Assistant {
    TokenStream chat(String message);
  }

  private Assistant buildAiService(String sessionId, String userId, List<ChatMessage> messages) {
    logger.info("Building AiService for sessionId {} and userId {}", sessionId, userId);

    var retrievalAugmentor = new MedicalRecordRAG(mongoDbConfig).getAugmentor(userId);
    var chatMemory = new ChatMemory().getChatMemory(sessionId, messages);

    return AiServices.builder(Assistant.class)
            .systemMessageProvider(__ -> systemMessage)
            .streamingChatLanguageModel(OpenAiUtils.streamingChatModel())
            .chatMemory(chatMemory)
            .retrievalAugmentor(retrievalAugmentor)
            .tools(new FitbitTool(fitbitClient), new SensorTool(componentClient))
            .build();
  }
}
