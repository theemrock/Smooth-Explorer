package net.osmand.plus.onlinerouting.engine;

import static net.osmand.plus.onlinerouting.engine.EngineType.TOMTOM_TYPE;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.smooth.routes.MonthlyQuota;
import net.osmand.smooth.routes.TomTomRoutesProtocol;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Route line comes from TomTom Routing API (traffic aware). Instructions come from OsmAnd's own
 * approximation of that line (GpxEngine). Trip time is spread over the points as timestamps so the
 * ETA follows TomTom's traffic estimate. The API key sits in the request URL: never log that URL.
 */
public class TomTomRoutesEngine extends GpxEngine {

	private static final String APPROXIMATION_PROFILE = "car";
	private static final String PREFS = "smooth_tomtom_quota";
	private static final String TAG = "SmoothRoutes";

	private static WeakReference<OsmandApplication> appRef = new WeakReference<>(null);
	private static MonthlyQuota quota;

	/** Mean speed (m/s) of the last TomTom answer; set by responseByContent, used by applyTravelTime. */
	private volatile float travelSpeedMps;

	public TomTomRoutesEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	/** Gives the engine the app context it needs to read the key; call before using getFullUrl outside a route calculation. */
	public static void attach(@NonNull OsmandApplication app) {
		appRef = new WeakReference<>(app);
	}

	/** Puts the stored key back into a URL shown masked (key=***); used only for the "test route" button. */
	@NonNull
	public static String revealKey(@NonNull String url) {
		String key = currentKey();
		return key == null ? url : url.replace("key=***", "key=" + key);
	}

	@Nullable
	private static String currentKey() {
		OsmandApplication app = appRef.get();
		return app == null ? null : new TomTomKeyStore(app).get();
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return TOMTOM_TYPE;
	}

	@NonNull
	@Override
	public String getTitle() {
		return "TomTom Routing";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "TOMTOM";
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return TomTomRoutesProtocol.BASE_URL;
	}

	@NonNull
	@Override
	public String getFullUrl(@NonNull List<LatLon> path, @Nullable Float startBearing) {
		if (path.size() < 2) {
			return getBaseUrl();
		}
		String key = currentKey(); // read per request, only used inside this URL
		return TomTomRoutesProtocol.buildUrl(path.get(0), path.get(path.size() - 1), key == null ? "" : key);
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path, @Nullable Float startBearing) {
		// not used: getFullUrl builds the whole URL
	}

	/**
	 * Called once per network request, so this is where the monthly counter is charged.
	 * Throwing makes RouteProvider fall back to offline routing.
	 */
	@Nullable
	@Override
	public String getRequestBody(@NonNull List<LatLon> path, @Nullable Float startBearing) throws JSONException {
		if (path.size() < 2) {
			throw new JSONException("Need start and end");
		}
		OsmandApplication app = appRef.get();
		if (app == null) {
			throw new JSONException("No app context");
		}
		if (!new TomTomKeyStore(app).isSet()) {
			app.showToastMessage(R.string.smooth_tomtom_key_missing);
			throw new JSONException("TomTom API key is not set");
		}
		if (!getQuota(app).tryAcquire(System.currentTimeMillis())) {
			app.showToastMessage(R.string.smooth_routes_quota_reached);
			throw new JSONException("TomTom monthly limit reached");
		}
		return null;
	}

	@NonNull
	private static synchronized MonthlyQuota getQuota(@NonNull OsmandApplication app) {
		if (quota == null) {
			SharedPreferences prefs = app.getSharedPreferences(PREFS, 0);
			quota = new MonthlyQuota(new MonthlyQuota.Store() {
				public String getMonth() {
					return prefs.getString("month", null);
				}

				public int getCount() {
					return prefs.getInt("count", 0);
				}

				public void save(String month, int count) {
					prefs.edit().putString("month", month).putInt("count", count).apply();
				}
			}, MonthlyQuota.DEFAULT_LIMIT);
		}
		return quota;
	}

	@Nullable
	@Override
	public OnlineRoutingResponse responseByContent(@NonNull OsmandApplication app, @NonNull String content,
	                                               boolean leftSideNavigation, boolean initialCalculation,
	                                               @Nullable RouteCalculationProgress calculationProgress) {
		try {
			TomTomRoutesProtocol.Route route = TomTomRoutesProtocol.parse(content);
			if (route != null) {
				travelSpeedMps = (float) route.averageSpeedMps();
				Log.i(TAG, "tomtom duration_s=" + Math.round(route.durationSeconds) + " distance_m="
						+ route.distanceMeters + " http=200");
				return responseByGpxFile(app, toGpx(route), initialCalculation, calculationProgress);
			}
		} catch (JSONException ignored) {
			// not JSON: handled below like any other error body
		}
		// the helper hands over only the body: any answer without a route is a non-200 HTTP answer
		Log.i(TAG, "tomtom http=error body=" + safeHead(content));
		if (TomTomRoutesProtocol.isRateLimited(content)) {
			app.showToastMessage(R.string.smooth_routes_rate_limited);
		}
		return null;
	}

	/**
	 * Gives every direction step the speed implied by TomTom's totals, so the trip time shown by OsmAnd
	 * (RouteCalculationResult.getLeftTime sums distance / averageSpeed per step) follows TomTom's traffic time.
	 * Called by RouteProvider after the route is built; no-op until a TomTom answer has been parsed.
	 */
	public void applyTravelTime(@NonNull RouteCalculationResult result) {
		float speed = travelSpeedMps;
		if (speed <= 0) {
			return;
		}
		List<RouteDirectionInfo> directions = result.getImmutableAllDirections();
		int sumExpectedTime = 0;
		for (int i = directions.size() - 1; i >= 0; i--) {
			RouteDirectionInfo direction = directions.get(i);
			direction.setAverageSpeed(speed);
			sumExpectedTime += direction.getExpectedTime();
			direction.afterLeftTime = sumExpectedTime;
		}
	}

	@NonNull
	private static String safeHead(@NonNull String content) {
		String head = content.substring(0, Math.min(300, content.length()));
		String masked = TomTomRoutesProtocol.maskKey(head);
		String key = currentKey();
		return key == null ? masked : masked.replace(key, "***");
	}

	@NonNull
	private static GpxFile toGpx(@NonNull TomTomRoutesProtocol.Route route) {
		double[] fractions = TomTomRoutesProtocol.fractionsAlong(route.points);
		long start = System.currentTimeMillis();
		List<WptPt> pts = new ArrayList<>(route.points.size());
		for (int i = 0; i < fractions.length; i++) {
			LatLon p = route.points.get(i);
			WptPt w = new WptPt(p.getLatitude(), p.getLongitude());
			w.setTime(start + Math.round(fractions[i] * route.durationSeconds * 1000));
			pts.add(w);
		}
		GpxFile gpx = new GpxFile("TomTom Routing");
		gpx.addTrkSegment(pts);
		return gpx;
	}

	@Override
	public boolean isResultOk(@NonNull StringBuilder errorMessage, @NonNull String content) {
		try {
			return TomTomRoutesProtocol.isResultOk(content, errorMessage);
		} catch (JSONException e) {
			errorMessage.append(e.getMessage());
			return false;
		}
	}

	@Override
	public boolean shouldApproximateRoute() {
		return true;
	}

	@Nullable
	@Override
	public String getApproximationRoutingProfile() {
		return APPROXIMATION_PROFILE;
	}

	@Nullable
	@Override
	public String getApproximationDerivedProfile() {
		return null;
	}

	@Override
	public boolean useExternalTimestamps() {
		return true;
	}

	@Override
	public boolean useRoutingFallback() {
		return true;
	}

	@Override
	public void updateRouteParameters(@NonNull RouteCalculationParams params, @Nullable RouteCalculationResult previousRoute) {
		appRef = new WeakReference<>(params.ctx);
		super.updateRouteParameters(params, previousRoute);
	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		// only makes the key card visible in the editor: the key itself goes to TomTomKeyStore, never to the params
		params.add(EngineParameter.API_KEY);
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new TomTomRoutesEngine(params);
	}
}
