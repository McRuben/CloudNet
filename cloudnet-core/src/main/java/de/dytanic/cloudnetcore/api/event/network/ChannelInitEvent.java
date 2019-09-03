/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnetcore.api.event.network;

import de.dytanic.cloudnet.event.Event;
import de.dytanic.cloudnetcore.network.components.INetworkComponent;
import io.netty.channel.Channel;

/**
 * Calls if a channel of some INetworkComponent is connected
 */
public class ChannelInitEvent extends Event {

    private final Channel channel;

    private final INetworkComponent iNetworkComponent;

    public ChannelInitEvent(final Channel channel, final INetworkComponent iNetworkComponent) {
        this.channel = channel;
        this.iNetworkComponent = iNetworkComponent;
    }

    public Channel getChannel() {
        return channel;
    }

    public INetworkComponent getINetworkComponent() {
        return iNetworkComponent;
    }
}
