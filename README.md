# dib-frontend

DIB Android 앱 저장소입니다. Kotlin과 Jetpack Compose를 사용하며 최소 지원 버전은 Android 8.0(API 26)입니다.

## 개발 환경

- Android Studio
- JDK 17(Android Studio의 JBR 사용 가능)
- Kotlin + Jetpack Compose
- Package: `com.ssafy.dib`
- 권장 에뮬레이터: Pixel 8 / Android 15 / API 35 / Google Play x86_64

## 처음 실행하기

1. 저장소를 clone하고 `develop` 브랜치로 이동합니다.

```bash
git clone https://github.com/dib-B101/dib-frontend.git
cd dib-frontend
git switch develop
```

2. Android Studio에서 저장소 루트를 열고 Gradle Sync를 완료합니다.
3. 에뮬레이터 또는 Android 실기기에서 `app` 구성을 실행합니다.

서버 주소를 설정하지 않아도 공개 화면의 샘플 콘텐츠를 확인할 수 있습니다. 로그인, 입찰, 거래, 마이페이지 등 실제 서버 데이터가 필요한 기능을 검증하려면 아래 로컬 설정이 필요합니다.

## 서버 연결 설정

실제 서버 주소는 저장소에 커밋하지 않습니다. 사용자 Gradle 설정 파일 `%USERPROFILE%\.gradle\gradle.properties`에 다음 값을 추가합니다.

```properties
DIB_API_BASE_URL=https://your-api-host
DIB_WS_URL=wss://your-websocket-host
DIB_TOSS_CLIENT_KEY=test_ck_your_toss_billing_client_key
DIB_SESSION_IDLE_TIMEOUT_MINUTES=30
```

- `DIB_API_BASE_URL`: REST API 호스트입니다. 앱이 `/api/v1/...` 경로를 붙여 요청하므로 경로 없이 호스트까지만 입력합니다.
- `DIB_WS_URL`: 백엔드 WebSocket 명세에서 사용하는 접속 URL을 입력합니다.
- `DIB_TOSS_CLIENT_KEY`: 백엔드 `TOSS_SECRET_KEY`와 짝이 맞는 토스페이먼츠 자동결제(빌링) 공개 클라이언트 키입니다. 카드 등록 화면에서만 사용하며 시크릿 키는 앱에 넣지 않습니다.
- `DIB_SESSION_IDLE_TIMEOUT_MINUTES`: 사용자 입력이 없을 때 자동 로그아웃할 시간입니다. 생략하면 30분입니다.

명령줄에서 일회성으로 실행할 때는 같은 값을 Gradle `-P` 옵션으로 전달할 수 있습니다.

```powershell
.\gradlew.bat :app:assembleDebug `
  -PDIB_API_BASE_URL=https://your-api-host `
  -PDIB_WS_URL=wss://your-websocket-host
```

값을 바꾼 뒤에는 Gradle Sync 또는 앱 재빌드가 필요합니다. 실제 팀 개발 서버 주소는 팀 내부 환경 설정을 사용하세요.

## 앱 구조

기준 경로는 `app/src/main/java/com/ssafy/dib`입니다.

```text
com.ssafy.dib/
├── core/
│   ├── navigation/  # 화면 경로와 NavHost
│   ├── network/     # HTTP 공통 처리와 서버 설정
│   ├── session/     # 인증 세션 만료 처리
│   └── ui/          # 공통 Compose UI
├── data/
│   ├── local/       # 암호화 세션과 기기 식별자 저장
│   ├── remote/      # REST/WebSocket 계약과 데이터 소스
│   └── repository/  # domain Repository 구현
├── domain/          # 도메인 모델과 Repository 계약
├── feature/
│   ├── auth/        # 가입·로그인·계정 복구
│   ├── auction/     # 상품 상세·입찰·경매 등록
│   ├── feed/        # 일반 및 Live 피드
│   ├── home/        # 홈·검색·카테고리
│   ├── live/        # Live 방송 관리
│   └── main/        # 거래·마이페이지와 하위 화면
├── ui/theme/        # 색상·타이포그래피·Compose 테마
└── MainActivity.kt
```

앱은 `MainActivity → AppNavHost → Splash` 순서로 시작합니다. Splash에서 저장된 인증 세션을 확인한 뒤 Welcome 또는 Home으로 이동합니다. 화면 경로는 `core/navigation/Screen.kt`에서 관리합니다.

## 검증

PR을 올리기 전에 저장소 루트에서 다음 명령을 실행합니다.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

로컬에 필요한 의존성이 이미 있으면 `--offline`을 추가할 수 있습니다.

## Git 정책

이 저장소는 `dib-orchestration`에 정의된 중앙 Git Flow 정책을 따릅니다. 정책은 참고하되 Android 작업과 문서 변경은 이 `dib-frontend` 저장소에서만 수행합니다.

1. 최신 `develop`에서 작업 브랜치를 생성합니다.
2. 브랜치 이름은 `feature/...`, `fix/...`, `refactor/...`, `docs/...` 중 변경 목적에 맞는 접두사를 사용합니다.
3. Conventional Commit 형식으로 커밋합니다. 타입은 영문, 설명은 한글로 작성합니다.
4. `develop`을 대상으로 PR을 만들고 필수 검사를 통과시킵니다.
5. PR은 squash merge하고 원격 작업 브랜치를 삭제합니다.

## 커밋하지 않는 로컬 파일

```text
.gradle/
.kotlin/
local.properties
/build/
```
