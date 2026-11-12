package airline.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 프로그램 전체가 "현재 시각"을 얻는 유일한 창구.
 *
 * LocalDateTime.now()를 여기저기서 직접 부르면 체크인 가능 시간이나 취소 수수료 변화를
 * 시연할 방법이 없다. 시각을 한 곳에서 관리하면 발표 때 시간을 앞으로 돌려 보여줄 수 있고,
 * 테스트에서는 고정 시각을 넣어 항상 같은 결과를 얻을 수 있다.
 */
public class AppClock {

    private final LocalDateTime fixedBase;   // null이면 실제 시스템 시각을 기준으로 한다
    private Duration offset = Duration.ZERO;
    private final List<Runnable> listeners = new ArrayList<>();

    /** 실제 시스템 시각을 따르는 시계 */
    public AppClock() {
        this(null);
    }

    private AppClock(LocalDateTime fixedBase) {
        this.fixedBase = fixedBase;
    }

    /** 테스트용: 항상 같은 시각에서 시작하는 시계 */
    public static AppClock fixedAt(LocalDateTime time) {
        return new AppClock(time);
    }

    public LocalDateTime now() {
        LocalDateTime base = (fixedBase != null) ? fixedBase : LocalDateTime.now();
        return base.plus(offset).withSecond(0).withNano(0);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    /** 시연용: 시각을 앞으로 이동한다. 과거로는 되돌릴 수 없다. */
    public void advance(Duration amount) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("시각은 앞으로만 이동할 수 있습니다.");
        }
        offset = offset.plus(amount);
        notifyListeners();
    }

    /** 이동한 시간을 모두 되돌린다. */
    public void reset() {
        offset = Duration.ZERO;
        notifyListeners();
    }

    public boolean isShifted() {
        return !offset.isZero();
    }

    /** 시각을 옮길 때마다 호출될 동작 등록 (MainFrame이 현재 페이지를 다시 그릴 때 사용) */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    /** 창을 닫을 때 등록을 해제한다 (로그아웃 후 닫힌 창이 계속 불리지 않도록) */
    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
