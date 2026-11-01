package smartcharge.station;

/** Public, aggregate wait estimate for a station; contains no user/request details. */
public record StationWaitEstimate(Long stationId, Double estimatedWaitMinutes) {
}
