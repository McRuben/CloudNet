/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.event.bukkit;

import de.dytanic.cloudnet.lib.player.CloudPlayer;
import org.bukkit.event.HandlerList;

/**
 * Calls if a player was updated on network
 */
public class BukkitPlayerUpdateEvent extends BukkitCloudEvent {

    private static final HandlerList handlerList = new HandlerList();
    private final CloudPlayer cloudPlayer;

    public BukkitPlayerUpdateEvent(final CloudPlayer cloudPlayer) {
        this.cloudPlayer = cloudPlayer;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public CloudPlayer getCloudPlayer() {
        return cloudPlayer;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
