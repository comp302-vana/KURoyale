# Relay Server Setup for NAT Traversal

## Problem
Direct host-to-client connections fail across different networks due to NAT (Network Address Translation). Local network connections work fine, but internet connections fail because:
- Host is behind NAT (no inbound connections possible)
- Client cannot reach host's private IP
- Port forwarding is complex and often not possible (dorms, public WiFi, etc.)

## Solution
A **rendezvous/forward server** (relay server) solves this by:
1. Both host and client connect **outbound** to a publicly accessible relay server
2. Relay forwards `NetworkMessage` objects between them
3. No NAT issues because all connections are outbound
4. **Zero game logic changes** - relay is completely transparent

## Architecture

```
Direct Mode (local network):
  Client ──▶ Host (listening on port)

Relay Mode (internet):
  Client ──▶ Relay Server ──▶ Host
  Host   ◀── Relay Server ◀── Client
```

## Setup

### Step 1: Enable Relay Mode

Edit `NetworkManager.java`:
```java
private static final boolean USE_RELAY = true; // Change to true
private static final String RELAY_SERVER_IP = "your-relay-server-ip";
private static final int RELAY_SERVER_PORT = 8081;
```

### Step 2: Start Relay Server

**Option A: Local Testing**
```bash
./start-relay.sh
```

**Option B: Manual**
```bash
java -cp "app/build/classes/java/main" kuroyale.mainpack.network.RelayServer 8081
```

**Option C: Production**
Deploy `RelayServer.java` to a public server (AWS, DigitalOcean, etc.) and run it there.

### Step 3: Play!

1. Start relay server (if not already running)
2. Host: Click "Create Lobby" → Connects to relay
3. Client: Click "Join Lobby" → Connects to relay
4. Both connect via relay → Works across any network!

## How It Works

1. **Host connects** to relay → Sends `CONNECT` message with `playerId=1`
2. **Client connects** to relay → Sends `CONNECT` message with `playerId=2`
3. **Relay identifies** each connection (HOST vs CLIENT)
4. **Relay forwards** all `NetworkMessage` objects:
   - Client messages → Relay → Host
   - Host messages → Relay → Client
5. **Game logic unchanged** - host remains authoritative

## Important Notes

- ✅ **No game logic changes** - relay only forwards messages
- ✅ **Host remains authoritative** - all game state decisions stay in game
- ✅ **Works across NAT/CGNAT** - all connections are outbound
- ✅ **No port forwarding needed** - relay server handles routing
- ✅ **Transparent to game** - `NetworkMessage` protocol unchanged

## Configuration

### Local Testing
```java
USE_RELAY = true
RELAY_SERVER_IP = "localhost"
RELAY_SERVER_PORT = 8081
```

### Production
```java
USE_RELAY = true
RELAY_SERVER_IP = "your-public-server-ip"
RELAY_SERVER_PORT = 8081
```

### Disable Relay (Direct Mode)
```java
USE_RELAY = false
```

## Troubleshooting

- **"Cannot connect to relay"**: Make sure relay server is running
- **"Connection timeout"**: Check `RELAY_SERVER_IP` is correct
- **Messages not forwarding**: Check relay server console for errors
- **Local works, internet doesn't**: Enable relay mode (`USE_RELAY = true`)

