package net.programmierecke.radiodroid2.utils;

import android.content.Context;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;

public class FocusSmoothScroller extends LinearSmoothScroller {

    private static final float MILLISECONDS_PER_INCH = 150f;
    private static final int DEFAULT_DURATION_MS = 300;

    public FocusSmoothScroller(Context context) {
        super(context);
    }

    @Override
    public int getVerticalSnapPreference() {
        return SNAP_TO_START;
    }

    @Override
    public int getHorizontalSnapPreference() {
        return SNAP_TO_START;
    }

    @Override
    public float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return Math.min(DEFAULT_DURATION_MS, super.calculateTimeForScrolling(dx));
    }

    @Override
    protected void onTargetFound(View targetView, androidx.recyclerview.widget.RecyclerView.State state, Action action) {
        PointF distance = computeScrollVectorForPosition(getTargetPosition());
        if (distance == null) {
            return;
        }

        int dx = (int) (distance.x * getVerticalSnapPreference());
        int dy = (int) (distance.y * getHorizontalSnapPreference());
        final int distanceSq = dx * dx + dy * dy;
        if (distanceSq == 0) {
            return;
        }

        float distanceMagnitude = (float) Math.sqrt(distanceSq);
        int time = calculateTimeForDeceleration((int) distanceMagnitude);
        action.update(-dx, -dy, time, mDecelerateInterpolator);
    }
}
