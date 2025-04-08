# Healthcare AI Agent

## Project Description

### Goal
Provide personalized, proactive, and accessible healthcare support for everyone.
Improving patient well-being and reducing the burden on traditional healthcare systems, where it's not necessary.

### Implementation
A web application will act as a personal healthcare agent, integrating data from various sources (wearables, medical records, patient input) to provide real-time health insights, personalized recommendations, a weekly report and immediate support.

### Functional Requirements
1. **Data Integration:**
    - The system shall integrate with wearable devices (e.g., smartwatches, fitness trackers) via APIs to collect real-time health data (heart rate, sleep patterns, activity levels, etc.).
    - The system shall securely integrate with electronic health record (EHR) systems via APIs to access patient medical history, lab results, and existing diagnoses.
2. **AI-Powered Chat Interface:**
    - The system shall provide a natural language chat interface for patients to ask health-related questions and describe symptoms.
    - The AI agent shall analyze patient input, interpret medical terminology, and provide accurate and relevant responses.
    - The AI agent shall be capable of providing personalized advice, suggesting exercises, recommending lifestyle changes and even prescribe medicine based on patient data and medical history. (Prescribing Medicine requires the approval from a doctor)
3. **Real-Time Health Monitoring and Alerts:**
    - The system shall continuously monitor patient health data from wearables and other sources.
    - The system shall define customizable thresholds for critical health metrics (e.g., heart rate, blood pressure).
    - The system shall automatically generate alerts when critical health metrics exceed defined thresholds and notify the patient.
4. **Personalized Health Reports:**
    - The system shall generate weekly health reports summarizing patient health data, trends, and insights.
    - The reports shall include personalized recommendations for improving health and well-being.
    - The reports should be easy to understand for non-medical personal.

### Non-Functional Requirements
1. **Performance:**
    - The system shall respond to user queries and generate reports within a reasonable timeframe (e.g., within seconds).
    - The system shall be able to handle a large volume of user data and concurrent requests.
2. **Usability:**
    - The web application shall have a user-friendly and intuitive interface.
    - The AI chat interface shall be easy to use and understand.
3. **Reliability:**
    - The system shall be available and reliable, with minimal downtime.
    - The system should provide reliable information, and clearly state when it is providing information that is not from a medical professional.
4. **Scalability:**
    - The system shall be scalable to accommodate a growing number of users and data sources.
5. **Maintainability:**
    - The system shall be designed for easy maintenance and updates.
6. **Accuracy:**
    - The AI must provide medical information that is as accurate as possible.
    - The AI must state when it is unsure of an answer, and recommend contacting a medical professional.

## Usage

Use Maven to build your project:
```shell
mvn compile
```

To start your service locally, run:
```shell
mvn compile exec:java
```

The endpoint is available at:
```shell
curl http://localhost:9000/hello
```

Build container image:
```shell
mvn clean install -DskipTests
```