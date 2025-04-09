package io.akka.health.index.domain;

import akka.Done;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.common.OpenAiUtils;
import com.mongodb.client.MongoClient;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Index {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final OpenAiEmbeddingModel embeddingModel;
  private final MongoDbEmbeddingStore embeddingStore;
  private final DocumentSplitter splitter;

  public Index(MongoClient mongoClient, String databaseName, String collectionName, String indexName) {
    this.embeddingModel = OpenAiUtils.embeddingModel();
    this.embeddingStore = MongoDbUtils.mongoDbEmbeddingStore(
      mongoClient,
      databaseName,
      collectionName,
      indexName);
    this.splitter = new DocumentByCharacterSplitter(500, 50, OpenAiUtils.buildTokenizer());
  }

  public static Index createForSensor(MongoClient mongoClient) {
    String databaseName = "health";
    String collectionName = "sensor";
    String indexName = "sensor-index";
    return new Index(mongoClient, databaseName, collectionName, indexName);
  }

  public static Index createForMedicalRecord(MongoClient mongoClient) {
      String databaseName = "health";
      String collectionName = "medicalrecord";
      String indexName = "medicalrecord-index";
      return new Index(mongoClient, databaseName, collectionName, indexName);
  }

  public CompletionStage<Done> indexSensorData(SensorData sensorData) {
    Metadata metadata = Metadata.metadata("source", sensorData.source());
    metadata.put("description", sensorData.description());
    Document document = Document.from(sensorData.value());
    List<TextSegment> segments = splitter.split(document);

    // index each segment
    var done = CompletableFuture.completedFuture(Done.getInstance());
    return segments.stream().reduce(
                    done,
                    (acc, seg) -> indexSegment(seg),
                    (a,b) -> done);
  }

  public CompletionStage<Done> indexMedicalRecord(MedicalRecord medicalRecord) {
    Metadata metadata = Metadata.metadata("patientId", medicalRecord.patientId());
    Document document = Document.from(medicalRecord.toString());
    List<TextSegment> segments = splitter.split(document);

    // index each segment
    var done = CompletableFuture.completedFuture(Done.getInstance());
    return segments.stream().reduce(
            done,
            (acc, seg) -> indexSegment(seg),
            (a,b) -> done);
  }

  private CompletableFuture<Done> indexSegment(TextSegment segment) {
    return CompletableFuture
            // embed the segment
            .supplyAsync(() -> embeddingModel.embed(segment))
            // store the embedding in MongoDB
            .thenCompose(res ->
                    CompletableFuture.supplyAsync(() -> embeddingStore.add(res.content(), segment)))
            .thenApply(__ -> Done.getInstance());
  }
}
