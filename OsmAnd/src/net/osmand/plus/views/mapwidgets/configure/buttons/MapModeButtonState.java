package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.SMALL_SIZE_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_RIGHT;
import static net.osmand.shared.grid.ButtonPositionSize.POS_TOP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;
import net.osmand.smooth.map.MapMode;

/** The map-mode button (offline / online / online with traffic) and the mode the user left it in. */
public class MapModeButtonState extends MapButtonState {

	public static final String ID = "smooth_map_mode_hud";

	private final CommonPreference<Boolean> visibilityPref;
	private final CommonPreference<String> modePref;

	public MapModeButtonState(@NonNull OsmandApplication app) {
		super(app, ID);
		visibilityPref = addPreference(settings.registerBooleanPreference("smooth_map_mode_visible", true)).makeProfile().cache();
		// not in the per-profile list: the map mode is one choice for the whole app, kept across restarts
		modePref = settings.registerStringPreference("smooth_map_mode", MapMode.OFFLINE.name()).makeGlobal().makeShared();
	}

	@NonNull
	public MapMode getMode() {
		return MapMode.fromName(modePref.get());
	}

	public void setMode(@NonNull MapMode mode) {
		modePref.set(mode.name());
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.smooth_map_mode_name);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.smooth_map_mode_descr);
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.smooth_map_mode_button;
	}

	@Override
	public int getDefaultSize() {
		return SMALL_SIZE_DP;
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	/** Icons that already ship with OsmAnd: offline map, cloud, car. */
	@NonNull
	@Override
	public String getDefaultIconName(@Nullable Boolean nightMode) {
		return switch (getMode()) {
			case OFFLINE -> "ic_action_offline";
			case ONLINE -> "ic_action_cloud";
			case ONLINE_TRAFFIC -> "ic_action_car_dark";
		};
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_RIGHT, POS_TOP, false, true);
	}
}
