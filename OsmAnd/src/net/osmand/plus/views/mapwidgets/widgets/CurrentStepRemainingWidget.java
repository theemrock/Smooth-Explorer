package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_STEP_REMAINING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class CurrentStepRemainingWidget extends SimpleWidget {

	public CurrentStepRemainingWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, CURRENT_STEP_REMAINING, customId, widgetsPanel);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		boolean followingMode = routingHelper.isFollowingMode() || locationProvider.getLocationSimulation().isRouteAnimating();
		NextDirectionInfo info = routingHelper.isRouteCalculated() && followingMode
				? routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true)
				: null;
		RouteDirectionInfo step = info == null ? null : info.directionInfo;
		if (step == null || info.distanceTo <= 0) {
			setText(null, null);
			return;
		}
		int leftSeconds = remainingTimeSeconds(step.getExpectedTime(), step.getDistance(), info.distanceTo);
		String distance = OsmAndFormatter.getFormattedDistance(info.distanceTo, app);
		setText(distance + " · " + formatDuration(leftSeconds), null);
	}

	/**
	 * Scales the step time by the share of the step distance that is still left.
	 * shortcut: linear scaling ignores road speed changes, traffic is already in the route average.
	 */
	static int remainingTimeSeconds(int stepTimeSeconds, int stepDistanceM, int leftDistanceM) {
		if (stepDistanceM <= 0 || stepTimeSeconds <= 0) {
			return 0;
		}
		if (leftDistanceM > stepDistanceM) {
			return stepTimeSeconds;
		}
		if (leftDistanceM <= 0) {
			return 0;
		}
		return (int) Math.round(stepTimeSeconds * ((double) leftDistanceM / stepDistanceM));
	}

	/**
	 * Formats seconds as m:ss, or h:mm from one hour on.
	 */
	static String formatDuration(int seconds) {
		int total = Math.max(0, seconds);
		int hours = total / 3600;
		int minutes = (total % 3600) / 60;
		if (hours > 0) {
			return String.format("%d:%02d", hours, minutes);
		}
		return String.format("%d:%02d", minutes, total % 60);
	}
}
