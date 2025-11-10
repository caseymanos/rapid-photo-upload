# Supabase PostgreSQL Integration Guide

## ✅ Completed Steps

### 1. Database Schema Migration
All photo upload tables have been successfully created in Supabase PostgreSQL:

- **`public.users`** - 7 users (linked to `auth.users`) ✅
- **`public.upload_sessions`** - Batch upload tracking ✅
- **`public.photos`** - Photo metadata with S3 keys ✅
- **`public.upload_parts`** - Multipart upload tracking ✅
- **`public.photo_events`** - Event sourcing ✅

**Key Features Implemented:**
- Row Level Security (RLS) policies on all tables
- Auto-update triggers for `updated_at` timestamps
- Automatic user creation trigger (`on_auth_user_created`)
- Proper indexes for performance
- Foreign key constraints for data integrity

---

## 🔧 Backend Configuration

### Step 1: Get Supabase Connection Details

You need the following from your Supabase project dashboard (https://supabase.com/dashboard/project/nhadlfbxbivlhtkbolve):

1. **Database Password**: Settings → Database → Connection string
2. **Project Reference**: `nhadlfbxbivlhtkbolve`
3. **JWT Secret**: Settings → API → JWT Settings → JWT Secret

### Step 2: Update `application.yml`

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    # Supabase PostgreSQL Connection (Session Pooler - Port 6543)
    url: jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require
    username: postgres.nhadlfbxbivlhtkbolve
    password: ${SUPABASE_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20  # Supabase pooler can handle this
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 300000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate  # Keep validate - schema already exists in Supabase
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  flyway:
    enabled: false  # Disable Flyway - we manage migrations via Supabase

security:
  jwt:
    secret: ${JWT_SECRET}  # Your custom JWT secret for backend-generated tokens
    expiration-ms: 86400000
  supabase:
    url: https://nhadlfbxbivlhtkbolve.supabase.co
    jwt-secret: ${SUPABASE_JWT_SECRET}  # From Supabase dashboard

aws:
  region: ${AWS_REGION:us-east-1}
  s3:
    bucket:
      name: ${S3_BUCKET_NAME}
```

### Step 3: Create Environment Variables

Create `backend/.env` file:

```bash
# Supabase Database (get from Supabase dashboard → Settings → Database)
SUPABASE_DB_PASSWORD=your_database_password_here

# Supabase JWT Secret (get from Supabase dashboard → Settings → API)
SUPABASE_JWT_SECRET=your_jwt_secret_here

# Your custom JWT secret (can keep existing or generate new)
JWT_SECRET=your_custom_jwt_secret

# AWS S3 (existing)
AWS_REGION=us-east-1
S3_BUCKET_NAME=rapid-photo-upload-dev
```

### Step 4: Update JwtService for Supabase Token Support

The backend needs to support **both** token types:
1. **Supabase tokens** - from mobile/web Supabase Auth
2. **Custom tokens** - for backend-to-backend communication (optional)

Create `backend/src/main/java/com/rapidphotoupload/infrastructure/security/SupabaseJwtValidator.java`:

```java
package com.rapidphotoupload.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * Validates Supabase JWT tokens.
 */
@Slf4j
@Component
public class SupabaseJwtValidator {
    
    @Value("${security.supabase.jwt-secret}")
    private String supabaseJwtSecret;
    
    /**
     * Validate Supabase JWT token.
     * @param token the JWT token from Supabase Auth
     * @return true if valid
     */
    public boolean validateSupabaseToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Supabase token validation failed", e);
            return false;
        }
    }
    
    /**
     * Extract user ID from Supabase token.
     * Supabase tokens have 'sub' claim with user UUID.
     */
    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        String sub = claims.getSubject();
        return UUID.fromString(sub);
    }
    
    /**
     * Extract email from Supabase token.
     */
    public String extractEmail(String token) {
        Claims claims = extractClaims(token);
        return claims.get("email", String.class);
    }
    
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = supabaseJwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### Step 5: Update JwtAuthenticationFilter

Modify `backend/src/main/java/com/rapidphotoupload/infrastructure/security/JwtAuthenticationFilter.java`:

Add support for Supabase tokens:

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final SupabaseJwtValidator supabaseJwtValidator;  // Add this
    
    @Override
    protected void doFilterInternal(/*...*/) {
        String jwt = extractJwtFromRequest(request);
        
        if (jwt != null) {
            try {
                // Try Supabase token first
                if (supabaseJwtValidator.validateSupabaseToken(jwt)) {
                    UUID userId = supabaseJwtValidator.extractUserId(jwt);
                    String email = supabaseJwtValidator.extractEmail(jwt);
                    
                    // Set authentication
                    UserPrincipal principal = new UserPrincipal(userId, email, Collections.emptyList());
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                } else if (jwtService.validateToken(jwt, extractEmail(jwt))) {
                    // Fallback to custom JWT
                    // ... existing logic
                }
            } catch (Exception e) {
                log.error("Token validation error", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Step 6: Remove Password Fields from User Entity

Since we're using Supabase Auth, update `UserEntity.java`:

```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;  // References auth.users(id)
    
    @Column(nullable = false, unique = true)
    private String email;
    
    // Remove password_hash - managed by Supabase Auth
    
    private Instant createdAt;
    private Instant updatedAt;
}
```

---

## 🧪 Testing the Integration

### 1. Verify Database Connection

```bash
cd backend
./mvnw spring-boot:run
```

Look for successful startup logs:
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Initialized JPA EntityManagerFactory
```

### 2. Test with Supabase Token

Get a token from your mobile/web app (after login), then test:

```bash
# Get token from browser/mobile app localStorage
TOKEN="eyJhbGc..."

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/photos
```

### 3. Check Database

Verify tables exist in Supabase:
```sql
SELECT table_name, row_security_active 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('users', 'photos', 'upload_sessions', 'upload_parts');
```

---

## 🔒 Security Architecture

### Authentication Flow

```
┌──────────────┐         ┌────────────────┐         ┌──────────────┐
│ Mobile/Web   │────────>│ Supabase Auth  │────────>│ Java Backend │
│ App          │ Login   │ (nhadlfbx...)  │  JWT    │  (validates) │
└──────────────┘         └────────────────┘         └──────────────┘
                                 │
                                 ↓
                         ┌────────────────┐
                         │  PostgreSQL    │
                         │  auth.users    │
                         │  public.users  │
                         └────────────────┘
```

### Row Level Security (RLS)

All tables have RLS policies ensuring users can only access their own data:

```sql
-- Example: photos table policy
CREATE POLICY "Users can view own photos"
  ON public.photos FOR SELECT
  USING (auth.uid() = user_id);
```

When the Java backend connects with the `postgres` role, it bypasses RLS. This is correct for your architecture since JWT validation happens at the application layer.

---

## 📊 Database Schema Overview

```
auth.users (Supabase managed)
    ↓ (FK)
public.users
    ↓ (FK)
├── upload_sessions
│       ↓ (FK)
│   photos ←──── upload_parts
│       ↓ (FK)
│   photo_events
```

### Table Details

#### **users**
- Links to `auth.users` 
- Auto-created via trigger when user signs up
- 7 users already present ✅

#### **upload_sessions**
- Tracks batch uploads
- Status: IN_PROGRESS, COMPLETED, FAILED, CANCELLED
- Counts: total_photos, completed_photos, failed_photos

#### **photos**
- S3 keys, metadata, status
- Upload status: PENDING, INITIATED, UPLOADING, COMPLETED, FAILED
- Supports multipart uploads
- Width/height for image dimensions
- JSONB fields for tags and metadata

#### **upload_parts**
- Tracks individual parts of multipart S3 uploads
- Part number, ETag, size

#### **photo_events**
- Event sourcing for audit trail
- JSONB event_data for flexibility

---

## 🚀 Performance Optimizations

### Connection Pooling
- Using Supabase Session Pooler (port 6543)
- Hikari pool: 20 max connections, 5 min idle
- Optimized for serverless/bursty traffic

### Indexes Created
```sql
-- Users
CREATE INDEX idx_users_email ON users(email);

-- Upload Sessions
CREATE INDEX idx_upload_sessions_user_id ON upload_sessions(user_id);
CREATE INDEX idx_upload_sessions_status ON upload_sessions(status);

-- Photos
CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_upload_session_id ON photos(upload_session_id);
CREATE INDEX idx_photos_upload_status ON photos(upload_status);
CREATE INDEX idx_photos_created_at ON photos(created_at DESC);
CREATE INDEX idx_photos_tags ON photos USING GIN (tags);

-- Upload Parts
CREATE INDEX idx_upload_parts_photo_id ON upload_parts(photo_id);

-- Photo Events
CREATE INDEX idx_photo_events_photo_id ON photo_events(photo_id);
CREATE INDEX idx_photo_events_occurred_at ON photo_events(occurred_at DESC);
```

---

## 🔄 Migration from Old Database

If you have existing data in another database:

### Option 1: Fresh Start (Recommended)
Since you already have 7 users in Supabase auth, just start fresh. The trigger will create user records automatically.

### Option 2: Migrate Existing Photos
```sql
-- Export from old DB
COPY (SELECT * FROM photos) TO '/tmp/photos.csv' CSV HEADER;

-- Import to Supabase (adjust user_id mappings)
COPY public.photos FROM '/tmp/photos.csv' CSV HEADER;
```

---

## ⚡ Next Steps

1. **Update environment variables** in `backend/.env`
2. **Get Supabase credentials** from dashboard
3. **Add SupabaseJwtValidator** class
4. **Update JwtAuthenticationFilter** to support Supabase tokens
5. **Remove password field** from UserEntity
6. **Test connection** with `./mvnw spring-boot:run`
7. **Test API calls** with Supabase JWT token
8. **Deploy and test** with mobile/web apps

---

## 📝 Important Notes

### Flyway vs Supabase Migrations
- **Disable Flyway** in production (set `spring.flyway.enabled=false`)
- Use Supabase migrations for schema changes
- Keep `V001__initial_schema.sql` for documentation only

### Authentication
- Mobile/Web apps use Supabase Auth SDK
- Backend validates Supabase JWT tokens
- Backend can still generate custom tokens if needed (e.g., for service-to-service)

### RLS Policies
- Supabase enforces RLS at database level
- Java backend connects as `postgres` role (bypasses RLS)
- Application-level security via JWT validation is your primary defense

### Connection String Options

**Session Pooler (Recommended for REST APIs):**
```
jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres
```

**Direct Connection (Use for long-running migrations):**
```
jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres
```

---

## 🐛 Troubleshooting

### "Connection refused"
- Check firewall/network settings
- Verify connection string format
- Ensure `sslmode=require` is set

### "Password authentication failed"
- Get password from Supabase dashboard → Database settings
- Make sure you're using `postgres.nhadlfbxbivlhtkbolve` as username

### "JWT validation failed"
- Get JWT secret from Supabase dashboard → API settings
- Ensure token hasn't expired
- Check token is being passed in `Authorization: Bearer <token>` header

### "Schema 'public' does not exist"
- This shouldn't happen - `public` schema exists by default
- Check you're connecting to the correct database

### "Table does not exist"
- Run the migrations via Supabase MCP tools or SQL editor
- Check `hibernate.ddl-auto` is set to `validate` not `create`

---

## 📞 Support Resources

- **Supabase Docs**: https://supabase.com/docs/guides/database
- **Spring Boot + PostgreSQL**: https://spring.io/guides/gs/accessing-data-jpa
- **Supabase Dashboard**: https://supabase.com/dashboard/project/nhadlfbxbivlhtkbolve

---

## ✅ Success Criteria

- [ ] Backend connects to Supabase PostgreSQL
- [ ] API calls work with Supabase JWT tokens
- [ ] Photo upload endpoints function correctly
- [ ] RLS policies work (users only see their photos)
- [ ] 100 concurrent uploads complete in <90 seconds
- [ ] WebSocket real-time updates work
- [ ] Mobile app can upload and view photos
- [ ] Web app can upload and view photos
