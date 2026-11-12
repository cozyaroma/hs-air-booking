package airline.repository;

import airline.domain.booking.Booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 메모리(Map)에 예약을 저장하는 구현. 예약번호로 바로 찾을 수 있도록 Map을 사용했다. */
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    @Override
    public void save(Booking booking) {
        bookings.put(booking.getBookingCode(), booking);
    }

    @Override
    public Optional<Booking> findByCode(String bookingCode) {
        return Optional.ofNullable(bookings.get(bookingCode));
    }

    @Override
    public boolean exists(String bookingCode) {
        return bookings.containsKey(bookingCode);
    }

    @Override
    public List<Booking> findByOwner(String username) {
        return bookings.values().stream()
                .filter(booking -> booking.getOwnerUsername().equals(username))
                .toList();
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(bookings.values());
    }
}
