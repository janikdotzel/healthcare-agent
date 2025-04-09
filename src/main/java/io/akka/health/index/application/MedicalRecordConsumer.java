package io.akka.health.index.application;


import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.consumer.Consumer;
import com.mongodb.client.MongoClient;
import io.akka.health.index.domain.Index;

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
                index.indexMedicalRecord(added.data());
                yield effects().done();
            }
        };
    }
}