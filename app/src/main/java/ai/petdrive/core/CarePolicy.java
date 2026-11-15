package ai.petdrive.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 멀미 위험 단계에 따라 차가 할 일을 정한다.
 *
 * <p>기본 단계(지금 공모전 SDK로 가능): 음성 안내, 휴게소 경유지 제안.
 * 확장 단계(차량 제어 SDK 공개 시): 뒷창문 환기, 뒷좌석 공조 조절.
 * 확장 동작은 {@code controlAvailable}이 true일 때만 내보낸다.
 */
public final class CarePolicy {

    public enum Action { SPEAK_COACHING, SUGGEST_REST_STOP, VENTILATE_REAR_WINDOW, COOL_REAR_SEAT }

    static final long COACHING_COOLDOWN_MS = 5 * 60 * 1000L; // 같은 안내를 5분 안에 반복하지 않는다

    private final boolean controlAvailable;
    private long lastCoachingMs = Long.MIN_VALUE / 2;
    private boolean restSuggested;

    public CarePolicy(boolean controlAvailable) { this.controlAvailable = controlAvailable; }

    public List<Action> decide(long nowMs, MotionRiskScorer.Result r, double cabinTempC) {
        return decide(nowMs, r.level, r.harshEvents, cabinTempC);
    }

    /** {@link LevelSmoother}로 다듬은 단계를 넣을 때 쓴다. */
    public List<Action> decide(long nowMs, MotionRiskScorer.Level level, int harshEvents, double cabinTempC) {
        List<Action> out = new ArrayList<>();
        if (level == MotionRiskScorer.Level.LOW) return out;

        if (nowMs - lastCoachingMs >= COACHING_COOLDOWN_MS && harshEvents >= 3) {
            out.add(Action.SPEAK_COACHING);
            lastCoachingMs = nowMs;
        }
        if (controlAvailable) out.add(Action.VENTILATE_REAR_WINDOW);
        if (level == MotionRiskScorer.Level.HIGH) {
            if (!restSuggested) { out.add(Action.SUGGEST_REST_STOP); restSuggested = true; }
            if (controlAvailable && cabinTempC > 23.0) out.add(Action.COOL_REAR_SEAT);
        }
        return out;
    }

    /** 휴게소에서 쉬고 나면 다시 제안할 수 있게 한다. */
    public void onRestFinished() { restSuggested = false; }
}
