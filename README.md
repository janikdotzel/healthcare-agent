# Personal Health Advisor (AI Agent)

## Description

A personal health advisor agent, integrating data from various sources (fitness trackers, medical records and other sensors)
to provide real-time health insights, personalized recommendations and immediate support for everyone.

Improving your well-being and reducing the burden on traditional healthcare systems, where it's not necessary.

## Architecture

```
┌────────────────────────────┐      ┌──────────────────────┐      ┌──────────────────────────────┐
│                            │      │                      │      │                              │
│  Streaming Endpoints       │◄────►│  Agent Orchestration │◄────►│  Agent Connectors            │
│  (Sensor, Medical Record,  │      │                      │      │  (LLM, Fitbit, Sensor,       │
│   Agent)                   │      │                      │      │   Vector DB)                 │
└────────────────────────────┘      └────────────┬─────────┘      └──────────────┬───────────────┘
                                                 │                               │
                                                 ▼                               ▼
                                    ┌──────────────────────┐            ┌────────────────────────┐
                                    │                      │            │                        │
                                    │  Agent Context DB    │            │  Vector DB             │
                                    │  (Session Entity)    │            │  (Medical Records)     │
                                    │                      │            │                        │
                                    └──────────────────────┘            └────────────────────────┘
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

> Currently, the agent is not orchestrated through Akka.  
> The agent does LLM calls and uses the SensorTool, FitbitTool as well as doing RAG on Medical Records. 
> But that functionality comes from the Langchain AI Service and is not executed durably.

## Usage

Setup Environment Variables

```text
Copy the `.env.example` file to `.env` and set the following environment variables:
```

Start the service locally:
```shell
mvn compile exec:java
```

The chat is available at:
```shell
http://localhost:9000/
```

Open the Akka Local Console for monitoring:
```shell
akka local console
```

## Testing

Visit:
```shell
http://localhost:9000/
```

### Sensor Data

Ask the agent:
```text
Can you tell me my heart rate by searching the sensor data?
```

Example response:
```text
It seems that there is no heart rate data available in your sensor data.
```  

Ingest Sensor Data:
```shell
curl -X POST http://localhost:9000/ingest/sensor -H "Content-Type: application/json" -d '{
  "userId": "demo-user",
  "data": {
    "userId": "demo-user",
    "source": "smartwatch",
    "description": "heart rate",
    "value": "90 bpm"
  }
}'
```

Ask the agent:
```text
Can you tell me my heart rate by searching the sensor data?
```

Example response:
```text
Your heart rate is 90 bpm.
```

### Medical Record (RAG)

Ask the agent:
```text
Can you tell the reason for my last visit to the doctor?
```

Example response:
```text
I don't have specific details of your medical records.
``` 

Ingest Medical Records:
```shell
curl -X POST http://localhost:9000/ingest/medical-record -H "Content-Type: application/json" -d '{
  "userId": "demo-user",
  "data": {
    "patientId": "demo-user",
    "reasonForVisit": "Severe lower back pain",
    "diagnosis": "Pinched nerve",
    "prescribedMedication": "Ibuprofen and massage therapy",
    "notes": "Has an office job. Sits for long hours. Doesnt do any exercise."
  }
}'
```

Ask the agent:
```text
Can you tell the reason for my last visit to the doctor?
```

Example response:
```text
The reason for your last visit to the doctor was severe lower back pain. You were diagnosed with a pinched nerve, and prescribed medication included ibuprofen along with massage therapy. The notes indicated that you have an office job, sit for long hours, and do not engage in any exercise.
```

### Fitbit Data

TODO


Ask the agent a question:
```text
How many steps did I take on the 26th April 2025?
```
(The agent uses the Fitbit Tool to get the data from the Fitbit API)

```text 
How much REM sleep did i get in the week from 21st april 27th april 2025 on each day? 
If I have less than 90 minutes on a day I feel exhausted after waking up.
What can I do to improve my sleep?
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