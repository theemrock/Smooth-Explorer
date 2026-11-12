package net.osmand.smooth.routes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.osmand.data.LatLon;

import org.json.JSONException;
import org.junit.Test;

public class TomTomRoutesProtocolTest {

	private static final String OK = "{\"formatVersion\":\"0.0.12\",\"routes\":[{\"summary\":{\"lengthInMeters\":152479,"
			+ "\"travelTimeInSeconds\":6346},\"legs\":[{\"points\":["
			+ "{\"latitude\":41.0,\"longitude\":28.9},{\"latitude\":40.9,\"longitude\":29.0}]},"
			+ "{\"points\":[{\"latitude\":40.9,\"longitude\":29.0},{\"latitude\":40.2,\"longitude\":29.1}]}]}]}";

	@Test
	public void urlHasCoordinatesAndRequiredParameters() {
		String url = TomTomRoutesProtocol.buildUrl(new LatLon(41.0369, 28.985), new LatLon(40.1828, 29.0665), "K3Y");
		assertTrue(url.startsWith("https://api.tomtom.com/routing/1/calculateRoute/41.036900,28.985000:40.182800,29.066500/json?"));
		assertTrue(url.contains("key=K3Y"));
		assertTrue(url.contains("traffic=true"));
		assertTrue(url.contains("travelMode=car"));
		assertTrue(url.contains("instructionsType=text"));
		assertTrue(url.contains("language=tr-TR"));
	}

	@Test
	public void maskKeyHidesTheKeyOnly() {
		String masked = TomTomRoutesProtocol.maskKey("https://x/json?key=SECRET&traffic=true");
		assertEquals("https://x/json?key=***&traffic=true", masked);
		assertFalse(masked.contains("SECRET"));
	}

	@Test
	public void parsesSummaryAndJoinsLegsWithoutDuplicatePoint() throws JSONException {
		TomTomRoutesProtocol.Route r = TomTomRoutesProtocol.parse(OK);
		assertEquals(6346.0, r.durationSeconds, 1e-9);
		assertEquals(152479, r.distanceMeters);
		assertEquals(3, r.points.size());
		assertEquals(40.2, r.points.get(2).getLatitude(), 1e-9);
		assertTrue(TomTomRoutesProtocol.isResultOk(OK, new StringBuilder()));
	}

	@Test
	public void averageSpeedComesFromSummaryTotals() throws JSONException {
		assertEquals(152479 / 6346.0, TomTomRoutesProtocol.parse(OK).averageSpeedMps(), 1e-9);
	}

	@Test
	public void missingSummaryFieldsGiveZeroSpeedNotACrash() throws JSONException {
		String noTotals = "{\"routes\":[{\"summary\":{},\"legs\":[{\"points\":["
				+ "{\"latitude\":41.0,\"longitude\":28.9},{\"latitude\":40.9,\"longitude\":29.0}]}]}]}";
		TomTomRoutesProtocol.Route r = TomTomRoutesProtocol.parse(noTotals);
		assertEquals(0, r.distanceMeters);
		assertEquals(0.0, r.durationSeconds, 1e-9);
		assertEquals(0.0, r.averageSpeedMps(), 1e-9);
	}

	@Test
	public void errorBodyIsNotOkAndGivesMessage() throws JSONException {
		String err = "{\"detailedError\":{\"code\":\"Unauthorized\",\"message\":\"You are missing valid authentication credentials\"}}";
		StringBuilder msg = new StringBuilder();
		assertFalse(TomTomRoutesProtocol.isResultOk(err, msg));
		assertTrue(msg.toString().contains("Unauthorized"));
		assertNull(TomTomRoutesProtocol.parse(err));
	}

	@Test
	public void rateLimitIsRecognisedOnlyForLimitWording() {
		assertTrue(TomTomRoutesProtocol.isRateLimited("{\"detailedError\":{\"code\":\"Too Many Requests\"}}"));
		assertTrue(TomTomRoutesProtocol.isRateLimited("<h1>Developer Over Qps</h1> quota exceeded"));
		assertFalse(TomTomRoutesProtocol.isRateLimited("{\"detailedError\":{\"code\":\"Unauthorized\"}}"));
	}

	@Test(expected = JSONException.class)
	public void nonJsonBodyThrows() throws JSONException {
		TomTomRoutesProtocol.parse("<html>oops</html>");
	}

	@Test
	public void fractionsGrowFromZeroToOne() throws JSONException {
		double[] f = TomTomRoutesProtocol.fractionsAlong(TomTomRoutesProtocol.parse(OK).points);
		assertEquals(0.0, f[0], 1e-12);
		assertEquals(1.0, f[2], 1e-12);
		assertTrue(f[1] > 0 && f[1] < 1);
	}
}
