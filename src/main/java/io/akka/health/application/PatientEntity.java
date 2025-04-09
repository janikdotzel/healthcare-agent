package io.akka.health.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.akka.health.domain.Patient;
import io.akka.health.domain.PatientEvent;

import java.time.LocalDate;

/**
 * Event sourced entity for managing patient data in a healthcare system.
 */
@ComponentId("patient")
public class PatientEntity extends EventSourcedEntity<Patient, PatientEvent> {

  private final String entityId;
  private static final Logger logger = LoggerFactory.getLogger(PatientEntity.class);

  public PatientEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Patient emptyState() {
    return Patient.createEmpty(entityId);
  }

  /**
   * Updates personal information of the patient.
   */
  public Effect<Done> updatePersonalInfo(String firstName, String lastName, LocalDate dateOfBirth, String gender) {
    if (!currentState().isActive()) {
      logger.info("Patient id={} is not active.", entityId);
      return effects().error("Patient is not active.");
    }

    var event = new PatientEvent.PersonalInfoUpdated(firstName, lastName, dateOfBirth, gender);

    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  /**
   * Updates or adds a piece of contact information for the patient.
   */
  public Effect<Done> updateContactInfo(String key, String value) {
    if (!currentState().isActive()) {
      logger.info("Patient id={} is not active.", entityId);
      return effects().error("Patient is not active.");
    }

    if (key == null || key.isBlank()) {
      return effects().error("Contact information key cannot be empty.");
    }

    var event = new PatientEvent.ContactInfoUpdated(key, value);

    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  /**
   * Updates or adds a piece of medical information for the patient.
   */
  public Effect<Done> updateMedicalInfo(String key, String value) {
    if (!currentState().isActive()) {
      logger.info("Patient id={} is not active.", entityId);
      return effects().error("Patient is not active.");
    }

    if (key == null || key.isBlank()) {
      return effects().error("Medical information key cannot be empty.");
    }

    var event = new PatientEvent.MedicalInfoUpdated(key, value);

    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  /**
   * Deactivate a patient.
   */
  public Effect<Done> deactivatePatient() {
    if (!currentState().isActive()) {
      logger.info("Patient id={} is already inactive.", entityId);
      return effects().reply(Done.getInstance());
    }

    var event = new PatientEvent.Deactivated();

    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  /**
   * Reactivate a patient.
   */
  public Effect<Done> reactivatePatient() {
    if (currentState().isActive()) {
      logger.info("Patient id={} is already active.", entityId);
      return effects().reply(Done.getInstance());
    }

    var event = new PatientEvent.Reactivated();

    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  /**
   * Get the current state of the patient.
   */
  public ReadOnlyEffect<Patient> getPatient() {
    return effects().reply(currentState());
  }

  @Override
  public Patient applyEvent(PatientEvent event) {
    return switch (event) {
      case PatientEvent.PersonalInfoUpdated evt -> currentState().onPersonalInfoUpdated(evt);
      case PatientEvent.ContactInfoUpdated evt -> currentState().onContactInfoUpdated(evt);
      case PatientEvent.MedicalInfoUpdated evt -> currentState().onMedicalInfoUpdated(evt);
      case PatientEvent.Deactivated evt -> currentState().onDeactivated(evt);
      case PatientEvent.Reactivated evt -> currentState().onReactivated(evt);
    };
  }
}