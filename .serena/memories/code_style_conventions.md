## Code Style and Conventions

Since this is a new project, we'll follow standard Java conventions and Akka best practices:

### Java Conventions
- **Naming**:
  - Classes: PascalCase (e.g., `PatientEntity`)
  - Methods: camelCase (e.g., `getPatientData()`)
  - Variables: camelCase (e.g., `healthData`)
  - Constants: UPPER_SNAKE_CASE (e.g., `MAX_HEART_RATE`)
  - Packages: lowercase (e.g., `io.akka.domain`)

- **Javadoc**:
  - All public classes, methods, and fields should have Javadoc comments
  - Follow standard Javadoc format with `@param`, `@return`, `@throws` tags where applicable

- **Code Organization**:
  - One class per file
  - Logical grouping of related classes in packages
  - Clear separation of concerns

### Akka Specific Conventions
- Prefer immutable objects for message passing
- Use typed actors and entities
- Follow event sourcing patterns with clear event definitions
- Separate command handlers from event handlers
- Define clear boundaries between different Akka components

### Test Conventions
- Use JUnit for unit testing
- Test class names should end with "Test"
- Test methods should clearly describe what they're testing
- Use meaningful assertions