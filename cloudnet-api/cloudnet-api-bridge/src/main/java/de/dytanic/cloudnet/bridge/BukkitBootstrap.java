/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge;

import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.api.config.CloudConfigLoader;
import de.dytanic.cloudnet.api.config.ConfigTypeLoader;
import de.dytanic.cloudnet.bridge.event.bukkit.BukkitCloudServerInitEvent;
import de.dytanic.cloudnet.bridge.internal.chat.DocumentRegistry;
import de.dytanic.cloudnet.bridge.internal.command.bukkit.CommandCloudServer;
import de.dytanic.cloudnet.bridge.internal.command.bukkit.CommandResource;
import de.dytanic.cloudnet.bridge.internal.listener.bukkit.BukkitListener;
import de.dytanic.cloudnet.bridge.internal.serverselectors.MobSelector;
import de.dytanic.cloudnet.bridge.internal.serverselectors.SignSelector;
import de.dytanic.cloudnet.bridge.internal.serverselectors.packet.in.PacketInMobSelector;
import de.dytanic.cloudnet.bridge.internal.serverselectors.packet.in.PacketInSignSelector;
import de.dytanic.cloudnet.bridge.vault.VaultInvoker;
import de.dytanic.cloudnet.lib.network.protocol.packet.PacketRC;
import de.dytanic.cloudnet.lib.server.ServerGroupMode;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.nio.file.Paths;

/**
 * Created by Tareko on 17.08.2017.
 */
public final class BukkitBootstrap extends JavaPlugin {

    private CloudServer cloudServer;

    @Override
    public void onLoad() {
        final CloudAPI cloudAPI = new CloudAPI(
            new CloudConfigLoader(Paths.get("CLOUD/connection.json"), Paths.get("CLOUD/config.json"), ConfigTypeLoader.INTERNAL),
            new BukkitCancelTask());
        cloudAPI.getNetworkConnection().getPacketManager().registerHandler(PacketRC.SERVER_SELECTORS + 1, PacketInSignSelector.class);
        cloudAPI.getNetworkConnection().getPacketManager().registerHandler(PacketRC.SERVER_SELECTORS + 2, PacketInMobSelector.class);

        cloudAPI.setLogger(getLogger());
    }

    @Override
    public void onDisable() {

        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        if (CloudAPI.getInstance() != null) {
            cloudServer.updateDisable();
            CloudAPI.getInstance().shutdown();

            CloudAPI.getInstance().getNetworkHandlerProvider().clear();

            if (SignSelector.getInstance() != null && SignSelector.getInstance().getWorker() != null) {
                SignSelector.getInstance().getWorker().interrupt();
            }

            if (MobSelector.getInstance() != null) {
                MobSelector.getInstance().shutdown();
            }

        }
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        cloudServer = new CloudServer(this, CloudAPI.getInstance());

        CloudAPI.getInstance().bootstrap();
        checkRegistryAccess();

        getServer().getPluginManager().registerEvents(new BukkitListener(), this);

        cloudServer.registerCommand(new CommandResource());
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "cloudnet:main");

        enableTasks();
        loadPlayers();
    }

    private static void checkRegistryAccess() {
        DocumentRegistry.fire();
    }

    private void enableTasks() {
        Bukkit.getScheduler().runTask(this, () -> {
            if (cloudServer.getGroupData() != null) {
                if (CloudAPI.getInstance().getServerGroupData(CloudAPI.getInstance().getGroup()).getMode() == ServerGroupMode.LOBBY ||
                    CloudAPI.getInstance().getServerGroupData(CloudAPI.getInstance().getGroup()).getMode() == ServerGroupMode.STATIC_LOBBY) {
                    final CommandCloudServer server = new CommandCloudServer();

                    getCommand("cloudserver").setExecutor(server);
                    getCommand("cloudserver").setPermission("cloudnet.command.cloudserver");
                    getCommand("cloudserver").setTabCompleter(server);
                }

                Bukkit.getPluginManager().callEvent(new BukkitCloudServerInitEvent(cloudServer));
                cloudServer.update();

                if (CloudAPI.getInstance().getServerGroupData(
                    CloudAPI.getInstance().getGroup()).getAdvancedServerConfig().isDisableAutoSavingForWorlds()) {
                    for (final World world : Bukkit.getWorlds()) {
                        world.setAutoSave(false);
                    }
                }
            }

            if (cloudServer.getGroupData() != null) {
                getServer().getScheduler().runTaskTimer(BukkitBootstrap.this, () -> {
                    try {
                        final ServerListPingEvent serverListPingEvent = new ServerListPingEvent(
                            new InetSocketAddress("127.0.0.1", 53345).getAddress(),
                            cloudServer.getMotd(),
                            Bukkit.getOnlinePlayers().size(),
                            cloudServer.getMaxPlayers());
                        Bukkit.getPluginManager().callEvent(serverListPingEvent);
                        if (!serverListPingEvent.getMotd().equalsIgnoreCase(cloudServer.getMotd())
                            || serverListPingEvent.getMaxPlayers() != cloudServer.getMaxPlayers()) {
                            cloudServer.setMotd(serverListPingEvent.getMotd());
                            cloudServer.setMaxPlayers(serverListPingEvent.getMaxPlayers());
                            if (serverListPingEvent.getMotd().toLowerCase().contains("running")
                                || serverListPingEvent.getMotd().toLowerCase().contains("ingame")) {
                                cloudServer.changeToIngame();
                            } else {
                                cloudServer.update();
                            }
                        }
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                    }
                }, 0, 5);
            }

            if (CloudAPI.getInstance().getPermissionPool() != null
                && (getServer().getPluginManager().isPluginEnabled("VaultAPI")
                    || getServer().getPluginManager().isPluginEnabled("Vault"))) {
                VaultInvoker.invoke();
            }
        });
    }

    private void loadPlayers() {
        for (final Player all : getServer().getOnlinePlayers()) {
            cloudServer.getPlayerAndCache(all.getUniqueId());
        }
    }

    private class BukkitCancelTask implements Runnable {

        @Deprecated
        @Override
        public void run() {
            Bukkit.getServer().getPluginManager().disablePlugin(BukkitBootstrap.this);
            Bukkit.shutdown();
        }

    }

}
