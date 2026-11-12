package airline.domain.seat;

import airline.domain.passenger.Passenger;
import airline.exception.SeatRestrictionException;

import java.time.LocalDate;

/**
 * 비상구 열 좌석. 일반석의 한 종류이므로 EconomySeat를 상속한다.
 *
 * 실제 항공사 규정: 비상시 승무원을 도와야 하므로 만 15세 이상만 앉을 수 있고,
 * 유아를 동반한 승객은 앉을 수 없다. 다리 공간이 넓어 유료로 판매하는 항공사가 많다.
 *
 * [상속·다형성] getExtraFee()와 checkOccupant()를 오버라이딩한다.
 *              BookingService는 좌석 종류를 몰라도 seat.checkOccupant()만 호출하면 된다.
 */
public class ExitRowSeat extends EconomySeat {

    public static final int MIN_AGE = 15;
    public static final long EXTRA_FEE = 30_000;

    public ExitRowSeat(int row, char column) {
        super(row, column);
    }

    @Override
    public long getExtraFee() {
        return EXTRA_FEE;
    }

    @Override
    public String getDescription() {
        return "비상구석 (다리 공간 넓음)";
    }

    @Override
    public void checkOccupant(Passenger passenger, LocalDate travelDate) throws SeatRestrictionException {
        super.checkOccupant(passenger, travelDate);   // 부모의 기본 규칙도 그대로 지킨다

        int age = passenger.ageOn(travelDate);
        if (age < MIN_AGE) {
            throw new SeatRestrictionException(String.format(
                    "비상구 좌석은 만 %d세 이상만 앉을 수 있습니다. (%s: 탑승일 기준 만 %d세)",
                    MIN_AGE, passenger.getFullName(), age));
        }
        if (passenger.isAccompanyingInfant()) {
            throw new SeatRestrictionException(
                    "유아를 동반한 승객은 비상구 좌석에 앉을 수 없습니다. (" + passenger.getFullName() + ")");
        }
    }
}
