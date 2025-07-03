# Redis Configuration Guide

## Overview

The application now supports environment-specific Redis configurations for local development and production deployment.

## Configuration Structure

### Environment Profiles

- **Local Environment**: `spring.profiles.active=local`
- **Dev Environment**: `spring.profiles.active=dev`

### Redis Connection Details

#### Local Environment (`application-local.properties`)

```properties
# Redis Configuration - Local Environment
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=2000ms
spring.data.redis.database=0

# Redis Connection Pool - Local
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-wait=-1ms
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

#### Dev Environment (`application-dev.properties`)

```properties
# Redis Configuration - Dev Environment
spring.data.redis.host=192.168.0.5
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.timeout=2000ms
spring.data.redis.database=0

# Redis Connection Pool - Dev
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-wait=-1ms
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

#### Common Configuration (`application.properties`)

```properties
# Redis Configuration - Environment specific
# TTL Configuration (24 hours in seconds)
redis.user.ttl=86400
```

## Usage

### Switching Environments

#### For Local Development

```properties
spring.profiles.active=local
```

- Connects to Redis at `localhost:6379`
- Uses local Redis instance

#### For Dev Server Deployment

```properties
spring.profiles.active=dev
```

- Connects to Redis at `192.168.0.5:6379`
- Uses dev server Redis instance

### Redis Data Structure

The application stores user data in Redis with the following structure:

#### User Data

- **Key Pattern**: `user:{userId}`
- **Value**: JSON serialized `ChatUser` object
- **TTL**: 24 hours (86400 seconds)

#### Socket Mapping

- **Key Pattern**: `socket:{userId}`
- **Value**: Socket ID string
- **TTL**: 24 hours (86400 seconds)

## Testing Redis Connection

### Local Environment

```bash
# Test local Redis connection
redis-cli ping
# Should return: PONG
```

### Dev Environment

```bash
# Test dev Redis connection
redis-cli -h 192.168.0.5 -p 6379 ping
# Should return: PONG
```

## Application Startup

The application will automatically connect to the appropriate Redis instance based on the active profile:

1. **Local**: Connects to `localhost:6379`
2. **Dev**: Connects to `192.168.0.5:6379`

## Troubleshooting

### Connection Issues

1. Verify Redis server is running on the target host
2. Check network connectivity (especially for dev environment)
3. Verify port 6379 is accessible
4. Check firewall settings

### Configuration Issues

1. Ensure correct profile is active
2. Verify Redis configuration in the appropriate properties file
3. Check application logs for Redis connection errors

## Security Considerations

- Redis password is currently empty (no authentication)
- Consider enabling Redis AUTH for production environments
- Network access should be restricted to authorized hosts only
- For dev environment, ensure 192.168.0.5 is accessible from deployment location

## Performance Tuning

The connection pool settings can be adjusted based on load:

```properties
# Increase for high-load scenarios
spring.data.redis.lettuce.pool.max-active=16
spring.data.redis.lettuce.pool.max-idle=16

# Adjust timeout for slower networks
spring.data.redis.timeout=5000ms
```
