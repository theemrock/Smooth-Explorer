package net.osmand.smooth.routes;

import net.osmand.map.TileSourceManager.TileSourceTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Shared part of the TomTom raster tile sources (traffic overlay, base map): builds the URL per call from
 * the key source, counts real requests against a monthly cap and returns no URL when the key is missing or
 * the cap is used up. The URL contains the key: never log it, use {@link #logLine}.
 */
public abstract class TomTomRasterTileSource extends TileSourceTemplate {

	private static final int PRUNE_AT = 4096;

	private static volatile int lastCount;

	public interface KeySource {
		/** @return the key, or null when none is stored */
		String get();
	}

	private final String urlFormat;
	private final long cacheMillis;
	private final KeySource keys;
	private final MonthlyQuota quota;
	private final LongSupplier clock;
	// Tiles already paid for in the current cache window. getUrlToLoad runs on every redraw and on every
	// 50 ms wait poll, so counting each call would burn the quota on repeats.
	private final Map<Long, Long> counted = new HashMap<>();

	/** @param urlFormat String.format pattern taking zoom, x, y and the key, in this order */
	protected TomTomRasterTileSource(String name, int minZoom, int maxZoom, int cacheMinutes, String urlFormat,
	                                 KeySource keys, MonthlyQuota quota, LongSupplier clock) {
		super(name, null, ".png", maxZoom, minZoom, 256, 16, 20000);
		this.urlFormat = urlFormat;
		this.cacheMillis = cacheMinutes * 60_000L;
		this.keys = keys;
		this.quota = quota;
		this.clock = clock;
		setExpirationTimeMinutes(cacheMinutes);
	}

	@Override
	public boolean couldBeDownloadedFromInternet() {
		return true;
	}

	@Override
	public String getUrlToLoad(int x, int y, int zoom) {
		String key = keys.get();
		if (key == null || !admit(zoom, x, y, clock.getAsLong())) {
			return null;
		}
		return String.format(Locale.US, urlFormat, zoom, x, y, key);
	}

	public boolean isLimitReached() {
		return quota.isExhausted(clock.getAsLong());
	}

	private synchronized boolean admit(int zoom, int x, int y, long now) {
		long id = ((long) zoom << 50) | ((long) x << 25) | y;
		Long last = counted.get(id);
		if (last != null && now - last < cacheMillis) {
			return true;
		}
		if (!quota.tryAcquire(now)) {
			return false;
		}
		lastCount = quota.used(now);
		counted.put(id, now);
		if (counted.size() > PRUNE_AT) {
			for (Iterator<Long> it = counted.values().iterator(); it.hasNext(); ) {
				if (now - it.next() >= cacheMillis) {
					it.remove();
				}
			}
		}
		return true;
	}

	/** True for any request URL of the TomTom raster sources (traffic flow or base map). */
	public static boolean isTomTomTileUrl(String url) {
		return url != null && (url.contains("api.tomtom.com/traffic/map/") || url.contains("api.tomtom.com/map/1/tile/"));
	}

	/** The only text that may be logged for a tile request: the HTTP code and the month counter, never the URL. */
	public static String logLine(int count, int httpCode) {
		return "tomtom tile http=" + httpCode + " count=" + count;
	}

	public static String logLine(int httpCode) {
		return logLine(lastCount, httpCode);
	}
}
