#!/bin/bash

# Quick test script to verify relay server connectivity

RELAY_IP="80.225.92.3"
RELAY_PORT="8081"

echo "🔍 Testing Relay Server Connection"
echo "=================================="
echo "IP: $RELAY_IP"
echo "Port: $RELAY_PORT"
echo ""

# Test 1: Ping
echo "1️⃣  Testing ping..."
if ping -c 3 $RELAY_IP > /dev/null 2>&1; then
    echo "   ✅ Server is reachable"
else
    echo "   ❌ Server is NOT reachable"
    echo "   → Check if IP address is correct"
    exit 1
fi

# Test 2: Port check
echo ""
echo "2️⃣  Testing port $RELAY_PORT..."
if timeout 5 bash -c "</dev/tcp/$RELAY_IP/$RELAY_PORT" 2>/dev/null; then
    echo "   ✅ Port $RELAY_PORT is OPEN"
    echo "   → Relay server should be accessible"
else
    echo "   ❌ Port $RELAY_PORT is CLOSED or unreachable"
    echo ""
    echo "   Possible issues:"
    echo "   → Relay server not running on Oracle"
    echo "   → Firewall blocking port 8081"
    echo "   → Oracle Cloud Security List not configured"
    echo ""
    echo "   Solutions:"
    echo "   1. SSH into Oracle and start relay server:"
    echo "      java kuroyale.mainpack.network.RelayServer 8081"
    echo ""
    echo "   2. Check Oracle Cloud Console:"
    echo "      Networking → Security Lists → Add Ingress Rule"
    echo "      TCP, Port 8081, Source: 0.0.0.0/0"
    echo ""
    exit 1
fi

# Test 3: Try telnet (if available)
echo ""
echo "3️⃣  Testing TCP connection..."
if command -v nc > /dev/null 2>&1; then
    if nc -zv -w 5 $RELAY_IP $RELAY_PORT 2>&1 | grep -q "succeeded"; then
        echo "   ✅ TCP connection successful"
    else
        echo "   ⚠️  TCP connection failed"
    fi
elif command -v telnet > /dev/null 2>&1; then
    echo "   (Using telnet to test - may take a moment)"
    timeout 3 telnet $RELAY_IP $RELAY_PORT 2>&1 | grep -q "Connected" && echo "   ✅ TCP connection successful" || echo "   ⚠️  TCP connection failed"
else
    echo "   (nc/telnet not available, skipping detailed test)"
fi

echo ""
echo "=================================="
echo "✅ Connection test complete!"
echo ""
echo "If all tests passed, the relay server should be accessible."
echo "If tests failed, check the troubleshooting guide: TROUBLESHOOTING_RELAY.md"

