package ai.petdrive.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 반려견 멀미 위험 점수(0~100)를 계산한다.
 *
 * <p>입력은 Pleos SDK에서 받는 값이다.
 * <ul>
 *   <li>속도 샘플: CarPropertyManager 차속 또는 FusedLocation 속도</li>
 *   <li>브레이크 페달: Vehicle SDK {@code register-brake-pedal-status}</li>
 *   <li>차 안 온도: Vehicle SDK {@code register-hvac-temperature-current}</li>
 *   <li>보호자 음성: SpeechToText로 "콩이 헥헥거려", "토할 것 같아" 같은 말을 받으면 {@link #onOwnerReport}</li>
 * </ul>
 *
 * <p>점수 = 급가감속 점수(최대 50) + 휴식 경과 점수(최대 30) + 실내 온도 점수(최대 20)
 * + 보호자가 말한 반려견 상태(최근 15분, 1회 15점, 최대 30점). 합계는 100으로 자른다.
 * 멀미가 있는 반려견은 급가감속 기준과 휴식 간격을 더 엄격하게 잡는다.
 * 처음에는 이 규칙으로 시작하고, 도착 후 컨디션 기록이 쌓이면 반려견별로 가중치를 조정한다.
 */
public final class MotionRiskScorer {

    /** 화면 표기: LOW 편안, MEDIUM 주의, HIGH 위험. */
    public enum Level { LOW, MEDIUM, HIGH }

    /** 반려견 프로필. */
    public static final class Profile {
        final boolean motionSensitive;
        public Profile(boolean motionSensitive) { this.motionSensitive = motionSensitive; }
        /** 급가감속 몇 번이면 최대 점수인지 (10분 창 기준). */
        int harshEventsForMax() { return motionSensitive ? 8 : 12; }
        /** 권장 휴식 간격(분). 수의사 권장 약 1시간 기준에서 조정. */
        int restIntervalMinutes() { return motionSensitive ? 80 : 110; }
    }

    /** 결과. */
    public static final class Result {
        public final int score;
        public final Level level;
        public final int harshEvents;
        /** 화면의 "점수 구성"용: 급가감속, 쉰 뒤 시간, 차 안 온도, 보호자 말 (반올림 전 점수). */
        public final double harshPart, restPart, tempPart, reportPart;
        Result(int score, Level level, int harshEvents, double harshPart, double restPart, double tempPart, double reportPart) {
            this.score = score; this.level = level; this.harshEvents = harshEvents;
            this.harshPart = harshPart; this.restPart = restPart; this.tempPart = tempPart; this.reportPart = reportPart;
        }
    }

    static final double HARSH_ACCEL_MS2 = 2.5;      // 이보다 크게 속도가 변하면 급가감속
    static final long WINDOW_MS = 10 * 60 * 1000L;  // 최근 10분만 본다
    static final double COMFORT_TEMP_C = 24.0;      // 이 온도부터 점수에 더한다
    static final double TEMP_RANGE_C = 4.0;         // +4℃에서 온도 점수 최대

    private final Profile profile;
    private final List<long[]> harshTimes = new ArrayList<>(); // [timestampMs]
    private final List<long[]> ownerReports = new ArrayList<>(); // [timestampMs]
    static final long REPORT_WINDOW_MS = 15 * 60 * 1000L;
    static final int REPORT_POINTS = 15, REPORT_MAX = 30;
    private long lastSpeedTs = -1;
    private double lastSpeedKmh;
    private boolean brakePressed;

    public MotionRiskScorer(Profile profile) { this.profile = profile; }

    /** 속도 샘플을 넣는다. 급가감속이면 기록한다. */
    public void onSpeed(long timestampMs, double speedKmh) {
        if (lastSpeedTs >= 0 && timestampMs > lastSpeedTs) {
            double dt = (timestampMs - lastSpeedTs) / 1000.0;
            double accel = ((speedKmh - lastSpeedKmh) / 3.6) / dt; // m/s²
            boolean harshDecel = accel <= -HARSH_ACCEL_MS2;
            boolean harshAccel = accel >= HARSH_ACCEL_MS2;
            // 감속은 브레이크를 밟고 있을 때만 센다 (내리막 관성 감속 오검출 방지)
            if (harshAccel || (harshDecel && brakePressed)) {
                harshTimes.add(new long[]{timestampMs});
            }
        }
        lastSpeedTs = timestampMs;
        lastSpeedKmh = speedKmh;
    }

    /** 브레이크 페달 상태를 넣는다. */
    public void onBrake(boolean pressed) { this.brakePressed = pressed; }

    /** 보호자가 반려견이 힘들어한다고 말했을 때 (SpeechToText 키워드 인식). */
    public void onOwnerReport(long timestampMs) { ownerReports.add(new long[]{timestampMs}); }

    /** 휴게소에서 쉬고 출발하면 호출한다. 급가감속·음성 기록도 비운다. */
    public void onRestFinished() { harshTimes.clear(); ownerReports.clear(); }

    /** 현재 점수를 계산한다. */
    public Result evaluate(long nowMs, double minutesSinceRest, double cabinTempC) {
        harshTimes.removeIf(t -> nowMs - t[0] > WINDOW_MS);
        ownerReports.removeIf(t -> nowMs - t[0] > REPORT_WINDOW_MS);
        Result base = score(profile, harshTimes.size(), minutesSinceRest, cabinTempC);
        int bonus = Math.min(REPORT_MAX, ownerReports.size() * REPORT_POINTS);
        int total = Math.min(100, base.score + bonus);
        return new Result(total, levelOf(total), base.harshEvents, base.harshPart, base.restPart, base.tempPart, bonus);
    }

    /** 순수 계산 함수. 테스트와 화면 시안의 숫자가 여기서 나온다. */
    public static Result score(Profile p, int harshEvents, double minutesSinceRest, double cabinTempC) {
        double harshPart = 50.0 * clamp01((double) harshEvents / p.harshEventsForMax());
        double restPart = 30.0 * clamp01(minutesSinceRest / p.restIntervalMinutes());
        double tempPart = 20.0 * clamp01((cabinTempC - COMFORT_TEMP_C) / TEMP_RANGE_C);
        int total = (int) Math.round(harshPart + restPart + tempPart);
        total = Math.max(0, Math.min(100, total));
        return new Result(total, levelOf(total), harshEvents, harshPart, restPart, tempPart, 0);
    }

    public static Level levelOf(int score) {
        if (score < 40) return Level.LOW;
        if (score < 70) return Level.MEDIUM;
        return Level.HIGH;
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
