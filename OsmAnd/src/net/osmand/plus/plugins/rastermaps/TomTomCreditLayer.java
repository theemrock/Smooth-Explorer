package net.osmand.plus.plugins.rastermaps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.engine.TomTomKeyStore;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.smooth.routes.TomTomTrafficTileSource;

/**
 * Draws the "© TomTom" credit for as long as the traffic overlay is on, and turns the overlay off when the
 * monthly cap is used up. It is a separate screen-pixel layer because the tile layer itself must be a normal
 * (map-space) layer: the map only asks those for their tile provider.
 */
public class TomTomCreditLayer extends OsmandMapLayer {

	private static final long KEY_RECHECK_MS = 5000;

	private final TomTomTrafficTileSource source;
	private final TomTomKeyStore keyStore;
	private final Runnable onLimitReached;
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final String credit;
	private final float pad;
	private long keyCheckedAt;
	private boolean hasKey;

	public TomTomCreditLayer(@NonNull Context context, @NonNull TomTomTrafficTileSource source,
	                         @NonNull TomTomKeyStore keyStore, @NonNull Runnable onLimitReached) {
		super(context);
		this.source = source;
		this.keyStore = keyStore;
		this.onLimitReached = onLimitReached;
		float density = context.getResources().getDisplayMetrics().density;
		credit = context.getString(R.string.smooth_traffic_attribution);
		pad = 8 * density;
		textPaint.setTextSize(11 * density);
		textPaint.setColor(0xFF202020);
		textPaint.setTextAlign(Paint.Align.RIGHT);
		outlinePaint.set(textPaint);
		outlinePaint.setStyle(Paint.Style.STROKE);
		outlinePaint.setStrokeWidth(3 * density);
		outlinePaint.setColor(0xCCFFFFFF);
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		long now = System.currentTimeMillis();
		if (now - keyCheckedAt > KEY_RECHECK_MS) {
			hasKey = keyStore.isSet(); // keystore read: not on every frame
			keyCheckedAt = now;
		}
		if (!hasKey) {
			return;
		}
		if (source.isLimitReached()) {
			onLimitReached.run();
			return;
		}
		// bottom row: ruler on the left of centre, zoom buttons on the right, so the gap between them is free
		float x = canvas.getWidth() * 0.66f;
		float y = canvas.getHeight() - 3 * pad;
		canvas.drawText(credit, x, y, outlinePaint);
		canvas.drawText(credit, x, y, textPaint);
	}
}
