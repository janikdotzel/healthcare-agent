# Personal Health Advisor (AI Agent)

## Description

A personal health advisor agent, integrating data from various sources (fitness trackers, medical records and other sensors)
to provide real-time health insights, personalized recommendations and immediate support for everyone.

Improving your well-being and reducing the burden on traditional healthcare systems, where it's not necessary.

## Architecture

TODO: Insert akka Blueprint for agentic services picture

```
┌─────────────────────┐      ┌────────────────────┐      ┌───────────────────┐
│                     │      │                    │      │                   │
│  External Systems   │◄────►│  Integration Layer │◄────►│  Domain Layer     │
│  (Wearables, EHRs)  │      │  (Endpoints)       │      │  (Entities)       │
│                     │      │                    │      │                   │
└─────────────────────┘      └────────────────────┘      └─────────┬─────────┘
                                                                   │
                                                                   ▼
                             ┌────────────────────┐      ┌───────────────────┐
                             │                    │      │                   │
                             │  User Interface    │◄────►│ Application Layer │
                             │  (Web Application) │      │ (Workflows,Views) │
                             │                    │      │                   │
                             └────────────────────┘      └───────────────────┘
```

### Streaming Endpoints
Respond quickly. Process inputs and respond partially without waiting for the entire input to be processed.

- **Ingestion Endpoint**: 
  - Stores sensor data both in-memory and in a persistent storage
  - Indexes Medical records and stores them in a vector database
- **Agent Endpoint**: Handles incoming requests from users

### Agent 

#### Agent Context Database
LLMs are stateless. We store and retrieve the context of each session.

- **Session Entity**: Represents a user session and stores context data

#### Agent Connectors
Talk to LLMs, Vector DBs, MCP Servers, enterprise APIs and other systems

- **Fitbit**: Access health data from Fitbit devices via an API
- **Sensor Data**: Access data from other sensors through reading our persistent storage
- **Medical Records**: Access Medical history and lab results through RAG (Retrieval-Augmented Generation)

#### Agent Orchestration
Execute reliably. Durable workflows that ensure agent actions and LLM calls execute reliably, even in the face of failures, timeouts, hallucinations, or restarts.








### Event Sourced Entities
- **PatientEntity**: Core domain entity that represents a patient and their health data
- **MedicalRecordEntity**: Stores patient medical history
- Events: HealthMetricRecorded, RecommendationProvided, AlertGenerated

### Key Value Entities
- **UserPreferencesEntity**: User settings and notification preferences
- **ThresholdConfigEntity**: Health metric thresholds for alerts

### Endpoints
- **HealthDataEndpoint (HTTP)**: API for wearable devices to push data
- **PatientQueryEndpoint (HTTP)**: API for patient queries and chat interface
- **MedicalSystemEndpoint (gRPC)**: Integration with EHR systems
- **WebSocketEndpoint**: Real-time communication with the web UI

### Views
- **PatientHealthView**: Materialized view of patient health data for reporting
- **AlertsView**: Aggregated view of patient alerts
- **RecommendationsView**: View of all recommendations for a patient

### Workflows
- **HealthMonitoringWorkflow**: Long-running process to monitor patient health metrics
- **WeeklyReportWorkflow**: Process to generate weekly health reports
- **AlertEscalationWorkflow**: Handles alert notifications and escalations

### Consumers
- **WearableDataConsumer**: Processes incoming wearable device data
- **MedicalRecordConsumer**: Processes updates from EHR systems

### Timed Actions
- **WeeklyReportScheduler**: Schedules generation of weekly reports
- **HealthCheckScheduler**: Periodically checks health metrics against thresholds

## Usage

Start the service locally:
```shell
mvn compile exec:java
```

The chat is available at:
```shell
curl http://localhost:9000/
```

Optionally, you can use the open-webui:
```shell
open-webui serve
```

Open the Akka Local Console for monitoring:
```shell
akka local console
```

## Testing

Add Medical Records with the following command:
```shell
curl -X POST http://localhost:9000/ingest/medical-record -H "Content-Type: application/json" -d '{
  "userId": "user-2",
  "data": {
    "patientId": "user-2",
    "reasonForVisit": "Routine check-up",
    "diagnosis": "Healthy",
    "prescribedMedication": "None",
    "notes": "No issues reported"
  }
}'
```

Add Sensor Data with the following command:
```shell
curl -X POST http://localhost:9000/ingest/sensor -H "Content-Type: application/json" -d '{
  "userId": "user-2",
  "data": {
    "userId": "user-2",
    "source": "smartwatch",
    "description": "heart rate",
    "value": "88 bpm"
  }
}'
```

## Long-Term Vision

### Functional Requirements
1. **Data Integration:**
   - The system shall integrate with wearable devices (e.g., smartwatches, fitness trackers) via APIs to collect real-time health data (heart rate, sleep patterns, activity levels, etc.).
   - The system shall integrate with electronic health record (EHR) systems via APIs to access users medical history, lab results, and existing diagnoses.
2. **AI-Powered Chat Interface:**
   - The system shall provide a natural language chat interface for users to ask health-related questions and describe symptoms.
   - The AI agent shall analyze user input, interpret medical terminology, and provide accurate and relevant responses.
   - The AI agent shall be capable of providing personalized advice, suggesting exercises, recommending lifestyle changes and even prescribe medicine based on patient data and medical history. (Prescribing Medicine requires the approval from a doctor)
3. **Real-Time Health Monitoring and Alerts:**
   - The system shall continuously monitor user health data from wearables and other sources.
   - The system shall define customizable thresholds for critical health metrics (e.g., heart rate, blood pressure).
   - The system shall automatically generate alerts when critical health metrics exceed defined thresholds and notify the user.
4. **Personalized Health Reports:**
   - The system shall generate weekly health reports summarizing user health data, trends, and insights.
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