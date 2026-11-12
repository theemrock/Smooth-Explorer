package net.osmand.smooth.routes;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** URL building and response parsing for TomTom Routing API v1 calculateRoute. */
public final class TomTomRoutesProtocol {

	public static final String BASE_URL = "https://api.tomtom.com/routing/1/calculateRoute/";

	private TomTomRoutesProtocol() {
	}

	/** The key travels as a query parameter (the API does not accept a header), so never log this URL unmasked. */
	public static String buildUrl(LatLon origin, LatLon destination, String apiKey) {
		return String.format(Locale.US, "%s%.6f,%.6f:%.6f,%.6f/json?key=%s&traffic=true&travelMode=car"
						+ "&instructionsType=text&language=tr-TR",
				BASE_URL, origin.getLatitude(), origin.getLongitude(),
				destination.getLatitude(), destination.getLongitude(), apiKey);
	}

	public static String maskKey(String text) {
		return text.replaceAll("([?&]key=)[^&]*", "$1***");
	}

	/** True when the response has a route; otherwise appends TomTom's error message (if any). */
	public static boolean isResultOk(String content, StringBuilder errorMessage) throws JSONException {
		JSONObject root = new JSONObject(content);
		JSONArray routes = root.optJSONArray("routes");
		if (routes != null && routes.length() > 0) {
			return true;
		}
		errorMessage.append(errorText(root));
		return false;
	}

	/** Best effort: 429 bodies are not documented, so look for the usual wording. Non-JSON bodies count too. */
	public static boolean isRateLimited(String content) {
		String c = content.toLowerCase(Locale.US);
		return c.contains("too many") || c.contains("rate limit") || c.contains("quota") || c.contains("429");
	}

	private static String errorText(JSONObject root) {
		JSONObject detailed = root.optJSONObject("detailedError");
		if (detailed != null) {
			return detailed.optString("code", "") + " " + detailed.optString("message", "");
		}
		JSONObject error = root.optJSONObject("error");
		return error != null ? error.optString("description", error.toString()) : "no route";
	}

	/** @return null when there is no usable route */
	public static Route parse(String content) throws JSONException {
		JSONObject root = new JSONObject(content);
		JSONArray routes = root.optJSONArray("routes");
		if (routes == null || routes.length() == 0) {
			return null;
		}
		JSONObject r = routes.getJSONObject(0);
		JSONObject summary = r.getJSONObject("summary");
		List<LatLon> points = new ArrayList<>();
		JSONArray legs = r.optJSONArray("legs");
		for (int i = 0; legs != null && i < legs.length(); i++) {
			JSONArray pts = legs.getJSONObject(i).getJSONArray("points");
			for (int j = 0; j < pts.length(); j++) {
				JSONObject p = pts.getJSONObject(j);
				LatLon ll = new LatLon(p.getDouble("latitude"), p.getDouble("longitude"));
				// the last point of a leg is the first point of the next one
				if (points.isEmpty() || !points.get(points.size() - 1).equals(ll)) {
					points.add(ll);
				}
			}
		}
		if (points.size() < 2) {
			return null;
		}
		return new Route(points, summary.optDouble("travelTimeInSeconds", 0), summary.optInt("lengthInMeters", 0));
	}

	/** Share (0..1) of the line length covered at every point; used to spread the trip time as timestamps. */
	public static double[] fractionsAlong(List<LatLon> points) {
		double[] cum = new double[points.size()];
		for (int i = 1; i < cum.length; i++) {
			cum[i] = cum[i - 1] + MapUtils.getDistance(points.get(i - 1), points.get(i));
		}
		double total = cum[cum.length - 1];
		for (int i = 0; i < cum.length; i++) {
			cum[i] = total > 0 ? cum[i] / total : 0;
		}
		return cum;
	}

	public static final class Route {
		public final List<LatLon> points;
		public final double durationSeconds;
		public final int distanceMeters;

		Route(List<LatLon> points, double durationSeconds, int distanceMeters) {
			this.points = points;
			this.durationSeconds = durationSeconds;
			this.distanceMeters = distanceMeters;
		}

		/** Mean speed implied by TomTom's totals (traffic included); 0 when a total is missing. */
		public double averageSpeedMps() {
			return durationSeconds > 0 && distanceMeters > 0 ? distanceMeters / durationSeconds : 0;
		}
	}
}
