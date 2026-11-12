package airline.service;

import airline.domain.booking.PriceBreakdown;
import airline.domain.flight.Flight;
import airline.domain.passenger.Passenger;
import airline.domain.seat.CabinClass;
import airline.domain.seat.Seat;
import airline.util.Formats;

/**
 * 승객 한 명의 요금을 계산한다 (단일 책임).
 *
 * 운임 = 일반석 성인 운임 × 좌석 등급 배율 × 승객 유형 비율
 * 세금 = 공항이용료 + 유류할증료 (유아 제외)
 * 좌석 요금 = 좌석 종류별 추가 요금 (비상구석 등)
 * 수하물 요금 = 노선별 수하물 규정 × 추가 단위
 *
 * 승객·좌석·항공권(국내선/국제선) 종류를 if문으로 나누지 않고, 각 객체에게 값을 물어보기만 한다(다형성).
 */
public class FareCalculator {

    public PriceBreakdown calculate(Flight flight, CabinClass cabin, Passenger passenger,
                                    Seat seat, int extraBaggageUnits) {
        long fare = Formats.round100(
                flight.getAdultEconomyFare() * cabin.getFareMultiplier() * passenger.getFareRate(flight));
        long tax = passenger.paysTax() ? flight.getTaxPerPassenger() : 0;
        long seatFee = (seat == null) ? 0 : seat.getExtraFee();
        long baggageFee = flight.getBaggagePolicy().calculateExtraFee(extraBaggageUnits);
        return new PriceBreakdown(fare, tax, seatFee, baggageFee);
    }

    /** 항공권 목록에 보여줄 성인 1인 요금 (운임 + 세금) */
    public long adultTotal(Flight flight, CabinClass cabin) {
        long fare = Formats.round100(flight.getAdultEconomyFare() * cabin.getFareMultiplier());
        return fare + flight.getTaxPerPassenger();
    }
}
