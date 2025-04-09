package io.akka.health.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain model for a Patient in a healthcare system.
 */
public record Patient(
    String patientId,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String gender,
    Map<String, String> contactInformation,
    Map<String, String> medicalInformation,
    boolean isActive) {

  /**
   * Creates an empty Patient with the given ID and sets it as active.
   */
  public static Patient createEmpty(String patientId) {
    return new Patient(
        patientId,
        null,
        null,
        null,
        null,
        new HashMap<>(),
        new HashMap<>(),
        true);
  }

  /**
   * Updates patient when personal information is added or changed.
   */
  public Patient onPersonalInfoUpdated(PatientEvent.PersonalInfoUpdated event) {
    return new Patient(
        patientId,
        event.firstName(),
        event.lastName(),
        event.dateOfBirth(),
        event.gender(),
        contactInformation,
        medicalInformation,
        isActive);
  }

  /**
   * Updates the patient when contact information is updated.
   */
  public Patient onContactInfoUpdated(PatientEvent.ContactInfoUpdated event) {
    Map<String, String> updatedContactInfo = new HashMap<>(contactInformation);
    updatedContactInfo.put(event.key(), event.value());
    
    return new Patient(
        patientId,
        firstName,
        lastName,
        dateOfBirth,
        gender,
        updatedContactInfo,
        medicalInformation,
        isActive);
  }

  /**
   * Updates the patient when medical information is updated.
   */
  public Patient onMedicalInfoUpdated(PatientEvent.MedicalInfoUpdated event) {
    Map<String, String> updatedMedicalInfo = new HashMap<>(medicalInformation);
    updatedMedicalInfo.put(event.key(), event.value());
    
    return new Patient(
        patientId,
        firstName,
        lastName,
        dateOfBirth,
        gender,
        contactInformation,
        updatedMedicalInfo,
        isActive);
  }

  /**
   * Deactivates a patient.
   */
  public Patient onDeactivated(PatientEvent.Deactivated event) {
    return new Patient(
        patientId,
        firstName,
        lastName,
        dateOfBirth,
        gender,
        contactInformation,
        medicalInformation,
        false);
  }

  /**
   * Reactivates a patient.
   */
  public Patient onReactivated(PatientEvent.Reactivated event) {
    return new Patient(
        patientId,
        firstName,
        lastName,
        dateOfBirth,
        gender,
        contactInformation,
        medicalInformation,
        true);
  }
}