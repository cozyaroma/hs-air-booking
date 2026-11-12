# C 담당: 결제 · 예약 완료 · 내 예약(조회·취소·체크인)

| 항목 | 내용 |
| --- | --- |
| 담당 파일 | `src/airline/ui/page/payment/` (`PaymentPage`, `ConfirmationPage`, `MyBookingsPage` + 새로 만드는 클래스) |
| 담당 테스트 | `test/airline/PaymentBookingTest.java` |
| 단독 실행 | `page.bat PAYMENT ROUNDTRIP` · `page.bat PAYMENT INTL` · `page.bat CONFIRMATION INTL_ROUNDTRIP` · `page.bat MY_BOOKINGS` |
| 앞 페이지 | B의 좌석 선택 |

## 1. 페이지 계약

| | 결제 `PAYMENT` | 예약 완료 `CONFIRMATION` | 내 예약 `MY_BOOKINGS` |
| --- | --- | --- | --- |
| 들어올 때 | 여정·승객·모든 구간 좌석 → `session().toRequest()` | `session().getLastBooking()` | `app.getCurrentUser()` (세션 조건 없음) |
| 나갈 때 | `createBooking(user, request, 결제수단)` 성공 → `session().completeBooking(b)` → `navigate(CONFIRMATION)` | [내 예약 보기] `navigate(MY_BOOKINGS)` / [새 예매] `app.startNewBooking()` | 없음 |

결제 페이지 상단 "선택한 항공권" 요약 바는 `BasePanel`이 자동으로 그립니다.

## 2. 기능 목록

### 결제 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| C1 | 결제 수단 선택 | 필수 | 간편결제만 | 신용카드 / 실시간 계좌이체 / 간편결제 (탭·라디오 + CardLayout) |
| C2 | **신용카드 입력** | 필수 | 없음 | 카드사(`CARD_COMPANIES`), 카드번호 16자리(4자리마다 띄움), 유효기간 MM/YY, CVC 3자리, 소유자 생년월일 6자리, 카드 비밀번호 앞 2자리, 할부(일시불, 2~12개월) |
| C3 | **계좌이체 입력** | 필수 | 없음 | 은행(`BANKS`), 계좌번호(10~14자리, 하이픈 허용), 예금주 |
| C4 | 민감 정보 처리 | 필수 | 없음 | CVC·비밀번호는 `JPasswordField`. 화면·콘솔에 전체 번호 출력 금지. "모의 결제입니다. 실제 카드 정보를 입력하지 마세요. 테스트 번호 4111 1111 1111 1111" 안내 |
| C5 | 입력 즉시 검사 | 권장 | 없음 | 카드번호 `PaymentFormats.isLuhnValid`, 자릿수. 최종 검사는 생성자가 함 (`InvalidPaymentInfoException`) |
| C6 | 구간별·승객별 추가 수하물 | 필수 | 없음 | 유아 제외. 무료 허용량(`policy.getFreeAllowance(cabin)`), 단위(`getExtraUnitName`), 단위 요금, 최대(`getMaxExtraUnits`). `session().setExtraBaggage(구간, 승객, n)` 후 금액 다시 계산 |
| C7 | 요금 상세표 | 필수 | 구간별 한 줄 | 승객별·구간별 운임/세금/좌석/수하물 (`quote(request, 구간, 승객)`), 구간 합계, 총액 |
| C8 | 결제 전 최종 확인 | 권장 | ✅ 확인창 | 금액, 결제 수단 이름(`getDisplayName`), 가려진 정보(`getMaskedInfo`) |
| C9 | 취소 규정 안내 | 권장 | 없음 | `flight.getCancellationPolicy().describeRules()`, 동의 체크박스 |
| C10 | 결제 실패 처리 | 필수 | ✅ 알림창 | 한도 초과·할부 최소 금액·유효기간 만료 → 예외 메시지 (좌석은 서비스가 자동으로 되돌림) |

### 예약 완료 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| C11 | 예약번호 크게 + 여정 요약 | 필수 | ✅ 글자 | 예약번호, 편도/왕복, 구간별 편명·시각 |
| C12 | 승객별 좌석·e-티켓 번호 | 필수 | ✅ 글자 | 구간별 표 또는 카드 |
| C13 | 결제 내역 | 필수 | ✅ 글자 | 수단, **가려진 번호**, 승인번호, 금액 |
| C14 | 영수증/e-티켓 모양 레이아웃 | 권장 | 없음 | |

### 내 예약 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| C15 | 내 예약 목록 | 필수 | ✅ 목록 | `getMyBookings(user)` (출발 빠른 순). 예약번호, 노선, 편도/왕복, 날짜, 상태 |
| C16 | 상태별 표시 | 권장 | 글자 | 예약 확정 / 가는 편 체크인 완료 / 체크인 완료 / 취소됨 색 구분, 지난 여정 흐리게 |
| C17 | 예약 상세 | 필수 | ✅ 글자 | 완료 페이지와 같은 내용 + 취소 정보 |
| C18 | 구간별 체크인 | 필수 | 구간 선택창 | 구간마다 버튼, `segment.isCheckInOpen(now)`일 때만 활성. 열리는 시각 안내 |
| C19 | 취소 | 필수 | ✅ 견적 확인 후 | 구간별 남은 시간·수수료율·수수료, 환불액, 환불 방법(`getRefundGuide`) 표 |
| C20 | 예약번호 검색 | 선택 | 없음 | `findMyBooking(user, code)` |
| C21 | 탑승권 보기 | 선택 | 없음 | 체크인 완료 구간만 |

## 3. 화면 예시 (참고용)

```
 [가는 편] HS1201 …  /  [오는 편] HS1202 …                    ← 공용 요약 바
 ┌ 추가 수하물 ───────────────────────────┐ ┌ 요금 ─────────────────────┐
 │ 무료 20kg · 10kg당 20,000원 · 최대 3   │ │ 가는 편  운임 182,900       │
 │            가는 편   오는 편            │ │          세금  38,400       │
 │ HONG/GILDONG [0 ▼]   [1 ▼]            │ │          수하물 0           │
 │ KIM/YUNA     [0 ▼]   [0 ▼]            │ │ 오는 편  …                  │
 └───────────────────────────────────────┘ │ 총 결제 금액 466,700원       │
 ┌ 결제 수단 [신용카드] [계좌이체] [간편결제] ──────────────────────────────┐
 │ 카드사 [신한카드 ▼]  카드번호 [4111 1111 1111 1111]  유효기간 [12/29]       │
 │ CVC [•••]  생년월일 [950315]  비밀번호 앞 2자리 [••]  할부 [일시불 ▼]       │
 │ ※ 모의 결제입니다. 실제 카드 정보를 입력하지 마세요.                         │
 └──────────────────────────────────────────────────────────────────────┘
                                                            [결제하기]
```

## 4. 사용할 공용 기능

```java
BookingService b = app.getBookingService();
BookingRequest req = session().toRequest();                 // 수하물을 바꿀 때마다 새로 만든다
PriceBreakdown one = b.quote(req, SegmentType.OUTBOUND, passenger);
PriceBreakdown seg = b.quoteSegment(req, segment);
PriceBreakdown all = b.quoteTotal(req);                     // fare(), tax(), seatFee(), baggageFee(), total()

BaggagePolicy policy = session().requireItinerary().getFlight(segment).getBaggagePolicy();
session().setExtraBaggage(segment, passenger, units);

PaymentMethod m = new CreditCardPayment(company, number, "MM/YY", cvc, birth6, pw2, months); // InvalidPaymentInfoException
PaymentMethod m = new BankTransferPayment(bank, account, holder);
PaymentMethod m = new SimplePayment(provider);
Booking booking = b.createBooking(app.getCurrentUser(), req, m);   // PaymentFailedException 등
session().completeBooking(booking);

List<Booking> mine = b.getMyBookings(app.getCurrentUser());
CancellationQuote q = b.quoteCancellation(user, booking);   // q.segmentFees(), q.totalFee(), q.refundAmount()
b.cancel(user, booking);
b.checkIn(user, booking, SegmentType.INBOUND);              // CheckInNotAvailableException
booking.getSegment(seg).isCheckInOpen(app.getClock().now());
```

## 5. 직접 확인할 시나리오

1. 카드번호 `4111 1111 1111 1112` → 번호 오류. `4111 1111 1111 1111`, 유효기간 지난 달 → 결제 실패.
2. 할부는 5만원 이상만 가능 (샘플 항공권은 모두 5만원이 넘어서 화면으로는 안 나옴 → `PaymentBookingTest`로 확인).
3. 계좌번호 `123-45` → 자릿수 오류. 정상 결제 후 완료 페이지에 `**********4567` 형태로만 보임.
4. `INTL_ROUNDTRIP` + 간편결제 → 200만원 한도 초과로 실패. 실패해도 좌석이 잡힌 채로 남지 않는다 (서비스가 되돌림).
5. 수하물 추가 → 총액이 즉시 바뀐다. 유아는 수하물 선택이 없다.
6. 내 예약(demo): 내일 12:10 왕복 → 체크인이 아직 안 열렸으면 [시각 이동]으로 출발 24시간 이내까지 이동 → 가는 편 체크인 버튼 활성화 → 체크인 → 이후 취소 불가 안내.
7. 취소 수수료(국내선): 출발 7일 이상 전 0%, 24시간 이상 전 10%, 24시간 이내 20%. 왕복은 구간마다 다르게 계산됨을 표로 보여줌.
8. 새 계정으로 가입 → 내 예약이 비어 있다 (다른 회원 예약이 보이지 않음).

## 6. 완료 기준

- [ ] `test.bat` 전체 통과 (특히 `PageSmokeTest`의 PAYMENT, CONFIRMATION, MY_BOOKINGS)
- [ ] 네 가지 데이터로 `page.bat PAYMENT` 확인, `page.bat MY_BOOKINGS`에서 시각 이동 시연 확인
- [ ] 입력 서식 클래스(예: 카드번호 자동 띄어쓰기 `DocumentFilter`)를 만들었다면 `PaymentBookingTest`에 테스트 추가
- [ ] 전체 카드번호·CVC가 화면, 콘솔, 예약 객체 어디에도 남지 않음
- [ ] 담당 폴더 밖 파일 변경 없음

## 7. 발표 때 설명할 OOP 포인트 (예시)

- 결제 입력 폼을 수단마다 클래스로 (`CardForm`, `BankForm`, `SimpleForm`이 같은 인터페이스 `PaymentForm.createPayment()` 구현)
  → 결제 페이지는 "지금 선택된 폼"에게 결제 객체를 달라고만 함 (다형성, 개방-폐쇄)
- 결제 절차는 `AbstractPayment.pay()`가 고정하고 수단별 검사만 다름 (템플릿 메서드)
- CVC를 필드로 저장하지 않고 가려진 번호만 보관 (캡슐화, 보안)
- 취소 수수료는 구간마다 `Flight`의 `CancellationPolicy`가 계산 → 화면은 국내선/국제선을 몰라도 됨
