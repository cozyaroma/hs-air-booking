# A 담당: 항공권 검색 · 항공권(시간대) 선택

| 항목 | 내용 |
| --- | --- |
| 담당 파일 | `src/airline/ui/page/search/` (`SearchPage`, `FlightSelectPage` + 새로 만드는 클래스) |
| 담당 테스트 | `test/airline/SearchTest.java` |
| 단독 실행 | `page.bat SEARCH` · `page.bat FLIGHT_SELECT ROUNDTRIP` · `page.bat FLIGHT_SELECT INTL` |
| 이어지는 페이지 | B의 승객 정보 (`Screen.PASSENGER`) |

## 1. 페이지 계약

| | 항공권 검색 `SEARCH` | 항공권 선택 `FLIGHT_SELECT` |
| --- | --- | --- |
| 들어올 때 | 조건 없음 | `session().getCriteria()`, `session().getSearchResult()` |
| 나갈 때 | `session().startSearch(criteria, result)` → `navigate(FLIGHT_SELECT)` | `session().selectOutbound(f)`, 왕복이면 `session().selectInbound(f)` → `navigate(PASSENGER)` |

## 2. 기능 목록

### 항공권 검색 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| A1 | 편도 / 왕복 선택 | 필수 | 라디오 버튼만 | 왕복이면 "오는 날" 선택이 보이고, 편도면 숨기거나 비활성화 |
| A2 | 출발지 선택 → 그 출발지의 도착지만 표시 | 필수 | ✅ 동작 | `getDestinations(origin)` 사용. 출발지를 바꾸면 도착지 목록을 새로 채움 |
| A3 | 출발지 ⇄ 도착지 바꾸기 버튼 | 권장 | 없음 | 바꾼 도착지가 새 출발지의 도착지 목록에 있을 때만 |
| A4 | **달력으로 날짜 선택** | 필수 | 콤보박스(임시) | 월 단위 달력, 이전/다음 달 이동, 오늘 이전·`getLastSearchableDate()` 이후 날짜 선택 불가 |
| A5 | 왕복이면 가는 날·오는 날 두 번 선택 | 필수 | 콤보박스(임시) | 오는 날 < 가는 날이면 선택 불가 또는 즉시 안내. 달력에 두 날짜 사이 구간 표시(권장) |
| A6 | 달력에 날짜별 최저가 표시 | 선택 | 없음 | `findLowestAdultPrice(...)` (빈 값이면 "-"). 한 달치를 매번 계산해도 빠름 |
| A7 | 성인·소아·유아 인원 선택 + 나이 안내 | 필수 | 스피너 + 안내 문구 | `PassengerType.getAgeGuide()`. 성인 최소 1명, 좌석 승객 9명, 유아 ≤ 성인 (검사는 서비스가 함) |
| A8 | **나이 계산기** | 필수 | 없음 | 인원 선택 옆 [나이 계산기] 버튼 → 생년월일 8자리 입력 → "탑승일(가는 날) 기준 만 N세 → 국내선 소아 / 국제선 성인" 표시. `AgeCalculator.calculate(문자열, 가는 날)` |
| A9 | 좌석 등급 선택 | 필수 | ✅ 콤보 | 일반석 / 비즈니스석 |
| A10 | 검색 결과 없음 안내 | 필수 | ✅ 알림창 | `result.hasOptions()`가 false면 안내하고 이동하지 않음 |
| A11 | 입력 오류를 입력칸 근처에 표시 | 선택 | 없음 | 알림창 대신 빨간 글씨 등 |

### 항공권(시간대) 선택 페이지

| # | 기능 | 중요도 | 뼈대 상태 | 설명 |
| --- | --- | --- | --- | --- |
| A12 | 가는 편 항공권 목록 (2~3개) | 필수 | ✅ 글자 목록 | 출발·도착 시각, 소요 시간, 편명, 기종, 남은 좌석, 성인 1인 요금 |
| A13 | 목록을 카드/표 형태로 | 권장 | 없음 | 선택한 항공권 강조, 남은 좌석 5석 이하 "매진 임박" 표시 등 |
| A14 | 시간대 필터 | 권장 | 없음 | 전체 / 새벽 / 오전 / 오후 / 저녁 (`flight.getTimeBand()`) |
| A15 | 정렬 | 선택 | 없음 | 출발 시각순 / 가격순 |
| A16 | 왕복: 오는 편 목록 | 필수 | ✅ 글자 목록 | 가는 편을 고르면 `result.inboundOptionsFor(가는 편)`으로 **시간이 맞는 것만** 보여줌 |
| A17 | 왕복 진행 방식 | 권장 | 좌우 두 목록 | 예) 가는 편 선택 → 오는 편 선택 2단계, 또는 좌우 배치. 무엇을 골랐는지 항상 보이게 |
| A18 | 선택 합계 | 권장 | 없음 | 성인 1인 기준 가는 편 + 오는 편 요금, 승객 수를 곱한 예상 금액(세금 포함 "예상") |
| A19 | 검색 조건 요약 | 필수 | ✅ 부제목 | `criteria.describe()` |

## 3. 화면 예시 (참고용, 똑같이 만들 필요 없음)

```
 1. 항공권 검색 > 2. 항공권 선택 > 3. 승객 정보 > 4. 좌석 선택 > 5. 결제
 항공권 검색
 ┌──────────────────────────────────────────────────────────────┐
 │ (●) 왕복  ( ) 편도                                            │
 │ 출발 [서울/김포 ▼]  ⇄  도착 [제주 ▼]   좌석 [일반석 ▼]          │
 │ ┌ 2026년 11월  ◀ ▶ ─────────────────┐                        │
 │ │ 일  월  화  수  목  금  토          │  가는 날 11/14 (토)     │
 │ │              12  13 [14] 15       │  오는 날 11/17 (화)     │
 │ │ 16 [17] 18 ...                     │                        │
 │ │ (지난 날짜 회색, 최저가 작게)        │                        │
 │ └───────────────────────────────────┘                        │
 │ 성인 [- 2 +] 국내선 만 13세 이상 …                              │
 │ 소아 [- 1 +] 만 2세 ~ …          [나이 계산기]                   │
 │ 유아 [- 0 +] 만 2세 미만 …                                      │
 └──────────────────────────────────────────────────────────────┘
                                                   [항공권 검색]
```

## 4. 사용할 공용 기능

```java
FlightSearchService s = app.getSearchService();
List<Airport> origins = s.getOrigins();
List<Airport> dests   = s.getDestinations(origin);
LocalDate first = s.getFirstSearchableDate(), last = s.getLastSearchableDate();
OptionalLong low = s.findLowestAdultPrice(origin, dest, date, cabin);

SearchCriteria c = SearchCriteria.roundTrip(origin, dest, goDate, backDate, cabin, adults, children, infants);
SearchResult r = s.search(c);                 // AirlineException: 인원·날짜·노선 오류
r.outbound(); r.inbound(); r.inboundOptionsFor(selectedOutbound); r.hasOptions();
long price = s.getAdultPrice(flight, cabin);

AgeCalculator.AgeResult age = AgeCalculator.calculate("20141113", goDate);   // InvalidPassengerException
age.age(); age.domesticType(); age.internationalType(); age.describe();
```

## 5. 직접 확인할 시나리오 (발표 시연 후보)

1. 출발지를 인천으로 바꾸면 도착지에 도쿄·방콕만 나온다. 도쿄를 출발지로 하면 도착지는 인천뿐.
2. 달력에서 어제 날짜, 30일 뒤 날짜를 누를 수 없다. 다음 달로 넘어갔다가 돌아올 수 있다.
3. 왕복에서 오는 날을 가는 날보다 앞으로 고르면 막힌다. 같은 날 왕복도 된다.
4. 같은 날 왕복으로 18:40 가는 편을 고르면 오는 편은 20:50편만 보인다.
5. 나이 계산기: `20141113` → 11/12 기준 만 11세, 11/13 기준 만 12세(국내선 소아, 국제선 성인).
6. 성인 0명, 유아 > 성인, 좌석 승객 10명 → 서비스 예외 메시지가 그대로 보인다.
7. 항공권을 고르고 [다음] → 콘솔 `[세션 검사] FLIGHT_SELECT → PASSENGER : 통과`.
8. [이전]으로 돌아와 다른 항공권을 고르면 승객 정보 페이지 상단 요약 바가 바뀐다.

## 6. 완료 기준

- [ ] `test.bat` 전체 통과 (특히 `PageSmokeTest`의 SEARCH, FLIGHT_SELECT)
- [ ] `page.bat FLIGHT_SELECT` 를 `ONEWAY`, `ROUNDTRIP`, `INTL`, `INTL_ROUNDTRIP` 네 가지로 열어 오류 없음
- [ ] 달력·나이 계산기처럼 계산이 있는 클래스는 `SearchTest`에 테스트 추가
- [ ] 담당 폴더 밖 파일 변경 없음 (`git status`)

## 7. 발표 때 설명할 OOP 포인트 (예시)

- 달력을 `JPanel`을 상속한 별도 컴포넌트(`CalendarPanel`)로 만들고, 날짜 선택을 리스너(인터페이스)로 알림 → 재사용·캡슐화
- 날짜 계산(그 달의 첫 요일, 일수)을 화면과 분리한 클래스로 만들어 테스트 → 단일 책임
- 페이지가 `BasePanel`을 상속하고 `onShow()`를 오버라이딩 → 템플릿 메서드, 다형성
- 검색 규칙은 `SearchCriteria.validate()`, 노선 규칙은 `FlightSearchService`에 있고 화면은 호출만 → 계층 분리
