package ai.petdrive.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ai.petdrive.core.CabinHeat;
import ai.petdrive.core.CarePolicy;
import ai.petdrive.core.LevelSmoother;
import ai.petdrive.core.MotionRiskScorer;
import ai.petdrive.core.ProfileTuner;

/**
 * 펫 드라이브 데모 앱. 주행(멀미 케어) · 펫 지킴이 · 도착 세 화면.
 *
 * Pleos 차량·에뮬레이터: CarSignals로 차량 신호를 구독한다.
 *   속도(CarPropertyManager) → scorer.onSpeed, 브레이크(Vehicle) → scorer.onBrake,
 *   실내 온도·에어컨(Vehicle) → cabin·펫 지킴이, 기어 P → 펫 지킴이 화면
 * 그 밖의 기기, 또는 "기록 재생"을 누르면 가상 주행 기록(DriveReplay와 같은 시나리오, 60배속)을 쓴다.
 * 음성은 Speaker(Pleos TTS, 없으면 안드로이드 TTS). 도착 기록은 아직 안드로이드 음성 인식.
 */
public final class MainActivity extends Activity {

    static final int START_MIN = 11 * 60 + 58;
    static final String VOICE_COACH = "급정거가 잦았어요. 부드럽게 운전해 주세요";
    static final String VOICE_REST = "콩이가 힘들어해요. 가까운 휴게소에서 쉬어 가요";
    static final int REQ_STT = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Typeface font;
    static Typeface bold;
    private Speaker speaker;
    private CarSignals signals;
    private RestStopNavi navi;
    private VoiceInput voice;
    private OutsideWeather weather;
    private double outsideC = 33; // 데모 시나리오 값. Pleos 차량에서는 FusedLocation 위치의 실제 기온으로 바뀐다
    private boolean outsideReal, outsideDemo;
    private long guardStartMs;
    private boolean carMode, tripStarted;
    private long carStartMs;
    private TextView clock, sourceLabel;
    private final List<TextView> tabs = new ArrayList<>();
    private final List<View> pages = new ArrayList<>();

    // 주행
    private final ProfileTuner tuner = new ProfileTuner(true);
    private MotionRiskScorer scorer;
    private LevelSmoother smoother;
    private CarePolicy policy;
    private int vsec, restStartSec, maxScore;
    private double speed, cabin = 23.0;
    private boolean driving, hardBrakeQueued;
    private RingView driveRing;
    private Bar bHarsh, bRest, bTemp, bReport;
    private TextView dHarsh, dRest, dSpeed, dNextTime, dNextNote, dVoice, dPlay;
    private LinearLayout dVoiceRow;
    private int restEtaMin = -1, restEtaKm, rests;
    private String maxAt = "";

    // 펫 지킴이
    private boolean guardOn, acOn = true;
    private int guardMin;
    private double guardTemp = 24.5;
    private RingView guardRing;
    private View gDot;
    private TextView gOutside, gOutsideNote;
    private TextView gState, gSub, gGone, gCall, gToggle;
    private LinearLayout gSide, gFeatures, gLog, gRecords, gDanger, gCallbox;
    private final List<String[]> tempLog = new ArrayList<>();

    // 도착
    private RingView arriveRing;
    private TextView aHeard, aInterval, aAsk, aWhoSub;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        font = Ui.font(getAssets(), 400);
        bold = Ui.font(getAssets(), 700);
        carMode = CarSignals.available(this);

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Color.parseColor("#FAFAFB"));
        root.setPadding(40, 24, carMode ? 150 : 40, 32); // Pleos 화면 오른쪽의 떠 있는 버튼을 피한다
        root.addView(topBar());
        FrameLayout body = new FrameLayout(this);
        pages.add(drivePage());
        pages.add(guardPage());
        pages.add(arrivePage());
        for (View p : pages) body.addView(p);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 30) { // 차 화면에서는 시스템 바를 숨긴다
            getWindow().getInsetsController().hide(android.view.WindowInsets.Type.systemBars());
        }

        speaker = new Speaker(this, carMode, this::status);
        resetDrive();
        show(0);
        if (carMode) startCar();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (signals != null) signals.stop();
        if (navi != null) navi.stop();
        if (voice != null) voice.stop();
        if (weather != null) weather.stop();
        speaker.release();
        super.onDestroy();
    }

    // ---------- 차량 신호 (Pleos) ----------

    private void startCar() {
        requestPermissions(new String[]{"android.car.permission.CAR_SPEED", "pleos.car.permission.FUSED_LOCATION"}, 1);
        carStartMs = android.os.SystemClock.elapsedRealtime();
        signals = new CarSignals(new CarSignals.Listener() {
            @Override public void onSpeed(float kmh) {
                if (driving) return; // 기록 재생 중에는 차량 신호를 쓰지 않는다
                speed = kmh;
                scorer.onSpeed(carNow(), kmh);
                dSpeed.setText(Math.round(kmh) + " km/h");
            }
            @Override public void onBrake(boolean pressed) { if (!driving) scorer.onBrake(pressed); }
            @Override public void onCabinTemp(float c) {
                if (driving) return;
                cabin = c;
                if (guardOn && acOn) { guardTemp = c; renderGuard(); }
            }
            @Override public void onAc(boolean on) { acOn = on; renderGuard(); }
            @Override public void onGear(boolean park) {
                if (driving) return;
                if (park && (vsec - restStartSec) > 60) arriveAtRest(); // P단 → 휴식 기록, 펫 지킴이 화면
                else if (!park) {
                    show(0);
                    if (!tripStarted && navi != null) { tripStarted = true; navi.startTrip(); backToFront(); }
                }
            }
            @Override public void onStatus(String text) { status(text); }
        });
        signals.start(this);
        navi = new RestStopNavi(new RestStopNavi.Listener() {
            @Override public void onStatus(String text) { status(text); }
            @Override public void onArrived() { arriveAtRest(); } // 내비 경유지 도착 = 휴식
            @Override public void onRestStopAdded(int minutes, int km) { restEtaMin = minutes; restEtaKm = km; evaluate(); }
        });
        navi.start(this);
        voice = new VoiceInput(new VoiceInput.Listener() {
            @Override public void onHeard(String text) { record(text); }
            @Override public void onStatus(String text) { aAsk.setText("●  " + text); }
        });
        voice.start(this);
        weather = new OutsideWeather(new OutsideWeather.Listener() {
            @Override public void onOutsideTemp(double c) { if (outsideDemo) return; outsideC = c; outsideReal = true; renderGuard(); }
            @Override public void onStatus(String text) { status(text); }
        });
        weather.start(this);
        handler.post(carTick);
    }

    /** 내비가 경로를 그리려고 앞으로 나오면, 잠시 뒤 펫 드라이브 화면을 다시 앞으로 가져온다. */
    private void backToFront() {
        handler.postDelayed(() -> startActivity(new android.content.Intent(this, MainActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | android.content.Intent.FLAG_ACTIVITY_NEW_TASK)), 6000);
    }

    private long carNow() { return android.os.SystemClock.elapsedRealtime() - carStartMs; }

    /** 차량 모드: 실제 시간으로 1초마다 진행, 5초마다 점수 갱신. */
    private final Runnable carTick = new Runnable() {
        @Override public void run() {
            if (!driving) {
                vsec = (int) (carNow() / 1000);
                if (vsec % 5 == 0) evaluate();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void status(String text) {
        sourceLabel.setText("   " + (carMode ? "Pleos 차량 신호" : "데모 · 가상 주행 기록 60배속") + " · " + text);
    }

    // ---------- 공통 ----------

    private View topBar() {
        LinearLayout bar = Ui.row(this);
        TextView title = Ui.text(this, font, 26, Ui.INK, true);
        title.setText("펫 드라이브");
        sourceLabel = Ui.text(this, font, 16, Ui.LIGHT, false);
        sourceLabel.setText(carMode ? "   Pleos 차량 신호" : "   데모 · 가상 주행 기록 60배속");
        sourceLabel.setSingleLine(true);
        sourceLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout left = Ui.row(this);
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(ai.petdrive.R.mipmap.ic_launcher);
        logo.setClipToOutline(true);
        logo.setBackground(Ui.round(Ui.INK, 0, 10));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(44, 44);
        llp.rightMargin = 14;
        left.addView(logo, llp);
        left.addView(title);
        left.addView(sourceLabel, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout seg = Ui.row(this);
        seg.setBackground(Ui.round(Ui.TRACK, 0, 40));
        seg.setPadding(6, 6, 6, 6);
        String[] names = {"주행", "펫 지킴이", "도착"};
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            TextView t = Ui.text(this, font, 20, Ui.GRAY, true);
            t.setText(names[i]);
            t.setPadding(34, 14, 34, 14);
            t.setOnClickListener(v -> show(k));
            tabs.add(t);
            seg.addView(t);
        }
        bar.addView(seg);
        clock = Ui.text(this, font, 24, Ui.INK, false);
        clock.setGravity(Gravity.END);
        bar.addView(clock, new LinearLayout.LayoutParams(0, -2, 1));
        bar.setPadding(8, 0, 8, 22);
        return bar;
    }

    private void show(int k) {
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setVisibility(i == k ? View.VISIBLE : View.GONE);
            tabs.get(i).setTextColor(i == k ? Color.WHITE : Ui.GRAY);
            tabs.get(i).setBackground(i == k ? Ui.round(Ui.INK, 0, 40) : null);
        }
        if (k == 2) renderArrive();
    }

    private void speak(String s) {
        speaker.speak(s);
        dVoice.setText("“" + s + "”");
        dVoiceRow.setVisibility(View.VISIBLE);
    }

    /** 시안의 카드: 왼쪽 흰 영역(링), 오른쪽 회색 패널(정보와 행동). */
    private LinearLayout card(LinearLayout left, LinearLayout right) {
        LinearLayout page = Ui.row(this);
        page.setGravity(Gravity.FILL_VERTICAL);
        page.setBackground(Ui.round(Color.WHITE, Ui.LINE, 32));
        left.setPadding(56, 48, 48, 44);
        page.addView(left, new LinearLayout.LayoutParams(0, -1, 11));
        right.setPadding(52, 48, 52, 44);
        right.setBackground(Ui.round(Ui.PANEL, 0, 32));
        page.addView(right, new LinearLayout.LayoutParams(0, -1, 9));
        return page;
    }

    /** "콩이와 동승  중형견, 멀미 있음" 같은 첫 줄. */
    private TextView who(LinearLayout parent, String name, String sub) {
        LinearLayout r = Ui.row(this);
        r.setGravity(Gravity.BOTTOM);
        TextView n = Ui.text(this, font, 30, Ui.INK, true);
        n.setText(name);
        TextView s = Ui.text(this, font, 22, Ui.GRAY, false);
        s.setText(sub);
        s.setPadding(16, 0, 0, 2);
        r.addView(n);
        r.addView(s);
        parent.addView(r);
        return s;
    }

    /** 링을 남는 공간 가운데에 둔다. */
    private void ringArea(LinearLayout parent, View ring, View aside) {
        LinearLayout area = Ui.row(this);
        area.setGravity(Gravity.CENTER);
        area.setPadding(0, 24, 0, 24);
        area.addView(ring, new LinearLayout.LayoutParams(0, -1, 1));
        if (aside != null) area.addView(aside, new LinearLayout.LayoutParams(-2, -2));
        parent.addView(area, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    /** 아래쪽 사실 3칸 (시안 01의 바깥·연료·뒷좌석 줄). */
    private TextView fact(LinearLayout row, String key) {
        LinearLayout c = Ui.column(this);
        TextView k = Ui.text(this, font, 18, Ui.GRAY, false);
        k.setText(key);
        TextView v = Ui.text(this, font, 30, Ui.INK, true);
        v.setPadding(0, 8, 0, 0);
        c.addView(k);
        c.addView(v);
        row.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
        return v;
    }

    private LinearLayout factRow(LinearLayout parent) {
        parent.addView(Ui.line(this));
        LinearLayout r = Ui.row(this);
        r.setPadding(0, 22, 0, 0);
        parent.addView(r);
        return r;
    }

    /** 오른쪽 패널의 한 줄: 이름(굵게) + 설명(작게). */
    private TextView item(LinearLayout parent, String title, String sub) {
        LinearLayout r = Ui.column(this);
        r.setPadding(0, 20, 0, 20);
        TextView t = Ui.text(this, font, 26, Ui.INK, true);
        t.setText(title);
        r.addView(t);
        if (sub != null) {
            TextView s = Ui.text(this, font, 19, Ui.GRAY, false);
            s.setText(sub);
            s.setPadding(0, 8, 0, 0);
            r.addView(s);
        }
        parent.addView(r);
        parent.addView(Ui.line(this));
        return t;
    }

    /** 점수 구성 한 줄: 이름 · 값 · 막대 · 점수. */
    private final class Bar {
        final TextView value, points;
        final View fill, rest;
        final int max;
        Bar(TextView value, TextView points, View fill, View rest, int max) {
            this.value = value; this.points = points; this.fill = fill; this.rest = rest; this.max = max;
        }
        void set(double part, String v) {
            float f = (float) Math.max(0, Math.min(max, part));
            ((LinearLayout.LayoutParams) fill.getLayoutParams()).weight = f;
            ((LinearLayout.LayoutParams) rest.getLayoutParams()).weight = max - f;
            fill.setVisibility(f > 0.4f ? View.VISIBLE : View.GONE);
            fill.requestLayout();
            value.setText(v);
            points.setText(Math.round(f) + "/" + max);
        }
    }

    private Bar bar(LinearLayout parent, String label, int max) {
        LinearLayout r = Ui.column(this);
        r.setPadding(0, 16, 0, 0);
        LinearLayout top = Ui.row(this);
        TextView k = Ui.text(this, font, 20, Ui.TEXT2, false);
        k.setText(label);
        top.addView(k);
        TextView v = Ui.text(this, font, 20, Ui.INK, true);
        v.setPadding(12, 0, 0, 0);
        top.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
        TextView pts = Ui.text(this, font, 18, Ui.GRAY, false);
        top.addView(pts);
        r.addView(top);
        LinearLayout track = Ui.row(this);
        track.setBackground(Ui.round(Ui.TRACK, 0, 6));
        View fill = new View(this);
        fill.setBackground(Ui.round(Ui.TEXT2, 0, 6));
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, 0));
        View rest = new View(this);
        track.addView(rest, new LinearLayout.LayoutParams(0, -1, max));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 12);
        lp.topMargin = 10;
        r.addView(track, lp);
        parent.addView(r);
        return new Bar(v, pts, fill, rest, max);
    }

    private TextView heading(LinearLayout parent, String text) {
        TextView h = Ui.text(this, font, 21, Ui.GRAY, true);
        h.setText(text);
        h.setPadding(0, 0, 0, 6);
        parent.addView(h);
        return h;
    }

    /** 시연용 버튼 줄. 화면 맨 아래, 작게. */
    private LinearLayout demoRow(LinearLayout parent) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, 18, 0, 0);
        TextView k = Ui.text(this, font, 16, Ui.LIGHT, true);
        k.setText("시연");
        k.setPadding(0, 0, 14, 0);
        r.addView(k);
        parent.addView(r);
        return r;
    }

    private TextView addChip(LinearLayout row, String label, View.OnClickListener l) {
        TextView b = Ui.chip(this, font, label);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = 10;
        row.addView(b, lp);
        return b;
    }

    // ---------- 주행: 멀미 케어 ----------

    private View drivePage() {
        LinearLayout left = Ui.column(this);
        who(left, "콩이와 동승", "중형견, 멀미 있음");
        driveRing = new RingView(this, font);
        ringArea(left, driveRing, null);
        LinearLayout facts = factRow(left);
        dSpeed = fact(facts, "속도");
        dHarsh = fact(facts, "급가감속 · 10분");
        dRest = fact(facts, "쉬고 나서");

        LinearLayout right = Ui.column(this);
        heading(right, "다음 휴식");
        LinearLayout next = Ui.row(this);
        TextView place = Ui.text(this, font, 34, Ui.INK, true);
        place.setText(RestStopNavi.REST_NAME);
        next.addView(place, new LinearLayout.LayoutParams(0, -2, 1));
        dNextTime = Ui.text(this, font, 34, Ui.INK, true);
        next.addView(dNextTime);
        right.addView(next);
        dNextNote = Ui.text(this, font, 19, Ui.GRAY, false);
        dNextNote.setPadding(0, 10, 0, 22);
        right.addView(dNextNote);
        right.addView(Ui.line(this));
        TextView sh = heading(right, "점수 구성");
        sh.setPadding(0, 26, 0, 4);
        bHarsh = bar(right, "급가감속", 50);
        bRest = bar(right, "쉬지 않고 달린 시간", 30);
        bTemp = bar(right, "차 안 온도", 20);
        bReport = bar(right, "보호자가 알린 증상", 30);
        right.addView(Ui.line(this));
        dVoiceRow = Ui.column(this);
        dVoiceRow.setPadding(0, 22, 0, 0);
        TextView vk = Ui.text(this, font, 18, Ui.GRAY, true);
        vk.setText("음성 안내");
        dVoice = Ui.text(this, font, 24, Ui.TEXT2, true);
        dVoice.setPadding(0, 10, 0, 0);
        dVoiceRow.addView(vk);
        dVoiceRow.addView(dVoice);
        dVoiceRow.setVisibility(View.GONE);
        right.addView(dVoiceRow);
        right.addView(Ui.space(this));
        LinearLayout demo = demoRow(right);
        addChip(demo, "급정거", v -> hardBrakeQueued = true);
        addChip(demo, "콩이 힘들어해", v -> { scorer.onOwnerReport(vsec * 1000L); evaluate(); });
        addChip(demo, "휴게소 도착", v -> arriveAtRest());
        dPlay = addChip(demo, "기록 재생", v -> toggleDrive());
        return card(left, right);
    }

    private void resetDrive() {
        scorer = new MotionRiskScorer(new MotionRiskScorer.Profile(true));
        smoother = new LevelSmoother();
        policy = new CarePolicy(false); // 차량 제어 SDK 공개 전: 음성·경로만
        vsec = restStartSec = maxScore = 0;
        speed = 0;
        cabin = 23.0;
        dVoiceRow.setVisibility(View.GONE);
        evaluate();
    }

    private void toggleDrive() {
        driving = !driving;
        dPlay.setText(driving ? "재생 멈춤" : "기록 재생");
        if (driving) handler.post(driveTick);
        else {
            handler.removeCallbacks(driveTick);
            if (carMode) carStartMs = android.os.SystemClock.elapsedRealtime() - vsec * 1000L; // 재생이 끝난 시각부터 이어서
        }
    }

    /** 50ms마다 가상 3초씩 진행 (60배속). 시나리오는 DriveReplay와 같다. */
    private final Runnable driveTick = new Runnable() {
        @Override public void run() {
            for (int i = 0; i < 3; i++) stepSecond();
            if (driving) handler.postDelayed(this, 50);
        }
    };

    private void stepSecond() {
        vsec++;
        boolean jam = vsec / 60 >= 57;
        double target = jam ? ((vsec / 40) % 2 == 0 ? 60 : 20) : (vsec / 60 < 3 ? 60 : 100);
        boolean brake = target < speed - 5;
        double step = brake ? (jam ? 3.8 : 1.2) : (jam ? 1.9 : 1.0);
        if (Math.abs(target - speed) <= step) speed = target;
        else speed += (target > speed ? step : -step);
        if (jam && brake && (vsec / 40) % 5 == 1) speed = Math.max(target, speed - 8.2);
        if (hardBrakeQueued && speed > 20) { // 버튼으로 넣는 급정거: 1초에 12km/h ≈ 3.3m/s²
            speed -= 12;
            brake = true;
            hardBrakeQueued = false;
        }
        scorer.onBrake(brake);
        scorer.onSpeed(vsec * 1000L, speed);
        cabin = vsec / 60 < 60 ? 23.0 : Math.min(25.5, 23.0 + (vsec / 60 - 60) * 0.14);
        if (vsec % 60 == 0) evaluate();
        else dSpeed.setText(Math.round(speed) + " km/h");
    }

    private void evaluate() {
        double sinceRest = (vsec - restStartSec) / 60.0;
        MotionRiskScorer.Result r = scorer.evaluate(vsec * 1000L, sinceRest, cabin);
        MotionRiskScorer.Level shown = smoother.update(r.score);
        if (r.score > maxScore) { maxScore = r.score; maxAt = clock.getText().toString(); }
        for (CarePolicy.Action a : policy.decide(vsec * 1000L, shown, r.harshEvents, cabin)) {
            if (a == CarePolicy.Action.SPEAK_COACHING && shown != MotionRiskScorer.Level.HIGH) speak(VOICE_COACH); // 위험 단계에서는 휴식 안내가 우선
            if (a == CarePolicy.Action.SUGGEST_REST_STOP) {
                speak(VOICE_REST);
                if (navi != null && !driving) { navi.addRestStop(); backToFront(); } // Pleos 내비 경유지에 반려견 휴게소
            }
        }
        int color = shown == MotionRiskScorer.Level.LOW ? Ui.GREEN : shown == MotionRiskScorer.Level.MEDIUM ? Ui.AMBER : Ui.RED;
        driveRing.set(r.score / 100f, color, Ui.levelName(shown), "멀미 위험 100점 중 " + r.score, "");
        dSpeed.setText(Math.round(speed) + " km/h");
        dHarsh.setText(r.harshEvents + "회");
        dRest.setText((int) sinceRest + "분");
        bHarsh.set(r.harshPart, r.harshEvents + "회");
        bRest.set(r.restPart, (int) sinceRest + "분");
        bTemp.set(r.tempPart, String.format(Locale.KOREA, "%.1f℃", cabin));
        bReport.set(r.reportPart, r.reportPart > 0 ? "있음" : "없음");
        if (restEtaMin >= 0) {
            dNextTime.setText(restEtaMin + "분 뒤");
            dNextNote.setText("반려견 놀이터 · 내비 경유지에 추가됨 · " + restEtaKm + "km");
        } else {
            dNextTime.setText(Math.max(0, tuner.restIntervalMinutes() - (int) sinceRest) + "분 뒤");
            dNextNote.setText("반려견 놀이터 · 쉬는 간격 " + tuner.restIntervalMinutes() + "분");
        }
        setClock(0);
    }

    private void setClock(int extraMin) {
        if (carMode && !driving) { // 차량 모드는 실제 시각
            clock.setText(new java.text.SimpleDateFormat("HH:mm", Locale.KOREA).format(new java.util.Date()));
            return;
        }
        int t = START_MIN + vsec / 60 + extraMin;
        clock.setText(String.format(Locale.KOREA, "%02d:%02d", (t / 60) % 24, t % 60));
    }

    private void arriveAtRest() {
        if (driving) toggleDrive();
        scorer.onRestFinished();
        policy.onRestFinished();
        smoother.reset();
        restStartSec = vsec;
        restEtaMin = -1;
        rests++;
        speed = 0;
        evaluate();
        show(1);
    }

    // ---------- 펫 지킴이 ----------

    private View guardPage() {
        LinearLayout left = Ui.column(this);
        LinearLayout st = Ui.row(this);
        gDot = Ui.dot(this, Ui.GREEN, 18);
        st.addView(gDot);
        gState = Ui.text(this, font, 32, Ui.INK, true);
        gState.setPadding(16, 0, 0, 0);
        st.addView(gState);
        gSub = Ui.text(this, font, 22, Ui.GRAY, false);
        gSub.setText("중형견, 멀미 있음");
        gSub.setPadding(16, 0, 0, 0);
        st.addView(gSub);
        left.addView(st);
        guardRing = new RingView(this, font);
        LinearLayout aside = Ui.column(this);
        aside.setPadding(36, 0, 0, 0);
        TextView ok = Ui.text(this, font, 20, Ui.GRAY, true);
        ok.setText("바깥");
        gOutside = Ui.text(this, font, 64, Ui.TEXT2, true);
        gOutsideNote = Ui.text(this, font, 16, Ui.LIGHT, false);
        aside.addView(ok);
        aside.addView(gOutside);
        aside.addView(gOutsideNote);
        gGone = asideKv(aside, "보호자가 떠난 지");
        gCall = asideKv(aside, "보호자");
        ringArea(left, guardRing, aside);

        gSide = Ui.column(this);
        gFeatures = Ui.column(this);
        heading(gFeatures, "펫 지킴이");
        item(gFeatures, "에어컨 22℃ 유지", "20분마다 보호자에게 연장 확인");
        item(gFeatures, "문 잠금", null);
        item(gFeatures, "바깥 안내 화면", "센서로 확인한 사실만 표시");
        gSide.addView(gFeatures);
        gLog = Ui.column(this);
        heading(gLog, "보호자 연락처");
        TextView tel = Ui.text(this, font, 36, Ui.INK, true);
        tel.setText("0504-0000-2831");
        tel.setPadding(0, 4, 0, 30);
        gLog.addView(tel);
        heading(gLog, "차 안 온도 기록");
        gRecords = Ui.column(this);
        gLog.addView(gRecords);
        gSide.addView(gLog);
        gDanger = Ui.column(this);
        heading(gDanger, "자동 대응");
        item(gDanger, "창문 3cm 열림", "차량 제어 SDK 공개 후");
        item(gDanger, "실내등 켜짐", "차량 제어 SDK 공개 후");
        item(gDanger, "보호자에게 전화 중", null);
        gSide.addView(gDanger);
        gSide.addView(Ui.space(this));
        gCallbox = Ui.column(this);
        TextView ck = Ui.text(this, font, 19, Ui.GRAY, true);
        ck.setText("보호자 연락처");
        TextView cn = Ui.text(this, font, 36, Ui.INK, true);
        cn.setText("0504-0000-2831");
        cn.setPadding(0, 6, 0, 6);
        TextView c119 = Ui.text(this, font, 19, Ui.GRAY, false);
        c119.setText("받지 않으면 119에 신고해 주세요");
        gCallbox.addView(ck); gCallbox.addView(cn); gCallbox.addView(c119);
        gCallbox.setPadding(0, 0, 0, 22);
        gSide.addView(gCallbox);
        gToggle = Ui.button(this, font, "펫 지킴이 켜기");
        gToggle.setOnClickListener(v -> toggleGuard());
        gSide.addView(gToggle);
        LinearLayout demo = demoRow(gSide);
        addChip(demo, "에어컨 멈춤", v -> { acOn = false; renderGuard(); });
        addChip(demo, "한여름 33℃", v -> { outsideC = 33; outsideReal = false; outsideDemo = true; renderGuard(); });
        renderGuard();
        return card(left, gSide);
    }

    private TextView asideKv(LinearLayout aside, String key) {
        TextView k = Ui.text(this, font, 20, Ui.GRAY, true);
        k.setText(key);
        k.setPadding(0, 34, 0, 0);
        TextView v = Ui.text(this, font, 32, Ui.INK, true);
        v.setPadding(0, 6, 0, 0);
        aside.addView(k);
        aside.addView(v);
        return v;
    }

    private void toggleGuard() {
        guardOn = !guardOn;
        if (guardOn) {
            guardMin = 0; guardTemp = carMode ? cabin : 24.5; acOn = true; tempLog.clear();
            guardStartMs = System.currentTimeMillis();
            handler.postDelayed(guardTick, 1000);
        } else handler.removeCallbacks(guardTick);
        renderGuard();
    }

    /** 1초 = 1분. 실제 앱은 1분마다 차 안 온도를 읽어 보호자 휴대폰으로 보낸다. */
    private final Runnable guardTick = new Runnable() {
        @Override public void run() {
            guardMin++;
            // 바깥 기온 기준 어림 (core/CabinHeat). 실제 차량에서는 실내 온도 신호가 들어오면 그 값으로 덮어쓴다
            guardTemp = CabinHeat.nextMinute(guardTemp, outsideC, acOn, 22.0);
            String at = new java.text.SimpleDateFormat("HH:mm", Locale.KOREA).format(new java.util.Date(guardStartMs + guardMin * 60_000L)); // 1초 = 1분 시연 시각
            tempLog.add(0, new String[]{at, String.format(Locale.KOREA, "%.1f℃", guardTemp)});
            if (tempLog.size() > 5) tempLog.remove(5);
            renderGuard();
            if (guardOn) handler.postDelayed(this, 1000);
        }
    };

    private void renderGuard() {
        boolean danger = guardOn && guardTemp > 27;
        int left = 20 - guardMin % 20;
        String temp = String.format(Locale.KOREA, "%.1f℃", guardTemp);
        if (clock != null) setClock(guardOn ? guardMin : 0);
        if (!guardOn) guardRing.set(0, Ui.INK, temp, "지금 차 안", "");
        else if (danger) guardRing.set(1, Ui.RED, temp, "반려견이 있는 차 안", "보호자를 부르는 중");
        else if (!acOn) {
            int m = CabinHeat.minutesUntil(guardTemp, outsideC, false, 22.0, 27.0);
            guardRing.set(left / 20f, Ui.AMBER, temp, "반려견이 있는 차 안", m >= 0 ? "약 " + m + "분 뒤 27℃" : "에어컨 꺼짐");
        }
        else guardRing.set(left / 20f, Ui.GREEN, temp, "반려견이 있는 차 안", "다음 확인까지 " + left + "분");
        gOutside.setText(carMode && !outsideReal && !outsideDemo ? "-" : String.format(Locale.KOREA, "%.0f℃", outsideC));
        gOutsideNote.setText(outsideDemo || !carMode ? "시연 값" : outsideReal ? "현재 위치 기온" : "위치 확인 중");
        gSub.setVisibility(guardOn ? View.GONE : View.VISIBLE);
        if (!guardOn && carMode) guardTemp = cabin; // 꺼져 있을 때는 지금 차 안 온도 그대로
        temp = String.format(Locale.KOREA, "%.1f℃", guardTemp);
        if (!guardOn) guardRing.set(0, Ui.INK, temp, "지금 차 안", "");
        gState.setText(!guardOn ? "콩이" : danger ? "에어컨 멈춤 · 차 안 온도 상승" : !acOn ? "에어컨 꺼짐 · 온도 오르는 중" : "반려견 탑승 중 · 에어컨 켜짐");
        gState.setTextColor(danger ? Ui.RED : Ui.INK);
        gDot.setVisibility(guardOn ? View.VISIBLE : View.GONE);
        ((android.graphics.drawable.GradientDrawable) gDot.getBackground()).setColor(danger ? Ui.RED : !acOn ? Ui.AMBER : Ui.GREEN);
        gGone.setText(guardOn ? guardMin + "분" : "-");
        gCall.setText(danger ? "전화하는 중" : guardOn ? "휴대폰 확인 중" : "-");
        gCall.setTextColor(danger ? Ui.RED : Ui.INK);
        gToggle.setText(guardOn ? "펫 지킴이 끄기" : "펫 지킴이 켜기");
        gToggle.setBackground(Ui.round(danger ? Ui.RED : Ui.BUTTON, 0, 24));
        gSide.setBackground(Ui.round(danger ? Ui.RED_BG : Ui.PANEL, 0, 32));
        gFeatures.setVisibility(guardOn ? View.GONE : View.VISIBLE);
        gLog.setVisibility(guardOn && !danger ? View.VISIBLE : View.GONE);
        gDanger.setVisibility(danger ? View.VISIBLE : View.GONE);
        gCallbox.setVisibility(danger ? View.VISIBLE : View.GONE);
        gRecords.removeAllViews();
        for (String[] rec : tempLog.subList(0, Math.min(4, tempLog.size()))) {
            LinearLayout r = Ui.row(this);
            r.setPadding(0, 16, 0, 16);
            TextView t = Ui.text(this, font, 24, Ui.GRAY, false);
            t.setText(rec[0]);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            TextView v = Ui.text(this, font, 24, Ui.INK, true);
            v.setText(rec[1]);
            r.addView(v);
            gRecords.addView(r);
            gRecords.addView(Ui.line(this));
        }
    }

    // ---------- 도착: 컨디션 기록 ----------

    private View arrivePage() {
        LinearLayout left = Ui.column(this);
        aWhoSub = who(left, "콩이와 도착", "");
        arriveRing = new RingView(this, font);
        ringArea(left, arriveRing, null);

        LinearLayout right = Ui.column(this);
        TextView h = Ui.text(this, font, 34, Ui.INK, true);
        h.setText("콩이 오늘 어땠나요?");
        right.addView(h);
        aAsk = Ui.text(this, font, 26, Color.WHITE, true);
        aAsk.setText("●  눌러서 말하기");
        aAsk.setPadding(30, 26, 30, 26);
        aAsk.setBackground(Ui.round(Ui.INK, 0, 22));
        aAsk.setOnClickListener(v -> listen());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 28;
        right.addView(aAsk, lp);
        LinearLayout heard = Ui.row(this);
        heard.setPadding(4, 26, 4, 20);
        TextView hk = Ui.text(this, font, 21, Ui.GRAY, false);
        hk.setText("기록");
        heard.addView(hk, new LinearLayout.LayoutParams(0, -2, 1));
        aHeard = Ui.text(this, font, 32, Ui.INK, true);
        aHeard.setText("-");
        heard.addView(aHeard);
        right.addView(heard);
        right.addView(Ui.line(this));
        TextView nk = Ui.text(this, font, 21, Ui.INK, true);
        nk.setText("다음 주행부터");
        nk.setPadding(4, 26, 0, 6);
        right.addView(nk);
        LinearLayout iv = Ui.row(this);
        iv.setPadding(4, 14, 4, 20);
        TextView ik = Ui.text(this, font, 24, Ui.GRAY, false);
        ik.setText("쉬는 간격");
        iv.addView(ik, new LinearLayout.LayoutParams(0, -2, 1));
        aInterval = Ui.text(this, font, 28, Ui.INK, true);
        aInterval.setText(tuner.restIntervalMinutes() + "분");
        iv.addView(aInterval);
        right.addView(iv);
        right.addView(Ui.line(this));
        right.addView(Ui.space(this));
        TextView done = Ui.button(this, font, "확인");
        done.setOnClickListener(v -> show(0));
        right.addView(done);
        LinearLayout demo = demoRow(right);
        addChip(demo, "괜찮았어", v -> record("콩이 오늘 괜찮았어"));
        addChip(demo, "멀미 조금 했어", v -> record("콩이 오늘 멀미 조금 했어"));
        addChip(demo, "토했어", v -> record("콩이 오늘 토했어"));
        return card(left, right);
    }

    private void listen() {
        if (voice != null && voice.listen()) return; // Pleos STT
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        try { startActivityForResult(i, REQ_STT); }
        catch (ActivityNotFoundException e) { aAsk.setText("●  음성 인식 없음 · 아래 버튼으로 기록"); }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_STT || res != RESULT_OK || data == null) return;
        ArrayList<String> said = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (said != null && !said.isEmpty()) record(said.get(0));
    }

    private void record(String utterance) {
        ProfileTuner.Condition c = ProfileTuner.parse(utterance);
        aAsk.setText("“" + utterance + "”");
        if (c == null) { aHeard.setText("다시 말해 주세요"); return; }
        int before = tuner.restIntervalMinutes();
        int after = tuner.onArrivalReport(c);
        aHeard.setText(c == ProfileTuner.Condition.FINE ? "괜찮음" : c == ProfileTuner.Condition.A_LITTLE_SICK ? "멀미 조금" : "토함");
        aInterval.setText(before + "분 → " + after + "분");
    }

    private void renderArrive() {
        arriveRing.set(maxScore / 100f, Ui.levelColor(maxScore), String.valueOf(maxScore), "오늘 가장 높았던 멀미 위험", maxAt);
        aWhoSub.setText(vsec / 60 + "분 주행, 휴식 " + rests + "번");
    }
}
