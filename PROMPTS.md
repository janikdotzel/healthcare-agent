# My Prompts

1. 
```
I'm planning to create an AI Agent for Healthcare. This is the readme.md that tells you what the project should do.

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

I'm going to build that app using the akka java sdk.

Read that doc about sdk components to get familiar with them.

We should create an architecture that uses those components to assemble the app.

Akka SDK Components Overview
The Akka SDK provides several key components that enable developers to build responsive, elastic, and resilient cloud applications. Here's an overview of the main components:
Entities
Entities are core components that provide persistence and state management, mapping to your domain aggregates. There are two types:
Event Sourced Entities
Persist events instead of state in an event journal
Current state is derived from these events
Enable independent access to the event journal for read models or business actions
Support multi-region replication
Implementing Event Sourced Entities
Key Value Entities
Store and retrieve objects based on a key/identifier
Every write persists the entire state of the object
Similar to database records
Use an underlying event-based architecture for subscription to updates
Implementing Key Value Entities
Endpoints
Endpoints define interaction points for services that allow external clients to communicate via:
HTTP Endpoints: REST-based communication
gRPC Endpoints: Protocol buffer-based communication
Endpoints facilitate integration between internal Akka components and external systems.
HTTP Endpoints gRPC Endpoints
Views
Views provide a way to materialize read-only state from multiple entities based on a query:
Can be created from Key Value Entities, Event Sourced Entities, or by subscribing to topics
Implement the Command Query Responsibility Segregation (CQRS) pattern
Separate reads from writes across multiple services
Support streaming projections
Implementing Views
Workflows
Workflows enable implementation of long-running, multi-step business processes:
Focus exclusively on domain and business logic
Technical concerns like delivery guarantees, scaling, error handling, and recovery are managed by Akka
Support durable, long-running processes orchestrated through Saga patterns
Implementing Workflows
Consumers
Consumers listen for and process events or messages from various sources:
Event Sourced Entities
Key Value Entities
External messaging systems
Can produce messages to topics, facilitating communication between different services
Consuming and Producing
Timed Actions
Timed Actions allow scheduling future calls:
Useful for verifying process completion
Persisted by the Akka Runtime
Guarantee execution at least once
Timed Actions
All these components are designed to work together seamlessly, enabling developers to build applications that can scale to handle hundreds of millions of concurrent users and terabytes of data while maintaining low latency and enabling no-downtime disaster recovery.

Then create a PLANNING.md and TASK.md

PLANNING.md  - high level direction, scope ,tech, etc.

TASK.md - initial tasks to knock out for the project.
```

> I adjusted the TASK.md and PLANNING.md a little bit. They were too extensive.

2.
```
I'm planning to create an AI Agent for Healthcare. 
The README.md tells you what the proejct is about.
The PLANNING.md tells you about the high level direction, scope, tech , etc.
The TASK.md tells you about all the tasks in the project.

Please update the tasks in the TASK.md file.
I want to start with a PoC and I dont' need the full functionality.
We can mock the integrations with external systems to bet the project started make it real later.
Please update the tasks accordingly. Add or remove tasks as needed.
```

3. 
```
Please do task 2 from the TASK.md file.
Use the Akka SDK as the implementation framework.
Docs about the SDK are in the .akka/docs folder.
```