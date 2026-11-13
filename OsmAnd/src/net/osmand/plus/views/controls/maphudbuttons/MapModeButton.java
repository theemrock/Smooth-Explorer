package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.profiles.SelectNavProfileBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapModeButtonState;

/** One button that cycles the map mode: offline, online, online with traffic. */
public class MapModeButton extends MapButton {

	private final MapModeButtonState buttonState;

	public MapModeButton(@NonNull Context context) {
		this(context, null);
	}

	public MapModeButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MapModeButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getMapModeButtonState();

		setOnClickListener(view -> {
			OsmandRasterMapsPlugin plugin = PluginsHelper.getPlugin(OsmandRasterMapsPlugin.class);
			if (plugin != null && mapActivity != null) {
				plugin.cycleMapMode(mapActivity);
				setInvalidated(true);
			}
		});
		setOnLongClickListener(view -> {
			if (mapActivity == null) {
				return false;
			}
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			SelectNavProfileBottomSheet.showInstance(mapActivity, null, appMode, appMode.getRoutingProfile(), false);
			return true;
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	public void update() {
		super.update();

		int description = switch (buttonState.getMode()) {
			case OFFLINE -> R.string.smooth_map_mode_offline;
			case ONLINE -> R.string.smooth_map_mode_online;
			case ONLINE_TRAFFIC -> R.string.smooth_map_mode_online_traffic;
		};
		setContentDescription(app.getString(description));
	}

	@Override
	protected boolean shouldShow() {
		return buttonState.isEnabled() && !routeDialogOpened && visibilityHelper.shouldShowTopButtons();
	}
}
