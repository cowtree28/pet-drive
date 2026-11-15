package ai.petdrive.app;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.api.model.area.SeatArea;
import ai.pleos.playground.vehicle.constant.area.SeatValue;
import ai.pleos.playground.vehicle.constant.control.VehicleGear;
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener;
import ai.pleos.playground.vehicle.listener.GearSelectionListener;
import ai.pleos.playground.vehicle.listener.HvacAcStateListener;
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener;

/**
 * 차량 신호 구독. Pleos Vehicle SDK(브레이크, 실내 온도, 에어컨, 기어)와
 * 안드로이드 CarPropertyManager(속도, Vehicle SDK에 속도 API가 없음)를 쓴다.
 * 모든 콜백은 메인 스레드로 넘긴다.
 */
final class CarSignals {

    interface Listener {
        void onSpeed(float kmh);
        void onBrake(boolean pressed);
        void onCabinTemp(float celsius);
        void onAc(boolean on);
        void onGear(boolean park);
        void onStatus(String text);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener out;
    private Vehicle vehicle;
    private Car car;
    private CarPropertyManager props;
    // Pleos 에뮬레이터의 공조는 앞·뒷줄을 한 영역(areaId 117)으로 보낸다. 같은 묶음으로 구독해야 이벤트가 온다.
    private final SeatArea cabinArea = new SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT,
            SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT);
    private final SeatArea rear = cabinArea;
    private final SeatArea driver = cabinArea;

    private final BrakePedalStatusListener brake = new BrakePedalStatusListener() {
        @Override public void onBrakePedalStatusUpdated(Boolean b) { main.post(() -> out.onBrake(Boolean.TRUE.equals(b))); }
        @Override public void onFailed(Exception e) { fail("브레이크", e); }
    };
    private final HvacTemperatureListener temp = new HvacTemperatureListener() {
        @Override public void onHvacTemperatureUpdated(Float t) { if (t != null) main.post(() -> out.onCabinTemp(t)); }
        @Override public void onFailed(Exception e) { fail("실내 온도", e); }
    };
    private final HvacAcStateListener ac = new HvacAcStateListener() {
        @Override public void onHvacAcStateUpdated(Boolean on) { main.post(() -> out.onAc(Boolean.TRUE.equals(on))); }
        @Override public void onFailed(Exception e) { fail("에어컨", e); }
    };
    private final GearSelectionListener gear = new GearSelectionListener() {
        @Override public void onSelected(VehicleGear g) { main.post(() -> out.onGear(g == VehicleGear.GEAR_PARK)); }
        @Override public void onFailed(Exception e) { fail("기어", e); }
    };
    private final CarPropertyManager.CarPropertyEventCallback speed = new CarPropertyManager.CarPropertyEventCallback() {
        @Override public void onChangeEvent(CarPropertyValue v) {
            Object val = v.getValue();
            if (val instanceof Float) main.post(() -> out.onSpeed(Math.abs((Float) val) * 3.6f)); // m/s → km/h
        }
        @Override public void onErrorEvent(int propId, int zone) { main.post(() -> out.onStatus("속도 신호 오류")); }
    };

    CarSignals(Listener out) { this.out = out; }

    /** Pleos 차량(자동차용 안드로이드)인지. 아니면 가상 주행 기록을 쓴다. */
    static boolean available(Context c) {
        return c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    void start(Context c) {
        try {
            vehicle = new Vehicle(c.getApplicationContext());
            vehicle.initialize();
            vehicle.getBrake().registerBrakePedalStatus(brake);
            vehicle.getHVAC().registerHvacTemperatureCurrent(rear, temp);
            vehicle.getHVAC().registerHvacAcState(driver, ac);
            vehicle.getGear().registerGearSelection(gear);
            out.onStatus("Pleos Vehicle SDK 연결");
        } catch (RuntimeException e) {
            fail("Vehicle SDK", e);
        }
        try {
            car = Car.createCar(c.getApplicationContext());
            props = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
            props.registerCallback(speed, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL);
        } catch (RuntimeException e) {
            fail("속도", e);
        }
    }

    void stop() {
        if (vehicle != null) {
            try {
                vehicle.getBrake().unregisterBrakePedalStatus(brake);
                vehicle.getHVAC().unregisterHvacTemperatureCurrent(rear, temp);
                vehicle.getHVAC().unregisterHvacAcState(driver, ac);
                vehicle.getGear().unregisterGearSelection(gear);
            } catch (RuntimeException ignored) {
                // 연결이 이미 끊겼으면 해제만 한다
            }
            vehicle.release();
        }
        if (props != null) props.unregisterCallback(speed);
        if (car != null) car.disconnect();
    }

    private void fail(String what, Exception e) {
        main.post(() -> out.onStatus(what + " 신호 실패: " + e.getClass().getSimpleName()));
    }
}
