package ai.petdrive.core;

/**
 * 정차 중 차 안 온도가 1분 뒤 얼마가 될지 어림한다 (펫 지킴이 시연·예측용).
 *
 * 에어컨이 켜져 있으면 설정 온도로 다가가고, 꺼지면 바깥 기온 쪽으로 다가간다.
 * 햇볕 때문에 닫힌 차는 바깥보다 더 뜨거워지므로(약 +10℃까지) 목표를 바깥 기온 + 10℃로 둔다.
 * 다가가는 속도는 1분에 남은 차이의 8% (바깥 33℃, 실내 22℃면 첫 1분에 약 +1.7℃).
 * 실제 차량에서는 Vehicle SDK의 실내 온도를 그대로 쓰고, 이 값은 "몇 분 뒤 위험해지는지" 안내에만 쓴다.
 */
public final class CabinHeat {

    static final double SUN_GAIN_C = 10.0;
    static final double RATE_AC_OFF = 0.08;
    static final double RATE_AC_ON = 0.35;

    private CabinHeat() {}

    /** 1분 뒤 차 안 온도. */
    public static double nextMinute(double cabinC, double outsideC, boolean acOn, double acSetC) {
        if (acOn) return cabinC + (acSetC - cabinC) * RATE_AC_ON;
        double target = outsideC + SUN_GAIN_C;
        return cabinC + Math.max(0, target - cabinC) * RATE_AC_OFF;
    }

    /** 지금 상태로 두면 몇 분 뒤 limitC를 넘는지. 넘지 않으면 -1 (최대 120분까지 본다). */
    public static int minutesUntil(double cabinC, double outsideC, boolean acOn, double acSetC, double limitC) {
        double t = cabinC;
        for (int m = 0; m <= 120; m++) {
            if (t > limitC) return m;
            t = nextMinute(t, outsideC, acOn, acSetC);
        }
        return -1;
    }
}
