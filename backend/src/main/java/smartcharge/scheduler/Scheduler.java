package smartcharge.scheduler;

import smartcharge.charging.ChargingRequest;

import java.util.List;

public interface Scheduler {

    List<ChargingRequest> schedule(List<ChargingRequest> requests);
}