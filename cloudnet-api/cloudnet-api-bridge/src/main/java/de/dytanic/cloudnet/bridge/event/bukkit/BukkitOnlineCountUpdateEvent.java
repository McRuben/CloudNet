/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.event.bukkit;

import org.bukkit.event.HandlerList;

/**
 * Created by Tareko on 11.10.2017.
 */
public class BukkitOnlineCountUpdateEvent extends BukkitCloudEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final int onlineCount;

    public BukkitOnlineCountUpdateEvent(final int onlineCount) {
        this.onlineCount = onlineCount;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
