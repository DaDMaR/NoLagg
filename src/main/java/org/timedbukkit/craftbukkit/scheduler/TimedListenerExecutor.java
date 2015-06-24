package org.timedbukkit.craftbukkit.scheduler;

import com.bergerkiller.bukkit.common.proxies.ProxyBase;
import com.bergerkiller.bukkit.nolagg.NoLaggUtil;
import com.bergerkiller.bukkit.nolagg.examine.ListenerMeasurement;
import com.bergerkiller.bukkit.nolagg.examine.PluginLogger;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

public class TimedListenerExecutor extends ProxyBase<EventExecutor> implements EventExecutor {
    public ListenerMeasurement meas;
    public PluginLogger logger;

    public TimedListenerExecutor(PluginLogger logger, EventExecutor base, ListenerMeasurement meas) {
        super(base);
        this.meas = meas;
        this.logger = logger;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (!logger.isRunning() || !(event instanceof Cancellable)) {
            // Disable this listening executor
            NoLaggUtil.exefield.set(meas.listener, base);
            // Execute like normal
            base.execute(listener, event);
            return;
        }
        final Cancellable canc = (Cancellable) event;
        final boolean wasCancelled = canc.isCancelled();
        base.execute(listener, event);
        if (!wasCancelled && canc.isCancelled()) {
            // Event was cancelled by this listener, keep track of this!
            this.meas.cancelCount++;
        }
    }
}
