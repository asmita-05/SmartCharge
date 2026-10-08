package smartcharge.scheduler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartcharge.charging.ChargingRequest;
import smartcharge.charging.ChargingRequestRepository;
import smartcharge.charging.ChargingSession;
import smartcharge.charging.ChargingSessionRepository;
import smartcharge.station.Charger;
import smartcharge.station.ChargerRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ChargingSchedulerService {

    private final ChargingRequestRepository requestRepository;
    private final ChargingSessionRepository sessionRepository;
    private final ChargerRepository chargerRepository;
    private final SchedulerService schedulerService;

    public ChargingSchedulerService(
            ChargingRequestRepository requestRepository,
            ChargingSessionRepository sessionRepository,
            ChargerRepository chargerRepository,
            SchedulerService schedulerService) {

        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
        this.chargerRepository = chargerRepository;
        this.schedulerService = schedulerService;
    }

    @Transactional
    public Optional<ChargingSession> processNextRequest() {

        List<ChargingRequest> pendingRequests =
                requestRepository.findByStatus("PENDING").stream()
                        .filter(this::isSchedulerEligible)
                        .toList();

        if (pendingRequests.isEmpty()) {
            return Optional.empty();
        }

        /*
         * Calculate estimated duration before scheduling.
         * This allows SJF to compare actual charging times.
         */
        for (ChargingRequest request : pendingRequests) {

            Optional<Charger> charger =
                    findCompatibleCharger(request);

            if (charger.isPresent()) {

                double estimatedMinutes =
                        calculateChargingMinutes(
                                request,
                                charger.get());

                request.setEstimatedMinutes(
                        estimatedMinutes);
            }
        }

        List<ChargingRequest> scheduledRequests =
                schedulerService.schedule(pendingRequests);

        // Approved emergencies outrank the configured queue discipline; the selected
        // FCFS/Priority/SJF policy still orders all requests within each group.
        scheduledRequests = java.util.stream.Stream.concat(
                scheduledRequests.stream().filter(this::isApprovedPriority),
                scheduledRequests.stream().filter(request -> !isApprovedPriority(request)))
                .toList();

        for (ChargingRequest request : scheduledRequests) {

            Optional<ChargingRequest> lockedRequest = requestRepository.findByIdForUpdate(request.getId());
            if (lockedRequest.isEmpty() || !"PENDING".equalsIgnoreCase(lockedRequest.get().getStatus())
                    || !isSchedulerEligible(lockedRequest.get())) continue;
            request = lockedRequest.get();

            Optional<Charger> chargerOptional =
                    findAndLockCompatibleCharger(request);

            if (chargerOptional.isEmpty()) {
                continue;
            }

            Charger charger = chargerOptional.get();

            /*
             * Re-check status after acquiring
             * the database lock.
             */
            if (!"AVAILABLE".equalsIgnoreCase(
                    charger.getStatus())) {

                continue;
            }

            double estimatedMinutes =
                    calculateChargingMinutes(
                            request,
                            charger);

            request.setEstimatedMinutes(
                    estimatedMinutes);

            return Optional.of(allocate(request, charger));
        }

        return Optional.empty();
    }

    /** Called synchronously after Admin approves an emergency/priority request. */
    @Transactional
    public Optional<ChargingSession> processPriorityRequest(Long requestId) {
        ChargingRequest priorityRequest = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Charging request not found"));
        if (!"PENDING".equalsIgnoreCase(priorityRequest.getStatus())
                || !"APPROVED".equalsIgnoreCase(priorityRequest.getEmergencyStatus())) {
            return Optional.empty();
        }

        Optional<Charger> available = findAndLockCompatibleCharger(priorityRequest);
        if (available.isPresent() && "AVAILABLE".equalsIgnoreCase(available.get().getStatus())) {
            return Optional.of(allocate(priorityRequest, available.get()));
        }

        List<Charger> occupied = chargerRepository.findByStatus("OCCUPIED").stream()
                .filter(charger -> isCompatible(priorityRequest, charger))
                .sorted(Comparator.comparing(Charger::getId))
                .toList();
        for (Charger candidate : occupied) {
            Optional<Charger> locked = chargerRepository.findByIdForUpdate(candidate.getId());
            if (locked.isEmpty() || !"OCCUPIED".equalsIgnoreCase(locked.get().getStatus())) continue;
            Optional<ChargingSession> activeSession = sessionRepository
                    .findFirstByChargerIdAndStatusOrderByStartTimeAsc(locked.get().getId(), "ACTIVE");
            if (activeSession.isEmpty()) continue;

            ChargingSession interrupted = activeSession.get();
            ChargingRequest displaced = interrupted.getRequest();
            if (displaced == null || isPriority(displaced)) continue;

            interruptSession(interrupted, displaced, locked.get());
            return Optional.of(allocate(priorityRequest, locked.get()));
        }
        return Optional.empty();
    }

    private boolean isSchedulerEligible(ChargingRequest request) {
        if (!Boolean.TRUE.equals(request.getEmergencyRequested())) return true;
        String emergencyStatus = request.getEmergencyStatus();
        return "APPROVED".equalsIgnoreCase(emergencyStatus)
                || "DECLINED".equalsIgnoreCase(emergencyStatus);
    }

    private boolean isPriority(ChargingRequest request) {
        return "APPROVED".equalsIgnoreCase(request.getEmergencyStatus())
                || "HIGH".equalsIgnoreCase(request.getPriority());
    }

    private boolean isApprovedPriority(ChargingRequest request) {
        return Boolean.TRUE.equals(request.getEmergencyRequested())
                && "APPROVED".equalsIgnoreCase(request.getEmergencyStatus());
    }

    private void interruptSession(ChargingSession session, ChargingRequest request, Charger charger) {
        LocalDateTime now = LocalDateTime.now();
        double minutes = session.getStartTime() == null ? 0
                : Math.max(0, Duration.between(session.getStartTime(), now).toSeconds() / 60.0);
        double energy = charger.getPowerKw() == null ? 0 : charger.getPowerKw() * minutes / 60.0;
        Double capacity = request.getVehicle() == null ? null : request.getVehicle().getBatteryCapacity();
        if (capacity != null && capacity > 0) {
            double remainingEnergy = capacity * (request.getTargetBattery() - request.getCurrentBattery()) / 100.0;
            energy = Math.min(energy, Math.max(0, remainingEnergy));
            request.setCurrentBattery(Math.min(request.getTargetBattery(),
                    request.getCurrentBattery() + energy / capacity * 100.0));
        }
        session.setEndTime(now);
        session.setStatus("INTERRUPTED");
        session.setActualMinutes(minutes);
        session.setEnergyUsed(energy);
        sessionRepository.save(session);
        request.setStatus("PENDING");
        request.setEstimatedMinutes(null);
        requestRepository.save(request);
    }

    private ChargingSession allocate(ChargingRequest request, Charger charger) {
        request.setEstimatedMinutes(calculateChargingMinutes(request, charger));
        request.setStatus("ALLOCATED");
        charger.setStatus("OCCUPIED");
        chargerRepository.save(charger);
        requestRepository.save(request);
        ChargingSession session = new ChargingSession();
        session.setRequest(request);
        session.setCharger(charger);
        session.setStatus("ACTIVE");
        return sessionRepository.save(session);
    }

    private Optional<Charger> findCompatibleCharger(
            ChargingRequest request) {

        if (request.getRequestedCharger() != null) {
            return chargerRepository.findById(request.getRequestedCharger().getId())
                    .filter(charger -> isCompatible(request, charger));
        }

        List<Charger> chargers =
                chargerRepository.findByStatus("AVAILABLE");

        return chargers.stream()
                .filter(charger ->
                        isCompatible(request, charger))
                .min(Comparator.comparing(Charger::getId));
    }

    private Optional<Charger> findAndLockCompatibleCharger(
            ChargingRequest request) {

        if (request.getRequestedCharger() != null) {
            return chargerRepository.findByIdForUpdate(request.getRequestedCharger().getId())
                    .filter(charger -> isCompatible(request, charger));
        }

        String connectorType =
                request.getRequiredConnectorType();

        if (connectorType == null
                || connectorType.isBlank()) {

            return chargerRepository
                    .findFirstByStatusOrderByIdAsc(
                            "AVAILABLE");
        }

        return chargerRepository
                .findFirstByStatusAndConnectorTypeOrderByIdAsc(
                        "AVAILABLE",
                        connectorType);
    }

    private boolean isCompatible(
            ChargingRequest request,
            Charger charger) {

        if (request.getRequestedCharger() != null
                && !request.getRequestedCharger().getId().equals(charger.getId())) {
            return false;
        }

        if (request.getStation() != null && (charger.getStation() == null
                || !request.getStation().getId().equals(charger.getStation().getId()))) {
            return false;
        }

        String requiredConnector =
                request.getRequiredConnectorType();

        if (requiredConnector == null
                || requiredConnector.isBlank()) {

            return true;
        }

        String chargerConnector =
                charger.getConnectorType();

        return chargerConnector != null
                && requiredConnector.equalsIgnoreCase(
                        chargerConnector);
    }

    private double calculateChargingMinutes(
            ChargingRequest request,
            Charger charger) {

        if (request.getVehicle() == null
                || request.getVehicle()
                .getBatteryCapacity() == null
                || request.getVehicle()
                .getBatteryCapacity() <= 0) {

            throw new RuntimeException(
                    "Valid vehicle battery capacity is required");
        }

        if (charger.getPowerKw() == null
                || charger.getPowerKw() <= 0) {

            throw new RuntimeException(
                    "Valid charger power is required");
        }

        double batteryPercentage =
                request.getTargetBattery()
                        - request.getCurrentBattery();

        double energyRequired =
                request.getVehicle()
                        .getBatteryCapacity()
                        * (batteryPercentage / 100.0);

        double hours =
                energyRequired
                        / charger.getPowerKw();

        return hours * 60.0;
    }
}
