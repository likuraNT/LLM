package dev.cartube.parked;

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;

/** Android Automotive OS only. Fail closed if driving state is unknown. */
final class CarSafetyController {
    interface Listener { void onRestricted(boolean restricted); }
    private final Context context;
    private final Listener callback;
    private Car car;
    private CarUxRestrictionsManager manager;
    private final CarUxRestrictionsManager.OnUxRestrictionsChangedListener listener;
    CarSafetyController(Context context, Listener callback) {
        this.context = context;
        this.callback = callback;
        this.listener = r -> this.callback.onRestricted(r == null || r.isRequiresDistractionOptimization());
    }
    void start() {
        callback.onRestricted(true);
        try {
            car = Car.createCar(context);
            if (car == null) return;
            Object service = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);
            if (!(service instanceof CarUxRestrictionsManager)) return;
            manager = (CarUxRestrictionsManager) service;
            manager.registerListener(listener);
            CarUxRestrictions state = manager.getCurrentCarUxRestrictions();
            callback.onRestricted(state == null || state.isRequiresDistractionOptimization());
        } catch (RuntimeException | LinkageError e) {
            callback.onRestricted(true);
        }
    }
    void stop() {
        try {
            if (manager != null) manager.unregisterListener();
            if (car != null) car.disconnect();
        } catch (RuntimeException | LinkageError ignored) {
        } finally { manager = null; car = null; }
    }
}
