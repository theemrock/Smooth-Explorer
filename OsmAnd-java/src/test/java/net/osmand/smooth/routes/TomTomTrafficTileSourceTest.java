package net.osmand.smooth.routes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TomTomTrafficTileSourceTest {

	private static final String KEY = "SuperSecretKey123";

	private static class MemStore implements MonthlyQuota.Store {
		String month;
		int count;

		public String getMonth() {
			return month;
		}

		public int getCount() {
			return count;
		}

		public void save(String m, int c) {
			month = m;
			count = c;
		}
	}

	private static TomTomTrafficTileSource source(String key, int limit, long[] now) {
		return new TomTomTrafficTileSource(() -> key, new MonthlyQuota(new MemStore(), limit), () -> now[0]);
	}

	@Test
	public void noKeyGivesNullAndCountsNothing() {
		MemStore store = new MemStore();
		TomTomTrafficTileSource s = new TomTomTrafficTileSource(() -> null,
				new MonthlyQuota(store, 10), () -> 1_000_000L);
		assertNull(s.getUrlToLoad(1, 2, 10));
		assertEquals(0, store.count);
	}

	@Test
	public void urlHasKeyAndTomTomFlowPath() {
		String url = source(KEY, 10, new long[]{1_000_000L}).getUrlToLoad(1217, 764, 11);
		assertEquals("https://api.tomtom.com/traffic/map/4/tile/flow/relative0/11/1217/764.png?tileSize=256&key="
				+ KEY, url);
	}

	@Test
	public void stopsAtLimitAndLayerReportsIt() {
		long[] now = {1_000_000L};
		TomTomTrafficTileSource s = source(KEY, 2, now);
		assertNotNull(s.getUrlToLoad(1, 1, 10));
		assertNotNull(s.getUrlToLoad(2, 1, 10));
		assertTrue(s.isLimitReached());
		assertNull(s.getUrlToLoad(3, 1, 10));
	}

	@Test
	public void sameTileInsideCacheWindowIsCountedOnce() {
		long[] now = {1_000_000L};
		MemStore store = new MemStore();
		TomTomTrafficTileSource s = new TomTomTrafficTileSource(() -> KEY, new MonthlyQuota(store, 100), () -> now[0]);
		for (int i = 0; i < 50; i++) {
			now[0] += 50; // the 50 ms wait poll of the tile loader
			assertNotNull(s.getUrlToLoad(5, 5, 12));
		}
		assertEquals(1, store.count);
		now[0] += 5 * 60_000L; // cache expired: a real refresh
		assertNotNull(s.getUrlToLoad(5, 5, 12));
		assertEquals(2, store.count);
	}

	@Test
	public void baseMapHasItsOwnUrlCounterAndCache() {
		MemStore baseStore = new MemStore();
		MemStore trafficStore = new MemStore();
		long[] now = {1_000_000L};
		TomTomBaseTileSource base = new TomTomBaseTileSource(() -> KEY, new MonthlyQuota(baseStore, 100), () -> now[0]);
		TomTomTrafficTileSource traffic = new TomTomTrafficTileSource(() -> KEY, new MonthlyQuota(trafficStore, 100), () -> now[0]);
		assertEquals("https://api.tomtom.com/map/1/tile/basic/main/12/2/3.png?tileSize=256&key=" + KEY,
				base.getUrlToLoad(2, 3, 12));
		assertEquals(1, baseStore.count);
		assertEquals(0, trafficStore.count);
		assertEquals(24 * 60, base.getExpirationTimeMinutes());
		assertEquals(5, traffic.getExpirationTimeMinutes());
		assertTrue(TomTomRasterTileSource.isTomTomTileUrl(base.getUrlToLoad(2, 3, 12)));
	}

	@Test
	public void cacheLifetimeIsFiveMinutes() {
		assertEquals(5, source(KEY, 1, new long[]{0}).getExpirationTimeMinutes());
	}

	@Test
	public void logLineNeverHasTheKeyOrUrl() {
		String url = source(KEY, 10, new long[]{1_000_000L}).getUrlToLoad(1, 2, 10);
		assertTrue(TomTomRasterTileSource.isTomTomTileUrl(url));
		String line = TomTomRasterTileSource.logLine(42, 200);
		assertEquals("tomtom tile http=200 count=42", line);
		assertFalse(line.contains(KEY));
		assertFalse(line.contains("tomtom.com"));
		assertFalse(TomTomRoutesProtocol.maskKey(url).contains(KEY));
	}
}
