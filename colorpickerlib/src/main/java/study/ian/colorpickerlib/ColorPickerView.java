package study.ian.colorpickerlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

public class ColorPickerView extends View implements OuterRingSelectedListener {

    private final String TAG = "ColorPickerView";

    private final Path outerPath = new Path();
    private final Path triPath = new Path();
    private final Region triRegion = new Region();
    private final PointF[] triPointFs = new PointF[3];
    private final PointF cursorPointF = new PointF();
    private final PointF centerPointF = new PointF();

    private final Paint outerRingPaint = new Paint();
    private final Paint outerIndicatorPaint = new Paint();
    private final Paint triPaint = new Paint();
    private final Paint selectCursorPaint = new Paint();

    private SweepGradient hueGradient;
    private SweepGradient satGradient;
    private LinearGradient valGradient;

    private OnColorSelectListener colorSelectListener;

    private final float[] selectHsv = {0, 1, 1};
    private float degree;
    private int width;
    private int touchMode = 0;
    private int selectColor = Color.RED;
    private float value = 1f;
    private float valueSlope;
    private float saturation = 1f;
    private final int MODE_CIRCLE = 0;
    private final int MODE_TRIANGLE = 1;

    @SuppressLint("ClickableViewAccessibility")
    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (triRegion.contains((int) x, (int) y)) {
                        touchMode = MODE_TRIANGLE;
                        cursorPointF.set(x, y);

                        value = getValue(cursorPointF);
                        saturation = getSaturation(cursorPointF);
                        selectHsv[1] = saturation;
                        selectHsv[2] = value;
                    } else {
                        touchMode = MODE_CIRCLE;
                        degree = getDegree(x - width * .5f, y - width * .5f);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (touchMode == MODE_TRIANGLE) {
                        cursorPointF.set(x, y);

                        if (!triRegion.contains((int) cursorPointF.x, (int) cursorPointF.y)) {
                            setCursor(cursorPointF, x, y);
                        }

                        value = getValue(cursorPointF);
                        saturation = getSaturation(cursorPointF);
                        selectHsv[1] = saturation;
                        selectHsv[2] = value;
                    } else {
                        degree = getDegree(x - width * .5f, y - width * .5f);
                    }
                    break;
                default:
                    break;
            }
            onDegreeSelected(degree);
            return true;
        });
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);

                width = getWidth();
                initialPointFs();
                initialGradient();
                initialPaths();
                initialPaints();
                initialRegion();

                valueSlope = getValueSlope();
            }
        });
    }

    public void setColorSelectListener(OnColorSelectListener listener) {
        colorSelectListener = listener;
    }

    private float getValue(PointF cursorPointF) {
        PointF centerP = getCenter(triPointFs[0], triPointFs[2]);
        PointF crossP = getCrossPoint(triPointFs[1], centerP, cursorPointF);
        return (float) (Math.hypot(triPointFs[1].x - crossP.x, triPointFs[1].y - crossP.y) /
                Math.hypot(triPointFs[1].x - centerP.x, triPointFs[1].y - centerP.y));
    }

    private float getValueSlope() {
        PointF centerP = getCenter(triPointFs[0], triPointFs[2]);
        return (centerP.y - triPointFs[1].y) / (centerP.x - triPointFs[1].x);
    }

    private float getSaturation(PointF cursorPointF) {
        float sAxisLength =
                (float) Math.hypot(triPointFs[0].x - triPointFs[2].x, triPointFs[0].y - triPointFs[2].y) * value;
        PointF crossP = getCrossPoint(triPointFs[1], triPointFs[2], cursorPointF, -1 / valueSlope);
        float crossLength =
                (float) Math.hypot(crossP.x - cursorPointF.x, crossP.y - cursorPointF.y);
        return crossLength / sAxisLength;
    }

    private void setCursor(PointF cursorPointF, float x, float y) {
        float degree = getDegree(x - width * .5f, y - width * .5f);

        if (degree >= 30 && degree < 150) {
            float d = getDegree(cursorPointF.x - triPointFs[0].x, cursorPointF.y - triPointFs[0].y);
            if (d < 60) {
                cursorPointF.set(triPointFs[2].x, triPointFs[2].y);
            } else if (d > 120) {
                cursorPointF.set(triPointFs[1].x, triPointFs[1].y);
            } else {
                cursorPointF.set(getCrossPoint(triPointFs[1], triPointFs[2], triPointFs[0], cursorPointF));
            }
        } else if (degree >= 150 && degree < 270) {
            float d = getDegree(cursorPointF.x - triPointFs[2].x, cursorPointF.y - triPointFs[2].y);
            if (d > 240) {
                cursorPointF.set(triPointFs[0].x, triPointFs[0].y);
            } else if (d < 180) {
                cursorPointF.set(triPointFs[1].x, triPointFs[1].y);
            } else {
                cursorPointF.set(getCrossPoint(triPointFs[0], triPointFs[1], triPointFs[2], cursorPointF));
            }
        } else {
            float d = getDegree(cursorPointF.x - triPointFs[1].x, cursorPointF.y - triPointFs[1].y);
            if (d < 10) {
                cursorPointF.set(triPointFs[2].x, triPointFs[2].y);
            } else if (d < 300) {
                cursorPointF.set(triPointFs[0].x, triPointFs[0].y);
            } else {
                cursorPointF.set(getCrossPoint(triPointFs[0], triPointFs[2], triPointFs[1], cursorPointF));
            }
        }
    }

    /*
        get cross point with four points, first pair is p0 and p1, second pair is p2 and p3
     */
    private PointF getCrossPoint(PointF p0, PointF p1, PointF p2, PointF p3) {
        PointF p = new PointF();

        // calculate slope
        float m0 = (p1.y - p0.y) / (p1.x - p0.x);
        float m1 = (p3.y - p2.y) / (p3.x - p2.x);

        // calculate constant
        float c0 = p0.y - m0 * p0.x;
        float c1 = p2.y - m1 * p2.x;

        p.x = (c0 - c1) / (m1 - m0);
        p.y = m1 * p.x + c1;
        return p;
    }

    /*
        get cross point with three points and one slope for p2, first pair is p0 and p1
     */
    private PointF getCrossPoint(PointF p0, PointF p1, PointF p2, float slope) {
        PointF p = new PointF();

        // calculate slope
        float m0 = (p1.y - p0.y) / (p1.x - p0.x);

        // calculate constant
        float c0 = p0.y - m0 * p0.x;
        float c1 = p2.y - slope * p2.x;

        p.x = (c0 - c1) / (slope - m0);
        p.y = slope * p.x + c1;
        return p;
    }

    /*
        get cross point with three points,
        the slope for p2 is perpendicular to line form by p0 and p1
    */
    private PointF getCrossPoint(PointF p0, PointF p1, PointF p2) {
        // calculate slope
        float m0 = (p1.y - p0.y) / (p1.x - p0.x);
        float m1 = -1 / m0;
        return getCrossPoint(p0, p1, p2, m1);
    }

    private PointF getCenter(PointF p0, PointF p1) {
        PointF p = new PointF();

        p.x = (p0.x + p1.x) / 2;
        p.y = (p0.y + p1.y) / 2;
        return p;
    }

    private float getDegree(float x, float y) {
        float degree = (float) Math.toDegrees(Math.atan2(y, x));
        if (degree < 0f) {
            degree += 360f;
        }
        return degree;
    }

    private void initialPointFs() {
        float triLength = width * .3f;
        triPointFs[0] = new PointF(width * .5f, width * .5f - triLength);
        triPointFs[1] = new PointF(width * .5f - triLength * (float) Math.sqrt(3) / 2, width * .5f + triLength * .5f);
        triPointFs[2] = new PointF(width * .5f + triLength * (float) Math.sqrt(3) / 2, width * .5f + triLength * .5f);

        cursorPointF.set(triPointFs[0]);

        centerPointF.set(width * .5f, width * .5f);
    }

    private void initialPaths() {
        outerPath.reset();
        outerPath.addCircle(width * .5f, width * .5f, width * .42f, Path.Direction.CCW);
        outerPath.addCircle(width * .5f, width * .5f, width * .36f, Path.Direction.CW);

        triPath.reset();
        triPath.moveTo(triPointFs[0].x, triPointFs[0].y);
        triPath.lineTo(triPointFs[1].x, triPointFs[1].y);
        triPath.lineTo(triPointFs[2].x, triPointFs[2].y);
    }

    private void initialPaints() {
        outerRingPaint.setAntiAlias(true);
        outerRingPaint.setShader(hueGradient);

        outerIndicatorPaint.setAntiAlias(true);
        outerIndicatorPaint.setStrokeWidth(6);

        triPaint.setAntiAlias(true);
        triPaint.setColor(Color.HSVToColor(selectHsv));

        selectCursorPaint.setAntiAlias(true);
        selectCursorPaint.setStrokeWidth(6);
        selectCursorPaint.setStyle(Paint.Style.STROKE);
    }

    private void initialGradient() {
        int[] COLORS = getResources().getIntArray(R.array.gradientColors);

        hueGradient =
                new SweepGradient(width * .5f, width * .5f, COLORS, null);

        setGradient();
    }

    private void initialRegion() {
        triRegion.setEmpty();
        //noinspection SuspiciousNameCombination
        triRegion.setPath(triPath, new Region(0, 0, width, width));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = MeasureSpec.getSize(widthMeasureSpec);
        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawOuterCircle(canvas);
        drawTriangle(canvas);
        drawCursor(canvas);
    }

    private void drawOuterCircle(Canvas canvas) {
        // draw ring
        canvas.drawPath(outerPath, outerRingPaint);

        // draw selected indicator
        canvas.save();
        canvas.translate(width * .5f, width * .5f);
        canvas.rotate(degree);
        canvas.drawLine(width * .43f, 0, width * .35f, 0, outerIndicatorPaint);
        canvas.restore();
    }

    private void drawTriangle(Canvas canvas) {
        triPaint.setShader(satGradient);
        canvas.drawPath(triPath, triPaint);

        triPaint.setShader(valGradient);
        canvas.drawPath(triPath, triPaint);
    }

    private void setGradient() {
        PointF cPointF = new PointF();
        satGradient = new SweepGradient(
                triPointFs[1].x, triPointFs[1].y,
                new int[]{Color.WHITE, Color.HSVToColor(new float[]{selectHsv[0], 1, 1}), Color.WHITE},
                new float[]{0f, 5f / 6f, 1f}
        );
        cPointF.set(getCenter(triPointFs[0], triPointFs[2]));
        valGradient = new LinearGradient(
                triPointFs[1].x, triPointFs[1].y,
                cPointF.x, cPointF.y,
                Color.BLACK, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
    }

    private void drawCursor(Canvas canvas) {
        selectCursorPaint.setColor(Color.WHITE);
        canvas.drawCircle(cursorPointF.x, cursorPointF.y, width * .012f, selectCursorPaint);
        selectCursorPaint.setColor(Color.BLACK);
        canvas.drawCircle(cursorPointF.x, cursorPointF.y, width * .018f, selectCursorPaint);
    }

    public int getSelectColor() {
        return selectColor;
    }

    @Override
    public void onDegreeSelected(float degree) {
        selectHsv[0] = degree;
        selectColor = Color.HSVToColor(selectHsv);

        setGradient();
        setBackgroundColor(Color.HSVToColor(selectHsv));
        if (colorSelectListener != null) {
            colorSelectListener.onSelectColor(selectColor);
        }
        postInvalidate();
    }
}