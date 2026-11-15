package ai.petdrive.app;

import android.content.Context;

import java.util.Locale;

import ai.pleos.playground.tts.TextToSpeech;
import ai.pleos.playground.tts.constant.ErrorType;
import ai.pleos.playground.tts.constant.Mode;
import ai.pleos.playground.tts.listener.OnInitializeListener;

/**
 * 음성 안내. Pleos 차량에서는 Pleos TTS(기기 내 합성, 앱 등록 불필요),
 * 그 밖의 기기에서는 안드로이드 기본 TTS를 쓴다.
 */
final class Speaker {

    private TextToSpeech pleos;
    private boolean pleosReady;
    private android.speech.tts.TextToSpeech basic;

    Speaker(Context c, boolean pleosCar, MainStatus status) {
        if (pleosCar) {
            try {
                pleos = new TextToSpeech(c.getApplicationContext(), Mode.ON_DEVICE);
                pleos.initialize(new OnInitializeListener() {
                    @Override public void onSuccess() {
                        pleosReady = true;
                        pleos.setLocale(Locale.KOREAN);
                        status.show("Pleos TTS 준비");
                    }
                    @Override public void onFailure(ErrorType e) { status.show("Pleos TTS 실패: " + e); }
                });
                return;
            } catch (RuntimeException e) {
                pleos = null;
                status.show("Pleos TTS 없음, 기본 TTS 사용");
            }
        }
        basic = new android.speech.tts.TextToSpeech(c, s -> {
            if (s == android.speech.tts.TextToSpeech.SUCCESS) basic.setLanguage(Locale.KOREAN);
        });
    }

    void speak(String text) {
        if (pleos != null) {
            if (pleosReady) pleos.speak(text, true);
        } else {
            basic.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "petdrive");
        }
    }

    void release() {
        if (pleos != null) pleos.release();
        if (basic != null) basic.shutdown();
    }

    interface MainStatus { void show(String text); }
}
