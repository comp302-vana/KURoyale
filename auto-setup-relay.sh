#!/bin/bash
# Automated setup: Start relay + tunnel + update code automatically

cd "$(dirname "$0")"

echo "=========================================="
echo "Automated Relay + Cloudflare Setup"
echo "=========================================="
echo ""

# Check if port 8081 is already in use and kill existing relay server
echo "1. Checking for existing relay server..."
EXISTING_PID=$(lsof -ti:8081 2>/dev/null)
if [ ! -z "$EXISTING_PID" ]; then
    echo "   Found existing process on port 8081 (PID: $EXISTING_PID), stopping it..."
    kill $EXISTING_PID 2>/dev/null
    sleep 1
fi

# Start relay server in background
echo "   Starting relay server..."
rm -f /tmp/relay-server.log
java -cp "app/build/classes/java/main" kuroyale.mainpack.network.RelayServer 8081 > /tmp/relay-server.log 2>&1 &
RELAY_PID=$!
sleep 3

# Check if process is still running
if ! kill -0 $RELAY_PID 2>/dev/null; then
    echo "ERROR: Failed to start relay server"
    echo ""
    echo "Error details:"
    cat /tmp/relay-server.log 2>/dev/null || echo "No error log found"
    echo ""
    echo "Troubleshooting:"
    echo "1. Make sure the project is compiled: ./gradlew compileJava"
    echo "2. Check if port 8081 is already in use: lsof -i:8081"
    echo "3. Try killing any Java processes using port 8081"
    exit 1
fi

# Check if there are errors in the log
if grep -qi "error\|exception\|failed" /tmp/relay-server.log 2>/dev/null; then
    echo "⚠️  Warning: Relay server may have errors:"
    grep -i "error\|exception\|failed" /tmp/relay-server.log | head -3
    echo ""
fi

echo "   ✓ Relay server started (PID: $RELAY_PID)"
echo ""

# Start Cloudflare tunnel and capture output
echo "2. Starting Cloudflare tunnel..."
echo "   (This may take a few seconds...)"
echo ""

# Clear old log
rm -f /tmp/cloudflare-tunnel.log

# Run cloudflared in background and capture output
cloudflared tunnel --url tcp://localhost:8081 > /tmp/cloudflare-tunnel.log 2>&1 &
TUNNEL_PID=$!

# Wait for tunnel to establish and show URL
echo "   Waiting for tunnel to establish..."
sleep 8

# Extract hostname/IP from the output
TUNNEL_HOST=""
TUNNEL_PORT="8081"

# Method 1: Try to find hostname (most common format)
if [ -f /tmp/cloudflare-tunnel.log ]; then
    # Look for trycloudflare.com hostname
    TUNNEL_HOST=$(grep -oE '[a-z0-9-]+\.trycloudflare\.com' /tmp/cloudflare-tunnel.log 2>/dev/null | head -1)
    
    # Method 2: Try to find IP:port pattern
    if [ -z "$TUNNEL_HOST" ]; then
        IP_PORT=$(grep -oE 'tcp://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+' /tmp/cloudflare-tunnel.log 2>/dev/null | head -1)
        if [ ! -z "$IP_PORT" ]; then
            TUNNEL_HOST=$(echo "$IP_PORT" | sed 's|tcp://||' | cut -d: -f1)
            TUNNEL_PORT=$(echo "$IP_PORT" | sed 's|tcp://||' | cut -d: -f2)
        fi
    fi
    
    # Method 3: Look for "https://" URL and extract hostname
    if [ -z "$TUNNEL_HOST" ]; then
        HTTPS_URL=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' /tmp/cloudflare-tunnel.log 2>/dev/null | head -1)
        if [ ! -z "$HTTPS_URL" ]; then
            TUNNEL_HOST=$(echo "$HTTPS_URL" | sed 's|https://||')
        fi
    fi
    
    # Method 4: Look for any hostname pattern
    if [ -z "$TUNNEL_HOST" ]; then
        TUNNEL_HOST=$(grep -oE '[a-z0-9-]{10,}\.trycloudflare\.com' /tmp/cloudflare-tunnel.log 2>/dev/null | head -1)
    fi
fi

# Wait a bit more and try again if still not found
if [ -z "$TUNNEL_HOST" ]; then
    echo "   Still waiting for tunnel URL..."
    sleep 5
    if [ -f /tmp/cloudflare-tunnel.log ]; then
        TUNNEL_HOST=$(grep -oE '[a-z0-9-]+\.trycloudflare\.com' /tmp/cloudflare-tunnel.log 2>/dev/null | head -1)
        if [ -z "$TUNNEL_HOST" ]; then
            IP_PORT=$(grep -oE 'tcp://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+' /tmp/cloudflare-tunnel.log 2>/dev/null | head -1)
            if [ ! -z "$IP_PORT" ]; then
                TUNNEL_HOST=$(echo "$IP_PORT" | sed 's|tcp://||' | cut -d: -f1)
                TUNNEL_PORT=$(echo "$IP_PORT" | sed 's|tcp://||' | cut -d: -f2)
            fi
        fi
    fi
fi

if [ -z "$TUNNEL_HOST" ]; then
    echo ""
    echo "⚠️  Could not automatically detect tunnel hostname/IP"
    echo ""
    echo "   Showing tunnel output (look for the URL):"
    echo "   ========================================="
    cat /tmp/cloudflare-tunnel.log
    echo "   ========================================="
    echo ""
    echo "   Please manually:"
    echo "   1. Find the hostname/IP from the output above"
    echo "   2. Update NetworkManager.java:"
    echo "      RELAY_SERVER_IP = \"your-hostname-here\""
    echo ""
    echo "   Keeping tunnel running... (PID: $TUNNEL_PID)"
    echo "   Press Ctrl+C to stop everything"
    tail -f /tmp/cloudflare-tunnel.log
    exit 1
fi

echo "   ✓ Tunnel established"
echo "   ✓ Detected: $TUNNEL_HOST:$TUNNEL_PORT"
echo ""

# Update NetworkManager.java
echo "3. Updating NetworkManager.java..."

NETWORK_MANAGER_FILE="app/src/main/java/kuroyale/mainpack/network/NetworkManager.java"

# Backup original
cp "$NETWORK_MANAGER_FILE" "$NETWORK_MANAGER_FILE.bak"

# Update RELAY_SERVER_IP
sed -i '' "s|private static final String RELAY_SERVER_IP = \".*\";|private static final String RELAY_SERVER_IP = \"$TUNNEL_HOST\";|" "$NETWORK_MANAGER_FILE"

# Update RELAY_SERVER_PORT if different
if [ "$TUNNEL_PORT" != "8081" ]; then
    sed -i '' "s|private static final int RELAY_SERVER_PORT = [0-9]*;|private static final int RELAY_SERVER_PORT = $TUNNEL_PORT;|" "$NETWORK_MANAGER_FILE"
fi

echo "   ✓ Code updated with: $TUNNEL_HOST:$TUNNEL_PORT"
echo ""

# Show what was changed
echo "4. Summary:"
echo "   Relay Server: Running (PID: $RELAY_PID)"
echo "   Cloudflare Tunnel: Running (PID: $TUNNEL_PID)"
echo "   RELAY_SERVER_IP: $TUNNEL_HOST"
echo "   RELAY_SERVER_PORT: $TUNNEL_PORT"
echo ""
echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Recompile: ./gradlew compileJava"
echo "2. Run game: ./gradlew run"
echo ""
echo "⚠️  Keep this terminal open (relay + tunnel are running)"
echo "   Press Ctrl+C to stop everything"
echo ""
echo "Full tunnel output saved to: /tmp/cloudflare-tunnel.log"
echo ""

# Keep tunnel running and show output
tail -f /tmp/cloudflare-tunnel.log

