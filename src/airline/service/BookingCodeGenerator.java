package airline.service;

import java.util.Random;
import java.util.function.Predicate;

/**
 * 예약번호(6자리)와 항공권 번호를 만든다.
 * 예약번호는 헷갈리기 쉬운 문자(O, 0, I, 1)를 빼고 만든다.
 */
public class BookingCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final String AIRLINE_PREFIX = "988";   // 가상의 항공사 코드

    private final Random random;
    private long ticketSequence = 2_026_000_000L;

    public BookingCodeGenerator(Random random) {
        this.random = random;
    }

    /** 이미 쓰인 번호와 겹치지 않는 예약번호 */
    public String nextBookingCode(Predicate<String> alreadyUsed) {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (alreadyUsed.test(code));
        return code;
    }

    /** 예: 988-2026000001 */
    public String nextTicketNumber() {
        ticketSequence++;
        return AIRLINE_PREFIX + "-" + ticketSequence;
    }
}
