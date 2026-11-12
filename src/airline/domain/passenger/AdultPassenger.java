package airline.domain.passenger;

import airline.domain.flight.Flight;
import airline.exception.InvalidPassengerException;

import java.time.LocalDate;

/** 성인 승객. 유아 1명의 보호자가 될 수 있다. */
public class AdultPassenger extends Passenger {

    private InfantPassenger infant;   // 동반하는 유아 (없으면 null)

    public AdultPassenger(String lastName, String firstName, Gender gender, LocalDate birthDate,
                          String passportNumber) throws InvalidPassengerException {
        super(lastName, firstName, gender, birthDate, passportNumber);
    }

    @Override
    public PassengerType getType() {
        return PassengerType.ADULT;
    }

    @Override
    public double getFareRate(Flight flight) {
        return 1.0;
    }

    @Override
    public void validateAge(Flight flight) throws InvalidPassengerException {
        int age = ageOn(flight.getDepartureDate());
        if (age < flight.getAdultMinAge()) {
            throw ageMismatch(flight, age, "만 " + flight.getAdultMinAge() + "세 이상");
        }
    }

    @Override
    public boolean isAccompanyingInfant() {
        return infant != null;
    }

    /** 이 항공권(운항편)에서 유아 보호자가 될 수 있는 나이인지 (국내선 만 13세, 국제선 만 18세 이상) */
    public boolean canBeGuardianOn(Flight flight) {
        return ageOn(flight.getDepartureDate()) >= flight.getGuardianMinAge();
    }

    /** 유아를 이 성인에게 연결한다. 성인 1명당 유아 1명까지만 가능하다. */
    public void attachInfant(InfantPassenger newInfant, Flight flight) throws InvalidPassengerException {
        if (infant != null && infant != newInfant) {
            throw new InvalidPassengerException("성인 1명당 유아 1명만 동반할 수 있습니다. (" + getFullName() + ")");
        }
        if (!canBeGuardianOn(flight)) {
            throw new InvalidPassengerException(String.format(
                    "%s 유아 보호자는 탑승일 기준 만 %d세 이상이어야 합니다. (%s: 만 %d세)",
                    flight.getTypeName(), flight.getGuardianMinAge(), getFullName(),
                    ageOn(flight.getDepartureDate())));
        }
        this.infant = newInfant;
        newInfant.setGuardian(this);
    }

    public InfantPassenger getInfant() {
        return infant;
    }
}
