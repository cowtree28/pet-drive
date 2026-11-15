package ai.petdrive.core;

import java.util.List;

/**
 * 가상 주행 기록을 1초 단위로 재생하며 멀미 위험 점수와 차의 동작을 출력한다.
 * 실제 앱에서는 같은 값이 Pleos SDK 구독 콜백으로 들어온다.
 *
 * 시나리오: 11:58 서울 출발 → 고속도로 순항 → 12:55부터 정체 구간 급정거 → 차 안 온도 상승 → 13:20 덕평자연휴게소
 *
 * 실행: java -cp build/test ai.petdrive.core.DriveReplay [제어SDK사용여부 true|false]
 */
public final class DriveReplay {

    static final int START_MIN = 11 * 60 + 58;

    public static void main(String[] args) {
        boolean control = args.length > 0 && Boolean.parseBoolean(args[0]);
        MotionRiskScorer scorer = new MotionRiskScorer(new MotionRiskScorer.Profile(true));
        CarePolicy policy = new CarePolicy(control);
        LevelSmoother smoother = new LevelSmoother();

        System.out.println("콩이(멀미 있음) 서울 → 덕평자연휴게소, 차량 제어 SDK " + (control ? "사용" : "미사용"));
        System.out.println("시각   점수  화면 단계  급가감속  차 안   차가 하는 일");

        double speed = 0;
        MotionRiskScorer.Level lastLevel = null;
        for (int sec = 0; sec <= 82 * 60; sec++) {
            int min = sec / 60;
            long now = sec * 1000L;

            // 속도 패턴: 출발 가속 → 100km/h 순항 → 57분부터 정체(급정거·급출발 반복)
            boolean jam = min >= 57;
            double target = jam ? ((sec / 40) % 2 == 0 ? 60 : 20) : (min < 3 ? 60 : 100);
            boolean brake = target < speed - 5;
            double step = brake ? (jam ? 3.8 : 1.2) : (jam ? 1.9 : 1.0); // km/h per sec
            if (Math.abs(target - speed) <= step) speed = target;
            else speed += (target > speed ? step : -step);

            // 급정거 강도: 정체 구간 일부는 더 세게 밟는다 (약 12km/h/s ≈ 3.3m/s²)
            if (jam && brake && (sec / 40) % 5 == 1) speed = Math.max(target, speed - 8.2);

            scorer.onBrake(brake);
            scorer.onSpeed(now, speed);

            double cabin = min < 60 ? 23.0 : Math.min(25.5, 23.0 + (min - 60) * 0.14); // 정체 중 햇볕에 실내 온도 상승

            if (sec % 60 == 0) {
                MotionRiskScorer.Result r = scorer.evaluate(now, min, cabin);
                MotionRiskScorer.Level shown = smoother.update(r.score);
                List<CarePolicy.Action> acts = policy.decide(now, shown, r.harshEvents, cabin);
                boolean print = min % 10 == 0 || shown != lastLevel || !acts.isEmpty();
                if (print) {
                    int clock = START_MIN + min;
                    System.out.printf("%02d:%02d  %3d  %-6s  %5d회  %4.1f℃  %s%n",
                            clock / 60, clock % 60, r.score, label(shown), r.harshEvents, cabin, describe(acts));
                }
                lastLevel = shown;
            }
        }
        System.out.println("13:20  덕평자연휴게소 도착 → onRestFinished()로 기록 초기화");
    }

    static String label(MotionRiskScorer.Level l) {
        switch (l) { case LOW: return "편안"; case MEDIUM: return "주의"; default: return "위험"; }
    }

    static String describe(List<CarePolicy.Action> acts) {
        if (acts.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (CarePolicy.Action a : acts) {
            if (sb.length() > 0) sb.append(", ");
            switch (a) {
                case SPEAK_COACHING: sb.append("음성 코칭"); break;
                case SUGGEST_REST_STOP: sb.append("휴게소 경유지 제안"); break;
                case VENTILATE_REAR_WINDOW: sb.append("뒷창문 환기"); break;
                case COOL_REAR_SEAT: sb.append("뒷좌석 냉방"); break;
            }
        }
        return sb.toString();
    }
}
