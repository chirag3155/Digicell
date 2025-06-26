# SSL Configuration Guide for Socket.IO Server

## Overview

This guide explains how to configure SSL/TLS for your Socket.IO server running on port 8099, separate from your Spring Boot REST API on port 8044.

## Current Configuration

### Spring Boot REST API (Port 8044)

- SSL Enabled: ✅ Yes
- Certificate: `/home/azureuser/chat_digicel.p12`
- Protocol: HTTPS

### Socket.IO Server (Port 8099)

- SSL Enabled: ✅ Yes (after this setup)
- Certificate: Same certificate file (`/home/azureuser/chat_digicel.p12`)
- Protocol: WSS (WebSocket Secure) / HTTPS

## Files Modified

### 1. ChatModule.java

Added SSL configuration support for netty-socketio 2.0.11:

- Direct keystore configuration using setKeyStore() and setKeyStorePassword()
- Simplified SSL setup compatible with the library version

### 2. application-dev.properties

Added Socket.IO SSL properties:

```properties
# Socket SSL Configuration
socket.ssl.enabled=true
socket.ssl.key-store=file:/home/azureuser/chat_digicel.p12
socket.ssl.key-store-password=tomcat
```

### 3. application-local.properties

Added Socket.IO SSL properties (disabled for local development):

```properties
# Socket SSL Configuration - Disabled for local development
socket.ssl.enabled=false
```

### 4. test-client.html

Updated to support both HTTP and HTTPS connections automatically.

## Deployment Steps

### 1. Ensure Certificate File Exists

Verify the certificate file exists on your remote server:

```bash
ls -la /home/azureuser/chat_digicel.p12
```

### 2. Deploy the Application

Deploy your updated application with the SSL configuration.

### 3. Test SSL Connection

#### Using Browser

Navigate to: `https://your-server:8099/socket.io/`
You should see a response (not an error page).

#### Using Test Client

1. Access your app via HTTPS: `https://your-server:8044`
2. Use the test client to connect to the Socket.IO server
3. Check the logs for successful SSL initialization

### 4. Verify Connections

Check the application logs for:

```
SSL configured successfully for Socket.IO server on port 8099
```

## Client Connection Changes

### Before (HTTP)

```javascript
const socket = io("http://your-server:8099");
```

### After (HTTPS/WSS)

```javascript
const socket = io("https://your-server:8099", {
  transports: ["websocket", "polling"],
});
```

## Troubleshooting

### Common Issues

1. **Certificate Not Found**

   - Error: `FileNotFoundException`
   - Solution: Verify certificate path and permissions

2. **Wrong Password**

   - Error: `UnrecoverableKeyException`
   - Solution: Verify keystore password

3. **Mixed Content Warnings**

   - Issue: HTTPS page trying to connect to HTTP socket
   - Solution: Ensure socket connection uses HTTPS/WSS

4. **Port Conflicts**
   - Issue: Port 8099 already in use
   - Solution: Check for other services using the port

### Verification Commands

```bash
# Check if port is listening with SSL
netstat -tlnp | grep 8099

# Test SSL handshake
openssl s_client -connect your-server:8099

# Check certificate details
openssl pkcs12 -info -in /home/azureuser/chat_digicel.p12
```

## Security Considerations

1. **Certificate Security**

   - Keep certificate files secure (600 permissions)
   - Use strong passwords
   - Regular certificate renewal

2. **Protocol Support**

   - Force HTTPS/WSS in production
   - Disable insecure protocols

3. **Firewall Rules**
   - Ensure port 8099 is open for HTTPS traffic
   - Block HTTP access to socket port in production

## Environment-Specific Configuration

### Development (Local)

- SSL: Disabled
- Protocol: HTTP/WS
- Port: 8099

### Production (Remote Server)

- SSL: Enabled
- Protocol: HTTPS/WSS
- Port: 8099
- Certificate: Production certificate

## Monitoring

Monitor the following in your application logs:

- SSL initialization success/failure
- Certificate loading status
- Socket connection attempts
- SSL handshake errors

## Next Steps

1. Deploy the updated application
2. Test SSL connections
3. Update all client applications to use HTTPS/WSS
4. Monitor for any SSL-related errors
5. Consider certificate auto-renewal setup
