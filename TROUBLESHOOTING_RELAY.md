# Relay Server Connection Troubleshooting

## ❌ **Connection Failed: "Host: Connecting to relay server at 80.225.92.3:8081"**

### **Quick Checks:**

1. **Is the relay server running on Oracle?**
   ```bash
   # SSH into Oracle server and check:
   ps aux | grep RelayServer
   # Should show: java kuroyale.mainpack.network.RelayServer
   ```

2. **Is port 8081 open?**
   ```bash
   # On Oracle server:
   sudo netstat -tlnp | grep 8081
   # OR
   sudo ss -tlnp | grep 8081
   # Should show: LISTEN on 0.0.0.0:8081
   ```

3. **Test connection from your local machine:**
   ```bash
   # Test if port is reachable
   telnet 80.225.92.3 8081
   # OR
   nc -zv 80.225.92.3 8081
   # OR
   timeout 5 bash -c "</dev/tcp/80.225.92.3/8081" && echo "Port is open" || echo "Port is closed"
   ```

### **Common Issues:**

#### **Issue 1: Relay Server Not Running**
**Solution:**
```bash
# SSH into Oracle server
ssh user@80.225.92.3

# Navigate to relay server directory
cd /path/to/relay-server

# Start the server
java kuroyale.mainpack.network.RelayServer 8081

# OR run in background:
nohup java kuroyale.mainpack.network.RelayServer 8081 > relay.log 2>&1 &
```

#### **Issue 2: Firewall Blocking Port 8081**
**Solution:**
```bash
# On Oracle server, check firewall:
sudo firewall-cmd --list-ports  # CentOS/RHEL
sudo ufw status                 # Ubuntu

# Open port 8081:
sudo firewall-cmd --permanent --add-port=8081/tcp  # CentOS/RHEL
sudo firewall-cmd --reload
# OR
sudo ufw allow 8081/tcp         # Ubuntu
sudo ufw reload
```

**Also check Oracle Cloud Console:**
- Go to: **Networking → Virtual Cloud Networks → Security Lists**
- Find your VCN → Ingress Rules
- Add rule: **TCP, Port 8081, Source: 0.0.0.0/0**

#### **Issue 3: Wrong IP Address**
**Solution:**
- Verify the public IP in Oracle Cloud Console
- It might be different from `80.225.92.3`
- Update `NetworkManager.java` with correct IP

#### **Issue 4: Server Not Accessible**
**Solution:**
```bash
# Test basic connectivity
ping 80.225.92.3

# Test if server is up
curl -v telnet://80.225.92.3:8081
```

### **Debug Steps:**

1. **Check Oracle server logs:**
   ```bash
   # If running with nohup:
   tail -f relay.log
   
   # Should see: "Relay Server running on port 8081"
   ```

2. **Check if Java process is running:**
   ```bash
   ps aux | grep java | grep RelayServer
   ```

3. **Check if port is listening:**
   ```bash
   sudo lsof -i :8081
   # Should show: java process listening on 8081
   ```

4. **Test from Oracle server itself:**
   ```bash
   # On Oracle server, test local connection:
   telnet localhost 8081
   # Should connect (Ctrl+] then 'quit' to exit)
   ```

### **Quick Fix Script:**

```bash
#!/bin/bash
# test-relay-connection.sh

RELAY_IP="80.225.92.3"
RELAY_PORT="8081"

echo "Testing relay server connection..."
echo "IP: $RELAY_IP"
echo "Port: $RELAY_PORT"
echo ""

# Test 1: Ping
echo "1. Testing ping..."
if ping -c 3 $RELAY_IP > /dev/null 2>&1; then
    echo "   ✅ Server is reachable"
else
    echo "   ❌ Server is NOT reachable"
    exit 1
fi

# Test 2: Port check
echo "2. Testing port $RELAY_PORT..."
if timeout 5 bash -c "</dev/tcp/$RELAY_IP/$RELAY_PORT" 2>/dev/null; then
    echo "   ✅ Port $RELAY_PORT is OPEN"
else
    echo "   ❌ Port $RELAY_PORT is CLOSED or unreachable"
    echo "   → Check if relay server is running"
    echo "   → Check firewall rules"
    exit 1
fi

echo ""
echo "✅ Connection test passed!"
```

### **Expected Behavior:**

**When working correctly, you should see:**
```
Host: Using internet mode (relay server: 80.225.92.3:8081)
Host: Connecting to relay server at 80.225.92.3:8081
Host: Connected to relay server
Host: Sent CONNECT message to relay (identified as HOST)
```

**If connection fails, you'll see:**
```
Host: Connecting to relay server at 80.225.92.3:8081
[Timeout after 10 seconds]
Error: Connection timeout. Check relay server IP/port.
```

### **Next Steps:**

1. ✅ Verify relay server is running on Oracle
2. ✅ Check firewall rules (both Oracle Cloud Console and server firewall)
3. ✅ Test port connectivity from your local machine
4. ✅ Check Oracle server logs for errors
5. ✅ Verify IP address is correct

