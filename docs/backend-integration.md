# Android 백엔드 연동 상태

이 문서는 REST API, WebSocket 이벤트, Kafka 이벤트와 공통 오류 코드 명세를 Android 앱에 적용한 현재 상태를 기록한다. 기준 브랜치는 `develop`이다.

## 실행 설정

REST와 WebSocket 주소는 저장소에 커밋하지 않고 Gradle property로 주입한다.

```properties
DIB_API_BASE_URL=https://assigned-api-host
DIB_WS_URL=wss://assigned-api-host/ws
DIB_SESSION_IDLE_TIMEOUT_MINUTES=30
```

주소가 없는 개발 빌드는 공개 화면 확인용 샘플 데이터를 사용한다. 주소가 설정된 빌드는 서버 오류를 샘플 데이터로 숨기지 않고 로딩·빈 상태·재시도를 표시한다.
비활동 세션 만료 시간은 기본 30분이며, 서버 정책이 확정되면 `DIB_SESSION_IDLE_TIMEOUT_MINUTES`로 맞춘다.

## 연결 완료

- 인증: 휴대전화 인증, 이메일 확인, 회원가입, 로그인, 비밀번호 재설정 링크·변경, 토큰 재발급, 비활동 자동 로그아웃, 로그아웃, Keystore 암호화 저장
- 회원: 내 정보 조회·닉네임 수정·탈퇴 제한 확인과 탈퇴
- 상품: 카테고리, 등록·수정·삭제, 내 상품 cursor 목록, 상품 검색 cursor 목록
- 경매: 추천, 검색·필터·카테고리 cursor 목록, 상세, 찜과 찜 cursor 목록, 내 입찰·공개 입찰 이력
- 입찰: WebSocket 구독·복구·입찰 ACK와 마감 연장
- Live: 피드 cursor, 상세, 댓글 과거 내역, 채팅, 신고, 내 방송 cursor 목록, 예약·수정·편성·송출 준비·시작·종료, LiveKit 송출과 피드 자동 시청
- 거래: 구매·판매 cursor 목록, 주문 상세, 낙찰 자동결제·실패 결제 재시도, 송장 등록·배송 조회·구매 확정
- 채팅: 주문·Live 채널 연결, 이전 메시지 페이지네이션, commandId ACK 추적과 재연결 재전송
- 알림: WebSocket 실시간 수신, 중복 제거, 인앱 배너와 관련 화면 이동
- 마이: 문의·신고·정산 cursor 목록과 상세, 정산 계좌, 배송지 조회·수정·삭제
- 배송지 등록: `POST /api/v1/members/me/addresses` DTO와 repository 계약까지 구현

REST 요청은 Access Token이 있으면 `Authorization: Bearer`를, 비회원 세션이면 `X-Guest-Session-Id`를 사용한다. 인증된 요청이 401을 받으면 Refresh rotation을 한 번 수행하고 새 Access Token으로 원래 요청을 한 번만 재시도한다. 동시에 여러 요청이 만료되어도 먼저 갱신된 토큰을 재사용한다. 공통 오류의 목표 계약과 축약 계약을 모두 해석하며, 변경 요청 자동 재시도는 `Idempotency-Key`가 있을 때만 허용한다. 추가 목록 조회에서 `INVALID_CURSOR`를 받으면 기존 목록을 첫 페이지 응답으로 교체해 만료되거나 변조된 커서를 복구한다.

## 확정이 필요한 외부 계약

| 항목 | 현재 상태 | 필요한 결정 |
|---|---|---|
| 배송지 신규 등록 UI | POST 계약 준비 완료, 화면 비활성 | `apiAddressId`를 발급하는 주소 검색 API·SDK·키와 결과 스키마 |
| 결제수단 기본 지정 | 단일 Toss 자동결제 카드 등록·조회·삭제 구현 | 복수 카드와 기본수단 지정 정책이 필요한지 |
| 입찰 사전 조건 | 보증금 없이 WebSocket `PLACE_BID`에 amount 전송 | 배송지를 주문 생성 시 어떤 방식으로 선택·스냅샷하는지 |
| 비밀번호 재설정 링크 진입 | 변경 API·토큰 입력 화면 구현 | 이메일 링크의 Android scheme/host/path와 token query 이름 |
| 차순위 구매 제안 | endpoint 없음 | 제안 목록·상세·수락·거절 API와 만료 시각 |
| 유사 상품 이동 | API가 ProductCard만 반환 | 활성 경매의 `auctionId` 포함 또는 productId→auctionId 조회 계약 |
| 알림 내역 | WebSocket 수신만 구현 | REST 알림 목록·읽음 처리·미수신 복구 endpoint |
| 판매자 공개 활동 | 상품 상세의 판매자 요약만 표시 | 판매자 공개 프로필·후기·판매 경매 목록 endpoint와 페이지네이션 계약 |
| LiveKit 장애 복구 | 송출·시청 연결과 수동 재연결 구현 | 토큰 만료, 네트워크 전환, 판매자 카메라 재시작 시 자동 복구 규칙 |

고정 카드 ID나 임의 `apiAddressId`처럼 서버에 존재하지 않는 값을 실제 데이터처럼 전송하지 않는다. 외부 계약이 확정되면 위 표의 항목별 브랜치에서 UI와 end-to-end 동작을 연결한다.

## 실시간 경계

Android는 Kafka broker에 직접 연결하지 않는다. 입찰·채팅 Command는 WebSocket으로 보내고 서버가 Kafka와 Redis를 거쳐 전달한 결과 이벤트만 처리한다. WebSocket은 `{eventType,eventId|commandId,occurredAt,payload}` envelope를 사용하며, 서버가 안내한 주기로 PING하고 eventId 중복을 제거한다. PONG이 3회 연속 누락되거나 heartbeat 전송에 실패하면 연결을 종료하고 지수 backoff로 재연결하며, `AUCTION_SNAPSHOT`과 `LIVE_SNAPSHOT`을 서버 시간 기준으로 적용해 상태를 복구한다.

## 서버 환경에서 남은 검증

1. 개발 REST·WebSocket 주소와 테스트 계정으로 인증 smoke test
2. 실제 cursor 값으로 목록 끝·빈 페이지·중복 항목 검증
3. 동시 입찰, ACK 유실, 재연결과 종료 직전 연장 검증
4. 낙찰 직후 Toss 자동결제와 실패 후 주문 결제 재시도·중복 승인 방지 검증
5. 송장 등록부터 배송 완료·구매 확정·정산까지 주문 생명주기 검증
