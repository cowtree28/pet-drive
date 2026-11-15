package ai.petdrive.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** 화면마다 하나씩 쓰는 진행 링. 가운데에 큰 값 하나와 설명 두 줄을 그린다. */
public final class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private float progress;
    private String value = "", label = "", hint = "";

    public RingView(Context c, Typeface font) {
        super(c);
        for (Paint p : new Paint[]{track, arc}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        track.setColor(Ui.TRACK);
        for (Paint p : new Paint[]{big, sub, note}) {
            p.setTypeface(font);
            p.setTextAlign(Paint.Align.CENTER);
        }
        big.setTypeface(MainActivity.bold);
        note.setTypeface(MainActivity.bold);
        sub.setColor(Ui.GRAY);
    }

    /** progress 0~1, color 링과 큰 글자 색. */
    public void set(float progress, int color, String value, String label, String hint) {
        this.progress = Math.max(0, Math.min(1, progress));
        arc.setColor(color);
        int textColor = color == Ui.AMBER ? Ui.AMBER_TEXT : color; // 주의 단계 글자는 더 진한 색
        big.setColor(color == Ui.GREEN || color == Ui.INK ? Ui.INK : textColor);
        note.setColor(color == Ui.INK ? Ui.GRAY : textColor);
        this.value = value; this.label = label; this.hint = hint;
        invalidate();
    }

    @Override protected void onMeasure(int w, int h) {
        int s = Math.min(MeasureSpec.getSize(w), MeasureSpec.getSize(h));
        setMeasuredDimension(s, s);
    }

    @Override protected void onDraw(Canvas c) {
        float s = getWidth(), stroke = s * 0.07f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        box.set(stroke, stroke, s - stroke, s - stroke);
        c.drawArc(box, 0, 360, false, track);
        if (progress > 0) c.drawArc(box, -90, 360 * progress, false, arc);

        big.setTextSize(s * (value.length() > 3 ? 0.19f : 0.22f));
        sub.setTextSize(s * 0.05f);
        note.setTextSize(s * 0.05f);
        float cy = s / 2;
        c.drawText(value, s / 2, cy + s * 0.05f, big);
        c.drawText(label, s / 2, cy + s * 0.15f, sub);
        c.drawText(hint, s / 2, cy + s * 0.225f, note);
    }
}
