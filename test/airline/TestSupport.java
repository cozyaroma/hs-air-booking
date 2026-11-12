package airline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 아주 작은 테스트 도구 (외부 라이브러리 없이 JDK만으로 실행하기 위해 직접 만들었다).
 *
 *   test("이름", () -> { ... check(조건, "실패 시 메시지"); ... });
 *   AirlineException e = expectThrows(InvalidRequestException.class, () -> ...);
 */
public final class TestSupport {

    @FunctionalInterface
    public interface Body {
        void run() throws Exception;
    }

    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    private TestSupport() {
    }

    public static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    public static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (AssertionError | Exception e) {
            failures.add(name + " → " + e);
            System.out.println("  FAIL  " + name);
            System.out.println("        " + e);
            StackTraceElement where = findTestLine(e);
            if (where != null) {
                System.out.println("        at " + where);
            }
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void equal(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(what + ": 기대값 <" + expected + "> 실제값 <" + actual + ">");
        }
    }

    /** body가 type 예외를 던져야 통과. 던진 예외를 돌려주므로 메시지도 확인할 수 있다. */
    public static <T extends Throwable> T expectThrows(Class<T> type, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            throw new AssertionError(type.getSimpleName() + "이(가) 나와야 하는데 " + t + "이(가) 나왔습니다.", t);
        }
        throw new AssertionError(type.getSimpleName() + "이(가) 나와야 하는데 아무 예외도 나오지 않았습니다.");
    }

    /** 결과 요약을 출력하고 실패 개수를 돌려준다 */
    public static int finish() {
        System.out.println();
        System.out.println("==================================================");
        System.out.printf("결과: %d개 통과, %d개 실패%n", passed, failures.size());
        failures.forEach(f -> System.out.println("  실패: " + f));
        System.out.println("==================================================");
        return failures.size();
    }

    private static StackTraceElement findTestLine(Throwable e) {
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().startsWith("airline.") && element.getClassName().endsWith("Test")) {
                return element;
            }
        }
        return null;
    }
}
