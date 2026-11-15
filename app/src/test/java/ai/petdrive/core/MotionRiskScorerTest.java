package ai.petdrive.core;

import java.util.List;

/**
 * JUnit 없이 javac만으로 돌릴 수 있는 테스트.
 * 실행: javac -d build/test app/src/main/java/ai/petdrive/core/*.java app/src/test/java/ai/petdrive/core/*.java
 *       java -cp build/test ai.petdrive.core.MotionRiskScorerTest
 */
public final class MotionRiskScorerTest {

    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        MotionRiskScorer.Profile sensitive = new MotionRiskScorer.Profile(true);

        // 화면 시안의 세 단계 숫자와 같은지
        MotionRiskScorer.Result low = MotionRiskScorer.score(sensitive, 1, 42, 23.0);
        check("편안 22점 (12:40, 급가감속 1회, 출발 후 42분, 23℃)", low.score == 22 && low.level == MotionRiskScorer.Level.LOW);
        MotionRiskScorer.Result mid = MotionRiskScorer.score(sensitive, 5, 67, 24.3);
        check("주의 58점 (13:05, 급가감속 5회, 출발 후 67분, 24.3℃)", mid.score == 58 && mid.level == MotionRiskScorer.Level.MEDIUM);
        MotionRiskScorer.Result high = MotionRiskScorer.score(sensitive, 7, 74, 25.9);
        check("위험 81점 (13:12, 급가감속 7회, 출발 후 74분, 25.9℃)", high.score == 81 && high.level == MotionRiskScorer.Level.HIGH);

        // 경계값
        check("점수는 100을 넘지 않음", MotionRiskScorer.score(sensitive, 50, 500, 40).score == 100);
        check("점수는 0보다 작지 않음", MotionRiskScorer.score(sensitive, 0, 0, 10).score == 0);
        check("멀미 없는 반려견은 같은 조건에서 점수가 낮음",
                MotionRiskScorer.score(new MotionRiskScorer.Profile(false), 5, 67, 24.3).score < mid.score);

        // 속도 샘플로 급가감속 감지: 브레이크를 밟은 급감속만 센다
        MotionRiskScorer s = new MotionRiskScorer(sensitive);
        long t = 0;
        s.onSpeed(t, 80);
        s.onBrake(true);  s.onSpeed(t += 1000, 68);   // -3.3 m/s², 브레이크 O → 셈
        s.onBrake(false); s.onSpeed(t += 1000, 56);   // -3.3 m/s², 브레이크 X → 안 셈
        s.onSpeed(t += 1000, 66);                     // +2.8 m/s² 급가속 → 셈
        s.onSpeed(t += 1000, 68);                     // 완만 → 안 셈
        check("급가감속 2회 감지", s.evaluate(t, 10, 23).harshEvents == 2);
        check("10분이 지나면 기록에서 빠짐", s.evaluate(t + 11 * 60 * 1000L, 10, 23).harshEvents == 0);

        // 보호자 음성 신호: "콩이 헥헥거려" 한 번에 15점, 15분 지나면 빠짐
        MotionRiskScorer v = new MotionRiskScorer(sensitive);
        int before = v.evaluate(0, 42, 23).score;
        v.onOwnerReport(0);
        check("보호자 음성 1회에 15점 더함", v.evaluate(1000, 42, 23).score == before + 15);
        check("음성 신호는 15분 뒤 빠짐", v.evaluate(16 * 60 * 1000L, 42, 23).score == before);

        // 정책: 제어 SDK가 없으면 창문·공조 동작을 내보내지 않음
        CarePolicy basic = new CarePolicy(false);
        List<CarePolicy.Action> a = basic.decide(0, high, 25.5);
        check("기본 단계: 음성 코칭 + 휴게소 제안만",
                a.contains(CarePolicy.Action.SPEAK_COACHING) && a.contains(CarePolicy.Action.SUGGEST_REST_STOP)
                        && !a.contains(CarePolicy.Action.VENTILATE_REAR_WINDOW));
        check("음성 코칭은 5분 안에 반복하지 않음", !basic.decide(60_000, high, 25.5).contains(CarePolicy.Action.SPEAK_COACHING));
        check("휴게소 제안은 한 번만", !basic.decide(400_000, high, 25.5).contains(CarePolicy.Action.SUGGEST_REST_STOP));

        CarePolicy full = new CarePolicy(true);
        List<CarePolicy.Action> b = full.decide(0, high, 25.5);
        check("확장 단계: 창문 환기 + 뒷좌석 냉방 추가",
                b.contains(CarePolicy.Action.VENTILATE_REAR_WINDOW) && b.contains(CarePolicy.Action.COOL_REAR_SEAT));
        check("편안이면 아무것도 안 함", full.decide(0, low, 23).isEmpty());

        // 단계 히스테리시스: 오를 땐 바로, 내릴 땐 3번 연속 충분히 낮아야
        LevelSmoother sm = new LevelSmoother();
        check("오를 때는 바로 위험", sm.update(75) == MotionRiskScorer.Level.HIGH);
        check("한 번 56점으로 떨어져도 위험 유지", sm.update(56) == MotionRiskScorer.Level.HIGH);
        check("하한-8점(62) 근처 66점이면 카운트 안 함", sm.update(66) == MotionRiskScorer.Level.HIGH);
        sm.update(55); sm.update(55);
        check("55점이 3번 연속이면 주의로", sm.update(55) == MotionRiskScorer.Level.MEDIUM);
        sm.reset();
        check("휴식 후 초기화하면 편안", sm.level() == MotionRiskScorer.Level.LOW);

        // 도착 후 기록으로 쉬는 간격 조정: 화면 시안의 80분 → 70분
        ProfileTuner tuner = new ProfileTuner(true);
        check("말에서 컨디션 고르기", ProfileTuner.parse("콩이 오늘 멀미 조금 했어") == ProfileTuner.Condition.A_LITTLE_SICK
                && ProfileTuner.parse("토했어") == ProfileTuner.Condition.VOMITED && ProfileTuner.parse("오늘 날씨 좋다") == null);
        check("멀미 조금 → 80분에서 70분", tuner.onArrivalReport(ProfileTuner.Condition.A_LITTLE_SICK) == 70);
        tuner.onArrivalReport(ProfileTuner.Condition.VOMITED); tuner.onArrivalReport(ProfileTuner.Condition.VOMITED);
        check("간격은 50분 아래로 내려가지 않음", tuner.restIntervalMinutes() == 50);

        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
        if (failed > 0) System.exit(1);
    }
}
