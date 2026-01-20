#!/bin/bash
# Helper script to get Cloudflare tunnel info
# Run this after starting the tunnel to see what URL/IP to use

echo "=========================================="
echo "Cloudflare Tunnel Info Extractor"
echo "=========================================="
echo ""
echo "After you run: cloudflared tunnel --url tcp://localhost:8081"
echo "Look for one of these in the output:"
echo ""
echo "1. A URL like: https://xxxxx-xxxxx-xxxxx.trycloudflare.com"
echo "2. An IP and port like: connect via: tcp://xxx.xxx.xxx.xxx:xxxxx"
echo ""
echo "Use the HOSTNAME (not the https:// part) or IP as RELAY_SERVER_IP"
echo "Use the PORT number as RELAY_SERVER_PORT"
echo ""
echo "Example:"
echo "  If you see: https://abc-123-def.trycloudflare.com"
echo "  Use: RELAY_SERVER_IP = \"abc-123-def.trycloudflare.com\""
echo ""
echo "  If you see: connect via: tcp://1.2.3.4:54321"
echo "  Use: RELAY_SERVER_IP = \"1.2.3.4\""
echo "       RELAY_SERVER_PORT = 54321"
echo ""

