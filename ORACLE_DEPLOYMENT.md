# Oracle Cloud Relay Server Deployment Guide

## What Needs to Be Deployed

The relay server is **minimal** - only 3 Java files with **zero external dependencies**:

### Required Files:
1. `RelayServer.java` - Main server class
2. `NetworkMessage.java` - Message class (used for serialization)
3. `MessageProtocol.java` - Protocol helper (sends/receives messages)

**Location in your project:**
```
app/src/main/java/kuroyale/mainpack/network/
├── RelayServer.java
├── NetworkMessage.java
└── MessageProtocol.java
```

## What the Relay Server Does

- ✅ Accepts TCP connections on port 8081
- ✅ Identifies clients as HOST (playerId=1) or CLIENT (playerId=2)
- ✅ Forwards `NetworkMessage` objects between host and client
- ❌ **NO game logic** - it's just a message forwarder
- ❌ **NO external dependencies** - only uses Java standard library

## Deployment Steps

### 1. Copy Files to Oracle Server

**Option A: Copy source files (.java)**
```bash
# On your local machine, copy these 3 files:
scp app/src/main/java/kuroyale/mainpack/network/RelayServer.java user@oracle-server:/path/to/deploy/
scp app/src/main/java/kuroyale/mainpack/network/NetworkMessage.java user@oracle-server:/path/to/deploy/
scp app/src/main/java/kuroyale/mainpack/network/MessageProtocol.java user@oracle-server:/path/to/deploy/
```

**Option B: Copy compiled classes (.class)**
```bash
# Copy from build directory:
scp app/build/classes/java/main/kuroyale/mainpack/network/RelayServer.class user@oracle-server:/path/to/deploy/kuroyale/mainpack/network/
scp app/build/classes/java/main/kuroyale/mainpack/network/NetworkMessage.class user@oracle-server:/path/to/deploy/kuroyale/mainpack/network/
scp app/build/classes/java/main/kuroyale/mainpack/network/MessageProtocol.class user@oracle-server:/path/to/deploy/kuroyale/mainpack/network/
```

### 2. On Oracle Server

**Create directory structure:**
```bash
mkdir -p kuroyale/mainpack/network
cd kuroyale/mainpack/network
```

**If using source files, compile:**
```bash
javac *.java
```

**Run the server:**
```bash
# From the parent directory (where kuroyale/ folder is)
java kuroyale.mainpack.network.RelayServer 8081
```

### 3. Keep Server Running

**Option A: Run in background with nohup**
```bash
nohup java kuroyale.mainpack.network.RelayServer 8081 > relay.log 2>&1 &
```

**Option B: Use systemd service (recommended for production)**
```bash
# Create /etc/systemd/system/relay-server.service
[Unit]
Description=KURoyale Relay Server
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/deploy
ExecStart=/usr/bin/java kuroyale.mainpack.network.RelayServer 8081
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target

# Enable and start
sudo systemctl enable relay-server
sudo systemctl start relay-server
```

### 4. Configure Firewall

**Open port 8081:**
```bash
# Oracle Cloud: Add ingress rule in Security List
# Protocol: TCP
# Port: 8081
# Source: 0.0.0.0/0 (or restrict to specific IPs)
```

**On the server (if using iptables):**
```bash
sudo iptables -A INPUT -p tcp --dport 8081 -j ACCEPT
sudo iptables-save
```

### 5. Update NetworkManager.java

**Update the relay server IP in your game:**
```java
// In NetworkManager.java
private static final String RELAY_SERVER_IP = "YOUR_ORACLE_SERVER_PUBLIC_IP";
private static final int RELAY_SERVER_PORT = 8081;
```

## File Size

- `RelayServer.java`: ~8 KB
- `NetworkMessage.java`: ~4 KB  
- `MessageProtocol.java`: ~1 KB

**Total: ~13 KB** (very lightweight!)

## Verification

**Test the server is running:**
```bash
# From your local machine
telnet YOUR_ORACLE_SERVER_IP 8081
# Should connect (Ctrl+] then 'quit' to exit)
```

**Check server logs:**
```bash
tail -f relay.log
# Should show: "Relay Server running on port 8081"
```

## Troubleshooting

**Port already in use:**
```bash
# Find process using port 8081
sudo lsof -i :8081
# Kill it
sudo kill -9 <PID>
```

**Java not found:**
```bash
# Install Java 11+ on Oracle Linux
sudo yum install java-11-openjdk-devel
# Or on Ubuntu
sudo apt install openjdk-11-jdk
```

**Connection refused:**
- Check firewall rules in Oracle Cloud Console
- Verify server is running: `ps aux | grep RelayServer`
- Check server logs for errors

## Security Notes

- The relay server has **no authentication** - anyone can connect
- For production, consider adding:
  - Connection rate limiting
  - IP whitelist
  - Authentication tokens
  - SSL/TLS encryption

## That's It!

The relay server is **extremely simple** - just 3 files, no dependencies, runs on any Java 11+ system. Perfect for Oracle Cloud Free Tier!

