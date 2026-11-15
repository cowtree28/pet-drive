package ai.petdrive.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;

import ai.pleos.playground.stt.SpeechToText;
import ai.pleos.playground.stt.constant.ErrorType;
import ai.pleos.playground.stt.constant.Mode;
import ai.pleos.playground.stt.listener.OnInitializeListener;
import ai.pleos.playground.stt.listener.ResultListener;

/** 도착 후 컨디션을 말로 받는다. Pleos STT(기기 내 인식, 앱 등록 불필요). */
final class VoiceInput {

    interface Listener {
        void onHeard(String text);
        void onStatus(String text);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener out;
    private SpeechToText stt;
    private boolean ready;

    private final ResultListener results = new ResultListener() {
        @Override public void onUpdated(String text, boolean isFinal) {
            if (isFinal && text != null && !text.isEmpty()) main.post(() -> out.onHeard(text));
            else if (text != null) post("듣는 중: " + text);
        }
        @Override public void onUpdatedEpdData(long on, long off) {}
        @Override public void onStartedRecognition() { post("듣는 중"); }
        @Override public void onEndedRecognition() {}
        @Override public void onError(ErrorType e) { post("음성 인식 실패: " + e); }
        @Override public void onReady() {}
        @Override public void onUpdatedRms(double rms) {}
    };

    VoiceInput(Listener out) { this.out = out; }

    void start(Context c) {
        try {
            stt = new SpeechToText(c.getApplicationContext(), Mode.ON_DEVICE);
            stt.addListener(results);
            stt.initialize(new OnInitializeListener() {
                @Override public void onSuccess() { ready = true; stt.setLocale(Locale.KOREA); } // ko-KR (언어만 주면 거절된다)
                @Override public void onFailure(ErrorType e) { post("Pleos STT 실패: " + e); }
            });
        } catch (RuntimeException e) {
            stt = null;
        }
    }

    /** Pleos STT를 쓸 수 있으면 듣기 시작하고 true. 아니면 false(호출한 쪽이 다른 방법을 쓴다). */
    boolean listen() {
        if (stt == null || !ready) return false;
        stt.request();
        return true;
    }

    void stop() {
        if (stt == null) return;
        stt.removeListener(results);
        stt.release();
    }

    private void post(String s) { main.post(() -> out.onStatus(s)); }
}
