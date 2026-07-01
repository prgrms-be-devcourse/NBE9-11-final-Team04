# 전문가 · 관리자 · 매칭 — 김민혁

> **담당 범위**  
> 메인: `expert` / 서브: `admin`, `match`  
> 핵심 클래스: `ExpertVerifyService`, `ExpertVerificationScheduler`, `ExpertAppealService`, `AdminExpertService`, `ExpertMatchService`, `ExpertReviewService`, `AdminDashboardService`

---

## 1. 어떤 방식으로 생각했는가

아이디어 AI 검증만으로는 **전문성**을 판단하기 어렵습니다. 실제 사업자·자격을 갖춘 전문가가 프로젝트를 리뷰하면 후원자 신뢰가 올라갑니다.

전문가 도메인은 세 가지 축으로 설계했습니다.

```
[인증]  사업자등록 / 국가자격 증명 → 국세청 API or 관리자 수동 심사
[매칭]  USER가 전문가에게 리뷰 요청 → EXPERT 수락/거절
[운영]  관리자가 전문가·아이디어·마일스톤·정산·분쟁을 통합 관리
```

관리자 API는 `/admin/**`로 분리하고, `@PreAuthorize("hasRole('ADMIN')")`로 보호합니다. 전문가 재검증은 `ExpertVerificationScheduler`가 주기적으로 수행합니다.

```
전문가 신청 → NTS API 자동 검증 (사업자)
           → 파일 업로드 + 관리자 심사 (국가자격)
           → EXPERT 역할 부여

매칭 요청 → EXPERT 수락 → ProjectVerification 상태 EXPERT_MATCHING
         → 리뷰 작성 → 후원자 의사결정 참고
```

---

## 2. 이 방법을 선택한 이유

| 대안 | 채택하지 않은 이유 | 선택한 방식 |
|------|-------------------|------------|
| 전문가 자가 인증 (서류만 업로드) | 위조 가능 | 국세청 API 실시간 검증 |
| 전문가 자동 배정 | 전문가 선택권 없음 | USER가 요청 → EXPERT가 수락/거절 |
| 관리자 기능을 각 도메인에 분산 | API 일관성·권한 관리 어려움 | `admin` 패키지로 통합 |
| 국가자격도 API 자동 검증 | 공공 API 없음 | 파일 업로드 + 관리자 수동 심사 |
| API 장애 시 신청 거부 | 사용자 이탈 | 보류(PENDING) 상태로 저장 후 관리자 처리 |

국세청 API(`ExternalVerifyClient`)가 장애일 때 **거부가 아닌 보류**로 처리한 것이 핵심 UX 결정입니다.

---

## 3. 이 기술을 고민한 이유

| 기술 | 고민 배경 |
|------|----------|
| **국세청 odcloud API** | 사업자등록번호 실시간 유효성 검증 |
| **@Profile("!local")` Mock** | 로컬 개발 시 외부 API 없이 테스트 |
| **S3 AppealStorageClient** | 국가자격 증명서·이의신청 파일 안전 저장 |
| **ExpertVerificationScheduler** | 사업자 재검증·격리 만료·SPONSOR 강등 자동화 |
| **ExpertMatch ↔ Verification 연동** | 수락 시 `EXPERT_MATCHING` 상태 전이로 검증 파이프라인 통합 |
| **AdminDashboardService** | 운영 지표(대기 심사·분쟁·정산) 한 화면 집계 |

---

## 4. 예상했던 문제

| 예상 문제 | 대비 설계 |
|----------|----------|
| 국세청 API 장애 | `EXTERNAL_API_FAILURE` → PENDING 보류 |
| 중복 전문가 신청 | `verified=true`이면 거부, 미검증이면 기존 삭제 후 재등록 |
| 미인증 전문가 매칭 | `ExpertStatus.ACTIVE` + `isVerified()` 검증 |
| 전문가 자격 만료 | 스케줄러 주기적 재검증 |
| 관리자 API 오남용 | Role.ADMIN + SecurityConfig `/admin/**` |
| 매칭 거절 사유 누락 | REJECTED 시 rejectReason 필수 검증 |

---

## 5. 실제 일어났던 문제

### 5.1 국세청 API 장애 시 사용자 경험

초기에는 API 실패 = 신청 거부(`EXPERT_NOT_VERIFIED`)였습니다. 일시적 장애에도 전문가 신청이 불가해 CS 문의가 발생했습니다.

**해결**: `EXTERNAL_API_FAILURE` catch → `ExpertProfile.ofPending()` 보류 저장.

### 5.2 국가자격 vs 사업자등록 처리 분기

두 자격 유형의 검증 방식이 다릅니다.

- **사업자등록**: NTS API 즉시 검증 → 성공 시 바로 `verified`
- **국가자격**: 파일 필수 → 업로드 후 `PENDING` → 관리자 승인

초기에 분기 없이 동일 플로우를 적용해 국가자격 신청 시 API 호출이 불필요하게 발생했습니다.

### 5.3 매칭 수락과 검증 상태 불일치

전문가가 매칭을 수락해도 `ProjectVerification` 상태가 갱신되지 않아, 프론트에서 "전문가 심사 대기" UI가 표시되지 않았습니다.

**해결**: `ExpertMatchService.respond()`에서 ACCEPTED 시 `verification.changeStatus(EXPERT_MATCHING)`.

### 5.4 관리자 API 역할 분리

초기에는 일반 API에 관리자 기능이 섞여 `@PreAuthorize` 누락 사례가 있었습니다.

**해결**: `AdminIdeaController`, `AdminExpertController` 등 7개 컨트롤러로 분리, SecurityConfig에서 `/admin/**` 일괄 ADMIN 역할.

### 5.5 전문가 격리(SUSPENDED) 후 SPONSOR 강등

격리 기간 만료 후에도 EXPERT 역할이 유지되면 제한된 사용자가 매칭 API를 호출할 수 있었습니다.

**해결**: `ExpertVerificationScheduler`에서 격리 만료 시 SPONSOR(일반 후원자) 역할로 강등.

---

## 6. 해결 사례

### 사례 1: 국세청 API 장애 → 보류 처리

```java
catch (CustomException e) {
    if (e.getErrorCode() == ErrorCode.EXTERNAL_API_FAILURE) {
        ExpertProfile pending = ExpertProfile.ofPending(user, ...);
        expertProfileRepository.save(pending);
        return ExpertVerifyResponse.from(pending);
    }
    throw e;
}
```

거부 대신 보류 → 관리자가 나중에 수동 검증.

### 사례 2: 자격 유형별 분기

```java
if (qualificationType == NATIONAL_QUALIFICATION) {
    String fileKey = appealStorageClient.upload(file);
    ExpertProfile pending = ExpertProfile.ofPending(...);
    return ExpertVerifyResponse.from(pending);
}
boolean verified = externalVerifyClient.verify(request, true);
if (!verified) throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
```

### 사례 3: 매칭-검증 상태 연동

```java
if (request.status() == MatchStatus.ACCEPTED) {
    match.accept();
    projectVerificationRepository.findByIdeaId(match.getIdeaId())
        .ifPresent(v -> v.changeStatus(VerificationStatus.EXPERT_MATCHING));
}
```

### 사례 4: 관리자 대시보드 집계

`AdminDashboardService`에서 대기 중인 아이디어 심사·전문가 심사·분쟁·마일스톤 보고서 건수를 한 API로 반환.

---

## 7. 해결의 이유

| 해결책 | 왜 효과적인가 |
|--------|--------------|
| PENDING 보류 | 외부 API 장애를 사용자 거부로 전환하지 않음 |
| 자격 유형 분기 | 검증 방식이 다른 두 유형을 올바른 플로우로 처리 |
| 매칭-검증 연동 | 도메인 간 상태 불일치 제거, UI 정확성 확보 |
| admin 패키지 분리 | 권한 관리 일원화, 보안 누락 방지 |
| 스케줄러 재검증 | 일회성 인증의 한계 보완, 지속적 신뢰 유지 |

전문가 도메인의 핵심은 **외부 API에 의존하되, 장애 시에도 서비스가 멈추지 않게** 만드는 것입니다.

---

## 8. 결과

### 구현 규모

| 항목 | 수량 |
|------|------|
| Expert API | 7개 |
| Match API | 5개 |
| Admin API | 7개 컨트롤러 (대시보드·사용자·아이디어·전문가·마일스톤·정산·분쟁) |
| 스케줄러 | ExpertVerificationScheduler |
| 단위 테스트 | ExpertVerifyServiceTest, ExpertAppealServiceTest, ExpertMatchServiceTest, ExpertReviewServiceTest |

### 담당 API 요약

| 구분 | 엔드포인트 |
|------|-----------|
| 전문가 | `POST /experts/verify`, `GET /experts`, `POST /experts/appeals` |
| 매칭 | `POST /matches`, `PATCH /experts/matches/{id}`, `POST /matches/{id}/reviews` |
| 관리자 | `GET /admin/dashboard`, `PATCH /admin/experts/{id}/approve` |

### 한 줄 요약

> 국세청 API 기반 전문가 자동 인증 + 장애 시 보류 처리 + 매칭-검증 상태 연동 + 관리자 통합 운영 API로 **전문가 신뢰 체계와 플랫폼 운영 기반**을 구축했습니다.

### 관련 파일

| 구분 | 경로 |
|------|------|
| 전문가 인증 | `backend/src/main/java/com/team04/domain/expert/service/ExpertVerifyService.java` |
| 매칭 서비스 | `backend/src/main/java/com/team04/domain/match/service/ExpertMatchService.java` |
| 관리자 대시보드 | `backend/src/main/java/com/team04/domain/admin/service/AdminDashboardService.java` |
| 재검증 스케줄러 | `backend/src/main/java/com/team04/domain/expert/scheduler/ExpertVerificationScheduler.java` |
