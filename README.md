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
Can you tell me the reason for my last visit to the doctor?
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
Can you tell me the reason for my last visit to the doctor?
```

Example response:
```text
The reason for your last visit to the doctor was severe lower back pain. You were diagnosed with a pinched nerve, and prescribed medication included ibuprofen along with massage therapy. The notes indicated that you have an office job, sit for long hours, and do not engage in any exercise.
```

### Fitbit Data

Ask the agent:
```text
How many steps did I take on the 26th April 2025?
```
(The agent uses the Fitbit Tool to get the data from the Fitbit API)

Example Response:
```text
On April 26, 2025, you took 17,243 steps.
```

Ask the agent
```text 
How much REM sleep did i get in the week from 21st april 27th april 2025 on each day?
```

Example Response:
```text
Here is the REM sleep you recorded for each day from April 21 to April 27, 2025:
April 21: 95 minutes
April 22: 121 minutes
April 23: 113 minutes
April 24: 105 minutes
April 25: 82 minutes
April 26: 121 minutes
April 27: 29 minutes
```

Ask the agent
```text
What other health metrics do you have for the 27th april 2025?
```

Example Response:
```text
On April 27, 2025, here are the health metrics for you:

Sleep Hours: Approximately 5.43 hours
Steps Walked: 7,107 steps
Resting Heart Rate: 63 bpm
Active Minutes for the Week: 510 minutes
REM Sleep Minutes: 29 minutes
Heart Rate Outside Safe Range: 0 (your heart rate was within the safe range)
Please remember to consider your back pain and the importance of incorporating exercise and movement into your daily routine, especially given the sedentary nature of your job.
```