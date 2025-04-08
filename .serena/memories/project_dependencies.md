## Project Dependencies

The project uses Maven for dependency management. Based on the requirements, the following dependencies will be needed:

### Core Dependencies
- **Akka Java SDK Parent** (version 3.2.3): Main framework for building reactive systems
- **Akka HTTP**: For REST API endpoints
- **Akka gRPC**: For efficient communication with external systems
- **JSON Processing**: For handling JSON data (e.g., Jackson or Gson)

### AI and Machine Learning
- **TensorFlow Java** or **DL4J**: For AI model integration
- **Natural Language Processing**: Libraries for processing patient queries
- **Apache OpenNLP** or similar: For medical text analysis

### Data Processing
- **Apache Kafka**: For real-time data streaming and event processing
- **Apache Spark** (optional): For larger scale data analytics

### External Integrations
- **FHIR Client**: For healthcare data standards
- **OAuth2 Client**: For secure authorization with external systems
- **HTTP Clients**: For API integrations with wearable devices

### Testing
- **JUnit**: For unit testing
- **Mockito**: For mocking dependencies in tests
- **AssertJ**: For fluent assertions