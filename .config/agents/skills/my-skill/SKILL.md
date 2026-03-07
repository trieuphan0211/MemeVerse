# MemeVerse - Hệ Sinh Thái Giải Trí Web cho Gen Z Việt Nam

> 🎯 **Tầm nhìn**: Xây dựng nền tảng giải trí tập trung vào meme cộng đồng, chat ẩn danh realtime và mini game cạnh tranh.

---

## 📊 Mục Tiêu & KPI

### Giai đoạn Khởi đầu (0-6 tháng)
| Chỉ số | Mục tiêu |
|--------|----------|
| User đăng ký | 1,000 |
| DAU (Daily Active Users) | 100-300 |
| Content mới/ngày | 50 memes |

### Giai đoạn Scale (6-18 tháng)
| Chỉ số | Mục tiêu |
|--------|----------|
| MAU (Monthly Active Users) | 10,000-50,000 |
| Concurrent Users | 1,000 |
| Doanh thu | 3-10 triệu VND/tháng |

---

## 💰 Mô Hình Kinh Doanh

1. **Google AdSense** - Quảng cáo display
2. **Affiliate Marketing** - Shopee, Lazada links
3. **Premium Account** - Tính năng nâng cao, remove ads
4. **Sponsored Meme** - Quảng cáo dạng native content
5. **In-Game Reward Ads** - Xem quảng cáo đổi lấy reward

---

## 🏗️ Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────┐
│         Client (NextJS)             │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│        API Gateway                  │
│  (Spring Cloud Gateway)             │
│  - Rate Limiting                    │
│  - JWT Validation                   │
│  - Routing                          │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│     Microservices Layer             │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │  User   │ │  Meme   │ │  Chat  │ │
│  └─────────┘ └─────────┘ └────────┘ │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │  Game   │ │  Ads    │ │Notif.  │ │
│  └─────────┘ └─────────┘ └────────┘ │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│       Event Bus (Kafka)             │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│  Analytics Service │ Notification   │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│     Keycloak (Identity Provider)    │
│  - Authentication                   │
│  - Authorization                    │
│  - User Management                  │
│  - Social Login                     │
└─────────────────────────────────────┘
```

---

## 🔧 Chi Tiết Microservices

### 1. API Gateway

**Công nghệ:** Spring Cloud Gateway

**Chức năng chính:**
- **Routing**: Điều hướng request đến service phù hợp
- **Authentication**: JWT validation
- **Rate Limiting**: Throttle request theo IP/user
- **Logging**: Request/response logging
- **API Versioning**: `/api/v1/...`

**Bảo mật & Vận hành:**
- CORS configuration
- Circuit breaker pattern
- Stateless design (dễ scale)

**Scale Strategy:**
- Deploy multiple instances behind Load Balancer
- Horizontal scaling không giới hạn

---

### 2. Keycloak - Identity Provider 🔐

**Tại sao chọn Keycloak:**
- Giảm complexity: Không cần tự xây dựng Auth Service
- Tích hợp sẵn OAuth2/OpenID Connect
- Social login (Google, Facebook) dễ dàng
- Admin Console quản lý users, roles, permissions
- Bảo mật theo chuẩn enterprise

**Chức năng:**
- **Authentication**: Login/Register, Forgot password, Email verification
- **Authorization**: Role-based access (USER, ADMIN, MODERATOR)
- **Token Management**: Access token (15 phút), Refresh token (7 ngày)
- **Social Login**: Google, Facebook (dễ dàng cấu hình)
- **User Federation**: LDAP, Active Directory (cho enterprise)
- **Session Management**: Revoke tokens, device tracking

**Realm Configuration:**
```json
{
  "realm": "MemeVerse",
  "enabled": true,
  "registrationAllowed": true,
  "registrationEmailAsUsername": false,
  "rememberMe": true,
  "verifyEmail": true,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "bruteForceProtected": true,
  "permanentLockout": false,
  "maxFailureWaitSeconds": 900,
  "minimumQuickLoginWaitSeconds": 60,
  "waitIncrementSeconds": 60,
  "maxQuickLoginWaitSeconds": 900,
  "failureFactor": 5
}
```

**Client Configuration (Backend Services):**
```json
{
  "clientId": "memeverse-api",
  "enabled": true,
  "clientAuthenticatorType": "client-secret",
  "secret": "********",
  "redirectUris": ["*"],
  "webOrigins": ["*"],
  "standardFlowEnabled": true,
  "implicitFlowEnabled": false,
  "directAccessGrantsEnabled": true,
  "serviceAccountsEnabled": true,
  "authorizationServicesEnabled": true
}
```

**Roles:**
- `USER`: Ngườì dùng thông thường
- `MODERATOR`: Kiểm duyệt viên (duyệt meme, xử lý report)
- `ADMIN`: Quản trị viên (quản lý users, settings)

**JWT Token Claims:**
```json
{
  "sub": "user-uuid",
  "preferred_username": "johndoe",
  "email": "john@example.com",
  "given_name": "John",
  "family_name": "Doe",
  "realm_access": {
    "roles": ["USER"]
  },
  "role": "USER",
  "exp": 1704067200,
  "iat": 1704066600
}
```

**Service Integration:**
```java
// Spring Boot Resource Server
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
            );
        return http.build();
    }
}
```

**Events (via Kafka/Event Listener):**
- `REGISTER` → Gửi welcome email
- `LOGIN` → Ghi log analytics
- `LOGOUT` → Xóa session cache

---

### 3. User Service

**Chức năng:**
- Profile management (view/update)
- Avatar upload, bio, username
- Follow system (follow/unfollow)
- User statistics (meme count, likes, game score)
- Block/Unblock user

**Database Schema:**
```sql
user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    display_name VARCHAR(100),
    bio TEXT,
    avatar_url VARCHAR(500),
    location VARCHAR(100),
    website VARCHAR(255),
    updated_at TIMESTAMP
)

followers (
    id UUID PRIMARY KEY,
    follower_id UUID REFERENCES users(id),
    following_id UUID REFERENCES users(id),
    created_at TIMESTAMP,
    UNIQUE(follower_id, following_id)
)

user_stats (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    total_memes INTEGER DEFAULT 0,
    total_likes_received INTEGER DEFAULT 0,
    total_comments INTEGER DEFAULT 0,
    game_score INTEGER DEFAULT 0,
    followers_count INTEGER DEFAULT 0,
    following_count INTEGER DEFAULT 0,
    updated_at TIMESTAMP
)

blocks (
    id UUID PRIMARY KEY,
    blocker_id UUID REFERENCES users(id),
    blocked_id UUID REFERENCES users(id),
    created_at TIMESTAMP,
    UNIQUE(blocker_id, blocked_id)
)
```

**Cache Strategy:**
- Redis cache cho profile hot user (LRU eviction)
- TTL: 1 giờ cho profile, 5 phút cho stats

**Events:**
- `UserFollowed`
- `UserUnfollowed`
- `UserBlocked`
- `ProfileUpdated`

---

### 4. Meme Service (Core) 🔥

**Chức năng:**
- Upload meme (image + caption + tags)
- Vote system (upvote/downvote, chống double vote)
- Comment (add/delete/reply)
- Feed algorithms:
  - **Latest**: Sort theo thởi gian
  - **Trending**: Score = (votes × weight) / time_decay
  - **Following**: Meme từ ngưởi đang follow
- Search (tag/keyword)
- Report system (spam/offensive)

**Database Schema:**
```sql
memes (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(500),
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'DELETED') DEFAULT 'PENDING',
    vote_score INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_status_created (status, created_at),
    INDEX idx_vote_score (vote_score)
)

meme_votes (
    id UUID PRIMARY KEY,
    meme_id UUID REFERENCES memes(id),
    user_id UUID REFERENCES users(id),
    vote_type ENUM('UP', 'DOWN'),
    created_at TIMESTAMP,
    UNIQUE(meme_id, user_id)
)

meme_comments (
    id UUID PRIMARY KEY,
    meme_id UUID REFERENCES memes(id),
    user_id UUID REFERENCES users(id),
    parent_id UUID REFERENCES meme_comments(id) NULL,
    content TEXT NOT NULL,
    likes_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)

tags (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    usage_count INTEGER DEFAULT 0
)

meme_tags (
    meme_id UUID REFERENCES memes(id),
    tag_id UUID REFERENCES tags(id),
    PRIMARY KEY (meme_id, tag_id)
)

reports (
    id UUID PRIMARY KEY,
    meme_id UUID REFERENCES memes(id),
    reporter_id UUID REFERENCES users(id),
    reason ENUM('SPAM', 'OFFENSIVE', 'COPYRIGHT', 'OTHER'),
    description TEXT,
    status ENUM('PENDING', 'RESOLVED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP
)
```

**Trending Algorithm:**
```python
# Wilson Score Interval hoặc Reddit Hot Algorithm
score = log10(max(abs(votes), 1))
sign = 1 if votes > 0 else -1 if votes < 0 else 0
order = log10(max(abs(votes), 1))
seconds = epoch_seconds(created_at) - 1134028003
hot_score = round(sign * order + seconds / 45000, 7)
```

**Storage:**
- MinIO (S3-compatible) cho local development
- AWS S3/Cloudflare R2 khi production
- CDN cho image delivery

**Cache Strategy:**
- Redis cho hot feed (top 100 trending)
- Cache invalidation: Write-through cho votes
- Feed pagination: Cursor-based

**Events:**
- `MemeCreated`
- `MemeVoted`
- `MemeCommented`
- `MemeReported`
- `MemeApproved`

**Scale Considerations:**
- Read replicas cho query nặng
- Sharding theo `created_at` khi > 10M memes
- CDN edge caching

---

### 5. Chat Service (Realtime)

**Stack:** WebSocket + Redis Pub/Sub + Node.js/Spring Boot

**Chế độ:**
- **Random Chat**: Kết nối ngẫu nhiên 2 user
- **Confession Box**: Gửi ẩn danh, hiển thị công khai
- **Room Chat**: Phòng chat theo chủ đề

**Database Schema:**
```sql
chat_rooms (
    id UUID PRIMARY KEY,
    name VARCHAR(100),
    type ENUM('RANDOM', 'CONFESSION', 'TOPIC'),
    topic VARCHAR(100),
    max_users INTEGER,
    created_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true
)

chat_messages (
    id UUID PRIMARY KEY,
    room_id UUID REFERENCES chat_rooms(id),
    user_id UUID REFERENCES users(id) NULL, -- NULL for anonymous
    session_id VARCHAR(255), -- For anonymous tracking
    content TEXT NOT NULL,
    message_type ENUM('TEXT', 'IMAGE', 'SYSTEM'),
    is_anonymous BOOLEAN DEFAULT false,
    created_at TIMESTAMP
)

chat_participants (
    id UUID PRIMARY KEY,
    room_id UUID REFERENCES chat_rooms(id),
    user_id UUID REFERENCES users(id),
    session_id VARCHAR(255),
    joined_at TIMESTAMP,
    left_at TIMESTAMP NULL
)
```

**Realtime Architecture:**
```
Client A ──► WS Server 1 ──┐
                           ├──► Redis Pub/Sub ◄──┤
Client B ──► WS Server 2 ──┘                      │
                                                  │
Client C ──► WS Server 3 ◄────────────────────────┘
```

**Bảo mật:**
- Rate limiting: 30 messages/phút/user
- Profanity filter (Vietnamese + English)
- Auto-moderation (spam detection)
- Image moderation (AI-based)

**Scale:**
- WebSocket cluster với sticky sessions
- Redis Pub/Sub để sync giữa instances
- Horizontal scaling không giới hạn

---

### 6. Game Service

**Game đề xuất:**
- **Reaction Speed**: Test phản xạ
- **Quiz Game**: Câu hỏi meme culture
- **Click Battle**: Đấu click 1v1
- **Meme Caption**: Thiết caption hay nhất

**Chức năng:**
- Tạo phòng (create room)
- Matchmaking (ELO-based)
- Real-time gameplay
- Leaderboard

**Database Schema:**
```sql
game_rooms (
    id UUID PRIMARY KEY,
    game_type ENUM('REACTION', 'QUIZ', 'CLICK_BATTLE', 'CAPTION'),
    status ENUM('WAITING', 'PLAYING', 'FINISHED'),
    host_id UUID REFERENCES users(id),
    max_players INTEGER,
    created_at TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP
)

game_scores (
    id UUID PRIMARY KEY,
    room_id UUID REFERENCES game_rooms(id),
    user_id UUID REFERENCES users(id),
    score INTEGER,
    rank INTEGER,
    played_at TIMESTAMP
)

leaderboards (
    id UUID PRIMARY KEY,
    game_type ENUM('REACTION', 'QUIZ', 'CLICK_BATTLE', 'CAPTION'),
    user_id UUID REFERENCES users(id),
    total_score BIGINT DEFAULT 0,
    games_played INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    rank_score FLOAT, -- ELO or similar
    updated_at TIMESTAMP
)
```

**Anti-cheat:**
- Server-side validation mọi action
- Check abnormal patterns (click speed impossible)
- Replay validation
- Device fingerprinting

**Cache:**
- Redis Sorted Set cho real-time leaderboard
- Daily/Weekly/Monthly leaderboards

---

### 7. Ads Service

**Chức năng:**
- Quản lý vị trí quảng cáo
- Log impression/click
- Reporting & Analytics
- A/B testing creatives

**Integration:**
- Google AdSense API
- Shopee Affiliate API
- Lazada Affiliate API
- Keycloak Admin API (cho user management)

**Database Schema:**
```sql
ad_placements (
    id UUID PRIMARY KEY,
    name VARCHAR(100),
    location ENUM('FEED', 'SIDEBAR', 'MEME_DETAIL', 'GAME_INTERSTITIAL'),
    dimensions VARCHAR(50),
    is_active BOOLEAN DEFAULT true
)

ad_campaigns (
    id UUID PRIMARY KEY,
    placement_id UUID REFERENCES ad_placements(id),
    type ENUM('ADSENSE', 'AFFILIATE', 'SPONSORED'),
    content JSON, -- Ad content/links
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    budget DECIMAL(10,2),
    is_active BOOLEAN DEFAULT true
)

ad_events (
    id UUID PRIMARY KEY,
    campaign_id UUID REFERENCES ad_campaigns(id),
    user_id UUID REFERENCES users(id),
    event_type ENUM('IMPRESSION', 'CLICK'),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP
)
```

**Events:**
- `AdViewed`
- `AdClicked`
- `AdConversion`

---

### 8. Notification Service

**Kênh:**
- In-app notification (WebSocket)
- Email (SendGrid/AWS SES)
- Push notification (Firebase Cloud Messaging)

**Event-driven:** Subscribe Kafka topics

**Database Schema:**
```sql
notifications (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    type ENUM('MEME_LIKE', 'COMMENT', 'FOLLOW', 'SYSTEM', 'CHAT'),
    title VARCHAR(255),
    content TEXT,
    data JSON, -- Payload tùy loại
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP
)

notification_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    email_notifications BOOLEAN DEFAULT true,
    push_notifications BOOLEAN DEFAULT true,
    meme_likes BOOLEAN DEFAULT true,
    comments BOOLEAN DEFAULT true,
    follows BOOLEAN DEFAULT true,
    chat_messages BOOLEAN DEFAULT true
)
```

**Events consumed:**
- `MemeVoted` → Notify meme owner
- `UserFollowed` → Notify user
- `MemeCommented` → Notify meme owner
- `UserRegistered` → Welcome email

---

### 9. Analytics Service

**Thu thập dữ liệu:**
- Page views, meme views
- Click events
- Session duration
- Chat engagement time
- Game play time
- Conversion funnel

**Pipeline:**
```
Client ──► API Gateway ──► Kafka ──► Analytics Service ──► ClickHouse/TimescaleDB
```

**Database Schema (Time-series):**
```sql
-- Sử dụng TimescaleDB hoặc ClickHouse
events (
    time TIMESTAMP NOT NULL,
    user_id UUID,
    session_id VARCHAR(255),
    event_type VARCHAR(50),
    page_url TEXT,
    metadata JSON,
    device_info JSON
)

-- Hypertable cho time-series data
SELECT create_hypertable('events', 'time');
```

**Metrics:**
- DAU/MAU/WAU
- Retention rate (1-day, 7-day, 30-day)
- Average session duration
- Top memes/tags
- Revenue per user (ARPU)
- Conversion rate (free → premium)

**Dashboard:**
- Grafana cho real-time monitoring
- Custom admin dashboard
- Automated reports (daily/weekly)

---

## 🚀 Lộ Trình Triển Khai

### Phase 1: MVP (Tháng 0-2)

**Mục tiêu:** Có sản phẩm chạy được, thu hút user đầu tiên

**Scope:**
- ✅ Keycloak (Authentication/Authorization)
- ✅ User Service (Profile cơ bản, sync với Keycloak)
- ✅ Meme Service (Upload, Vote, Basic Feed)
- ✅ Frontend NextJS cơ bản
- ✅ Deploy Docker trên VPS

**Technical decisions:**
- Keycloak làm Identity Provider (thay vì tự xây Auth Service)
- User Service lưu thông tin profile mở rộng
- PostgreSQL single instance
- Redis cho session/cache
- MinIO cho storage
- **Không cần**: Kafka, K8s, Elasticsearch

**Milestone:** Website live, 100 user đầu tiên

**Note:** Việc sử dụng Keycloak giúp team tập trung vào business logic thay vì bảo mật authentication. Keycloak cung cấp đầy đủ tính năng: social login, MFA, session management, brute force protection,... mà không cần code thêm.

---

### Phase 2: Realtime & Monetization (Tháng 2-4)

**Mục tiêu:** Tăng engagement, bắt đầu monetize

**Thêm vào:**
- ✅ Chat Service (WebSocket, Random chat)
- ✅ Mini game đơn giản (Quiz)
- ✅ Redis cache nâng cao
- ✅ Google AdSense integration
- ✅ Affiliate links
- ✅ Basic notification

**Technical:**
- Tách Meme Service thành standalone
- WebSocket cluster
- CDN cho images

**Milestone:** 300-500 DAU, doanh thu đầu tiên từ ads

---

### Phase 3: Scale Architecture (Tháng 4-6)

**Mục tiêu:** Chuẩn bị cho scale lớn

**Thêm vào:**
- ✅ Kafka Event Bus
- ✅ Full microservices architecture
- ✅ Analytics Service
- ✅ Notification Service hoàn chỉnh
- ✅ CDN toàn diện
- ✅ Rate limiting nâng cao
- ✅ Search (Elasticsearch)

**Technical:**
- Docker Compose → Kubernetes
- Database read replicas
- Redis Cluster
- Monitoring (Prometheus + Grafana)

**Milestone:** 1,000 concurrent users, 10k+ MAU

---

### Phase 4: High Scalability (Tháng 6-8)

**Mục tiêu:** Scale lên 50k+ MAU

**Thêm vào:**
- ✅ Auto-scaling (HPA)
- ✅ Database sharding
- ✅ Global CDN
- ✅ Multi-region deployment
- ✅ Advanced caching strategies
- ✅ ML-based recommendation feed
- ✅ Advanced anti-cheat

**Technical:**
- Kubernetes with auto-scaling
- Database sharding theo user_id
- Redis Cluster multi-node
- Service mesh (Istio)
- A/B testing framework

**Milestone:** 50k MAU, 10M VND/tháng doanh thu

---

## 🛠️ Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| Frontend | Next.js 14, TailwindCSS, React Query |
| API Gateway | Spring Cloud Gateway |
| Identity Provider | Keycloak |
| Services | Spring Boot, Node.js |
| Database | PostgreSQL 15, Redis 7 |
| Message Queue | Apache Kafka |
| Search | Elasticsearch |
| Storage | MinIO (dev), AWS S3 (prod) |
| CDN | Cloudflare |
| Realtime | WebSocket, Redis Pub/Sub |
| Monitoring | Prometheus, Grafana, Loki |
| DevOps | Docker, Kubernetes, GitHub Actions |
| Analytics | TimescaleDB/ClickHouse |

---

## 🔐 Security Checklist

- [ ] HTTPS everywhere
- [x] Keycloak JWT validation
- [ ] HttpOnly cookies cho refresh token
- [ ] Rate limiting tất cả endpoints
- [ ] Input validation & sanitization
- [ ] SQL injection prevention
- [ ] XSS protection
- [ ] CSRF tokens
- [ ] Content Security Policy
- [ ] Image upload validation (type, size, virus scan)
- [ ] DDoS protection (Cloudflare)
- [ ] Secrets management (Vault)
- [ ] Regular security audits

---

## 📈 Monitoring & Alerting

**Metrics:**
- Request rate, latency, error rate (RED method)
- CPU, Memory, Disk usage
- Database connections, slow queries
- Cache hit/miss ratio
- Queue depth (Kafka)

**Alerts:**
- Error rate > 1%
- P95 latency > 500ms
- 5xx errors spike
- Database connection pool exhausted
- Disk usage > 80%

**Tools:**
- Prometheus + Grafana
- PagerDuty/Opsgenie cho alerting
- ELK stack cho logging
- Jaeger cho distributed tracing

---

## 💡 Best Practices

1. **API Design**: RESTful + versioning, consistent error responses
2. **Database**: Indexing strategy, connection pooling, migrations
3. **Caching**: Cache-aside pattern, proper invalidation
4. **Async**: Event-driven cho operations không đồng bộ
5. **Testing**: Unit > Integration > E2E, TDD cho critical paths
6. **Documentation**: OpenAPI/Swagger, Architecture Decision Records
7. **CI/CD**: Automated testing, blue-green deployment
8. **Observability**: Distributed tracing, structured logging

---

*Document version: 1.0*  
*Last updated: 2024*  
*Owner: MemeVerse Team*
