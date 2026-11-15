package ai.petdrive.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import ai.pleos.playground.navi.constants.NaviErrorCode;
import ai.pleos.playground.navi.constants.RouteOption;
import ai.pleos.playground.navi.data.RouteInfo;
import ai.pleos.playground.navi.constants.WaypointIndex;
import ai.pleos.playground.navi.data.RequestWaypointInfo;
import ai.pleos.playground.navi.data.WaypointArrivedInfo;
import ai.pleos.playground.navi.data.WaypointChangedInfo;
import ai.pleos.playground.navi.helper.NaviHelper;
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener;

/**
 * Pleos NaviHelper로 반려견 휴게소를 경유지에 넣고, 경유지에 도착하면 알려 준다.
 * 경유지는 최대 2개라 첫 번째 자리(FIRST)를 쓴다.
 */
final class RestStopNavi {

    interface Listener {
        void onStatus(String text);
        void onArrived();
        void onRestStopAdded(int minutes, int km);
    }

    // 덕평자연휴게소 (영동고속도로, 반려견 놀이터가 있는 휴게소)
    static final double REST_LAT = 37.2419, REST_LON = 127.3594;
    static final String REST_NAME = "덕평자연휴게소";
    // 이번 여행 목적지 (출발 화면에서 정한 강릉)
    static final double DEST_LAT = 37.7519, DEST_LON = 128.8761;
    static final String DEST_NAME = "강릉";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener out;
    private NaviHelper navi;

    private final NaviHelperEventListener events = new NaviHelperEventListener() {
        @Override public void onWaypointChanged(WaypointChangedInfo info) {
            post("경유지 추가: " + REST_NAME);
            if (info != null && info.getRouteStartInfo() != null) { // 내비가 계산한 휴게소까지 시간·거리
                int min = Math.round(info.getRouteStartInfo().getDuration() / 60f);
                int km = Math.round(info.getRouteStartInfo().getDistance() / 1000f);
                main.post(() -> out.onRestStopAdded(min, km));
            }
        }
        @Override public void onWayPointArrived(WaypointArrivedInfo info) { main.post(out::onArrived); }
        @Override public void onError(NaviErrorCode code) { post("내비 오류: " + code); }
        @Override public void onRoutingCompleted() { post("경로 안내 시작: " + DEST_NAME); }
    };

    RestStopNavi(Listener out) { this.out = out; }

    void start(Context c) {
        try {
            navi = new NaviHelper(c.getApplicationContext());
            navi.initialize();
            navi.addListener(events);
        } catch (RuntimeException e) {
            navi = null;
            post("NaviHelper 없음");
        }
    }

    /** 출발할 때 목적지 경로를 잡는다. 경유지는 안내 중인 경로가 있어야 들어간다. */
    void startTrip() {
        if (navi == null) return;
        try {
            navi.requestRoute(new RouteInfo(DEST_LAT, DEST_LON, null, null, DEST_NAME, "", "", "", RouteOption.HIGHWAY, "", false));
            post("경로 요청: " + DEST_NAME);
        } catch (RuntimeException e) {
            post("경로 실패: " + e.getClass().getSimpleName());
        }
    }

    /** 위험 단계에서 한 번 부른다. */
    void addRestStop() {
        if (navi == null) return;
        try {
            navi.addWaypoint(new RequestWaypointInfo(REST_LAT, REST_LON, REST_NAME, WaypointIndex.FIRST, "", "", "", ""));
            post("경유지 요청: " + REST_NAME);
        } catch (RuntimeException e) {
            post("경유지 실패: " + e.getClass().getSimpleName());
        }
    }

    void stop() {
        if (navi == null) return;
        navi.removeListener(events);
        navi.release();
    }

    private void post(String s) { main.post(() -> out.onStatus(s)); }
}
