# 홈 Full Scroll·상품 상세 UI 구현

## 디자인 원본

- [01_Wireframe / Full Scroll Views / 01_Home_Full](https://www.figma.com/design/cPD4EKaoKutovkJdDtW5w9/Wireframe?node-id=53-50)
- [02_UI Design / 03_Product_Detail](https://www.figma.com/design/cPD4EKaoKutovkJdDtW5w9/Wireframe?node-id=814-250)
- [02_UI Design / 03A_Bid_Bottom_Sheet](https://www.figma.com/design/cPD4EKaoKutovkJdDtW5w9/Wireframe?node-id=814-251)
- [00_Components](https://www.figma.com/design/cPD4EKaoKutovkJdDtW5w9/Wireframe?node-id=434-123)

홈 화면은 360×1332 크기의 `01_Home_Full`을 기준으로 합니다. 추천 경매, 마감 임박, 인기 경매, 전체 경매 순서를 Compose 스크롤 콘텐츠로 구성합니다. Figma는 화면 구성, 색상, 간격, 컴포넌트와 상태의 기준으로 사용합니다.

## 코드 대응

| 디자인과 기능 | 코드 |
| --- | --- |
| 홈, 검색, 네 개 경매 섹션, 스크롤 | `feature/home/HomeScreen.kt` |
| 예시 경매 데이터와 남은 시간 | `feature/home/HomeAuction.kt` |
| 상품 상세, 공유, 입찰 바텀시트 | `feature/auction/ProductDetailScreen.kt` |
| 홈 → 상품 상세 Navigation | `core/navigation/AppNavHost.kt`, `core/navigation/Screen.kt` |
| 메인 하단 탭과 활성 상태 | `core/ui/DibBottomNavigation.kt` |
| 찜 기본·선택 상태 | `core/ui/DibWishlistButton.kt` |
| UI 색상 토큰 | `ui/theme/WireframeColors.kt` |

경로 기준은 `app/src/main/java/com/ssafy/dib`입니다.

## 현재 동작

- 홈 본문과 상품 상세 본문은 세로로 스크롤됩니다. 홈 헤더·검색·하단 탭과 상세 화면 상단 바·입찰 버튼은 고정됩니다.
- 홈에서 카드형·목록형 보기를 전환할 수 있고, 검색 화면에서 키워드·카테고리·가격·경매 상태를 조합해 조회합니다.
- 로그인 회원의 찜 버튼은 서버 상태와 연결되며 홈·피드·상세에서 같은 하트 아이콘과 선택 상태를 사용합니다.
- 남은 시간이 매초 줄어듭니다.
- 상품 카드를 누르면 상품 상세로 이동하고 뒤로 가기와 Android 공유 화면이 동작합니다.
- 입찰 버튼은 바텀시트를 열어 현재가보다 큰 금액을 입력하고 WebSocket으로 바로 요청합니다. 낙찰되면 등록된 Toss 자동결제 카드로 낙찰가 전액 결제를 요청합니다.
- 피드·등록·내 거래·마이 하단 탭과 알림 화면이 실제 화면 및 서버 계약에 연결됩니다. 인증이 필요한 탭은 로그인 화면으로 이동합니다.
- 앱 실행 중 받은 실시간 알림은 인앱 배너에 표시되며 경매·Live·거래 화면으로 이동할 수 있습니다.

## 데이터 연결

서버 URL이 설정된 빌드는 상품·경매·찜·입찰·거래 데이터를 REST와 WebSocket으로 조회하고 변경합니다. 서버 URL이 없는 개발 빌드만 화면 확인용 예시 데이터를 표시합니다. 결제수단·배송지 스냅샷과 PG 복귀처럼 서버 또는 외부 사업자 계약이 필요한 범위는 [backend-integration.md](backend-integration.md)에 기록합니다.

## 에셋

`app/src/main/res/drawable-xxhdpi`의 PNG는 2026-09-08에 Figma 원본에서 3배 크기로 내보냈습니다. 앱은 원격 임시 URL에 의존하지 않습니다.

## 확인 항목

1. 홈에서 세로 스크롤, 카드·목록 전환, 검색, 카테고리 필터, 찜 선택과 남은 시간 변화를 확인합니다.
2. 상품 카드를 눌러 상세로 이동하고 상세 본문을 스크롤합니다.
3. 전체 이미지 보기, 공유 버튼과 뒤로 가기를 확인합니다.
4. 입찰창에서 현재가 초과 검사를 확인하고, 일반·Live 입찰이 별도 결제 화면 없이 실시간 요청되는지 확인합니다.
5. 알림 배너와 하단 탭이 대상 화면으로 이동하며 인증 필요 화면은 로그인을 요구하는지 확인합니다.
