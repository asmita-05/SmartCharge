package smartcharge.scheduler;

import smartcharge.charging.ChargingRequest;

import java.util.Comparator;
import java.util.List;

public class SJF implements Scheduler {

    @Override
    public List<ChargingRequest> schedule(List<ChargingRequest> requests) {

        return requests.stream()
                .sorted(
                        Comparator
                                .comparing(ChargingRequest::getEstimatedMinutes,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(ChargingRequest::getArrivalTime)
                )
                .toList();
    }
}
