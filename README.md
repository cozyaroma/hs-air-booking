# HS AIR 항공권 예매 시스템

한성대학교 객체지향프로그래밍 팀 프로젝트 · Java 17 · Swing · 외부 라이브러리 없음

> **팀원 시작하기**
> 1. `test.bat` (macOS/Linux `./test.sh`)로 테스트 통과 확인 → 2. `run.bat`으로 실행 (`demo` / `demo1234`)
> 3. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) 1~5절 읽기 → 4. 내 페이지 문서 [`docs/pages/`](docs/pages)
> 5. [`docs/AI-GUIDE.md`](docs/AI-GUIDE.md)의 첫 메시지로 AI 작업 시작 → 6. [`docs/GIT-GUIDE.md`](docs/GIT-GUIDE.md)대로 브랜치·PR

이 README는 제출용 9개 항목 틀입니다. `(작성)` 표시는 개발이 끝난 뒤 채웁니다.

---

## 1. 프로젝트 개요

### 프로젝트명
HS AIR 항공권 예매 시스템

### 개발 목적
실제 항공사 예매 흐름(검색 → 항공권 선택 → 승객 정보 → 좌석 → 결제 → 예약 조회·취소·체크인)을
객체지향 설계로 구현합니다. 국내선/국제선, 성인/소아/유아, 좌석 종류, 결제 수단마다 규칙이 달라서
상속·다형성·추상화를 자연스럽게 적용할 수 있는 주제입니다.

### 주요 기능
- 회원가입(가입 즉시 로그인) · 로그인 · 로그아웃
- 편도 / 왕복 검색, 달력 날짜 선택, 출발지에 따라 갈 수 있는 도착지만 표시
- 노선·날짜별 2~3개 항공권 중 시간대 선택
- 승객 정보(영문 이름, 성별, 생년월일, 국제선 여권), 만 나이 계산기
- 구간별 좌석 선택 (비상구석 나이·유아 동반 제한, 유료 좌석)
- 추가 수하물, 요금 상세, 신용카드 / 계좌이체 / 간편결제 (모의 결제, 정보 마스킹)
- 내 예약 조회, 구간별 온라인 체크인, 남은 시간에 따른 취소 수수료
- 승객 정보~결제 화면 상단에 선택한 항공권 요약

### 화면
(작성: 개발 완료 후 주요 화면 캡처)

## 2. 팀 구성

| 담당 | 이름 | 맡은 부분 | 주로 사용한 AI |
| --- | --- | --- | --- |
| A | (작성) | 항공권 검색 · 항공권(시간대) 선택 · 달력 · 나이 계산기 | (작성) |
| B | (작성) | 승객 정보 · 좌석 선택 | (작성) |
| C | (작성) | 결제 · 예약 완료 · 내 예약(취소·체크인) | (작성) |
| 공용 | (통합 담당) | 도메인·서비스·페이지 틀·로그인·테스트 도구 | |

## 3. 개발 환경

| 항목 | 내용 |
| --- | --- |
| 언어 | Java 17 이상 (JDK 17, 21에서 확인) |
| UI | Swing (Metal 테마, 운영체제와 관계없이 같은 모양) |
| 외부 라이브러리 | 없음 (테스트 도구도 직접 작성) |
| IDE | IntelliJ IDEA 권장 (명령줄 스크립트로도 실행 가능) |
| 협업 | GitHub (브랜치 + Pull Request) |
| 인코딩 | UTF-8 |

## 4. 프로젝트 폴더 구조

```
airline-booking/
├─ AGENTS.md, CLAUDE.md, GEMINI.md, .github/   AI 작업 규칙 (모든 AI 공통)
├─ run / test / page / context (.bat, .sh)     실행 · 테스트 · 페이지 단독 실행 · AI 자료 묶음
├─ docs/
│  ├─ ARCHITECTURE.md     구조와 페이지 연결 (페이지 계약)
│  ├─ pages/              담당별 기능 목록 (A, B, C)
│  ├─ AI-GUIDE.md         AI에게 말하는 법 (복사용 템플릿)
│  ├─ GIT-GUIDE.md        브랜치 · 커밋 · PR 규칙
│  ├─ class-diagram.md    클래스 다이어그램 (Mermaid)
│  └─ ai-log.md           생성형 AI 활용 기록
├─ src/airline/
│  ├─ app/        시작점, 객체 조립, 샘플 데이터
│  ├─ domain/     user, flight, seat, passenger, booking
│  ├─ policy/     취소·수하물 규정
│  ├─ payment/    결제 수단
│  ├─ repository/ 저장소 (인터페이스 + 메모리 구현)
│  ├─ service/    회원 · 검색 · 예약 서비스
│  ├─ exception/  업무 규칙 예외
│  ├─ util/       시계, 표시 형식
│  ├─ dev/        개발 도구 (페이지 단독 실행, 테스트 데이터, AI 자료 묶음)
│  └─ ui/         공용 틀 + login + common + page/search(A) · page/passenger(B) · page/payment(C)
└─ test/airline/  공용 테스트 + 담당별 테스트 + 페이지 스모크 테스트
```

자세한 설명: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## 5. 실행 방법

### 명령줄 (Windows)
```
run.bat                      프로그램 실행 (로그인 창부터)
test.bat                     전체 자동 테스트
page.bat SEAT ROUNDTRIP      좌석 페이지만 바로 열기 (개발 모드)
context.bat B                B 담당용 AI 자료 묶음 만들기
```
macOS/Linux는 `./run.sh`, `./test.sh`, `./page.sh SEAT ROUNDTRIP`, `./context.sh B`

### IntelliJ IDEA
`File → Open`으로 폴더 열기 → `src`를 Sources Root, `test`를 Test Sources Root로 지정 →
`airline.app.Main` 실행 (테스트는 `airline.AllTests`, 페이지 단독 실행은 `airline.dev.PageLauncher`에 인자 `SEAT ROUNDTRIP`)

### 시연 팁
- 시연용 계정 `demo` / `demo1234` (예약 2건 포함), 새 계정 가입도 가능
- 모의 결제 테스트 카드 `4111 1111 1111 1111`, 유효기간은 미래 날짜 아무거나
- 헤더 `[시각 이동]`으로 시간을 앞당겨 체크인 가능 시간·취소 수수료 변화를 보여줄 수 있음
- 회원·예약은 메모리에 저장되어 프로그램을 끄면 초기화됨

## 6. OOP 적용 내용

(작성: 최종 코드 기준으로 클래스 이름과 함께. 아래는 베이스에 이미 있는 예)

| 원리 | 적용 예 |
| --- | --- |
| 은닉성 | `Seat.reserved`는 `reserve()/release()`로만 변경, `Booking` 상태는 `cancel()/checkIn()`으로만 변경, `User`는 비밀번호 해시만 보관, 결제 객체는 CVC를 저장하지 않고 가려진 번호만 보관 |
| 상속 | `Flight ← DomesticFlight/InternationalFlight`, `Seat ← EconomySeat ← ExitRowSeat`, `Passenger ← Adult/Child/Infant`, `AbstractPayment ← 카드/계좌/간편`, 모든 페이지 `BasePanel` 상속 |
| 다형성 | 요금 계산이 `passenger.getFareRate(flight)`, 좌석 제한이 `seat.checkOccupant()`, 취소 수수료가 `flight.getCancellationPolicy()`로 종류를 묻지 않고 처리, 예외마다 `getTitle()` 오버라이딩 |
| 추상화 | `PaymentMethod`, `CancellationPolicy`, `BaggagePolicy`, 저장소 인터페이스, 페이지가 의존하는 `AppContext` |
| 그 밖에 | 템플릿 메서드(`AbstractPayment.pay`, `BasePanel.refresh`), 팩토리(`PassengerFactory`, `PageRegistry`), 의존성 주입(`AppFactory`), 값 객체(record), 체크 예외 계층 |

## 7. 생성형 AI 활용

(작성: [`docs/ai-log.md`](docs/ai-log.md)를 요약. 담당별 도구, 활용 수준(0~4), 주요 활용 내용, 팀이 검토·수정한 부분, 팀이 직접 결정한 설계)

## 8. 기능 테스트 결과

- 자동 테스트: `test.bat` → 공용·A·B·C 영역 테스트와 페이지 스모크 테스트 (베이스 기준 52개 통과)
- 페이지 스모크 테스트: 7개 페이지 × 4가지 데이터(국내선 편도, 국내선 왕복, 국제선 유아 동반, 국제선 왕복 가족)
- (작성: 시나리오별 수동 테스트 결과 표, 발견해 고친 오류)

## 9. 객체지향 설계 개선 내용

### 초기 설계 (v1)
[`docs/class-diagram.md`](docs/class-diagram.md)

### 최종 설계 (v2) 및 변경 이유
(작성: 12주차 SOLID 검토 후 `docs/class-diagram-v2.md`와 함께)
