package net.osmand.smooth.routes;

import java.util.function.LongSupplier;

/**
 * TomTom Map Display raster base map (Maps v1 path, layer basic, style main, 256 px PNG). It has its own
 * monthly counter, separate from the traffic overlay: TomTom counts each API on its own.
 */
public final class TomTomBaseTileSource extends TomTomRasterTileSource {

	public static final String NAME = "TomTom base";
	/** TomTom's terms were not readable, so the shortest sensible cache time is used; raise it once confirmed. */
	public static final int CACHE_MINUTES = 24 * 60;
	/** 75 % of the free 200,000 raster tiles per month. */
	public static final int MONTHLY_LIMIT = 150_000;
	private static final String URL_FORMAT =
			"https://api.tomtom.com/map/1/tile/basic/main/%d/%d/%d.png?tileSize=256&key=%s";

	public TomTomBaseTileSource(KeySource keys, MonthlyQuota quota, LongSupplier clock) {
		super(NAME, 3, 22, CACHE_MINUTES, URL_FORMAT, keys, quota, clock);
	}
}
