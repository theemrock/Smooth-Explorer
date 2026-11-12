package net.osmand.smooth.routes;

import net.osmand.map.TileSourceManager.TileSourceTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * TomTom Traffic Flow raster tiles (API v4, style relative0, 256 px, transparent PNG) for use as a map overlay.
 * The URL is built per call from the key source and contains the key: never log it, use {@link #logLine}.
 */
public final class TomTomTrafficTileSource extends TileSourceTemplate {

	public static final String NAME = "TomTom traffic";
	public static final int CACHE_MINUTES = 5;
	public static final int MONTHLY_LIMIT = 150_000;
	private static final String URL_PREFIX = "https://api.tomtom.com/traffic/map/4/tile/flow/relative0/";
	private static final String URL_FORMAT = URL_PREFIX + "%d/%d/%d.png?tileSize=256&key=%s";
	private static final long CACHE_MILLIS = CACHE_MINUTES * 60_000L;
	private static final int PRUNE_AT = 4096;

	private static volatile int lastCount;

	public interface KeySource {
		/** @return the key, or null when none is stored */
		String get();
	}

	private final KeySource keys;
	private final MonthlyQuota quota;
	private final LongSupplier clock;
	// Tiles already paid for in the current cache window. getUrlToLoad runs on every redraw and on every
	// 50 ms wait poll, so counting each call would burn the quota on repeats.
	private final Map<Long, Long> counted = new HashMap<>();

	public TomTomTrafficTileSource(KeySource keys, MonthlyQuota quota, LongSupplier clock) {
		super(NAME, null, ".png", 22, 7, 256, 16, 20000);
		this.keys = keys;
		this.quota = quota;
		this.clock = clock;
		setExpirationTimeMinutes(CACHE_MINUTES);
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
		return String.format(Locale.US, URL_FORMAT, zoom, x, y, key);
	}

	public boolean isLimitReached() {
		return quota.isExhausted(clock.getAsLong());
	}

	private synchronized boolean admit(int zoom, int x, int y, long now) {
		long id = ((long) zoom << 50) | ((long) x << 25) | y;
		Long last = counted.get(id);
		if (last != null && now - last < CACHE_MILLIS) {
			return true;
		}
		if (!quota.tryAcquire(now)) {
			return false;
		}
		lastCount = quota.used(now);
		counted.put(id, now);
		if (counted.size() > PRUNE_AT) {
			for (Iterator<Long> it = counted.values().iterator(); it.hasNext(); ) {
				if (now - it.next() >= CACHE_MILLIS) {
					it.remove();
				}
			}
		}
		return true;
	}

	public static boolean isTrafficUrl(String url) {
		return url != null && url.contains("api.tomtom.com/traffic/map/");
	}

	/** The only text that may be logged for a tile request: the HTTP code and the month counter, never the URL. */
	public static String logLine(int count, int httpCode) {
		return "tomtom traffic tile http=" + httpCode + " count=" + count;
	}

	public static String logLine(int httpCode) {
		return logLine(lastCount, httpCode);
	}
}
