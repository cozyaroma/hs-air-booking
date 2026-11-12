# B 담당: 승객 정보 · 좌석 선택

| 항목 | 내용 |
| --- | --- |
| 담당 파일 | `src/airline/ui/page/passenger/` (`PassengerPage`, `SeatPage` + 새로 만드는 클래스) |
| 담당 테스트 | `test/airline/PassengerSeatTest.java` |
| 단독 실행 | `page.bat PASSENGER ROUNDTRIP` · `page.bat PASSENGER INTL_ROUNDTRIP` · `page.bat SEAT ROUNDTRIP` · `page.bat SEAT INTL` |
| 앞 페이지 / 다음 페이지 | A의 항공권 선택 / C의 결제 (`Screen.PAYMENT`) |

## 1. 페이지 계약

| | 승객 정보 `PASSENGER` | 좌석 선택 `SEAT` |
| --- | --- | --- |
| 들어올 때 | `session().requireItinerary()` (가는 편, 왕복이면 오는 편), `session().getCriteria().toPassengerTypes()` | 위 + `session().getPassengers()`, `getSeatedPassengers()`(유아 제외) |
| 나갈 때 | `bookingService.validatePassengers(itinerary, list)` 통과 → `session().setPassengers(list)` → `navigate(SEAT)` | 여정의 **모든 구간**에서 좌석 승객 전원 `session().assignSeat(구간, 승객, 좌석)` → `allSeatsAssigned()` true → `navigate(PAYMENT)` |

상단 "선택한 항공권" 요약 바는 `BasePanel`이 자동으로 그립니다. 따로 만들지 않습니다.

## 2. 기능 목록

### 승객 정보 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| B1 | 승객 수만큼 입력 카드 (성인 → 소아 → 유아 순) | 필수 | ✅ 한 줄 입력 | 카드마다 "승객 1 · 성인" 제목, 나이 기준 안내 |
| B2 | 영문 성 / 영문 이름 | 필수 | ✅ 입력칸 | 입력 즉시 대문자로(권장), 여권과 같은 철자 안내 |
| B3 | **성별** | 필수 | ✅ 콤보 | 남성 / 여성 (`Gender`). 라디오 버튼으로 바꿔도 됨 |
| B4 | 생년월일 8자리 + **만 나이 자동 표시** | 필수 | 입력칸만 | `AgeCalculator.tryCalculate(입력, 탑승일)`로 "탑승일 기준 만 N세" 표시. 유형(성인/소아/유아)과 안 맞으면 빨간 안내 |
| B5 | 국제선이면 여권번호 | 필수 | ✅ 입력칸 | `itinerary.requiresPassport()`일 때만 보임. `Passenger.isValidPassportFormat()`으로 즉시 검사(권장) |
| B6 | 유아 보호자 안내 | 권장 | 없음 | "유아는 성인 1명과 함께 앉습니다 (국제선 보호자 만 18세 이상)". 검증 후 연결된 보호자 이름 표시 |
| B7 | 검증 오류 표시 | 필수 | ✅ 알림창 | `validatePassengers`가 던진 메시지 표시. 문제 있는 카드 강조(선택) |
| B8 | 이전으로 갔다 와도 입력값 유지 | 권장 | 저장된 승객만 | 검색 조건이 바뀌지 않았으면 입력 중이던 값도 유지 |
| B9 | 시연용 "예시 입력" 버튼 | 선택 | 없음 | 발표 시간 절약용 (개발 중에만 보이게 해도 됨) |

### 좌석 선택 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| B10 | 구간 전환 (가는 편 / 오는 편 탭) | 필수 | 구간별 카드 | 편도는 탭 하나. 각 탭에 편명·날짜·기종 표시 |
| B11 | **좌석 배치도** | 필수 | 없음 (자동 배정만) | `flight.getSeats()`로 버튼 격자. 비즈니스 2-2(A,C,D,F), 일반석 3-3(A~F), 가운데 통로, 열 번호 |
| B12 | 좌석 상태별 색 + 범례 | 필수 | 없음 | 선택 가능 / 이미 예약됨(`!isAvailable`) / 내가 선택 / 비상구(유료) / 다른 등급(선택 불가) |
| B13 | 승객 고르고 좌석 클릭 | 필수 | 없음 | 왼쪽 승객 목록에서 승객 선택 → 좌석 클릭 → `checkSeat(...)` 통과 시 `assignSeat`. 같은 좌석 다시 누르면 해제 |
| B14 | 좌석 규칙 위반 안내 | 필수 | 없음 | 비상구석 만 15세 미만·유아 동반 불가, 다른 등급 불가 → `checkSeat`의 예외 메시지 |
| B15 | 자동 배정 | 필수 | ✅ 버튼 | 남은 승객만 앞에서부터 (`autoAssignSeats`, 비상구 제외) |
| B16 | 승객별 좌석 요약 + 추가 요금 | 권장 | 글자 목록 | "HONG/GILDONG 가는 편 3B · 오는 편 12C(비상구 +30,000원)" |
| B17 | 한 구간이라도 빠지면 다음 불가 | 필수 | ✅ 경고 | 어느 구간·어느 승객이 남았는지 알려 주기(권장) |

## 3. 화면 예시 (참고용)

```
 [가는 편] HS1201 11/14 (토) 07:30 김포 → 08:40 제주        왕복 · 일반석 · 성인 2, 소아 1   ← 공용 요약 바
 [오는 편] HS1202 11/17 (화) 09:40 제주 → 10:50 김포
 ┌ [가는 편 HS1201] [오는 편 HS1202] ───────────────────────────────┐
 │ 승객               │        A  B  C     D  E  F                 │
 │ ▶ HONG/GILDONG 3B  │   1   [ ]   [ ]   [ ]   [ ]   ← 비즈니스(선택 불가) │
 │   KIM/YUNA     -   │   3   [■][●][ ]   [ ][■][ ]                   │
 │   HONG/JIHO    -   │  10   [비][비][비] [비][비][비]  ← 비상구 +30,000 │
 │ [자동 배정]         │  범례: □ 선택 가능 ■ 예약됨 ● 선택 비 비상구        │
 └──────────────────────────────────────────────────────────────────┘
```

## 4. 사용할 공용 기능

```java
Itinerary it = session().requireItinerary();
for (SegmentType seg : it.getSegments()) { Flight f = it.getFlight(seg); ... }
boolean passport = it.requiresPassport();
LocalDate travelDate = it.getOutbound().getDepartureDate();

Passenger p = PassengerFactory.create(type, last, first, gender, AgeCalculator.parseBirthDate(birth), passport);
app.getBookingService().validatePassengers(it, list);          // 모든 구간 기준 나이·여권·보호자 검사

f.getSeats();  seat.getRow(); seat.getColumn(); seat.getCode(); seat.isAvailable();
seat.getCabin(); seat.getExtraFee(); seat.getDescription(); seat instanceof ExitRowSeat
f.getAircraft().getTotalRows();  AircraftType.ALL_COLUMNS;
app.getBookingService().checkSeat(f, it.getCabin(), passenger, seat);   // 규칙 위반이면 예외
app.getBookingService().autoAssignSeats(f, it.getCabin(), session().getSeatedPassengers(), session().getSeats(seg));
session().assignSeat(seg, p, seat); session().unassignSeat(seg, p); session().findPassengerAt(seg, seat);
```

주의: `Seat`의 `reserve()`는 **부르지 않습니다.** 실제 좌석 확보는 결제 때 `createBooking`이 합니다.
좌석 페이지는 "고른 것"을 세션에 적기만 합니다.

## 5. 직접 확인할 시나리오

1. `INTL_ROUNDTRIP`: 여권번호 칸이 보이고, `12345`는 형식 오류. 유아는 좌석 목록에 없다.
2. 생년월일에 `2014111` (7자리) → 나이 표시 없음, `20141113` → "만 12세" 등 즉시 표시.
3. 가는 날 만 12세(소아)·오는 날 만 13세가 되는 아이를 소아로 입력 → 오는 편 날짜가 들어간 오류 메시지.
4. 국제선에서 만 16세 성인 1명 + 유아 → 보호자 없음 오류.
5. 소아가 비상구석 클릭 → "만 15세 이상" 안내. 유아 동반 성인도 비상구 불가.
6. 이미 예약된(회색) 좌석, 비즈니스석(일반석 예약 시)은 선택 불가.
7. 가는 편만 고르고 [다음] → 막힘. 오는 편까지 고르면 콘솔 `[세션 검사] SEAT → PAYMENT : 통과`.
8. 좌석 페이지에서 [이전] → 승객 정보 수정 → 다시 오면 좌석이 초기화되어 있다 (세션 규칙).

## 6. 완료 기준

- [ ] `test.bat` 전체 통과 (특히 `PageSmokeTest`의 PASSENGER, SEAT)
- [ ] `page.bat PASSENGER`·`page.bat SEAT`를 네 가지 데이터로 열어 오류 없음 (유아 포함 데이터 꼭 확인)
- [ ] 좌석 배치 계산(행·열 → 격자 위치 등)을 별도 클래스로 만들었다면 `PassengerSeatTest`에 테스트 추가
- [ ] 담당 폴더 밖 파일 변경 없음

## 7. 발표 때 설명할 OOP 포인트 (예시)

- 좌석 버튼(`SeatButton extends JButton`)이 좌석 객체를 갖고 상태에 따라 스스로 색을 정함 → 캡슐화
- 좌석 제한은 `Seat.checkOccupant()`를 `ExitRowSeat`가 오버라이딩한 것 → 화면은 좌석 종류를 몰라도 됨 (다형성)
- 승객 유형별 객체는 `PassengerFactory`가 만들고 화면은 `Passenger` 타입으로만 다룸 → 추상화
- 승객 나이 규칙은 국내선/국제선 `Flight`가 기준을 가짐 → 왕복이면 구간마다 다시 검사
