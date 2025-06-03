package io.akka.health.agent.application;

import com.mongodb.client.MongoClient;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.common.OpenAiUtils;

public class MedicalRecordRAG {
    private final RetrievalAugmentor retrievalAugmentor;
    private final ContentInjector contentInjector = new DefaultContentInjector();

    public MedicalRecordRAG(MongoClient mongoClient, String userId) {
        var mongoDbConfig = new MongoDbUtils.MongoDbConfig(
                mongoClient,
                "health",
                "medicalrecord",
                "medicalrecord-index");
        var contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(MongoDbUtils.embeddingStore(mongoDbConfig))
                .embeddingModel(OpenAiUtils.embeddingModel())
                .maxResults(10)
                .minScore(0.1)
                // Currently the patientId must equal the userId
                .filter(MetadataFilterBuilder.metadataKey("patientId").isEqualTo(userId))
                .build();

        this.retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();
    }

    public String retrieve(String question, String userId) {
        var chatMessage = new UserMessage(question);
        var metadata = Metadata.from(chatMessage, null, null);
        var augmentationRequest = new AugmentationRequest(chatMessage, metadata);

        var result = retrievalAugmentor.augment(augmentationRequest);
        UserMessage augmented = (UserMessage) contentInjector.inject(result.contents(), chatMessage);
        return augmented.singleText();
    }

}
