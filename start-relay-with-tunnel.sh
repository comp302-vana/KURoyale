#!/bin/bash
# Start relay server and Cloudflare tunnel together

cd "$(dirname "$0")"

echo "=========================================="
echo "Starting Relay Server + Cloudflare Tunnel"
echo "=========================================="
echo ""

# Start relay server in background
echo "1. Starting relay server on port 8081..."
java -cp "app/build/classes/java/main" kuroyale.mainpack.network.RelayServer 8081 &
RELAY_PID=$!

# Wait a moment for relay to start
sleep 2

echo "2. Relay server started (PID: $RELAY_PID)"
echo ""
echo "3. Starting Cloudflare tunnel..."
echo "   (This will show you the public URL/IP to use)"
echo ""

# Start Cloudflare tunnel
cloudflared tunnel --url tcp://localhost:8081

# Cleanup on exit
echo ""
echo "Stopping relay server..."
kill $RELAY_PID 2>/dev/null

