package io.akka.health.index.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.consumer.Consumer;
import com.mongodb.client.MongoClient;
import io.akka.health.index.domain.Index;

@ComponentId("sensor-consumer")
@Consume.FromEventSourcedEntity(SensorEntity.class)
public class SensorConsumer extends Consumer {

    private final MongoClient mongoClient;

    public SensorConsumer(MongoClient mongoClient) {
        super();
        this.mongoClient = mongoClient;
    }

    public Effect onEvent(SensorEntity.Event event) {
        return switch (event) {
            case SensorEntity.Event.Added added -> {
                Index index = Index.createForSensor(mongoClient);
                index.indexSensorData(added.data());
                yield effects().done();
            }
        };
    }
}