package io.akka.health.ingest.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import io.akka.health.ingest.domain.MedicalRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@ComponentId("medicalrecord-entity")
public class MedicalRecordEntity extends EventSourcedEntity<MedicalRecordEntity.State, MedicalRecordEntity.Event> {

  public record State(String id, List<MedicalRecord> data) {}
  public sealed interface Event {
    record Added(MedicalRecord data) implements Event {}
  }

  private final String entityId;
  private static final Logger logger = LoggerFactory.getLogger(SensorEntity.class);

  public MedicalRecordEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public MedicalRecordEntity.State emptyState() {
    return new MedicalRecordEntity.State(entityId, Collections.emptyList());
  }

  public Effect<Done> addData(MedicalRecord data) {
    var event = new Event.Added(data);

    return effects()
            .persist(event)
            .thenReply(newState -> Done.getInstance());
  }

  public ReadOnlyEffect<MedicalRecordEntity.State> getState() {
    return effects().reply(currentState());
  }

  @Override
  public MedicalRecordEntity.State applyEvent(MedicalRecordEntity.Event event) {
    return switch (event) {
      case MedicalRecordEntity.Event.Added added -> {
        var newData = currentState().data();
        newData.add(added.data);
        yield new MedicalRecordEntity.State(currentState().id(), newData);
      }
    };
  }
}
