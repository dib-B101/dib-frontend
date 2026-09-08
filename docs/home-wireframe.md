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
- 검색어에 따라 전체 상품이 필터링되며 결과가 없으면 초기화할 수 있습니다.
- 찜 버튼은 선택·해제할 수 있고 화면 재생성에도 로컬 상태가 유지됩니다.
- 남은 시간이 매초 줄어듭니다.
- 상품 카드를 누르면 상품 상세로 이동하고 뒤로 가기와 Android 공유 화면이 동작합니다.
- 입찰 버튼은 바텀시트를 열며 숫자 입력, 최소 금액, 500원 단위 검증과 금액 증가 버튼이 동작합니다.
- 아직 구현하지 않은 알림과 다른 하단 탭은 탭했을 때 준비 중 안내를 표시합니다.

## 연동 전 범위

상품과 경매 값은 현재 예시 데이터이며 서버에서 조회하지 않습니다. 찜과 입찰 값도 서버에 저장되지 않습니다. `보증금 결제로 계속`은 결제 API가 연결되기 전까지 다음 단계 안내만 표시합니다. 피드·등록·거래·마이 화면은 각 화면 구현 단계에서 Navigation에 추가합니다.

## 에셋

`app/src/main/res/drawable-xxhdpi`의 PNG는 2026-09-08에 Figma 원본에서 3배 크기로 내보냈습니다. 앱은 원격 임시 URL에 의존하지 않습니다.

## 확인 항목

1. 홈에서 세로 스크롤, 검색, 카테고리 필터, 찜 선택과 남은 시간 변화를 확인합니다.
2. 상품 카드를 눌러 상세로 이동하고 상세 본문을 스크롤합니다.
3. 공유 버튼과 뒤로 가기를 확인합니다.
4. 입찰창에서 금액을 직접 입력하고 500원·1,000원·5,000원 증가 버튼과 유효성 검사를 확인합니다.
5. 알림과 미구현 하단 탭에서 준비 중 안내가 표시되는지 확인합니다.
