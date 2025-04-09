package io.akka.health.ingest.application;


import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.consumer.Consumer;
import com.mongodb.client.MongoClient;
import io.akka.health.ingest.domain.Index;

import java.util.concurrent.CompletionStage;

@ComponentId("medicalrecord-consumer")
@Consume.FromEventSourcedEntity(MedicalRecordEntity.class)
public class MedicalRecordConsumer extends Consumer {

    private final MongoClient mongoClient;

    public MedicalRecordConsumer(MongoClient mongoClient) {
        super();
        this.mongoClient = mongoClient;
    }

    public Effect onEvent(MedicalRecordEntity.Event event) {
        return switch (event) {
            case MedicalRecordEntity.Event.Added added -> {
                Index index = Index.createForMedicalRecord(mongoClient);
                yield effects().asyncDone(index.indexMedicalRecord(added.data()));
            }
        };
    }
}