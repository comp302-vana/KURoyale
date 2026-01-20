#!/bin/bash
# Start the Relay Server for NAT traversal

cd "$(dirname "$0")"

echo "Starting Relay Server on port 8081..."
echo "Press Ctrl+C to stop"
echo ""

# Run the relay server using the compiled classes
java -cp "app/build/classes/java/main" kuroyale.mainpack.network.RelayServer 8081

