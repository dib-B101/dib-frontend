# DIB Admin Web

DIB 운영자를 위한 React 관리자 콘솔입니다. 브라우저는 AI 서버를 직접 호출하지 않고, 관리자 JWT로 백엔드 `/api/v1/admin/**` API만 호출합니다.

## 제공 기능

- 관리자 계정 로그인, 토큰 갱신, 로그아웃
- 고객 문의 조회와 답변 등록
- AI 상품 검수 보류·반려 사유 확인과 승인·반려
- 이상 입찰 위험 점수 조회와 대상 회원 제재 연결
- 신고 판정, 환불 처리, 신고와 회원 정지 동시 처리
- 회원 검색, 정지·제명·정지 해제

## 로컬 실행

백엔드를 먼저 `http://localhost:8080`에서 실행한 뒤 다음 명령을 사용합니다.

```powershell
npm install
npm run dev
```

기본 주소는 `http://localhost:5173`입니다. 다른 백엔드 주소를 사용할 때는 `.env.local`에 아래 값을 설정합니다.

```properties
VITE_API_BASE_URL=http://localhost:8080
```

전체 통합 환경은 orchestration 루트에서 실행합니다.

```powershell
docker compose --profile app up -d --build
```

관리자 웹은 `http://localhost:3000`에서 열립니다. 로컬 시드 관리자 계정은 `admin@example.com`, 공통 테스트 비밀번호는 `Test1234!`입니다.

상품 검수는 Compose에서 기본 활성화됩니다. `ANTHROPIC_API_KEY` 또는 `OPENAI_API_KEY`가 없으면 명백한 금지 품목은 규칙으로 자동 반려하고, 정상 후보는 안전하게 관리자 검토 대기 상태로 보냅니다.

## 검증

```powershell
npm run typecheck
npm run build
```
