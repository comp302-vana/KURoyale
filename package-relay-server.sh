#!/bin/bash

# Script to package relay server files for Oracle Cloud deployment

set -e

echo "📦 Packaging Relay Server for Oracle Cloud..."

# Create deployment directory
DEPLOY_DIR="relay-server-deploy"
mkdir -p "$DEPLOY_DIR/kuroyale/mainpack/network"

# Copy required files
echo "Copying files..."
cp app/src/main/java/kuroyale/mainpack/network/RelayServer.java "$DEPLOY_DIR/kuroyale/mainpack/network/"
cp app/src/main/java/kuroyale/mainpack/network/NetworkMessage.java "$DEPLOY_DIR/kuroyale/mainpack/network/"
cp app/src/main/java/kuroyale/mainpack/network/MessageProtocol.java "$DEPLOY_DIR/kuroyale/mainpack/network/"

# Create README for deployment
cat > "$DEPLOY_DIR/README.txt" << 'EOF'
KURoyale Relay Server - Deployment Package

FILES:
- kuroyale/mainpack/network/RelayServer.java
- kuroyale/mainpack/network/NetworkMessage.java
- kuroyale/mainpack/network/MessageProtocol.java

DEPLOYMENT:
1. Upload this entire folder to your Oracle server
2. cd into the folder (where kuroyale/ is)
3. Compile: javac kuroyale/mainpack/network/*.java
4. Run: java kuroyale.mainpack.network.RelayServer 8081

REQUIREMENTS:
- Java 11 or higher
- Port 8081 open in firewall

See ORACLE_DEPLOYMENT.md for detailed instructions.
EOF

# Create a simple start script
cat > "$DEPLOY_DIR/start-relay.sh" << 'EOF'
#!/bin/bash
# Start relay server
cd "$(dirname "$0")"
java kuroyale.mainpack.network.RelayServer 8081
EOF

chmod +x "$DEPLOY_DIR/start-relay.sh"

# Create tar archive
echo "Creating archive..."
tar -czf relay-server-deploy.tar.gz "$DEPLOY_DIR/"

# Show summary
echo ""
echo "✅ Package created: relay-server-deploy.tar.gz"
echo ""
echo "📊 Package contents:"
du -sh "$DEPLOY_DIR"
echo ""
echo "📋 Files included:"
find "$DEPLOY_DIR" -type f -name "*.java" | sed 's/^/  /'
echo ""
echo "🚀 To deploy:"
echo "  1. scp relay-server-deploy.tar.gz user@oracle-server:/path/"
echo "  2. ssh user@oracle-server"
echo "  3. tar -xzf relay-server-deploy.tar.gz"
echo "  4. cd relay-server-deploy"
echo "  5. javac kuroyale/mainpack/network/*.java"
echo "  6. java kuroyale.mainpack.network.RelayServer 8081"
echo ""

