package smartcharge.scheduler;

import smartcharge.charging.ChargingRequest;

import java.util.Comparator;
import java.util.List;

public class FCFS implements Scheduler {

    @Override
    public List<ChargingRequest> schedule(List<ChargingRequest> requests) {

        return requests.stream()
                .sorted(Comparator.comparing(ChargingRequest::getArrivalTime))
                .toList();
    }
}