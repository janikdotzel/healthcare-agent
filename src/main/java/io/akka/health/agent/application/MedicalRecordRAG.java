package io.akka.health.agent.application;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.common.OpenAiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MedicalRecordRAG {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordRAG.class);
    private final MongoDbUtils.MongoDbConfig mongoDbConfig;

    public MedicalRecordRAG(MongoDbUtils.MongoDbConfig mongoDbConfig) {
            this.mongoDbConfig = mongoDbConfig;
    }

    public DefaultRetrievalAugmentor getAugmentor(String userId) {
        log.info("Creating retrieval augmentor for userId: {}", userId);

        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(MongoDbUtils.embeddingStore(mongoDbConfig))
                .embeddingModel(OpenAiUtils.embeddingModel())
                .maxResults(10)
                .minScore(0.1)
                // Currently the patientId must equal the userId
                .filter(MetadataFilterBuilder.metadataKey("patientId").isEqualTo(userId))
                .build();

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();
    }
}
