# Digicell Application Logging Setup

## Overview

The Digicell application now includes comprehensive logging that outputs to both the terminal (console) and log files. This setup preserves all existing logging functionality while adding robust file-based logging capabilities.

## Log Files Generated

The application creates the following log files in the `logs/` directory:

1. **`Digicell-all.log`** - Contains all log messages (DEBUG, INFO, WARN, ERROR)
2. **`Digicell-error.log`** - Contains only ERROR level messages
3. **`Digicell-info.log`** - Contains INFO, WARN, and ERROR messages (excludes DEBUG)
4. **`Digicell-debug.log`** - Contains only DEBUG level messages

## Log File Features

### Rolling Policy

- **Daily rotation**: New log files are created daily
- **Size-based rotation**: Files are rolled when they exceed 10MB
- **Compression**: Old log files are automatically compressed (.gz)
- **Retention**:
  - All logs: 30 days (1GB total)
  - Error logs: 90 days (500MB total)
  - Info logs: 60 days (800MB total)
  - Debug logs: 7 days (200MB total)

### Performance

- **Async logging**: Uses asynchronous appenders for better performance
- **Non-blocking**: Logging won't block your application threads

## Log Format

Each log entry includes:

- **Timestamp**: yyyy-MM-dd HH:mm:ss.SSS
- **Thread**: Thread name
- **Level**: DEBUG, INFO, WARN, ERROR
- **Logger**: Class name (abbreviated to 50 characters)
- **Context Information**:
  - Request ID
  - User ID
  - Agent ID
  - Conversation ID
- **Message**: The actual log message

Example log entry:

```
2024-03-15 10:30:45.123 [http-nio-8044-exec-1] INFO  c.a.d.services.AliasService [ReqId:abc12345] [User:user123] [Agent:agent456] [Conv:conv789] - Creating new alias with key: mykey
```

## Using the Logging System

### 1. Existing Logging (No Changes Required)

Your existing logging code continues to work without any modifications:

```java
// Using @Slf4j annotation (Lombok)
@Slf4j
@Service
public class MyService {
    public void myMethod() {
        log.info("This will appear in both console and files");
        log.error("Error messages go to error.log too");
    }
}

// Using traditional SLF4J logger
public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

    public void myMethod() {
        logger.debug("Debug message");
        logger.info("Info message");
    }
}
```

### 2. Enhanced Logging with Context

Use the new `LoggingUtils` class for enhanced logging with context:

```java
@Autowired
private LoggingUtils loggingUtils;

public void handleRequest(String userId, String conversationId) {
    // Set context for this request
    String requestId = LoggingUtils.initializeRequestContext();
    LoggingUtils.setUserContext(userId);
    LoggingUtils.setConversationContext(conversationId);

    try {
        // Your business logic here
        logger.info("Processing request"); // This will include context

        // Log method execution time
        long startTime = System.currentTimeMillis();
        // ... do work ...
        LoggingUtils.logExecutionTime(logger, "processRequest", startTime);

    } finally {
        // Clear context when done
        LoggingUtils.clearContext();
    }
}
```

### 3. Specialized Logging Methods

```java
// Log API requests
LoggingUtils.logApiRequest(logger, "POST", "/api/v1/aliases", userId);

// Log database operations
LoggingUtils.logDatabaseOperation(logger, "CREATE", "Alias", aliasId);

// Log business operations
LoggingUtils.logBusinessOperation(logger, "ALIAS_CREATED", "New alias created successfully");

// Log security events
LoggingUtils.logSecurityEvent(logger, "UNAUTHORIZED_ACCESS", "Invalid token used");

// Log performance metrics
Map<String, Object> metrics = new HashMap<>();
metrics.put("responseTime", 150);
metrics.put("recordsProcessed", 100);
LoggingUtils.logPerformanceMetrics(logger, "processAliases", metrics);
```

## Configuration

### Environment Variables

You can customize the log directory by setting the `LOG_DIR` environment variable:

```bash
export LOG_DIR=/path/to/your/logs
java -jar your-app.jar
```

Or pass it as a system property:

```bash
java -DLOG_DIR=/path/to/your/logs -jar your-app.jar
```

### Log Levels

Configure log levels in `application.properties`:

```properties
# Root logging level
logging.level.root=INFO

# Your application logging
logging.level.com.api.digicell=DEBUG

# Spring framework logging
logging.level.org.springframework=INFO

# Database/SQL logging
logging.level.org.hibernate.SQL=DEBUG
```

### Custom Configuration

The logging is configured in `src/main/resources/logback-spring.xml`. You can modify this file to:

- Change log patterns
- Adjust file sizes and retention policies
- Add new appenders
- Modify logging levels per class/package

## Monitoring Log Files

### Viewing Real-time Logs

```bash
# View all logs in real-time
tail -f logs/Digicell-all.log

# View only errors
tail -f logs/Digicell-error.log

# View with context filtering
grep "ERROR" logs/Digicell-all.log | tail -20

# Follow logs for specific user
tail -f logs/Digicell-all.log | grep "User:user123"
```

### Log Analysis

Since logs include structured context, you can easily filter and analyze:

```bash
# Find all operations for a specific user
grep "User:user123" logs/Digicell-all.log

# Find all operations for a specific conversation
grep "Conv:conv789" logs/Digicell-all.log

# Find slow operations (logged as warnings)
grep "Slow operation detected" logs/Digicell-all.log

# Find all database operations
grep "Database operation" logs/Digicell-all.log
```

## Best Practices

1. **Use appropriate log levels**:

   - DEBUG: Detailed information for debugging
   - INFO: General information about application flow
   - WARN: Warning conditions that should be addressed
   - ERROR: Error conditions that need immediate attention

2. **Include context**: Use LoggingUtils to set user, conversation, and request context

3. **Log entry and exit of important methods**: Use LoggingUtils.logMethodEntry/Exit

4. **Log performance**: Use LoggingUtils.logExecutionTime for slow operations

5. **Structured logging**: Include relevant IDs and structured data in log messages

6. **Don't log sensitive data**: Avoid logging passwords, tokens, or PII

## Troubleshooting

### Log Files Not Created

- Check if the application has write permissions to the logs directory
- Verify the LOG_DIR environment variable or default ./logs directory exists
- Check for any startup errors in the console

### High Disk Usage

- Review retention policies in logback-spring.xml
- Consider adjusting maxHistory and totalSizeCap values
- Monitor debug logs as they can be verbose

### Performance Issues

- Async appenders are already configured for optimal performance
- If needed, adjust queue sizes in logback-spring.xml
- Consider reducing DEBUG level logging in production

## Integration with Your Existing Code

**Important**: All your existing logging code will continue to work exactly as before. The new setup simply adds file output to your existing console logging without requiring any code changes.

Your existing patterns like:

- `logger.info("message")`
- `log.debug("debug info")`
- Exception logging with stack traces

All continue to work and now also write to the appropriate log files automatically.
