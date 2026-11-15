package com.example.campussuperapp;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Image par pinch-zoom, drag aur naam wale labels dikhane wali view (Google ki zaroorat nahi). */
public class CampusMapView extends View {

    public static class Spot {
        public final String name;
        public final float fx, fy;          // image ke andar position (0..1)
        final RectF hit = new RectF();      // screen par label ka area (touch ke liye)

        public Spot(String name, float fx, float fy) {
            this.name = name;
            this.fx = fx;
            this.fy = fy;
        }
    }

    public interface OnSpotClickListener {
        void onSpotClick(Spot spot);
    }

    public interface OnLongPressListener {
        void onLongPress(float fx, float fy); // image ke andar position (0..1)
    }

    private Bitmap bitmap;
    private final Matrix matrix = new Matrix();
    private final float[] m = new float[9];
    private final float[] pt = new float[2];
    private float minScale = 1f, maxScale = 6f;
    private boolean ready = false;

    private final List<Spot> spots = new ArrayList<>();
    private Spot selected;
    private OnSpotClickListener listener;
    private OnLongPressListener longPressListener;

    private final float density;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    public CampusMapView(Context context) {
        this(context, null);
    }

    public CampusMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.2f * density);
        ringPaint.setColor(Color.WHITE);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        if (!ready) return false;
                        float cur = currentScale();
                        float target = clampF(cur * d.getScaleFactor(), minScale, maxScale);
                        float factor = target / cur;
                        matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                        clampMatrix();
                        invalidate();
                        return true;
                    }
                });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (!ready || scaleDetector.isInProgress()) return false;
                matrix.postTranslate(-dx, -dy);
                clampMatrix();
                invalidate();
                return true;
            }

            // Long press: jis jagah dabaya uske image coordinates bata do (jagah jorne ke liye)
            @Override
            public void onLongPress(MotionEvent e) {
                if (!ready || longPressListener == null) return;
                Matrix inv = new Matrix();
                matrix.invert(inv);
                float[] p = {e.getX(), e.getY()};
                inv.mapPoints(p);
                longPressListener.onLongPress(p[0] / bitmap.getWidth(), p[1] / bitmap.getHeight());
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!ready) return false;
                if (currentScale() > minScale * 1.5f) {
                    animateTo(minScale, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
                } else {
                    Matrix inv = new Matrix();
                    matrix.invert(inv);
                    float[] p = {e.getX(), e.getY()};
                    inv.mapPoints(p);
                    animateTo(minScale * 2.5f, p[0], p[1]);
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!ready) return false;
                for (int i = spots.size() - 1; i >= 0; i--) {
                    Spot s = spots.get(i);
                    if (s.hit.contains(e.getX(), e.getY())) {
                        selectSpot(s, true);
                        if (listener != null) listener.onSpotClick(s);
                        return true;
                    }
                }
                selected = null;      // khali jagah dabane par selection hata do
                invalidate();
                return true;
            }
        });
    }

    // ------------------------------------------------------------- Public API

    public void setMapBitmap(Bitmap b) {
        bitmap = b;
        ready = false;
        if (bitmap != null && getWidth() > 0) fitToView();
        invalidate();
    }

    public void setSpots(List<Spot> list) {
        spots.clear();
        spots.addAll(list);
        invalidate();
    }

    public void setOnSpotClickListener(OnSpotClickListener l) {
        listener = l;
    }

    public void setOnLongPressListener(OnLongPressListener l) {
        longPressListener = l;
    }

    public void selectSpot(@Nullable Spot s, boolean zoomIn) {
        selected = s;
        if (s != null && zoomIn && ready) {
            animateTo(Math.max(currentScale(), minScale * 2.5f),
                    s.fx * bitmap.getWidth(), s.fy * bitmap.getHeight());
        }
        invalidate();
    }

    // ------------------------------------------------------------- Matrix

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bitmap != null && w > 0 && h > 0) fitToView();
    }

    private void fitToView() {
        float vw = getWidth(), vh = getHeight();
        float bw = bitmap.getWidth(), bh = bitmap.getHeight();
        minScale = Math.min(vw / bw, vh / bh);
        maxScale = minScale * 5f;
        matrix.reset();
        matrix.postScale(minScale, minScale);
        matrix.postTranslate((vw - bw * minScale) / 2f, (vh - bh * minScale) / 2f);
        ready = true;
        invalidate();
    }

    private float currentScale() {
        matrix.getValues(m);
        return m[Matrix.MSCALE_X];
    }

    private void clampMatrix() {
        matrix.getValues(m);
        float scale = m[Matrix.MSCALE_X];
        float tx = m[Matrix.MTRANS_X], ty = m[Matrix.MTRANS_Y];
        float cw = bitmap.getWidth() * scale, ch = bitmap.getHeight() * scale;
        float vw = getWidth(), vh = getHeight();
        float nx = cw <= vw ? (vw - cw) / 2f : Math.min(0f, Math.max(vw - cw, tx));
        float ny = ch <= vh ? (vh - ch) / 2f : Math.min(0f, Math.max(vh - ch, ty));
        matrix.postTranslate(nx - tx, ny - ty);
    }

    private void animateTo(float targetScale, float imgX, float imgY) {
        matrix.getValues(m);
        final float s0 = m[Matrix.MSCALE_X], x0 = m[Matrix.MTRANS_X], y0 = m[Matrix.MTRANS_Y];
        final float s1 = clampF(targetScale, minScale, maxScale);
        final float x1 = getWidth() / 2f - imgX * s1;
        final float y1 = getHeight() / 2f - imgY * s1;

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(350);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            float s = s0 + (s1 - s0) * t;
            matrix.reset();
            matrix.postScale(s, s);
            matrix.postTranslate(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t);
            clampMatrix();
            invalidate();
        });
        a.start();
    }

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------- Drawing

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || !ready) return;

        canvas.drawBitmap(bitmap, matrix, bitmapPaint);

        for (Spot s : spots) {
            if (s != selected) drawSpot(canvas, s, false);
        }
        if (selected != null) drawSpot(canvas, selected, true); // selected sabse upar
    }

    private void drawSpot(Canvas c, Spot s, boolean sel) {
        pt[0] = s.fx * bitmap.getWidth();
        pt[1] = s.fy * bitmap.getHeight();
        matrix.mapPoints(pt);
        float x = pt[0], y = pt[1];

        int navy = Color.parseColor("#1E295B");
        int orange = Color.parseColor("#EA580C");
        int main = sel ? orange : navy;

        // dot
        float r = (sel ? 6f : 4f) * density;
        ringPaint.setColor(Color.WHITE);
        c.drawCircle(x, y, r + 1.5f * density, ringPaint);
        dotPaint.setColor(main);
        c.drawCircle(x, y, r, dotPaint);

        // label (dot ke upar)
        textPaint.setTextSize((sel ? 12f : 10f) * density);
        textPaint.setColor(sel ? Color.WHITE : navy);
        float tw = textPaint.measureText(s.name);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float th = fm.descent - fm.ascent;
        float padH = 7 * density, padV = 4 * density;

        float boxW = tw + padH * 2, boxH = th + padV * 2;
        float left = x - boxW / 2f;
        float bottom = y - r - 4 * density;
        float top = bottom - boxH;

        RectF box = new RectF(left, top, left + boxW, bottom);
        boxPaint.setColor(sel ? orange : Color.WHITE);
        strokePaint.setColor(main);
        c.drawRoundRect(box, 8 * density, 8 * density, boxPaint);
        c.drawRoundRect(box, 8 * density, 8 * density, strokePaint);
        c.drawText(s.name, left + padH, top + padV - fm.ascent, textPaint);

        // touch area: label + dot
        s.hit.set(left - 4 * density, top - 4 * density,
                left + boxW + 4 * density, y + r + 4 * density);
    }

    // ------------------------------------------------------------- Touch

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean a = scaleDetector.onTouchEvent(e);
        boolean b = gestureDetector.onTouchEvent(e);
        return a || b || super.onTouchEvent(e);
    }
}