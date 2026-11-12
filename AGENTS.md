# AI 작업 규칙 (모든 AI 공통)

이 저장소는 3명이 각자 다른 AI로 **페이지를 나눠** 개발하는 Java Swing 과제 프로젝트(HS AIR 항공권 예매)입니다.
AI는 아래 규칙을 반드시 지킵니다. 사용자가 먼저 "나는 A(또는 B, C) 담당"이라고 알려 줍니다.

## 1. 담당 범위 (가장 중요)

| 담당 | 수정해도 되는 파일 | 요구사항 문서 |
| --- | --- | --- |
| A | `src/airline/ui/page/search/` 안의 파일, `test/airline/SearchTest.java` | `docs/pages/A-search.md` |
| B | `src/airline/ui/page/passenger/` 안의 파일, `test/airline/PassengerSeatTest.java` | `docs/pages/B-passenger-seat.md` |
| C | `src/airline/ui/page/payment/` 안의 파일, `test/airline/PaymentBookingTest.java` | `docs/pages/C-payment-booking.md` |

- 위 표 밖의 파일은 **공용 코드**이며 읽기 전용입니다. (domain, service, payment, policy, repository, util, exception, app, dev, ui 바로 아래 파일, ui/common, ui/login)
- 자기 패키지 안에는 새 클래스를 만들어도 됩니다. (예: `ui/page/search/CalendarPanel.java`, `ui/page/search/AgeCalculatorDialog.java`)
- 공용 코드를 바꿔야 할 것 같으면 **고치지 말고** "공용 코드 변경 요청서"(docs/AI-GUIDE.md 6번 형식)를 써서 사용자에게 줍니다.
- 다른 담당의 페이지 클래스를 import하거나 호출하지 않습니다. 페이지끼리는 `BookingSession`으로만 데이터를 주고받습니다.

## 2. 기술 조건

- Java 17, Swing만 사용. 외부 라이브러리(JCalendar, FlatLaf, Lombok, JUnit 등) 추가 금지.
- 파일 인코딩 UTF-8, 주석과 화면 문구는 한국어. 화면에서는 "항공편"이 아니라 **"항공권"**이라고 씁니다.
- 색·글꼴·버튼은 `UiTheme`의 것을 씁니다 (`UiTheme.primaryButton`, `UiTheme.card`, `UiTheme.heading` 등).
- 현재 시각은 `LocalDateTime.now()` 대신 `app.getClock().now()` / `app.getClock().today()`를 씁니다 (시연용 시각 이동 때문).

## 3. 페이지 작성 규칙

- 모든 페이지는 `BasePanel`을 상속하고, 생성자에서 `super(app, Screen.XXX)`를 부릅니다.
- 화면이 보일 때마다 `onShow()`가 호출됩니다. 여기서 `session()`의 최신 값으로 다시 그립니다.
  `onShow()` 안에서는 알림창(JOptionPane)을 띄우지 않습니다. (자동 테스트가 화면 없이 부릅니다)
- 페이지 이동은 `app.navigate(Screen.다음페이지)`만 씁니다. 이동 전에 다음 페이지의 입장 조건(`Screen.check`)을 만족하도록 세션을 채웁니다.
- 업무 규칙(나이, 좌석 제한, 요금, 결제 형식, 취소 수수료)은 이미 공용 코드에 있습니다. **페이지에서 규칙을 다시 만들지 말고** 서비스/도메인 메서드를 호출합니다.
  규칙 위반은 `AirlineException`(체크 예외)으로 오므로 `catch (AirlineException e) { showError(e); }`로 처리합니다.
- 계산이 들어간 UI 로직(달력 날짜 계산, 입력 자동 서식 등)은 별도 클래스로 빼고, 담당 테스트 파일에 테스트를 추가합니다.
- 가짜 결제입니다. 실제 카드번호를 저장·출력하지 않고 `PaymentMethod.getMaskedInfo()`만 화면에 보여줍니다. CVC·카드 비밀번호는 `JPasswordField`로 받습니다.

## 4. 코드 답변 형식

- 수정한 파일은 **파일 전체**를 보여줍니다 (부분 조각만 주지 않음). 파일 경로를 맨 위에 적습니다.
- 한 번에 기능 하나씩. 바꾼 이유와 사용한 OOP 개념(상속, 다형성, 캡슐화 등)을 3~5줄로 설명합니다.
- 학생이 발표에서 설명할 수 있도록, 처음 쓰는 Swing 기법에는 짧은 주석을 답니다.

## 5. 확인 방법 (작업 후 사용자에게 안내)

1. `test.bat` (macOS/Linux `./test.sh`) → "모든 테스트를 통과" 확인
2. `page.bat 내페이지 ROUNDTRIP` 와 `page.bat 내페이지 INTL` 로 직접 눌러 보기
   → 콘솔의 `[세션 검사] ... : 통과` 확인
3. `run.bat`으로 로그인부터 전체 흐름 확인
4. `git status`로 담당 폴더 밖의 파일이 바뀌지 않았는지 확인

자세한 구조는 `docs/ARCHITECTURE.md`, 페이지 요구사항은 `docs/pages/`를 읽습니다.
