# 알림 · 인증 · 회원 · 분쟁 — 김경탁

> **담당 범위**  
> 메인: `notification` / 서브: `auth`, `user`, `dispute`  
> 핵심 클래스: `NotificationService`, `NotificationOutboxProcessor`, `NotificationEventListener`, `AuthService`, `OAuthService`, `UserService`, `DisputeService`

---

## 1. 어떤 방식으로 생각했는가

플랫폼의 **기반 인프라**를 담당했습니다. 사용자가 서비스에 들어오고(인증), 문제가 생기면 신고하고(분쟁), 모든 이벤트를 알림으로 받습니다(알림).

### 인증·회원

```
이메일 가입: OTP 이메일 인증(Redis) → 회원가입 → JWT 발급
소셜 로그인: OAuth state(Redis) → 콜백 → JWT 발급
토큰 관리: Access(JWT) + Refresh(Redis, 14일) → HttpOnly Cookie + Bearer
```

### 알림

도메인 이벤트(결제 완료, 마일스톤 승인, 분쟁 접수 등)가 10개 이상 모듈에서 발생합니다. 각 모듈이 직접 알림을내면 **트랜잭션 실패 시 알림만 발송**되는 문제가 생깁니다.

그래서 **Transactional Outbox 패턴**을 적용했습니다.

```
비즈니스 TX 안: NotificationEvent 발행 → Outbox INSERT (BEFORE_COMMIT)
비즈니스 TX 커밋 후: NotificationOutboxScheduler (10초) → Outbox 처리 → Notification INSERT
프론트: SSE로 실시간 알림 수신
```

### 분쟁

```
신고 접수 → 관리자 알림(Outbox) → 관리자 판정 → DisputeResolvedEvent → 정산/환불 연동
이의신청 → 증거 파일(S3) → 관리자 재심사
```

---

## 2. 이 방법을 선택한 이유

| 대안 | 채택하지 않은 이유 | 선택한 방식 |
|------|-------------------|------------|
| 알림을 비즈니스 TX에서 직접 INSERT | TX 롤백 시 알림만 남음 | Outbox 패턴 |
| @Async로 알림 발송 | TX 커밋 전 발송 가능 | BEFORE_COMMIT Outbox 저장 |
| Session 기반 인증 | 서버 스케일아웃 어려움 | Stateless JWT |
| Refresh Token을 DB에 저장 | 조회 부하 | Redis TTL 저장 |
| 분쟁을 아이디어 도메인에 포함 | 관심사 혼합 | dispute 독립 패키지 |
| OTP를 DB에 저장 | 만료 관리·조회 부하 | Redis TTL 5분 |

Outbox의 핵심 이점: **비즈니스 데이터와 알림 의도가 같은 트랜잭션에서 커밋**되므로, 롤백 시 알림도 함께 사라집니다.

---

## 3. 이 기술을 고민한 이유

| 기술 | 고민 배경 |
|------|----------|
| **Redis** | OTP·Refresh Token·OAuth state — 빠른 TTL 기반 임시 데이터 |
| **JWT (jjwt)** | Stateless 인증, 다중 인스턴스 대응 |
| **HttpOnly Cookie + Bearer** | XSS 방지(쿠키) + API 클라이언트 호환(헤더) |
| **Transactional Outbox** | 알림 유실·유령 알림 방지 |
| **REQUIRES_NEW (Outbox 처리)** | 개별 알림 실패가 배치 전체를 롤백하지 않음 |
| **SSE (SseEmitterStorage)** | 폴링 없이 실시간 알림 전달 |
| **DisputeResolvedEvent** | 분쟁 판정 → 정산 도메인 느슨한 결합 |

---

## 4. 예상했던 문제

| 예상 문제 | 대비 설계 |
|----------|----------|
| TX 롤백 후 알림 발송 | Outbox BEFORE_COMMIT |
| Outbox 처리 실패 | retryCount 3회 → FAILED |
| Refresh Token 탈취 | Redis 저장 + 로그아웃 시 삭제 |
| OAuth CSRF | state를 Redis에 저장 후 콜백 검증 |
| 중복 분쟁 신고 | 동일 대상 ACTIVE 상태 존재 시 거부 |
| 알림 폭주 | BATCH_SIZE 50, 10초 주기 |

---

## 5. 실제 일어났던 문제

### 5.1 알림 유실 — Outbox 도입 전

초기에는 `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`으로 알림을 직접 INSERT했습니다. 비동기 스레드에서 DB 장애·타임아웃 시 **알림이 유실**되고, 재시도 메커니즘이 없었습니다.

**해결**: Outbox 테이블에 먼저 저장 → 스케줄러가 안정적으로 처리.

### 5.2 Outbox 배치 전체 롤백

초기 Outbox 처리를 단일 `@Transactional`로 묶었더니, 50건 중 1건 실패 시 49건도 롤백되었습니다.

**해결**: `processOne()`에 `@Transactional(propagation = REQUIRES_NEW)` — 건별 독립 트랜잭션.

### 5.3 OAuth state 불일치

Google/Kakao 콜백 시 state 파라미터가 Redis에 없으면 CSRF 또는 만료 요청입니다. 초기에는 state TTL이 없어 오래된 state도 유효했습니다.

**해결**: `OAuthStateRepository`에 TTL 설정.

### 5.4 분쟁 판정 후 정산 연동 누락

관리자가 분쟁을 해결해도 환불·정산이 자동으로 트리거되지 않았습니다.

**해결**: `DisputeResolvedEvent` 발행 → `DisputeSettlementEventHandler`에서 정산 도메인 호출.

### 5.5 자기 자신 신고

`reporterId.equals(reportedUserId)` 검증 누락 시 본인 신고 가능.

### 5.6 관리자 초대 토큰

관리자 가입을 일반 가입과 분리하기 위해 Redis 기반 `AdminInviteRepository`로 1회용 초대 토큰 관리.

---

## 6. 해결 사례

### 사례 1: Transactional Outbox 패턴

```java
// NotificationEventListener — BEFORE_COMMIT
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
public void handleNotificationEvent(NotificationEvent event) {
    outboxRepository.save(NotificationOutbox.forUser(
        event.userId(), event.notificationType(),
        event.title(), event.message(), event.targetId(),
        event.priority()
    ));
}

// NotificationOutboxScheduler — 10초마다
@Scheduled(fixedDelay = 10000)
public void processOutbox() {
    for (Long id : outboxProcessor.findPendingIds()) {
        outboxProcessor.processOne(id);  // REQUIRES_NEW
    }
}
```

### 사례 2: 건별 독립 트랜잭션

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void processOne(Long outboxId) {
    try {
        notificationService.createNotification(...);
        entry.markSent();
    } catch (Exception e) {
        entry.incrementRetryOrFail();  // 3회 후 FAILED
    }
}
```

### 사례 3: JWT + Redis Refresh Token

```java
// 로그인
String accessToken = jwtUtil.generateAccessToken(userId, role);
String refreshToken = jwtUtil.generateRefreshToken(userId);
refreshTokenRepository.save(userId, refreshToken, Duration.ofDays(14));

// 재발급
String stored = refreshTokenRepository.find(userId);
if (!stored.equals(request.refreshToken())) throw ...;
// 새 토큰 쌍 발급
```

### 사례 4: 분쟁 → 정산 이벤트

```java
// DisputeService — 관리자 판정
eventPublisher.publishEvent(new DisputeResolvedEvent(disputeId, resolution));

// DisputeSettlementEventHandler — 정산 도메인 연동
@TransactionalEventListener
public void handle(DisputeResolvedEvent event) { ... }
```

---

## 7. 해결의 이유

| 해결책 | 왜 효과적인가 |
|--------|--------------|
| BEFORE_COMMIT Outbox | 비즈니스 TX와 알림 의도가 원자적으로 커밋 |
| REQUIRES_NEW | 1건 실패가 나머지 알림 처리를 막지 않음 |
| retryCount 3회 | 일시적 DB 장애 흡수, 영구 실패는 FAILED로 격리 |
| Redis TTL | OTP·state·refresh 자동 만료, DB 부하 없음 |
| DisputeResolvedEvent | 분쟁 도메인이 정산 구현을 몰라도 연동 가능 |
| participantValidator | 신고 대상·피신고자 관계 검증 중앙화 |

알림 도메인의 핵심은 **"보내야 할 알림을 반드시 기록하고, 실패해도 재시도할 수 있게"** 만드는 것입니다.

---

## 8. 결과

### 구현 규모

| 항목 | 수량 |
|------|------|
| Auth API | 14개 (가입·로그인·OTP·OAuth·관리자 초대) |
| User API | 9개 (프로필·비밀번호·탈퇴·사업자) |
| Notification API | 4개 + SSE |
| Dispute API | 5개 + Admin 4개 |
| 스케줄러 | NotificationOutboxScheduler (10초), NotificationScheduler (마감 알림) |
| 단위 테스트 | AuthServiceTest, OAuthServiceTest, UserServiceTest, NotificationOutboxProcessorTest, NotificationEventListenerTest, DisputeServiceTest |

### Outbox 처리 흐름

```
이벤트 발생 → Outbox(PENDING) → Scheduler → Notification INSERT → Outbox(SENT)
                                    ↓ 실패
                              retryCount++ → 3회 초과 → FAILED
```

### 담당 API 요약

| 구분 | 엔드포인트 |
|------|-----------|
| 인증 | `POST /auth/signup`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/oauth/{provider}` |
| 회원 | `GET /users/me`, `PATCH /users/me/profile` |
| 알림 | `GET /notifications`, `GET /notifications/stream` (SSE) |
| 분쟁 | `POST /disputes`, `POST /disputes/{id}/appeals`, `PATCH /admin/disputes/{id}` |

### 한 줄 요약

> Transactional Outbox 패턴으로 **알림 유실·유령 알림을 방지**하고, JWT+Redis 인증·OAuth·분쟁-정산 이벤트 연동으로 플랫폼 **기반 인프라의 안정성**을 확보했습니다.

### 관련 파일

| 구분 | 경로 |
|------|------|
| Outbox 처리 | `backend/src/main/java/com/team04/domain/notification/service/NotificationOutboxProcessor.java` |
| 이벤트 리스너 | `backend/src/main/java/com/team04/domain/notification/service/NotificationEventListener.java` |
| 인증 서비스 | `backend/src/main/java/com/team04/domain/auth/service/AuthService.java` |
| 분쟁 서비스 | `backend/src/main/java/com/team04/domain/dispute/service/DisputeService.java` |
| Outbox 스케줄러 | `backend/src/main/java/com/team04/infra/batch/NotificationOutboxScheduler.java` |
| JWT 필터 | `backend/src/main/java/com/team04/global/security/JwtFilter.java` |
