package ai.petdrive.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.pleos.playground.location.fused.FusedLocation;
import ai.pleos.playground.location.fused.listener.FusedLocationListener;
import ai.pleos.playground.location.fused.model.LocationData;

/**
 * 바깥 기온. Pleos Vehicle SDK에는 바깥 온도가 없어서
 * Pleos FusedLocation으로 차 위치를 받고, 공개 날씨 API(Open-Meteo, 키 불필요)에서 현재 기온을 읽는다.
 * 위치는 소수 둘째 자리(약 1km)로 줄여서 보낸다. 10분마다 갱신.
 */
final class OutsideWeather {

    interface Listener {
        void onOutsideTemp(double celsius);
        void onStatus(String text);
    }

    static final long REFRESH_MS = 10 * 60 * 1000L;
    private static final Pattern CURRENT_TEMP = Pattern.compile("\"current\"\\s*:\\s*\\{[^}]*\"temperature_2m\"\\s*:\\s*(-?[0-9.]+)");

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener out;
    private FusedLocation location;
    private long lastFetchMs;

    private final FusedLocationListener onLocation = new FusedLocationListener() {
        @Override public void onChanged(LocationData d) {
            if (d == null) return;
            long now = System.currentTimeMillis();
            if (now - lastFetchMs < REFRESH_MS) return;
            lastFetchMs = now;
            fetch(d.getLatitude(), d.getLongitude());
        }
        @Override public void onError(Exception e) { post("위치 없음 (차량 개인정보 설정 확인)"); }
    };

    OutsideWeather(Listener out) { this.out = out; }

    void start(Context c) {
        try {
            location = new FusedLocation(c.getApplicationContext());
            location.initialize();
            location.getLastKnownLocation(onLocation);
            location.registerFusedLocationCallback(onLocation);
        } catch (RuntimeException e) {
            location = null;
            post("FusedLocation 없음");
        }
    }

    void stop() {
        if (location == null) return;
        location.unregisterFusedLocationCallback(onLocation);
        location.release();
    }

    private void fetch(double lat, double lon) {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m", lat, lon);
        new Thread(() -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                try (InputStream in = con.getInputStream(); Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A")) {
                    Double t = parse(s.hasNext() ? s.next() : "");
                    if (t != null) main.post(() -> out.onOutsideTemp(t));
                    else post("바깥 기온 응답 해석 실패");
                }
            } catch (Exception e) {
                post("바깥 기온 못 받음");
            } finally {
                if (con != null) con.disconnect();
            }
        }, "outside-weather").start();
    }

    /** Open-Meteo 응답에서 current.temperature_2m만 꺼낸다. */
    static Double parse(String json) {
        Matcher m = CURRENT_TEMP.matcher(json);
        return m.find() ? Double.valueOf(m.group(1)) : null;
    }

    private void post(String s) { main.post(() -> out.onStatus(s)); }
}
