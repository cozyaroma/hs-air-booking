package airline.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * AI에게 줄 "작업 자료 묶음" 파일을 만드는 도구 (공용 파일).
 *
 * 저장소 파일을 직접 읽지 못하는 AI(웹 채팅형 ChatGPT, Gemini, Claude 등)를 쓰는 사람을 위한 것이다.
 * 담당 페이지의 요구사항 문서 + 작업 규칙 + 꼭 알아야 하는 공용 코드 + 지금 내 코드를 파일 하나로 모은다.
 *
 * 사용법:  context.bat B     (macOS/Linux: ./context.sh B)
 *   → 프로젝트 폴더에 context-pack-B.md 가 생긴다. 이 파일을 AI 채팅에 첨부하고
 *     docs/AI-GUIDE.md의 "첫 메시지" 템플릿을 붙여 넣는다.
 */
public final class ContextPack {

    private static final List<String> COMMON_DOCS = List.of(
            "AGENTS.md", "docs/ARCHITECTURE.md");

    private static final List<String> COMMON_CODE = List.of(
            "src/airline/ui/AppContext.java",
            "src/airline/ui/BasePanel.java",
            "src/airline/ui/BookingSession.java",
            "src/airline/ui/Screen.java",
            "src/airline/ui/UiTheme.java",
            "src/airline/util/Formats.java",
            "src/airline/exception/AirlineException.java",
            "src/airline/domain/booking/Itinerary.java",
            "src/airline/domain/booking/SegmentType.java",
            "src/airline/domain/flight/Flight.java");

    private ContextPack() {
    }

    public static void main(String[] args) throws IOException {
        String owner = args.length > 0 ? args[0].trim().toUpperCase(Locale.ROOT) : "";
        List<String> docs = new ArrayList<>(COMMON_DOCS);
        List<String> code = new ArrayList<>(COMMON_CODE);
        String ownPackage;
        String ownTest;
        switch (owner) {
            case "A" -> {
                docs.add("docs/pages/A-search.md");
                code.addAll(List.of(
                        "src/airline/service/FlightSearchService.java",
                        "src/airline/service/SearchCriteria.java",
                        "src/airline/service/SearchResult.java",
                        "src/airline/domain/booking/TripType.java",
                        "src/airline/domain/flight/Airport.java",
                        "src/airline/domain/flight/TimeBand.java",
                        "src/airline/domain/passenger/AgeCalculator.java",
                        "src/airline/domain/passenger/PassengerType.java",
                        "src/airline/domain/seat/CabinClass.java"));
                ownPackage = "src/airline/ui/page/search";
                ownTest = "test/airline/SearchTest.java";
            }
            case "B" -> {
                docs.add("docs/pages/B-passenger-seat.md");
                code.addAll(List.of(
                        "src/airline/service/BookingService.java",
                        "src/airline/service/SearchCriteria.java",
                        "src/airline/domain/passenger/Passenger.java",
                        "src/airline/domain/passenger/PassengerFactory.java",
                        "src/airline/domain/passenger/PassengerType.java",
                        "src/airline/domain/passenger/Gender.java",
                        "src/airline/domain/passenger/AgeCalculator.java",
                        "src/airline/domain/passenger/AdultPassenger.java",
                        "src/airline/domain/passenger/InfantPassenger.java",
                        "src/airline/domain/seat/Seat.java",
                        "src/airline/domain/seat/ExitRowSeat.java",
                        "src/airline/domain/seat/CabinClass.java",
                        "src/airline/domain/flight/AircraftType.java"));
                ownPackage = "src/airline/ui/page/passenger";
                ownTest = "test/airline/PassengerSeatTest.java";
            }
            case "C" -> {
                docs.add("docs/pages/C-payment-booking.md");
                code.addAll(List.of(
                        "src/airline/service/BookingService.java",
                        "src/airline/service/BookingRequest.java",
                        "src/airline/domain/booking/Booking.java",
                        "src/airline/domain/booking/BookingSegment.java",
                        "src/airline/domain/booking/Ticket.java",
                        "src/airline/domain/booking/PriceBreakdown.java",
                        "src/airline/domain/booking/CancellationQuote.java",
                        "src/airline/payment/PaymentMethod.java",
                        "src/airline/payment/AbstractPayment.java",
                        "src/airline/payment/CreditCardPayment.java",
                        "src/airline/payment/BankTransferPayment.java",
                        "src/airline/payment/SimplePayment.java",
                        "src/airline/payment/PaymentFormats.java",
                        "src/airline/policy/BaggagePolicy.java",
                        "src/airline/policy/CancellationPolicy.java"));
                ownPackage = "src/airline/ui/page/payment";
                ownTest = "test/airline/PaymentBookingTest.java";
            }
            default -> {
                System.out.println("사용법: context.bat A   (A, B, C 중 하나)");
                return;
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("# 작업 자료 묶음 (담당 ").append(owner).append(")\n\n")
                .append("이 파일은 `context.bat ").append(owner).append("`로 자동 생성했습니다. ")
                .append("AI는 아래 규칙과 요구사항을 따르고, '내 코드' 부분의 파일만 수정합니다. ")
                .append("공용 코드는 읽기 전용입니다.\n\n");
        out.append("## 1. 규칙과 요구사항\n\n");
        for (String doc : docs) {
            appendFile(out, doc, "markdown");
        }
        out.append("## 2. 공용 코드 (읽기 전용, 호출만 가능)\n\n");
        for (String file : code) {
            appendFile(out, file, "java");
        }
        out.append("## 3. 내 코드 (수정 대상)\n\n");
        try (Stream<Path> files = Files.walk(Path.of(ownPackage))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                appendFile(out, file.toString().replace('\\', '/'), "java");
            }
        }
        appendFile(out, ownTest, "java");

        Path target = Path.of("context-pack-" + owner + ".md");
        Files.writeString(target, out.toString(), StandardCharsets.UTF_8);
        System.out.printf("[ContextPack] %s 생성 (%,d자). AI 채팅에 이 파일을 첨부하세요.%n",
                target.toAbsolutePath(), out.length());
    }

    private static void appendFile(StringBuilder out, String path, String language) throws IOException {
        Path file = Path.of(path);
        out.append("### `").append(path).append("`\n\n");
        if (!Files.exists(file)) {
            out.append("(파일 없음)\n\n");
            return;
        }
        String fence = language.equals("markdown") ? "````" : "```";
        out.append(fence).append(language).append('\n')
                .append(Files.readString(file, StandardCharsets.UTF_8).stripTrailing())
                .append('\n').append(fence).append("\n\n");
    }
}
