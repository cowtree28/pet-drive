package ai.petdrive.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pleos 디자인 토큰 색과 화면 시안(design/screens/ring.css)에 맞춘 뷰 만들기. */
final class Ui {
    static final int GREEN = Color.parseColor("#02C265");
    static final int AMBER = Color.parseColor("#F59E0B");
    static final int AMBER_TEXT = Color.parseColor("#B45309"); // 흰 바탕에서 읽히는 주의 글자색
    static final int RED = Color.parseColor("#FE3D16");
    static final int RED_BG = Color.parseColor("#FFF3F0");
    static final int INK = Color.parseColor("#131417");
    static final int TEXT2 = Color.parseColor("#44464E");
    static final int GRAY = Color.parseColor("#686A72");
    static final int LIGHT = Color.parseColor("#9C9EA6");
    static final int LINE = Color.parseColor("#E0E1E6");
    static final int TRACK = Color.parseColor("#EDEEF2");
    static final int PANEL = Color.parseColor("#F7F8FA");
    static final int BUTTON = Color.parseColor("#45464E");

    private Ui() {}

    /** Asta Sans는 가변 폰트라 굵기 축(wght)을 직접 지정한다. */
    static Typeface font(android.content.res.AssetManager a, int weight) {
        return new Typeface.Builder(a, "fonts/AstaSans.ttf").setFontVariationSettings("'wght' " + weight).build();
    }

    static TextView text(Context c, Typeface f, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setTypeface(bold ? MainActivity.bold : f);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** 화면의 주 버튼 (시안의 .btn.filled). */
    static TextView button(Context c, Typeface f, String label) {
        TextView b = text(c, f, 28, Color.WHITE, true);
        b.setText(label);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(24, 34, 24, 34);
        b.setBackground(round(BUTTON, 0, 24));
        return b;
    }

    /** 시연용 작은 버튼. 주 버튼보다 눈에 덜 띄게. */
    static TextView chip(Context c, Typeface f, String label) {
        TextView b = text(c, f, 17, TEXT2, true);
        b.setText(label);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(22, 14, 22, 14);
        b.setBackground(round(Color.WHITE, LINE, 40));
        return b;
    }

    static GradientDrawable round(int fill, int stroke, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(2, stroke);
        return d;
    }

    static View dot(Context c, int color, int size) {
        View v = new View(c);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        v.setBackground(d);
        v.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return v;
    }

    static View line(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, 2));
        return v;
    }

    static View space(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, 0, 1));
        return v;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static int levelColor(int score) { return score < 40 ? GREEN : score < 70 ? AMBER : RED; }

    static String levelName(ai.petdrive.core.MotionRiskScorer.Level l) {
        switch (l) {
            case LOW: return "편안";
            case MEDIUM: return "주의";
            default: return "위험";
        }
    }
}
