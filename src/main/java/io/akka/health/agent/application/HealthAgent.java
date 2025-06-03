package io.akka.health.agent.application;

import akka.japi.Function;
import akka.japi.function.Function2;
import akka.japi.function.Function3;
import akka.japi.function.Function4;
import akka.javasdk.agent.Agent;
import akka.javasdk.agent.MemoryProvider;
import akka.javasdk.agent.ModelProvider;
import akka.javasdk.annotations.AgentDescription;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.client.ComponentClient;
import dev.langchain4j.spi.prompt.PromptTemplateFactory;
import io.akka.fitbit.FitbitClient;

import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("health-agent")
@AgentDescription(name = "Health Agent", description = "A personal health assistant with knowledge about the user's health data.")
public class HealthAgent extends Agent {

  private final static Logger logger = LoggerFactory.getLogger(HealthAgent.class);
  private final ComponentClient componentClient;
  private final String systemMessage = """
    You are a personal health assistant that helps the user to stay healthy.
    You have access to the user's health data that is observed through fitness trackers and made available through Fitbit.
    You have access to the user's medical records.
    Answer the question in a concise way.
    """;
  private final String userId;
  private final FitbitClient fitbitClient;
  private final MedicalRecordRAG medicalRecordRAG;

  public HealthAgent(ComponentClient componentClient, MongoClient mongoClient, FitbitClient fitbitClient) {
    this.componentClient = componentClient;
    // The context().sessionId() looks like this `userId + "-" + sessionId`
    this.userId = context().sessionId().split("-")[0];
    this.fitbitClient = fitbitClient;
    this.medicalRecordRAG = new MedicalRecordRAG(mongoClient, userId);
  }

  public Agent.StreamEffect ask(String question) throws Exception {
    // The userId is needed to query the user's Fitbit data.
    String promptTemplate = """
        Question: %s
        Knowledge: %s
        UserId: %s
        """;
    Function4<String, String, String, String, String> render = String::format;

    String knowledge = medicalRecordRAG.retrieve(question, userId);
    String prompt = render.apply(promptTemplate, question, knowledge, userId);

    var fitbitTool = new FitbitTool(fitbitClient);
    var sensorTool = new SensorTool(componentClient);

    //TODO: Add the FitbitTool and the SensorTool to the streamEffect
    return streamEffects()
            .memory(MemoryProvider.limitedWindow())
            .model(ModelProvider.openAi())
            .systemMessage(systemMessage)
            .userMessage(prompt)
            .thenReply();
  }
}
