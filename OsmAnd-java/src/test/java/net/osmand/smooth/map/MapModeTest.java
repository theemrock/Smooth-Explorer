package net.osmand.smooth.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.osmand.smooth.map.MapModePolicy.Reason;
import net.osmand.smooth.map.MapModePolicy.Result;

import org.junit.Test;

public class MapModeTest {

	@Test
	public void cycleIsOfflineOnlineTrafficOffline() {
		assertEquals(MapMode.ONLINE, MapMode.OFFLINE.next());
		assertEquals(MapMode.ONLINE_TRAFFIC, MapMode.ONLINE.next());
		assertEquals(MapMode.OFFLINE, MapMode.ONLINE_TRAFFIC.next());
	}

	@Test
	public void unknownSavedValueMeansOffline() {
		assertEquals(MapMode.ONLINE, MapMode.fromName("ONLINE"));
		assertEquals(MapMode.OFFLINE, MapMode.fromName(null));
		assertEquals(MapMode.OFFLINE, MapMode.fromName(""));
		assertEquals(MapMode.OFFLINE, MapMode.fromName("online-ish"));
	}

	@Test
	public void offlineNeverShowsAnythingOnline() {
		Result r = MapModePolicy.decide(MapMode.OFFLINE, true, true, true, true);
		assertFalse(r.showBase);
		assertFalse(r.showTraffic);
		assertEquals(Reason.NONE, r.reason);
	}

	@Test
	public void everythingAvailableShowsWhatWasPicked() {
		Result online = MapModePolicy.decide(MapMode.ONLINE, true, true, true, true);
		assertTrue(online.showBase);
		assertFalse(online.showTraffic);
		Result traffic = MapModePolicy.decide(MapMode.ONLINE_TRAFFIC, true, true, true, true);
		assertTrue(traffic.showBase);
		assertTrue(traffic.showTraffic);
		assertEquals(Reason.NONE, traffic.reason);
	}

	@Test
	public void noInternetFallsBackToOfflineInBothOnlineModes() {
		for (MapMode mode : new MapMode[]{MapMode.ONLINE, MapMode.ONLINE_TRAFFIC}) {
			Result r = MapModePolicy.decide(mode, false, true, true, true);
			assertFalse(r.showBase);
			assertFalse(r.showTraffic);
			assertEquals(Reason.NO_INTERNET, r.reason);
		}
	}

	@Test
	public void noKeyFallsBackToOffline() {
		Result r = MapModePolicy.decide(MapMode.ONLINE_TRAFFIC, true, false, true, true);
		assertFalse(r.showBase);
		assertFalse(r.showTraffic);
		assertEquals(Reason.NO_KEY, r.reason);
	}

	@Test
	public void baseQuotaUsedUpFallsBackToOffline() {
		Result r = MapModePolicy.decide(MapMode.ONLINE, true, true, false, true);
		assertFalse(r.showBase);
		assertEquals(Reason.BASE_QUOTA, r.reason);
	}

	@Test
	public void trafficQuotaUsedUpKeepsTheBaseMap() {
		Result r = MapModePolicy.decide(MapMode.ONLINE_TRAFFIC, true, true, true, false);
		assertTrue(r.showBase);
		assertFalse(r.showTraffic);
		assertEquals(Reason.TRAFFIC_QUOTA, r.reason);
	}

	@Test
	public void noInternetWinsOverOtherProblems() {
		assertEquals(Reason.NO_INTERNET, MapModePolicy.decide(MapMode.ONLINE, false, false, false, false).reason);
	}
}
