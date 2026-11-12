package net.osmand.smooth.map;

/**
 * Decides what is really shown for the mode the user picked. The picked mode never changes here: when the
 * network, the key or a quota is missing, the result falls back to the offline map and says why.
 */
public final class MapModePolicy {

	public enum Reason {
		NONE, NO_INTERNET, NO_KEY, BASE_QUOTA, TRAFFIC_QUOTA
	}

	public static final class Result {
		public final boolean showBase;
		public final boolean showTraffic;
		public final Reason reason;

		Result(boolean showBase, boolean showTraffic, Reason reason) {
			this.showBase = showBase;
			this.showTraffic = showTraffic;
			this.reason = reason;
		}
	}

	private MapModePolicy() {
	}

	public static Result decide(MapMode mode, boolean internet, boolean hasKey,
	                            boolean baseQuotaLeft, boolean trafficQuotaLeft) {
		if (mode == MapMode.OFFLINE) {
			return new Result(false, false, Reason.NONE);
		}
		if (!internet) {
			return new Result(false, false, Reason.NO_INTERNET);
		}
		if (!hasKey) {
			return new Result(false, false, Reason.NO_KEY);
		}
		if (!baseQuotaLeft) {
			return new Result(false, false, Reason.BASE_QUOTA);
		}
		if (mode == MapMode.ONLINE_TRAFFIC && !trafficQuotaLeft) {
			return new Result(true, false, Reason.TRAFFIC_QUOTA);
		}
		return new Result(true, mode == MapMode.ONLINE_TRAFFIC, Reason.NONE);
	}
}
