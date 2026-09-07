# dib-frontend

## Git 정책

이 저장소는 dib-orchestration의 중앙 Git Flow 정책을 따릅니다.

## Android 개발 환경

- Language: Kotlin
- UI: Jetpack Compose
- Minimum SDK: 26
- Package Name: `com.ssafy.dib`

### 권장 테스트 환경

- Device: Pixel 8
- Android: 15
- API: 35
- System Image: Google Play x86_64

### 실행 방법

1. 저장소를 clone합니다.

```bash
git clone https://github.com/dib-B101/dib-frontend.git
cd dib-frontend
git switch develop
```

2. Android Studio에서 `dib-frontend` 폴더를 엽니다.
3. Gradle Sync가 완료될 때까지 기다립니다.
4. 아래 중 하나를 준비합니다.
   - Pixel 8 / API 35 에뮬레이터
   - Android 실기기
5. `app` 모듈을 실행합니다.

### 로컬 파일

아래 파일 및 폴더는 Git에 포함하지 않습니다.

```text
.gradle/
.kotlin/
local.properties
/build/
```