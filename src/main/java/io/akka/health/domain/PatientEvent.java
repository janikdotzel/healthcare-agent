package io.akka.health.domain;

import akka.javasdk.annotations.TypeName;
import java.time.LocalDate;

/**
 * Events that can occur to a Patient.
 */
public sealed interface PatientEvent {

  @TypeName("personal-info-updated")
  record PersonalInfoUpdated(
      String firstName,
      String lastName,
      LocalDate dateOfBirth,
      String gender) implements PatientEvent {
  }

  @TypeName("contact-info-updated")
  record ContactInfoUpdated(
      String key,   // e.g., "phone", "email", "address"
      String value) implements PatientEvent {
  }

  @TypeName("medical-info-updated")
  record MedicalInfoUpdated(
      String key,   // e.g., "allergies", "blood-type", "conditions"
      String value) implements PatientEvent {
  }

  @TypeName("patient-deactivated")
  record Deactivated() implements PatientEvent {
  }

  @TypeName("patient-reactivated")
  record Reactivated() implements PatientEvent {
  }
}