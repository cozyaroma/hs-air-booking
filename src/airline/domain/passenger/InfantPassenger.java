package airline.domain.passenger;

import airline.domain.flight.Flight;
import airline.exception.InvalidPassengerException;

import java.time.LocalDate;

/**
 * 유아 승객: 만 2세 미만. 좌석 없이 보호자와 함께 탑승한다.
 * 운임은 국내선 무료, 국제선 성인 운임의 10%라서 Flight에게 비율을 물어본다.
 */
public class InfantPassenger extends Passenger {

    public static final int MAX_AGE_EXCLUSIVE = 2;
    public static final int MIN_DAYS_OLD = 7;   // 생후 7일부터 탑승 가능

    private AdultPassenger guardian;

    public InfantPassenger(String lastName, String firstName, Gender gender, LocalDate birthDate,
                          String passportNumber) throws InvalidPassengerException {
        super(lastName, firstName, gender, birthDate, passportNumber);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.INFANT;
    }

    @Override
    public double getFareRate(Flight flight) {
        return flight.getInfantFareRate();
    }

    @Override
    public void validateAge(Flight flight) throws InvalidPassengerException {
        LocalDate travelDate = flight.getDepartureDate();
        int age = ageOn(travelDate);
        if (age >= MAX_AGE_EXCLUSIVE) {
            throw ageMismatch(flight, age, "만 2세 미만 (좌석이 필요하면 소아로 예약)");
        }
        if (daysOldOn(travelDate) < MIN_DAYS_OLD) {
            throw new InvalidPassengerException(
                    getFullName() + " 유아는 탑승일 기준 생후 " + MIN_DAYS_OLD + "일이 지나야 탑승할 수 있습니다.");
        }
    }

    @Override
    public boolean occupiesSeat() {
        return false;
    }

    @Override
    public boolean paysTax() {
        return false;
    }

    public AdultPassenger getGuardian() {
        return guardian;
    }

    /** AdultPassenger.attachInfant()에서만 호출한다 (같은 패키지에서만 접근 가능) */
    void setGuardian(AdultPassenger guardian) {
        this.guardian = guardian;
    }
}
