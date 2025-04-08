## Suggested Commands

### Project Management
- `mvn clean install` - Clean and build the project
- `mvn compile` - Compile the project
- `mvn test` - Run tests
- `mvn package` - Create a JAR package

### Development
- `mvn exec:java -Dexec.mainClass="io.akka.Main"` - Run the application (adjust main class as needed)
- `mvn dependency:tree` - Show dependency tree

### Maven Lifecycle Commands
- `mvn validate` - Validate project configuration
- `mvn compile` - Compile source code
- `mvn test` - Run tests
- `mvn package` - Package compiled code
- `mvn verify` - Run integration tests
- `mvn install` - Install package to local repository

### System Commands (macOS)
- `find . -name "*.java"` - Find all Java files
- `grep -r "pattern" --include="*.java" .` - Search for text in Java files
- `ls -la` - List all files with details
- `git status` - Check Git status
- `git add .` - Add all changes to Git
- `git commit -m "message"` - Commit changes
- `git push` - Push changes to remote repository
- `git pull` - Pull changes from remote repository