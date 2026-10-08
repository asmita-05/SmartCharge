package smartcharge.scheduler;

import smartcharge.charging.ChargingRequest;

import java.util.Comparator;
import java.util.List;

public class Priority implements Scheduler {

    @Override
    public List<ChargingRequest> schedule(List<ChargingRequest> requests) {

        return requests.stream()
                .sorted(
                        Comparator
                                .comparingInt(this::getPriorityValue)
                                .thenComparing(ChargingRequest::getArrivalTime)
                )
                .toList();
    }

    private int getPriorityValue(ChargingRequest request) {

        if ("APPROVED".equalsIgnoreCase(request.getEmergencyStatus())) {
            return 1;
        }

        if ("HIGH".equalsIgnoreCase(request.getPriority())) {
            return 2;
        }

        if ("NORMAL".equalsIgnoreCase(request.getPriority())) {
            return 3;
        }

        if ("LOW".equalsIgnoreCase(request.getPriority())) {
            return 4;
        }

        return 5;
    }
}