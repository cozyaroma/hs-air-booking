package airline.service;

import airline.domain.booking.TripType;
import airline.domain.flight.Airport;
import airline.domain.passenger.PassengerType;
import airline.domain.seat.CabinClass;
import airline.exception.AirlineException;
import airline.exception.InvalidPassengerException;
import airline.exception.InvalidRequestException;
import airline.util.Formats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 항공권 검색 조건 (편도/왕복).
 * 편도면 returnDate는 항상 null로 정리된다.
 */
public record SearchCriteria(TripType tripType, Airport origin, Airport destination,
                             LocalDate departureDate, LocalDate returnDate, CabinClass cabin,
                             int adults, int children, int infants) {

    /** 좌석을 쓰는 승객(성인+소아)은 한 예약에 최대 9명 */
    public static final int MAX_SEATED_PASSENGERS = 9;

    public SearchCriteria {
        if (tripType == TripType.ONE_WAY) {
            returnDate = null;
        }
    }

    public static SearchCriteria oneWay(Airport origin, Airport destination, LocalDate date, CabinClass cabin,
                                        int adults, int children, int infants) {
        return new SearchCriteria(TripType.ONE_WAY, origin, destination, date, null, cabin, adults, children, infants);
    }

    public static SearchCriteria roundTrip(Airport origin, Airport destination, LocalDate departureDate,
                                           LocalDate returnDate, CabinClass cabin,
                                           int adults, int children, int infants) {
        return new SearchCriteria(TripType.ROUND_TRIP, origin, destination, departureDate, returnDate, cabin,
                adults, children, infants);
    }

    public void validate() throws AirlineException {
        if (tripType == null || origin == null || destination == null || departureDate == null || cabin == null) {
            throw new InvalidRequestException("여정, 출발지, 도착지, 가는 날, 좌석 등급을 모두 선택하세요.");
        }
        if (origin == destination) {
            throw new InvalidRequestException("출발지와 도착지가 같습니다.");
        }
        if (isRoundTrip()) {
            if (returnDate == null) {
                throw new InvalidRequestException("왕복은 오는 날을 선택해야 합니다.");
            }
            if (returnDate.isBefore(departureDate)) {
                throw new InvalidRequestException("오는 날은 가는 날과 같거나 그 이후여야 합니다.");
            }
        }
        if (adults < 0 || children < 0 || infants < 0) {
            throw new InvalidPassengerException("인원 수는 음수일 수 없습니다.");
        }
        if (adults < 1) {
            throw new InvalidPassengerException("성인이 1명 이상 포함되어야 합니다. (소아·유아 단독 예약은 지원하지 않습니다)");
        }
        if (getSeatedCount() > MAX_SEATED_PASSENGERS) {
            throw new InvalidPassengerException("좌석을 사용하는 승객(성인+소아)은 최대 " + MAX_SEATED_PASSENGERS + "명까지 예약할 수 있습니다.");
        }
        if (infants > adults) {
            throw new InvalidPassengerException("유아는 성인 1명당 1명까지 동반할 수 있습니다. (유아 " + infants + "명, 성인 " + adults + "명)");
        }
    }

    public boolean isRoundTrip() {
        return tripType == TripType.ROUND_TRIP;
    }

    public int getSeatedCount() {
        return adults + children;
    }

    public int getTotalCount() {
        return adults + children + infants;
    }

    /** 승객 정보 입력 순서: 성인 → 소아 → 유아 */
    public List<PassengerType> toPassengerTypes() {
        List<PassengerType> types = new ArrayList<>();
        types.addAll(Collections.nCopies(adults, PassengerType.ADULT));
        types.addAll(Collections.nCopies(children, PassengerType.CHILD));
        types.addAll(Collections.nCopies(infants, PassengerType.INFANT));
        return types;
    }

    /** 예) "성인 2, 소아 1" */
    public String describePassengers() {
        StringBuilder sb = new StringBuilder("성인 " + adults);
        if (children > 0) {
            sb.append(", 소아 ").append(children);
        }
        if (infants > 0) {
            sb.append(", 유아 ").append(infants);
        }
        return sb.toString();
    }

    /** 예) "서울/김포 → 제주 · 왕복 11월 13일 (금) ~ 11월 16일 (월) · 성인 2 · 일반석" */
    public String describe() {
        String dates = Formats.shortDate(departureDate);
        if (isRoundTrip() && returnDate != null) {
            dates += " ~ " + Formats.shortDate(returnDate);
        }
        return String.format("%s → %s · %s %s · %s · %s",
                origin.getCityName(), destination.getCityName(), tripType.getDisplayName(), dates,
                describePassengers(), cabin.getDisplayName());
    }
}
