package io.akka.health.common;

import com.mongodb.client.MongoClient;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;

public class MongoDbUtils {

  public static EmbeddingStore<TextSegment> embeddingStore(
          MongoClient mongoClient,
          String databaseName,
          String collectionName,
          String indexName) {
    return MongoDbEmbeddingStore.builder()
      .fromClient(mongoClient)
      .databaseName(databaseName)
      .collectionName(collectionName)
      .indexName(indexName)
      .createIndex(true)
      .build();
  }

  public static MongoDbEmbeddingStore mongoDbEmbeddingStore(
          MongoClient mongoClient,
          String databaseName,
          String collectionName,
          String indexName) {
    return (MongoDbEmbeddingStore) embeddingStore(mongoClient, databaseName, collectionName, indexName);
  }
}
