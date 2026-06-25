# SeedLink Frontend (Next.js)

신뢰 기반 크라우드펀딩 플랫폼 **SeedLink** 프론트엔드

## 기술 스택

- **Next.js 16** (App Router)
- React 19 + TypeScript
- Tailwind CSS v4
- React Query · Zustand · Axios

## 시작하기

```bash
npm install
npm run dev
```

http://localhost:3000

### 환경 변수

| 파일 | 변수 | 설명 |
|------|------|------|
| `.env` | `NEXT_PUBLIC_API_BASE_URL` | 운영 API (`https://api.seedlink.com`) |
| `.env.development` | `NEXT_PUBLIC_API_BASE_URL=/api` | 로컬 프록시 (→ `localhost:8080`) |

## 주요 라우트

| 경로 | 설명 |
|------|------|
| `/login`, `/signup` | 인증 |
| `/ideas` | 아이디어 목록 |
| `/ideas/[id]` | 아이디어 상세 |
| `/fundings/idea/[id]` | 펀딩 + SSE |
| `/mypage` | 프로필 |
| `/mypage/payments` | 결제 내역 |
| `/mypage/notifications` | 알림 (SSE) |
| `/admin` | 관리자 |

## 폴더 구조

```
src/
├── app/           # Next.js App Router 페이지
├── api/           # Axios API 모듈
├── components/    # UI + Layout
├── hooks/         # SSE, 알림
├── store/         # Zustand
├── types/         # TypeScript 타입
└── utils/
```

## 빌드

```bash
npm run build
npm start
```
