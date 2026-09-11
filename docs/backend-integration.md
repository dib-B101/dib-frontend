# Backend integration foundation

이 문서는 REST API, WebSocket 이벤트, Kafka 이벤트, 공통 오류 코드 명세를 Android 앱에 적용하는 경계를 기록한다.

## 현재 적용 범위

- REST와 WebSocket 주소를 각각 `DIB_API_BASE_URL`, `DIB_WS_URL` Gradle property로 주입한다.
- REST 요청은 Access Token이 있으면 `Authorization: Bearer`를, 비회원 세션이면 `X-Guest-Session-Id`를 사용한다.
- 공통 오류의 목표 계약 `{timestamp,status,code,message,path,traceId,fieldErrors?}`와 현재 축약 계약 `{code,message}`를 모두 해석한다.
- POST, PATCH, DELETE 자동 재시도는 `Idempotency-Key`가 있을 때만 허용한다.
- 인증의 휴대전화 인증, 이메일 확인, 회원가입, 로그인, 토큰 재발급, 로그아웃 계약을 DTO와 RemoteDataSource로 정의했다.
- 로그인 토큰은 Android Keystore의 AES/GCM 키로 암호화해 저장하고, 앱 시작과 Access Token 만료 직전에 Refresh Token rotation을 수행한다.
- 서버 주소가 없는 개발 빌드는 가짜 로그인 상태를 만들지 않으며, 시작 화면의 비회원 둘러보기로 공개 화면을 확인한다.
- 구현 완료 상태인 `GET /api/v1/auctions`와 `GET /api/v1/auctions/{auctionId}`를 홈·상세 화면에 연결했다. 서버 미설정 빌드만 와이어프레임 샘플 데이터를 사용한다.
- 경매 목록·상세는 로딩, 재시도와 공통 오류 메시지를 표시하며 숫자·문자열 ID와 서버 기준 종료 시각을 화면 모델로 변환한다.
- 일반 경매 상세는 WebSocket 경매 채널을 구독하고 스냅샷·최고가 갱신·마감 연장·종료 이벤트를 화면 가격과 타이머에 반영한다.
- 소켓이 끊기면 1~30초 지수 backoff로 재연결하며 마지막 `occurredAt`을 재구독 요청에 포함해 최신 스냅샷을 복구한다.
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
- 최종 확인된 입찰 정책은 상품별 첫 입찰 전에 1,000원 보증금을 결제하고 재입찰에는 추가 결제하지 않는 방식이다. REST 보증금 API와 WebSocket의 `DEPOSIT_REQUIRED` 계약을 기준으로 연결하며, 요구사항 표의 “보증금 없이 입찰” 문구는 정책 문서에서 정정이 필요하다.
- 입찰 Command에는 선택한 `paymentMethodId`와 `addressId`를 직접 보내거나 사전 참여 정보 ID를 보내야 하지만 현재 WebSocket 명세에는 해당 필드가 없다.
- 판매자 본인 경매 입찰 금지는 인증 회원 ID와 경매 판매자 ID를 서버에서 최종 검증하고, Android도 조회 응답의 소유자 정보를 기준으로 입찰 버튼을 비활성화해야 한다.
- 공통 오류 응답은 목표 계약과 현재 서버 구현이 다르므로 서버가 전환되기 전까지 선택 필드로 처리해야 한다.
- 샘플 UI를 실제 서버 데이터로 교체하려면 화면별 ViewModel과 Repository가 추가로 필요하다.

## Kafka 경계

Kafka는 WebSocket Gateway와 백엔드 도메인 서비스 사이의 내부 계약이다. Android 앱은 Kafka broker에 연결하지 않는다. 앱은 입찰 Command를 WebSocket으로 보내고 `BID_ACCEPTED`, `BID_REJECTED`, `HIGHEST_BID_UPDATED` 등의 결과만 WebSocket으로 받는다.

## 다음 연결 순서

1. 개발 API와 WebSocket 호스트, 테스트 계정을 전달받아 인증 API smoke test를 수행한다.
2. 개발 서버 응답으로 경매 목록·상세 DTO 필드와 페이지네이션을 smoke test한다.
3. 개발 WebSocket으로 구독·재연결·이벤트 순서를 smoke test하고 `PLACE_BID`의 참여 정보 계약을 확정한다.
4. 입찰 Command와 `BID_ACCEPTED`·`BID_REJECTED` 결과를 보증금 결제 흐름에 연결한다.
5. 서버의 `구현=FALSE` 항목이 완료되는 순서대로 결제, 거래, 마이 화면의 샘플 상태를 교체한다.
