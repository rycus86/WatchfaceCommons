package hu.rycus.watchface.commons;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    private static final String TAG = "Scheduler";

    private static final int MSG_INTERACTIVE_TIME_TICK = 0xFFFF;

    private final Map<Integer, List<Component>> registeredComponents = new HashMap<>();
    private final Map<Integer, Long> intervals = new HashMap<>();

    private final BaseCanvasWatchFaceService.BaseEngine engine;
    private SchedulerHandler handler;

    public Scheduler(final BaseCanvasWatchFaceService.BaseEngine engine) {
        this.engine = engine;
    }

    public void initialize() {
        this.handler = new SchedulerHandler(this);

        registerInteractiveTimeUpdater();

        if (!engine.isInAmbientMode()) {
            startInteractiveTimeUpdater();
        }
    }

    private boolean shouldNotRunHandlerTasks() {
        return !engine.isVisible() || engine.isInAmbientMode();
    }

    private void registerInteractiveTimeUpdater() {
        Log.d(TAG, String.format("Register interactive time updater with code %x",
                MSG_INTERACTIVE_TIME_TICK));

        this.intervals.put(MSG_INTERACTIVE_TIME_TICK, TimeUnit.MINUTES.toMillis(1L));
    }

    public void register(final Component component, final int what, final long interval) {
        Log.d(TAG, String.format("Register/start messages with code %x for %s",
                what, component.getClass().getSimpleName()));

        List<Component> components = registeredComponents.get(what);
        if (components == null) {
            components = new LinkedList<>();
            registeredComponents.put(what, components);
        }

        if (!components.contains(component)) {
            components.add(component);
        }

        if (intervals.containsKey(what)) {
            final long running = intervals.get(what);
            if (running != interval) {
                final String message = String.format(Locale.getDefault(),
                        "An interval of %d ms is already configured for %x, " +
                        "the requested %d will not be applied for %s!",
                        running, what, interval, component.getClass());
                Log.w(TAG, message);
            }
        } else {
            intervals.put(what, interval);
        }

        start(what);
    }

    public void unregister(final Component component, final int what) {
        Log.d(TAG, String.format("Unregister %s from messages with code %x",
                component.getClass().getSimpleName(), what));

        final List<Component> components = registeredComponents.get(what);
        if (components != null) {
            if (components.remove(component)) {
                if (components.isEmpty()) {
                    registeredComponents.remove(what);
                    stop(what);
                }
            }
        }
    }

    public void enable() {
        startInteractiveTimeUpdater();

        for (final int what : registeredComponents.keySet()) {
            start(what);
        }
    }

    public void disable() {
        stopInteractiveTimeUpdater();

        for (final int what : registeredComponents.keySet()) {
            stop(what);
        }
    }

    public void startInteractiveTimeUpdater() {
        start(MSG_INTERACTIVE_TIME_TICK);
    }

    public void stopInteractiveTimeUpdater() {
        stop(MSG_INTERACTIVE_TIME_TICK);
    }

    private void start(final int what) {
        if (handler != null) {
            if (handler.hasMessages(what)) {
                return;
            }

            final Long interval = intervals.get(what);
            if (interval != null) {
                final long delay = interval - (System.currentTimeMillis() % interval);
                handler.sendEmptyMessageDelayed(what, delay);
            }
        }
    }

    private static boolean isInteractiveTimeUpdater(final int what) {
        return what == MSG_INTERACTIVE_TIME_TICK;
    }

    private void stop(final int what) {
        if (handler != null) {
            handler.removeMessages(what);
        }
    }

    private static class SchedulerHandler extends Handler {

        private final Scheduler parent;

        private SchedulerHandler(final Scheduler parent) {
            this.parent = parent;
        }

        @Override
        public void handleMessage(final Message msg) {
            if (parent.shouldNotRunHandlerTasks()) {
                return;
            }

            final int what = msg.what;

            boolean shouldScheduleAgain = false;
            boolean shouldInvalidate = false;

            if (isInteractiveTimeUpdater(what)) {
                Log.d(TAG, "Sending time tick in interactive mode");

                parent.engine.onTimeTick();

                shouldScheduleAgain = true;
                shouldInvalidate = true;
            } else {
                final List<Component> components = parent.registeredComponents.get(what);
                if (components == null) {
                    return;
                }

                for (final Component component : components) {
                    try {
                        if (component.isActive() && component.needsScheduler()) {
                            component.onHandleMessage(what);

                            shouldScheduleAgain = true;
                            shouldInvalidate |= component.shouldInvalidate();
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, String.format("Failed to handle message %x for %s",
                                what, component.getClass().getSimpleName()), ex);
                    }
                }
            }

            if (shouldScheduleAgain) {
                parent.start(what);
            }

            if (shouldInvalidate) {
                parent.engine.invalidate();
            }
        }

    }

}
