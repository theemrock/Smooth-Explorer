package net.osmand.smooth.routes;

import java.util.function.LongSupplier;

/** TomTom Traffic Flow raster tiles (API v4, style relative0, 256 px, transparent PNG), used as a map overlay. */
public final class TomTomTrafficTileSource extends TomTomRasterTileSource {

	public static final String NAME = "TomTom traffic";
	public static final int CACHE_MINUTES = 5;
	public static final int MONTHLY_LIMIT = 150_000;
	private static final String URL_FORMAT =
			"https://api.tomtom.com/traffic/map/4/tile/flow/relative0/%d/%d/%d.png?tileSize=256&key=%s";

	public TomTomTrafficTileSource(KeySource keys, MonthlyQuota quota, LongSupplier clock) {
		super(NAME, 7, 22, CACHE_MINUTES, URL_FORMAT, keys, quota, clock);
	}
}
