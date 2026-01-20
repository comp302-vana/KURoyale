# Quick Start - Automated Setup

## One Command Setup! 🚀

Just run:
```bash
./auto-setup-relay.sh
```

This script will:
1. ✅ Start the relay server
2. ✅ Start Cloudflare tunnel
3. ✅ Automatically detect the tunnel URL/IP
4. ✅ Update `NetworkManager.java` with the correct values
5. ✅ Show you what was configured

## After Running

1. **Recompile:**
   ```bash
   ./gradlew compileJava
   ```

2. **Run the game:**
   ```bash
   ./gradlew run
   ```

3. **Test:**
   - Host: Click "Create Lobby"
   - Client: Click "Join Lobby"
   - Both should connect via relay!

## Important

- **Keep the terminal open** - The relay server and tunnel must stay running
- **Press Ctrl+C** to stop everything when done
- The tunnel URL changes each time you restart (the script updates it automatically)

## Manual Setup (if automation fails)

If the auto-detection doesn't work:

1. Run: `./start-relay-with-tunnel.sh`
2. Copy the hostname/IP from the output
3. Manually update `NetworkManager.java`:
   ```java
   private static final String RELAY_SERVER_IP = "your-hostname-here";
   ```

## Troubleshooting

- **"Cannot connect"**: Make sure the script is still running (relay + tunnel)
- **"Connection timeout"**: Check that `RELAY_SERVER_IP` in NetworkManager.java matches what Cloudflare showed
- **URL changes**: Just run `./auto-setup-relay.sh` again to update automatically

