# Backend Restart Instructions

The backend needs to be restarted to pick up the performance optimizations.

## Quick Restart

1. **Find the backend process**:
   ```bash
   ps aux | grep "spring-boot:run" | grep -v grep
   ```

2. **Kill it** (use the PID from step 1):
   ```bash
   kill <PID>
   ```

3. **Restart it**:
   ```bash
   cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/backend
   mvn spring-boot:run > backend.out 2>&1 &
   ```

4. **Verify it's running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Or One-Command Restart

```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/backend && \
pkill -f "spring-boot:run" && \
sleep 2 && \
mvn spring-boot:run > backend.out 2>&1 &
```

## Verify Optimizations Are Active

After restart, check the logs:
```bash
tail -f backend/backend.out | grep -E "(parallel|Performance)"
```

Then upload a photo via the web UI (http://localhost:3004) and you should see:
```
INFO - Generating 20 presigned URLs for key: ...
INFO - Generated 20 presigned URLs in 387ms (parallel)
```

## Alternative: Clean Restart

For a fresh start:
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload/backend
pkill -f "spring-boot:run"
mvn clean
mvn spring-boot:run > backend.out 2>&1 &
```

This will:
1. Stop the backend
2. Clean compiled files
3. Recompile with optimizations
4. Start fresh

## Monitoring

Watch logs in real-time:
```bash
cd /Users/caseymanos/GauntletAI/rapidPhotoUpload
tail -f backend/backend.out
```

Press Ctrl+C to stop watching.
