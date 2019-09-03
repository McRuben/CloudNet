/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.event.proxied;

import de.dytanic.cloudnet.lib.server.info.ServerInfo;

/**
 * Calls if one server was removed from the network
 */
public class ProxiedServerRemoveEvent extends ProxiedCloudEvent {

    private final ServerInfo serverInfo;

    public ProxiedServerRemoveEvent(final ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}
