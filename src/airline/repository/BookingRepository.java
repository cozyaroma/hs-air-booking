package airline.repository;

import airline.domain.booking.Booking;

import java.util.List;
import java.util.Optional;

/** 예약 저장소 */
public interface BookingRepository {

    void save(Booking booking);

    Optional<Booking> findByCode(String bookingCode);

    boolean exists(String bookingCode);

    /** 회원 한 명의 예약 목록 (저장한 순서) */
    List<Booking> findByOwner(String username);

    List<Booking> findAll();
}
