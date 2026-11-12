# 클래스 다이어그램 (베이스 설계 v1)

GitHub에서 이 파일을 열면 그림으로 보입니다 (Mermaid).
12주차 SOLID 검토 후 바뀐 구조는 `class-diagram-v2.md`로 따로 남겨 두세요. 초기 설계와 최종 설계 비교가 README와 발표자료의 필수 항목입니다.
페이지 담당자가 자기 폴더에 클래스를 추가하면 4번 그림에 덧붙입니다.

## 1. 회원 · 항공권 · 좌석 · 승객

```mermaid
classDiagram
    direction TB
    class User {
        -String username
        -HashedPassword password
        +checkPassword(String, PasswordHasher) boolean
    }
    class PasswordHasher {
        +hash(String) HashedPassword
        +matches(String, HashedPassword) boolean
    }
    User ..> PasswordHasher

    class Flight {
        <<abstract>>
        -String flightNumber
        -LocalDateTime departureTime
        -Map~String, Seat~ seats
        +getTypeName()* String
        +getAdultMinAge()* int
        +getGuardianMinAge()* int
        +getInfantFareRate()* double
        +requiresPassport()* boolean
        +getCancellationPolicy()* CancellationPolicy
        +getBaggagePolicy()* BaggagePolicy
        +getTimeBand() TimeBand
        +isBookable(LocalDateTime) boolean
    }
    Flight <|-- DomesticFlight
    Flight <|-- InternationalFlight
    Flight --> Route
    Flight --> AircraftType
    Flight "1" *-- "many" Seat

    class Seat {
        <<abstract>>
        -boolean reserved
        +reserve()
        +release()
        +getCabin()* CabinClass
        +getExtraFee() long
        +checkOccupant(Passenger, LocalDate)
    }
    Seat <|-- EconomySeat
    EconomySeat <|-- ExitRowSeat
    Seat <|-- BusinessSeat

    class Passenger {
        <<abstract>>
        -String lastName
        -String firstName
        -Gender gender
        -LocalDate birthDate
        +getType()* PassengerType
        +getFareRate(Flight)* double
        +validateAge(Flight)*
        +validateDocuments(Flight)
        +occupiesSeat() boolean
    }
    Passenger <|-- AdultPassenger
    Passenger <|-- ChildPassenger
    Passenger <|-- InfantPassenger
    AdultPassenger "1" -- "0..1" InfantPassenger : 보호자
    Passenger --> Gender
    class AgeCalculator {
        <<utility>>
        +parseBirthDate(String) LocalDate
        +calculate(LocalDate, LocalDate) AgeResult
    }
    Passenger ..> AgeCalculator
```

## 2. 여정 · 예약 · 결제 · 규정

```mermaid
classDiagram
    direction TB
    class Itinerary {
        -Flight outbound
        -Flight inbound
        -CabinClass cabin
        +oneWay(Flight, CabinClass)$ Itinerary
        +roundTrip(Flight, Flight, CabinClass)$ Itinerary
        +canPair(Flight, Flight)$ boolean
        +getSegments() List~SegmentType~
        +getFlight(SegmentType) Flight
    }
    Itinerary --> TripType
    Itinerary --> SegmentType

    class Booking {
        -String bookingCode
        -String ownerUsername
        -BookingStatus status
        +quoteCancellation(LocalDateTime) CancellationQuote
        +cancel(LocalDateTime) CancellationQuote
        +checkIn(SegmentType, LocalDateTime)
        +isOwnedBy(User) boolean
    }
    class BookingSegment {
        -SegmentType type
        -LocalDateTime checkedInAt
        +isCheckInOpen(LocalDateTime) boolean
        +getPrice() PriceBreakdown
    }
    Booking --> Itinerary
    Booking "1" *-- "1..2" BookingSegment
    BookingSegment "1" *-- "1..*" Ticket
    Ticket --> PriceBreakdown
    Booking --> PaymentMethod
    Booking ..> CancellationQuote

    class PaymentMethod {
        <<interface>>
        +getDisplayName() String
        +getMaskedInfo() String
        +pay(long, LocalDateTime) PaymentResult
        +getRefundGuide() String
    }
    class AbstractPayment {
        <<abstract>>
        +pay(long, LocalDateTime) PaymentResult
        #validate(long, LocalDateTime)*
    }
    PaymentMethod <|.. AbstractPayment
    AbstractPayment <|-- CreditCardPayment
    AbstractPayment <|-- BankTransferPayment
    AbstractPayment <|-- SimplePayment
    CreditCardPayment ..> PaymentFormats
    BankTransferPayment ..> PaymentFormats

    class CancellationPolicy {
        <<interface>>
        +getFeeRate(Duration) double
    }
    CancellationPolicy <|.. TieredCancellationPolicy
    TieredCancellationPolicy <|-- DomesticCancellationPolicy
    TieredCancellationPolicy <|-- InternationalCancellationPolicy
    class BaggagePolicy {
        <<interface>>
    }
    BaggagePolicy <|.. DomesticBaggagePolicy
    BaggagePolicy <|.. InternationalBaggagePolicy
```

## 3. 서비스 · 저장소

```mermaid
classDiagram
    direction LR
    class AuthService {
        +signUp(String, String, String) User
        +login(String, String) User
        +logout()
        +getCurrentUser() Optional~User~
    }
    class FlightSearchService {
        +getOrigins() List~Airport~
        +getDestinations(Airport) List~Airport~
        +search(SearchCriteria) SearchResult
        +findLowestAdultPrice(...) OptionalLong
    }
    class BookingService {
        +validatePassengers(Itinerary, List~Passenger~)
        +checkSeat(Flight, CabinClass, Passenger, Seat)
        +autoAssignSeats(...) Map~Passenger, Seat~
        +quoteTotal(BookingRequest) PriceBreakdown
        +createBooking(User, BookingRequest, PaymentMethod) Booking
        +getMyBookings(User) List~Booking~
        +cancel(User, Booking) CancellationQuote
        +checkIn(User, Booking, SegmentType)
    }
    class AppServices {
        <<record>>
    }
    AppServices --> AuthService
    AppServices --> FlightSearchService
    AppServices --> BookingService
    AuthService --> UserRepository
    FlightSearchService --> FlightRepository
    BookingService --> BookingRepository
    BookingService --> FareCalculator
    UserRepository <|.. InMemoryUserRepository
    FlightRepository <|.. InMemoryFlightRepository
    BookingRepository <|.. InMemoryBookingRepository
    FlightSearchService ..> SearchCriteria
    FlightSearchService ..> SearchResult
    BookingService ..> BookingRequest
```

## 4. 화면 (Swing)

```mermaid
classDiagram
    direction TB
    class Application {
        +launch(AppServices, String)$
        +launchAt(AppServices, User, BookingSession, Screen)$
    }
    class AppContext {
        <<interface>>
        +navigate(Screen)
        +getSession() BookingSession
        +getCurrentUser() User
        +getSearchService() FlightSearchService
        +getBookingService() BookingService
    }
    class BasePanel {
        <<abstract>>
        +refresh()
        #onShow()*
        #session() BookingSession
        #showError(AirlineException)
    }
    class Screen {
        <<enumeration>>
        +check(BookingSession) List~String~
        +getOwner() PageOwner
    }
    class BookingSession {
        +startSearch(SearchCriteria, SearchResult)
        +selectOutbound(Flight)
        +selectInbound(Flight)
        +setPassengers(List~Passenger~)
        +assignSeat(SegmentType, Passenger, Seat)
        +toRequest() BookingRequest
        +completeBooking(Booking)
    }
    Application --> LoginFrame
    Application --> MainFrame
    JFrame <|-- LoginFrame
    JFrame <|-- MainFrame
    AppContext <|.. MainFrame
    MainFrame --> BookingSession
    MainFrame ..> PageRegistry
    MainFrame ..> Screen
    JPanel <|-- BasePanel
    BasePanel --> AppContext
    BasePanel --> TicketSummaryBar
    BasePanel <|-- SearchPage : A
    BasePanel <|-- FlightSelectPage : A
    BasePanel <|-- PassengerPage : B
    BasePanel <|-- SeatPage : B
    BasePanel <|-- PaymentPage : C
    BasePanel <|-- ConfirmationPage : C
    BasePanel <|-- MyBookingsPage : C
```
