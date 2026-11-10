package net.osmand.plus.smooth;

/**
 * Smooth Explorer build switches. A plugin stays in the source tree for easy upstream merges,
 * it is just not registered in PluginsHelper.initPlugins when its switch is false.
 */
public final class SmoothFeatures {

	public static final boolean PLUGIN_OSM_EDITING = false;
	public static final boolean PLUGIN_MAPILLARY = false;
	public static final boolean PLUGIN_PANORAMAX = false;
	public static final boolean PLUGIN_NAUTICAL = false;
	public static final boolean PLUGIN_AIS = false;
	public static final boolean PLUGIN_SKI = false;
	public static final boolean PLUGIN_ACCESSIBILITY = false;

	private SmoothFeatures() {
	}
}
