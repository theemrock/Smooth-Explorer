package net.osmand.smooth.map;

/** What the map button cycles through: OFFLINE -> ONLINE -> ONLINE_TRAFFIC -> OFFLINE. */
public enum MapMode {
	OFFLINE,
	ONLINE,
	ONLINE_TRAFFIC;

	public MapMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	/** Anything unknown (empty, old or damaged value) is OFFLINE, the safe mode that needs no network. */
	public static MapMode fromName(String name) {
		for (MapMode mode : values()) {
			if (mode.name().equals(name)) {
				return mode;
			}
		}
		return OFFLINE;
	}
}
