package airline.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** 화면과 메시지에서 공통으로 쓰는 날짜·금액 표시 형식 */
public final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd (E) HH:mm", Locale.KOREAN);

    private Formats() {
    }

    public static String money(long won) {
        return String.format("%,d원", won);
    }

    /** 100원 단위로 반올림한다. 운임과 수수료 계산에 사용한다. */
    public static long round100(double won) {
        return Math.round(won / 100.0) * 100;
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    public static String shortDate(LocalDate date) {
        return date.format(SHORT_DATE);
    }

    public static String time(LocalDateTime time) {
        return time.format(TIME);
    }

    public static String dateTime(LocalDateTime time) {
        return time.format(DATE_TIME);
    }

    public static String duration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours == 0) {
            return minutes + "분";
        }
        return minutes == 0 ? hours + "시간" : hours + "시간 " + minutes + "분";
    }

    /** 취소 수수료 안내용: "3일 5시간 전" 같은 형태 */
    public static String remaining(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        if (days > 0) {
            return days + "일 " + hours + "시간";
        }
        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }
        return minutes + "분";
    }
}
