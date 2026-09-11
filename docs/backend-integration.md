# Backend integration foundation

이 문서는 REST API, WebSocket 이벤트, Kafka 이벤트, 공통 오류 코드 명세를 Android 앱에 적용하는 경계를 기록한다.

## 현재 적용 범위

- REST와 WebSocket 주소를 각각 `DIB_API_BASE_URL`, `DIB_WS_URL` Gradle property로 주입한다.
- REST 요청은 Access Token이 있으면 `Authorization: Bearer`를, 비회원 세션이면 `X-Guest-Session-Id`를 사용한다.
- 공통 오류의 목표 계약 `{timestamp,status,code,message,path,traceId,fieldErrors?}`와 현재 축약 계약 `{code,message}`를 모두 해석한다.
- POST, PATCH, DELETE 자동 재시도는 `Idempotency-Key`가 있을 때만 허용한다.
- 인증의 휴대전화 인증, 이메일 확인, 회원가입, 로그인, 토큰 재발급, 로그아웃 계약을 DTO와 RemoteDataSource로 정의했다.
- WebSocket은 명세의 `{eventType,eventId|commandId,occurredAt,payload}` envelope를 사용한다. 연결 후 서버가 안내한 주기로 PING하고 eventId 중복을 제거한다.
- 경매 구독·동기화·입찰, 주문 구독·채팅, Live 구독·채팅 Command 생성기를 제공한다.

개발자별 URL은 저장소에 커밋하지 않고 사용자 Gradle 설정이나 명령행에 둔다.

```properties
DIB_API_BASE_URL=https://assigned-api-host
DIB_WS_URL=wss://assigned-api-host/ws/v1
```

## 연동을 보류한 이유

- API와 WebSocket의 실행 호스트가 명세에 없다.
- REST 표에서 배송지, 결제수단, 북마크, 입찰 참여 정보, 배송 조회, 정산 계좌 등 화면에 필요한 일부 API가 아직 `구현=FALSE`다.
- 최신 요구사항은 보증금 없는 입찰과 등록 결제수단 자동 결제를 요구한다. 기존 REST의 보증금 API와 WebSocket의 `DEPOSIT_REQUIRED` 계약은 최신 요구사항과 충돌하므로 백엔드 계약을 개정하기 전 Android에서 연결하지 않는다.
- 입찰 Command에는 선택한 `paymentMethodId`와 `addressId`를 직접 보내거나 사전 참여 정보 ID를 보내야 하지만 현재 WebSocket 명세에는 해당 필드가 없다.
- 판매자 본인 경매 입찰 금지는 인증 회원 ID와 경매 판매자 ID를 서버에서 최종 검증하고, Android도 조회 응답의 소유자 정보를 기준으로 입찰 버튼을 비활성화해야 한다.
- 공통 오류 응답은 목표 계약과 현재 서버 구현이 다르므로 서버가 전환되기 전까지 선택 필드로 처리해야 한다.
- Access/Refresh Token의 영구 저장 방식과 기기 식별자 생성 정책이 확정되지 않았다.
- 샘플 UI를 실제 서버 데이터로 교체하려면 화면별 ViewModel과 Repository가 추가로 필요하다.

## Kafka 경계

Kafka는 WebSocket Gateway와 백엔드 도메인 서비스 사이의 내부 계약이다. Android 앱은 Kafka broker에 연결하지 않는다. 앱은 입찰 Command를 WebSocket으로 보내고 `BID_ACCEPTED`, `BID_REJECTED`, `HIGHEST_BID_UPDATED` 등의 결과만 WebSocket으로 받는다.

## 다음 연결 순서

1. 개발 API와 WebSocket 호스트, 테스트 계정을 전달받아 인증 API smoke test를 수행한다.
2. Android Keystore 기반 세션 저장소와 Refresh Token rotation을 구현한다.
3. 로그인 화면을 AuthRepository와 연결한다.
4. 구현 완료된 일반 경매 목록·상세 REST와 경매 WebSocket을 홈·상세 화면에 연결한다.
5. 서버의 `구현=FALSE` 항목이 완료되는 순서대로 결제, 거래, 마이 화면의 샘플 상태를 교체한다.
