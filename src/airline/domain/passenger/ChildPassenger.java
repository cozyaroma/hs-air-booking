package airline.domain.passenger;

import airline.domain.flight.Flight;
import airline.exception.InvalidPassengerException;

import java.time.LocalDate;

/** 소아 승객: 만 2세 이상 ~ 성인 기준 나이 미만. 성인 운임의 75%. */
public class ChildPassenger extends Passenger {

    public static final double FARE_RATE = 0.75;
    public static final int MIN_AGE = 2;

    public ChildPassenger(String lastName, String firstName, Gender gender, LocalDate birthDate,
                          String passportNumber) throws InvalidPassengerException {
        super(lastName, firstName, gender, birthDate, passportNumber);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.CHILD;
    }

    @Override
    public double getFareRate(Flight flight) {
        return FARE_RATE;
    }

    @Override
    public void validateAge(Flight flight) throws InvalidPassengerException {
        int age = ageOn(flight.getDepartureDate());
        if (age < MIN_AGE || age >= flight.getAdultMinAge()) {
            throw ageMismatch(flight, age, "만 " + MIN_AGE + "세 이상 만 " + flight.getAdultMinAge() + "세 미만");
        }
    }
}
