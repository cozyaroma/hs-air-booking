package airline.domain.flight;

import java.time.LocalTime;

/**
 * 출발 시간대. 항공권 선택 화면에서 "오전 출발만 보기" 같은 필터에 사용한다.
 * 경계: 새벽 00~06시, 오전 06~12시, 오후 12~18시, 저녁 18~24시
 */
public enum TimeBand {
    DAWN("새벽", 0, 6),
    MORNING("오전", 6, 12),
    AFTERNOON("오후", 12, 18),
    EVENING("저녁", 18, 24);

    private final String displayName;
    private final int startHour;      // 포함
    private final int endHour;        // 미포함

    TimeBand(String displayName, int startHour, int endHour) {
        this.displayName = displayName;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public static TimeBand of(LocalTime time) {
        for (TimeBand band : values()) {
            if (time.getHour() >= band.startHour && time.getHour() < band.endHour) {
                return band;
            }
        }
        throw new IllegalStateException("시간대를 찾을 수 없습니다: " + time);
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 예) "오전 (06:00~12:00)" */
    public String describe() {
        return String.format("%s (%02d:00~%02d:00)", displayName, startHour, endHour);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
