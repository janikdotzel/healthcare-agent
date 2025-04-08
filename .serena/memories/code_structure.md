## Code Structure

The project follows a standard Maven project structure with Java source code:

```
healthcare-agent/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/
│   │   │       └── akka/
│   │   │           ├── api/        # API endpoints and controllers
│   │   │           ├── application/ # Application services and business logic
│   │   │           └── domain/     # Domain models and entities
│   │   └── resources/  # Configuration files and resources
│   └── test/           # Test cases
└── pom.xml            # Maven project configuration
```

### Package Organization
- **io.akka.api**: Contains HTTP and gRPC endpoints for external communication
- **io.akka.application**: Application services, workflows, and business logic
- **io.akka.domain**: Domain models, entities, and business rules

This structure follows a clean architecture approach with clear separation of concerns.