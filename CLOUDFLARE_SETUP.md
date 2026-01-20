# Cloudflare Tunnel Setup Guide

## Quick Start

### Step 1: Start Everything
Run this command in a terminal:
```bash
./start-relay-with-tunnel.sh
```

This will:
- Start the relay server on port 8081
- Start Cloudflare tunnel pointing to it
- Show you the public URL/IP to use

### Step 2: Get the Tunnel Info
When Cloudflare tunnel starts, you'll see output like:

**Option A - Hostname:**
```
https://abc-123-def-456.trycloudflare.com
```
Use the hostname part: `abc-123-def-456.trycloudflare.com`

**Option B - IP and Port:**
```
connect via: tcp://1.2.3.4:54321
```
Use IP: `1.2.3.4` and Port: `54321`

### Step 3: Update NetworkManager.java
Open `app/src/main/java/kuroyale/mainpack/network/NetworkManager.java` and update:

```java
private static final String RELAY_SERVER_IP = "abc-123-def-456.trycloudflare.com"; // Your tunnel hostname
private static final int RELAY_SERVER_PORT = 8081; // Or the port from Cloudflare if shown
```

**Important:** Remove the `https://` part if present, use just the hostname.

### Step 4: Recompile and Test
```bash
./gradlew compileJava
./gradlew run
```

## Manual Setup (Alternative)

If you prefer to run things separately:

**Terminal 1 - Relay Server:**
```bash
./start-relay.sh
```

**Terminal 2 - Cloudflare Tunnel:**
```bash
cloudflared tunnel --url tcp://localhost:8081
```

Then follow Step 2-4 above.

## Important Notes

- **Keep both running:** Relay server AND Cloudflare tunnel must stay running
- **URL changes:** Each time you restart the tunnel, the URL changes (unless you set up a permanent tunnel)
- **Update code:** If the URL changes, update `RELAY_SERVER_IP` in NetworkManager.java
- **Both players:** Both players need the same `RELAY_SERVER_IP` and `RELAY_SERVER_PORT` in their code

## Troubleshooting

- **"Cannot connect to relay"**: Make sure relay server is running first, then start tunnel
- **"Connection timeout"**: Check that you're using the correct hostname/IP from Cloudflare output
- **URL keeps changing**: This is normal for quick tunnels. Consider setting up a permanent tunnel later

