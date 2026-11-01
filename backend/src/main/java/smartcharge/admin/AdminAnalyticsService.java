package smartcharge.admin;

import org.springframework.stereotype.Service;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingRequestRepository;
import smartcharge.charging.ChargingSession;
import smartcharge.charging.ChargingSessionRepository;
import smartcharge.station.Charger;
import smartcharge.station.ChargerRepository;
import smartcharge.station.Station;
import smartcharge.station.StationRepository;
import smartcharge.payment.BookingPayment;
import smartcharge.payment.BookingPaymentRepository;
import smartcharge.payment.Payment;
import smartcharge.payment.PaymentRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d");

    private final ChargingRequestRepository requestRepository;
    private final ChargingSessionRepository sessionRepository;
    private final ChargerRepository chargerRepository;
    private final StationRepository stationRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final PaymentRepository paymentRepository;

    public AdminAnalyticsService(ChargingRequestRepository requestRepository,
                                 ChargingSessionRepository sessionRepository,
                                 ChargerRepository chargerRepository,
                                 StationRepository stationRepository,
                                 BookingPaymentRepository bookingPaymentRepository,
                                 PaymentRepository paymentRepository) {
        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
        this.chargerRepository = chargerRepository;
        this.stationRepository = stationRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.paymentRepository = paymentRepository;
    }

    public AnalyticsResponse getAnalytics(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.toLocalDate().minusDays(days - 1L).atStartOfDay();
        List<ChargingRequest> requests = requestRepository.findAll().stream()
                .filter(request -> request.getArrivalTime() != null && !request.getArrivalTime().isBefore(from))
                .toList();
        List<ChargingSession> sessions = sessionRepository.findAll();
        List<ChargingSession> periodSessions = sessions.stream()
                .filter(session -> session.getStartTime() != null && !session.getStartTime().isBefore(from))
                .toList();
        List<ChargingSession> completedInPeriod = sessions.stream()
                .filter(session -> "COMPLETED".equalsIgnoreCase(session.getStatus())
                        && session.getEndTime() != null && !session.getEndTime().isBefore(from))
                .toList();
        List<Charger> chargers = chargerRepository.findAll();

        double averageWait = periodSessions.stream()
                .filter(session -> session.getRequest() != null && session.getRequest().getArrivalTime() != null)
                .mapToDouble(session -> Math.max(0, Duration.between(
                        session.getRequest().getArrivalTime(), session.getStartTime()).toSeconds() / 60.0))
                .average().orElse(0);

        double energy = completedInPeriod.stream().map(ChargingSession::getEnergyUsed)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        double utilization = utilization(sessions, from, now, chargers.size());

        Map<String, Long> statusCounts = requests.stream().collect(Collectors.groupingBy(
                request -> normalizeStatus(request.getStatus()), TreeMap::new, Collectors.counting()));
        List<StatusMetric> statuses = statusCounts.entrySet().stream()
                .map(entry -> new StatusMetric(entry.getKey(), entry.getValue())).toList();

        List<DailyMetric> daily = new ArrayList<>();
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate date = now.toLocalDate().minusDays(offset);
            long requestCount = requests.stream().filter(request -> request.getArrivalTime().toLocalDate().equals(date)).count();
            long completedCount = completedInPeriod.stream()
                    .filter(session -> session.getEndTime().toLocalDate().equals(date)).count();
            daily.add(new DailyMetric(date.toString(), date.format(DAY_LABEL), requestCount, completedCount));
        }

        Map<Long, List<Charger>> chargersByStation = chargers.stream().filter(charger -> charger.getStation() != null)
                .collect(Collectors.groupingBy(charger -> charger.getStation().getId()));
        Map<Long, Double> revenueByStation = revenueByStation();
        List<StationMetric> stationMetrics = stationRepository.findAll().stream().map(station -> {
            Long stationId = station.getId();
            List<Charger> stationChargers = chargersByStation.getOrDefault(stationId, List.of());
            double stationUtilization = utilization(sessions, from, now, stationChargers.size(), stationId);
            long completed = completedInPeriod.stream().filter(session -> session.getCharger() != null
                    && session.getCharger().getStation() != null
                    && Objects.equals(session.getCharger().getStation().getId(), stationId)).count();
            return new StationMetric(stationId, station.getName(), stationChargers.size(),
                    round(stationUtilization, 1), completed, round(revenueByStation.getOrDefault(stationId, 0.0), 2));
        }).sorted(Comparator.comparing(StationMetric::stationName, String.CASE_INSENSITIVE_ORDER)).toList();
        double totalRevenue = revenueByStation.values().stream().mapToDouble(Double::doubleValue).sum();

        long completedRequests = requests.stream().filter(request -> "COMPLETED".equalsIgnoreCase(request.getStatus())).count();
        long pendingRequests = requests.stream().filter(request -> "PENDING".equalsIgnoreCase(request.getStatus())).count();
        long activeSessions = sessions.stream().filter(session -> "ACTIVE".equalsIgnoreCase(session.getStatus())).count();

        return new AnalyticsResponse(days, now,
                new Summary(requests.size(), completedRequests, pendingRequests, activeSessions,
                        round(averageWait, 1), round(energy, 2), round(utilization, 1), chargers.size()),
                daily, statuses, stationMetrics, round(totalRevenue, 2));
    }

    private Map<Long, Double> revenueByStation() {
        Map<Long, Double> revenue = new HashMap<>();
        for (BookingPayment payment : bookingPaymentRepository.findAll()) {
            if ("PAID".equalsIgnoreCase(payment.getStatus()) && payment.getRequest().getStation() != null) {
                revenue.merge(payment.getRequest().getStation().getId(), payment.getAmount(), Double::sum);
            }
        }
        for (Payment payment : paymentRepository.findAll()) {
            if (!"PAID".equalsIgnoreCase(payment.getStatus()) || payment.getSession() == null) continue;
            var session = payment.getSession();
            var charger = session.getCharger();
            var station = charger == null ? null : charger.getStation();
            if (station == null && session.getRequest() != null) station = session.getRequest().getStation();
            if (station != null) revenue.merge(station.getId(), payment.getAmount(), Double::sum);
        }
        return revenue;
    }

    private double utilization(List<ChargingSession> sessions, LocalDateTime from, LocalDateTime to,
                               int chargerCount) {
        return utilization(sessions, from, to, chargerCount, null);
    }

    private double utilization(List<ChargingSession> sessions, LocalDateTime from, LocalDateTime to,
                               int chargerCount, Long stationId) {
        if (chargerCount == 0) return 0;
        double occupiedMinutes = sessions.stream()
                .filter(session -> session.getStartTime() != null)
                .filter(session -> session.getCharger() != null)
                .filter(session -> stationId == null || (session.getCharger().getStation() != null
                        && Objects.equals(session.getCharger().getStation().getId(), stationId)))
                .mapToDouble(session -> {
                    LocalDateTime sessionEnd = session.getEndTime() == null || session.getEndTime().isAfter(to)
                            ? to : session.getEndTime();
                    LocalDateTime overlapStart = session.getStartTime().isBefore(from) ? from : session.getStartTime();
                    return sessionEnd.isAfter(overlapStart)
                            ? Duration.between(overlapStart, sessionEnd).toMinutes() : 0;
                }).sum();
        double availableMinutes = Duration.between(from, to).toMinutes() * chargerCount;
        return availableMinutes <= 0 ? 0 : Math.min(100, occupiedMinutes * 100 / availableMinutes);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "UNKNOWN" : status.trim().toUpperCase(Locale.ROOT);
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public record AnalyticsResponse(int periodDays, LocalDateTime generatedAt, Summary summary,
                                    List<DailyMetric> daily, List<StatusMetric> statuses,
                                    List<StationMetric> stations, double totalRevenue) { }
    public record Summary(long totalRequests, long completedRequests, long pendingRequests,
                          long activeSessions, double averageWaitingMinutes, double energyKwh,
                          double chargerUtilizationPercent, long totalChargers) { }
    public record DailyMetric(String date, String label, long requests, long completedSessions) { }
    public record StatusMetric(String status, long count) { }
    public record StationMetric(long stationId, String stationName, long chargerCount,
                                double utilizationPercent, long completedSessions, double revenue) { }
}
