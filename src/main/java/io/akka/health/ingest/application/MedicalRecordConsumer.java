package io.akka.health.ingest.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.consumer.Consumer;
import com.mongodb.client.MongoClient;
import io.akka.health.common.MongoDbUtils;
import io.akka.health.ingest.domain.Index;

@ComponentId("medicalrecord-consumer")
@Consume.FromEventSourcedEntity(MedicalRecordEntity.class)
public class MedicalRecordConsumer extends Consumer {

    private final MongoDbUtils.MongoDbConfig mongoDbConfig;

    public MedicalRecordConsumer(MongoClient mongoClient) {
        super();
        this.mongoDbConfig = new MongoDbUtils.MongoDbConfig(
                mongoClient,
                "health",
                "medicalrecord",
                "medicalrecord-ingest");
    }

    public Effect onEvent(MedicalRecordEntity.Event event) {
        return switch (event) {
            case MedicalRecordEntity.Event.Added added -> {
                Index index = new Index(mongoDbConfig);
                yield effects().asyncDone(index.indexMedicalRecord(added.data()));
            }
        };
    }
}