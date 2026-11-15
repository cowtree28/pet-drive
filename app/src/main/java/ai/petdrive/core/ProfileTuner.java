package ai.petdrive.core;

/**
 * 도착 후 보호자가 말로 남긴 컨디션(SpeechToText)으로 다음 주행의 쉬는 간격을 조정한다.
 *
 * <ul>
 *   <li>"괜찮았어" → 5분 늘림</li>
 *   <li>"멀미 조금 했어" → 10분 줄임</li>
 *   <li>"토했어" → 20분 줄임</li>
 * </ul>
 * 간격은 50~110분 안에서만 움직인다. 수의사 권장(약 1시간)에서 크게 벗어나지 않게 하려는 범위다.
 */
public final class ProfileTuner {

    public enum Condition { FINE, A_LITTLE_SICK, VOMITED }

    static final int MIN_INTERVAL = 50, MAX_INTERVAL = 110;

    private int restIntervalMinutes;

    public ProfileTuner(boolean motionSensitive) {
        this.restIntervalMinutes = motionSensitive ? 80 : 110;
    }

    public int restIntervalMinutes() { return restIntervalMinutes; }

    /** 도착 후 기록을 반영하고 바뀐 간격을 돌려준다. */
    public int onArrivalReport(Condition c) {
        int delta;
        switch (c) {
            case FINE: delta = 5; break;
            case A_LITTLE_SICK: delta = -10; break;
            default: delta = -20; break;
        }
        restIntervalMinutes = Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, restIntervalMinutes + delta));
        return restIntervalMinutes;
    }

    /** STT로 받은 문장에서 컨디션을 고른다. 못 고르면 null. */
    public static Condition parse(String utterance) {
        String u = utterance.replace(" ", "");
        if (u.contains("토했") || u.contains("토를")) return Condition.VOMITED;
        if (u.contains("멀미") || u.contains("힘들어") || u.contains("헥헥")) return Condition.A_LITTLE_SICK;
        if (u.contains("괜찮") || u.contains("잘잤") || u.contains("편했")) return Condition.FINE;
        return null;
    }
}
