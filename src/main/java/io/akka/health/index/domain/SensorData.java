package io.akka.health.index.domain;

/**
 * Represents the data collected from a sensor.
 *
 * @param type        The type of the sensor (e.g., "smartwatch", "fitness-tracker", "medical-device").
 * @param description A brief description of the sensor (e.g., "temperature", "heart-rate", "blood-pressure").
 * @param value       The value recorded by the sensor (e.g., "98.6°F", "72 bpm", "120/80 mmHg").
 */
public record SensorData(String type, String description, String value) {}
