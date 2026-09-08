# 01_Wireframe 구현 상태

## 구현 완료

- `01_Home_Full`: 전체 세로 스크롤, 검색, 찜, 카운트다운, 피드/상세 이동
- `02_Auction_Card_Feed`: 세로 상품 전환, 가로 이미지 전환, 상세 이동
- `02A_Feed_First_Entry`: 최초 사용 안내
- `02B_Feed_New_Bid`: 입찰 후 가격·입찰 수 갱신과 새 입찰 피드백
- `02C_Feed_Under_1Min`: 1분 미만 타이머 강조
- `02D_Feed_Extended`: 종료 30초 이내 입찰 시 30초 연장
- `02E_Feed_Favorited`: 선택 하트와 중앙 하트 애니메이션
- `02F_Feed_Bid_BottomSheet`: 금액 입력, 최소 금액·500원 단위 검증
- `03_Product_Detail`, `03F0_Product_Detail_Full_Content_Spec`: 상세 세로 스크롤과 고정 입찰 영역
- `03B_Product_Detail_Favorited`, `03B2_Product_Detail_Unfavorited`: 찜 상태와 저장·삭제 안내
- 상세 입찰 바텀시트: 금액 입력과 검증

## 후속 화면에서 구현

- `03A_Image_Viewer`: 전체 화면 이미지 갤러리
- `03C_Product_Detail_Highest_Bidder`: 최고 입찰자 상태
- `03D_Product_Detail_Auction_Ended`: 경매 종료 상태
- `03E_Product_Detail_Won`, `03E2_Product_Detail_Lost`, `03E3_Product_Detail_Transaction_Urgent`: 낙찰·패찰·거래 촉구 상태
- `03I_Product_Detail_Price_Changed`, `03J_Product_Detail_Auction_Extended`, `03K_Product_Detail_Bid_Failed`: 실시간 경매 예외 상태
- `03H_Seller_Profile`, 판매자 후기·판매 상품
- 상품·판매자 신고 선택과 완료 화면

서버 이벤트에 의존하는 상태는 현재 로컬 예시 상태로 동작합니다. API와 실시간 경매 스트림 연결 시 ViewModel 상태로 교체합니다.
