## Tech Stack

### Core Technologies
- Java (JDK version unspecified, assuming latest stable)
- Akka Java SDK (version 3.2.3)
- Maven for dependency management and build

### Akka Components Used
1. **Event Sourced Entities** - For persisting patient data and health events
2. **Key Value Entities** - For storing configuration and user preferences
3. **HTTP/gRPC Endpoints** - For external integrations with wearables and EHR systems
4. **Views** - For materializing read-only state for health reports and monitoring
5. **Workflows** - For orchestrating long-running health monitoring processes
6. **Consumers** - For processing events from wearables and other sources
7. **Timed Actions** - For scheduling health checks and periodic reports

### Development Environment
- Operating System: macOS (Darwin)
- Build Tool: Maven