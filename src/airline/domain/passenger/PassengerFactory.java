package airline.domain.passenger;

import airline.exception.InvalidPassengerException;

import java.time.LocalDate;

/**
 * 승객 유형에 맞는 객체를 만드는 팩토리.
 * 유형에 따른 분기는 이 한 곳에만 두고, 나머지 코드는 Passenger 타입으로만 다룬다.
 * 새 승객 유형이 생기면 하위 클래스 하나와 여기 한 줄만 추가하면 된다.
 */
public final class PassengerFactory {

    private PassengerFactory() {
    }

    /**
     * @param passportNumber 국내선은 null이나 빈 문자열이어도 된다
     * @throws InvalidPassengerException 이름이 영문이 아니거나 성별·생년월일이 없을 때
     */
    public static Passenger create(PassengerType type, String lastName, String firstName, Gender gender,
                                   LocalDate birthDate, String passportNumber) throws InvalidPassengerException {
        return switch (type) {
            case ADULT -> new AdultPassenger(lastName, firstName, gender, birthDate, passportNumber);
            case CHILD -> new ChildPassenger(lastName, firstName, gender, birthDate, passportNumber);
            case INFANT -> new InfantPassenger(lastName, firstName, gender, birthDate, passportNumber);
        };
    }
}
