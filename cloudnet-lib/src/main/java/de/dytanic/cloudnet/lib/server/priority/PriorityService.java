package de.dytanic.cloudnet.lib.server.priority;

/**
 * Created by Tareko on 18.07.2017.
 */
public class PriorityService {

    private final int stopTimeInSeconds;

    private final PriorityConfig global;

    private final PriorityConfig group;

    public PriorityService(final int stopTimeInSeconds, final PriorityConfig global, final PriorityConfig group) {
        this.stopTimeInSeconds = stopTimeInSeconds;
        this.global = global;
        this.group = group;
    }

    public int getStopTimeInSeconds() {
        return stopTimeInSeconds;
    }

    public PriorityConfig getGlobal() {
        return global;
    }

    public PriorityConfig getGroup() {
        return group;
    }
}
