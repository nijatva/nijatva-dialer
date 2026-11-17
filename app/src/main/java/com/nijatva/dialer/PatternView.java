package com.nijatva.dialer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/* 3x3 pattern lock view: smooth gesture tracking, selection-order lines */
public class PatternView extends View {
    public interface OnPatternListener {
        void onPatternComplete(List<Integer> list);
    }

    private final List<Integer> cells = new ArrayList<>();
    private float curX = 0.0f;
    private float curY = 0.0f;
    private boolean tracking = false;
    private OnPatternListener listener;
    private final Paint dotPaint = new Paint(1);
    private final Paint activePaint = new Paint(1);
    private final Paint ringPaint = new Paint(1);
    private final Paint linePaint = new Paint(1);

    public PatternView(Context context) {
        super(context);
        init();
    }

    public PatternView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        this.dotPaint.setStyle(Paint.Style.FILL);
        this.dotPaint.setColor(0x66888888);
        this.activePaint.setStyle(Paint.Style.FILL);
        this.activePaint.setColor(0xff2fbf4f);
        this.ringPaint.setStyle(Paint.Style.STROKE);
        this.ringPaint.setColor(0x662fbf4f);
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeWidth(8.0f);
        this.linePaint.setStrokeCap(Paint.Cap.ROUND);
        this.linePaint.setStrokeJoin(Paint.Join.ROUND);
        this.linePaint.setColor(0xff2fbf4f);
    }

    public void setOnPatternListener(OnPatternListener onPatternListener) {
        this.listener = onPatternListener;
    }

    public List<Integer> getCells() {
        return this.cells;
    }

    public String patternString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.cells.size(); i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(this.cells.get(i));
        }
        return sb.toString();
    }

    public void clearPattern() {
        this.cells.clear();
        this.tracking = false;
        invalidate();
    }

    public void setAccent(int i) {
        this.activePaint.setColor(i);
        this.linePaint.setColor(i);
    }

    private float side() {
        return Math.min(getWidth(), getHeight());
    }

    private float cx(int i) {
        return ((float) (getWidth() - (int) side()) / 2.0f) + side() * (0.25f + 0.25f * (i % 3));
    }

    private float cy(int i) {
        return ((float) (getHeight() - (int) side()) / 2.0f) + side() * (0.25f + 0.25f * (i / 3));
    }

    @Override // android.view.View
    public void onMeasure(int i, int i2) {
        int size = MeasureSpec.getSize(i);
        setMeasuredDimension(size, size);
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float f = side() / 30.0f;
        this.ringPaint.setStrokeWidth(f * 0.35f);
        Path path = new Path();
        boolean first = true;
        for (int i = 0; i < 9; i++) {
            if (!this.cells.contains(Integer.valueOf(i))) {
                canvas.drawCircle(cx(i), cy(i), f, this.dotPaint);
            }
        }
        for (int k = 0; k < this.cells.size(); k++) {
            int c = this.cells.get(k).intValue();
            canvas.drawCircle(cx(c), cy(c), f * 1.5f, this.activePaint);
            canvas.drawCircle(cx(c), cy(c), f * 2.1f, this.ringPaint);
            if (first) {
                path.moveTo(cx(c), cy(c));
                first = false;
            } else {
                path.lineTo(cx(c), cy(c));
            }
        }
        if (this.tracking && this.cells.size() > 0) {
            path.lineTo(this.curX, this.curY);
        }
        if (this.cells.size() > 0) {
            canvas.drawPath(path, this.linePaint);
        }
    }

    private int hitCell(float f, float f2) {
        float side = side() / 5.0f;
        for (int i = 0; i < 9; i++) {
            if (Math.hypot(f - cx(i), f2 - cy(i)) <= side) {
                return i;
            }
        }
        return -1;
    }

    /* Add any dots that sit on a straight line between the last picked dot and a new one */
    private void addIntermediate(int from, int to) {
        int r1 = from / 3;
        int c1 = from % 3;
        int r2 = to / 3;
        int c2 = to % 3;
        int dr = Integer.signum(r2 - r1);
        int dc = Integer.signum(c2 - c1);
        if ((r1 == r2 || c1 == c2 || Math.abs(r1 - r2) == Math.abs(c1 - c2)) && Math.max(Math.abs(r1 - r2), Math.abs(c1 - c2)) > 1) {
            int rr = r1 + dr;
            int cc = c1 + dc;
            while (rr != r2 || cc != c2) {
                int mid = rr * 3 + cc;
                if (!this.cells.contains(Integer.valueOf(mid))) {
                    this.cells.add(Integer.valueOf(mid));
                }
                rr += dr;
                cc += dc;
            }
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        this.curX = motionEvent.getX();
        this.curY = motionEvent.getY();
        if (action == 0) {
            this.cells.clear();
            int hitCell = hitCell(this.curX, this.curY);
            if (hitCell >= 0) {
                this.cells.add(Integer.valueOf(hitCell));
                this.tracking = true;
            }
        } else if (action == 2 && this.tracking) {
            int hitCell2 = hitCell(this.curX, this.curY);
            if (hitCell2 >= 0 && !this.cells.contains(Integer.valueOf(hitCell2))) {
                if (this.cells.size() > 0) {
                    addIntermediate(this.cells.get(this.cells.size() - 1).intValue(), hitCell2);
                }
                this.cells.add(Integer.valueOf(hitCell2));
            }
        } else if (action == 1 && this.tracking) {
            this.tracking = false;
            if (this.listener != null && this.cells.size() >= 1) {
                this.listener.onPatternComplete(this.cells);
            }
        }
        invalidate();
        return true;
    }
}
