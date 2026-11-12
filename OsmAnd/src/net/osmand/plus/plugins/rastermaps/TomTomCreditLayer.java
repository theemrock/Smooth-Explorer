package net.osmand.plus.plugins.rastermaps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

/**
 * Draws the "© TomTom" credit for as long as a TomTom layer (base map or traffic) is on. Every few seconds it
 * asks the plugin to re-check the map mode, so a used-up cap or a deleted key takes the layers away without a
 * button press. It is a separate screen-pixel layer because the tile layers themselves must be normal
 * (map-space) layers: the map only asks those for their tile provider.
 */
public class TomTomCreditLayer extends OsmandMapLayer {

	private static final long RECHECK_MS = 5000;

	private final Runnable recheck;
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final String credit;
	private final float pad;
	private long recheckedAt;

	public TomTomCreditLayer(@NonNull Context context, @NonNull Runnable recheck) {
		super(context);
		this.recheck = recheck;
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
		if (now - recheckedAt > RECHECK_MS) {
			recheckedAt = now;
			recheck.run();
		}
		// bottom row: ruler on the left of centre, zoom buttons on the right, so the gap between them is free
		float x = canvas.getWidth() * 0.66f;
		float y = canvas.getHeight() - 3 * pad;
		canvas.drawText(credit, x, y, outlinePaint);
		canvas.drawText(credit, x, y, textPaint);
	}
}
