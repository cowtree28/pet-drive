package ai.petdrive.core;

/**
 * 화면과 안내에 쓰는 단계가 출렁이지 않게 잡아 준다 (히스테리시스).
 *
 * <p>점수는 최근 10분 창으로 계산해서 급정거가 몰렸다 빠질 때 크게 오르내린다.
 * 그대로 화면에 쓰면 단계가 주의와 위험 사이를 오가며 운전자의 시선을 끈다.
 * <ul>
 *   <li>오를 때: 바로 올린다 (반려견이 힘들어지는 건 빨리 알아야 한다)</li>
 *   <li>내릴 때: 점수가 그 단계 하한보다 {@link #MARGIN}점 이상 낮은 상태가
 *       {@link #HOLD_EVALS}번 연속(1분 간격이면 3분) 이어질 때만 한 단계 내린다</li>
 * </ul>
 */
public final class LevelSmoother {

    static final int MARGIN = 8;
    static final int HOLD_EVALS = 3;

    private MotionRiskScorer.Level level = MotionRiskScorer.Level.LOW;
    private int belowCount = 0;

    public MotionRiskScorer.Level update(int score) {
        MotionRiskScorer.Level raw = MotionRiskScorer.levelOf(score);
        if (raw.ordinal() > level.ordinal()) {
            level = raw;
            belowCount = 0;
        } else if (raw.ordinal() < level.ordinal() && score < lowerBound(level) - MARGIN) {
            if (++belowCount >= HOLD_EVALS) {
                level = MotionRiskScorer.Level.values()[level.ordinal() - 1];
                belowCount = 0;
            }
        } else {
            belowCount = 0;
        }
        return level;
    }

    public MotionRiskScorer.Level level() { return level; }

    /** 휴식 후 출발하면 처음부터 다시 본다. */
    public void reset() { level = MotionRiskScorer.Level.LOW; belowCount = 0; }

    static int lowerBound(MotionRiskScorer.Level l) {
        switch (l) { case HIGH: return 70; case MEDIUM: return 40; default: return 0; }
    }
}
