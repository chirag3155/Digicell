# SSL Testing Commands for Socket.IO Server

## 1. Test SSL Certificate and Port Connectivity

### Check if port 8099 is listening with SSL

```bash
netstat -tlnp | grep 8099
```

### Test SSL handshake

```bash
openssl s_client -connect eva-prod.bngrenew.com:8099 -servername eva-prod.bngrenew.com
```

### Test Socket.IO endpoint availability

```bash
curl -k -v "https://eva-prod.bngrenew.com:8099/socket.io/"
```

## 2. Test WebSocket Connection

### Using wscat (install with: npm install -g wscat)

```bash
wscat -c "wss://eva-prod.bngrenew.com:8099/socket.io/?clientType=agent&userId=1&EIO=4&transport=websocket"
```

### Using curl for WebSocket upgrade

```bash
curl -v \
  --http1.1 \
  --header "Connection: Upgrade" \
  --header "Upgrade: websocket" \
  --header "Sec-WebSocket-Version: 13" \
  --header "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==" \
  "wss://eva-prod.bngrenew.com:8099/socket.io/?clientType=agent&userId=1&EIO=4&transport=websocket"
```

## 3. Browser Testing

### JavaScript Console Test

```javascript
// Open browser console on any HTTPS page and run:
const socket = io("wss://eva-prod.bngrenew.com:8099", {
  query: {
    clientType: "agent",
    userId: "1",
  },
  transports: ["websocket"],
});

socket.on("connect", () => {
  console.log("Connected successfully!", socket.id);
});

socket.on("connect_error", (error) => {
  console.error("Connection failed:", error);
});
```

## 4. Application Log Analysis

### What to look for in your application logs:

#### Successful SSL initialization:

```
SSL configured successfully for Socket.IO server on port 8099
```

#### Successful connections:

```
Socket client connected - IP: /103.92.41.145:55668, SessionId: abc123, Secure: true
Socket connection attempt - IP: /103.92.41.145:55668, SessionId: abc123, ClientType: agent, UserId: 1
User connected successfully. SocketId: abc123, UserId: 1, Type: agent
```

#### Failed connections (these are normal for invalid requests):

```
Blocked wrong socket.io-context request! url: /bad-request
Connection rejected: No clientType parameter
Connection rejected: Invalid clientType
```

## 5. Expected Results

### ✅ Good Signs:

- Port 8099 listening
- SSL handshake successful
- Socket.IO endpoint returns 200 OK
- WebSocket upgrade successful
- Application logs show "Secure: true"
- User connections succeed with proper parameters

### ❌ Issues to investigate:

- Port not listening
- SSL handshake fails
- Certificate errors
- WebSocket upgrade fails
- All connections show "Secure: false"

## 6. Common Issues and Solutions

### Issue: Certificate verification failed

**Solution:** Ensure certificate is valid and properly configured

### Issue: Port not accessible

**Solution:** Check firewall rules and ensure port 8099 is open

### Issue: Mixed content warnings

**Solution:** Ensure frontend uses wss:// not ws://

### Issue: Connection rejected

**Solution:** Verify clientType=agent and userId parameters are included

## 7. Monitoring Commands

### Real-time log monitoring:

```bash
tail -f /path/to/your/application.log | grep -E "(SSL|Socket|Connection)"
```

### Check active connections:

```bash
ss -tulpn | grep 8099
lsof -i :8099
```
