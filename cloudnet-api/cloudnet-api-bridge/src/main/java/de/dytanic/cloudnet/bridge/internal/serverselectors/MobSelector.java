/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.internal.serverselectors;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.api.handlers.adapter.NetworkHandlerAdapter;
import de.dytanic.cloudnet.bridge.CloudServer;
import de.dytanic.cloudnet.bridge.event.bukkit.BukkitMobInitEvent;
import de.dytanic.cloudnet.bridge.event.bukkit.BukkitMobUpdateEvent;
import de.dytanic.cloudnet.bridge.internal.util.ItemStackBuilder;
import de.dytanic.cloudnet.bridge.internal.util.ReflectionUtil;
import de.dytanic.cloudnet.lib.NetworkUtils;
import de.dytanic.cloudnet.lib.Value;
import de.dytanic.cloudnet.lib.server.ServerState;
import de.dytanic.cloudnet.lib.server.info.ServerInfo;
import de.dytanic.cloudnet.lib.serverselectors.mob.MobConfig;
import de.dytanic.cloudnet.lib.serverselectors.mob.MobItemLayout;
import de.dytanic.cloudnet.lib.serverselectors.mob.MobPosition;
import de.dytanic.cloudnet.lib.serverselectors.mob.ServerMob;
import de.dytanic.cloudnet.lib.utility.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by Tareko on 25.08.2017.
 */
public final class MobSelector {

    private static MobSelector instance;
    private final Map<String, ServerInfo> servers = NetworkUtils.newConcurrentHashMap();
    private Map<UUID, MobImpl> mobs;
    private MobConfig mobConfig;

    public MobSelector(final MobConfig mobConfig) {
        instance = this;
        this.mobConfig = mobConfig;
    }

    public static MobSelector getInstance() {
        return instance;
    }

    public Map<UUID, MobImpl> getMobs() {
        return mobs;
    }

    public void setMobs(final Map<UUID, MobImpl> mobs) {
        this.mobs = mobs;
    }

    public MobConfig getMobConfig() {
        return mobConfig;
    }

    public void setMobConfig(final MobConfig mobConfig) {
        this.mobConfig = mobConfig;
    }

    public Map<String, ServerInfo> getServers() {
        return servers;
    }

    public void init() {
        CloudAPI.getInstance().getNetworkHandlerProvider().registerHandler(new NetworkHandlerAdapterImplx());

        Bukkit.getScheduler().runTask(CloudServer.getInstance().getPlugin(), () -> {
            NetworkUtils.addAll(servers,
                MapWrapper
                    .collectionCatcherHashMap(CloudAPI.getInstance().getServers(), new Catcher<String, ServerInfo>() {
                        @Override
                        public String doCatch(final ServerInfo key) {
                            return key.getServiceId().getServerId();
                        }
                    }));
            Bukkit.getScheduler().runTaskAsynchronously(CloudServer.getInstance().getPlugin(), new Runnable() {
                @Override
                public void run() {
                    for (final ServerInfo serverInfo : servers.values()) {
                        handleUpdate(serverInfo);
                    }
                }
            });
        });

        if (ReflectionUtil.forName("org.bukkit.entity.ArmorStand") != null) {
            try {
                Bukkit.getPluginManager().registerEvents((Listener) ReflectionUtil
                        .forName("de.dytanic.cloudnet.bridge.internal.listener.v18_112.ArmorStandListener").newInstance(),
                    CloudServer.getInstance().getPlugin());
            } catch (final InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        Bukkit.getPluginManager().registerEvents(new ListenrImpl(), CloudServer.getInstance().getPlugin());
    }

    public void handleUpdate(final ServerInfo serverInfo) {
        if (serverInfo.getServiceId().getGroup() == null) {
            return;
        }

        for (final MobImpl mob : this.mobs.values()) {
            if (mob.getMob().getTargetGroup().equals(serverInfo.getServiceId().getGroup())) {
                mob.getEntity().setTicksLived(Integer.MAX_VALUE);
                updateCustom(mob.getMob(), mob.getDisplayMessage());
                Bukkit.getPluginManager().callEvent(new BukkitMobUpdateEvent(mob.getMob()));

                mob.getServerPosition().clear();
                filter(serverInfo.getServiceId().getGroup());
                final Collection<ServerInfo> serverInfos = filter(serverInfo.getServiceId().getGroup());

                final Value<Integer> index = new Value<>(0);

                for (final ServerInfo server : serverInfos) {
                    if (server.isOnline() && server.getServerState() == ServerState.LOBBY && !server.getServerConfig().isHideServer() &&
                        !server.getServerConfig().getProperties().contains(NetworkUtils.DEV_PROPERTY)) {
                        while (mobConfig.getDefaultItemInventory().containsKey((index.getValue() + 1))) {
                            index.setValue(index.getValue() + 1);
                        }

                        if ((mobConfig.getInventorySize() - 1) <= index.getValue()) {
                            break;
                        }

                        final int value = index.getValue();
                        Bukkit.getScheduler().runTask(CloudServer.getInstance().getPlugin(), () -> {
                            mob.getInventory().setItem(value, transform(mobConfig.getItemLayout(), server));
                            mob.getServerPosition().put(value, server.getServiceId().getServerId());
                        });
                        index.setValue(index.getValue() + 1);
                    }
                }

                while (index.getValue() < (mob.getInventory().getSize())) {
                    if (!mobConfig.getDefaultItemInventory().containsKey(index.getValue() + 1)) {
                        mob.getInventory().setItem(index.getValue(), new ItemStack(Material.AIR));
                    }
                    index.setValue(index.getValue() + 1);
                }
            }
        }
    }

    public void updateCustom(final ServerMob serverMob, final Object armorStand) {
        final Return<Integer, Integer> x = getOnlineCount(serverMob.getTargetGroup());
        if (armorStand != null) {
            try {

                armorStand.getClass().getMethod("setCustomName", String.class).invoke(armorStand,
                    ChatColor.translateAlternateColorCodes('&',
                        serverMob
                            .getDisplayMessage() +
                        NetworkUtils.EMPTY_STRING)
                        .replace("%max_players%",
                            x.getSecond() +
                            NetworkUtils.EMPTY_STRING).replace(
                        "%group%",
                        serverMob.getTargetGroup()).replace(
                        "%group_online%",
                        x.getFirst() + NetworkUtils.EMPTY_STRING));
            } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    private Collection<ServerInfo> filter(final String group) {
        return CollectionWrapper.filterMany(servers.values(), new Acceptable<ServerInfo>() {
            @Override
            public boolean isAccepted(final ServerInfo value) {
                return value.getServiceId().getGroup().equals(group);
            }
        });
    }

    private ItemStack transform(final MobItemLayout mobItemLayout, final ServerInfo serverInfo) {
        final Material material = ItemStackBuilder.getMaterialIgnoreVersion(mobItemLayout.getItemName(), mobItemLayout.getItemId());
        return material == null ? null : ItemStackBuilder.builder(material, 1, mobItemLayout.getSubId()).lore(new ArrayList<>(
            CollectionWrapper.transform(mobItemLayout.getLore(), new Catcher<String, String>() {
                @Override
                public String doCatch(final String key) {
                    return initPatterns(ChatColor.translateAlternateColorCodes('&', key), serverInfo);
                }
            }))).displayName(initPatterns(ChatColor.translateAlternateColorCodes('&', mobItemLayout.getDisplay()), serverInfo)).build();
    }

    public Return<Integer, Integer> getOnlineCount(final String group) {
        int atomicInteger = 0;
        int atomicInteger1 = 0;
        for (final ServerInfo serverInfo : this.servers.values()) {
            if (serverInfo.getServiceId().getGroup().equalsIgnoreCase(group)) {
                atomicInteger = atomicInteger + serverInfo.getOnlineCount();
                atomicInteger1 = atomicInteger1 + serverInfo.getMaxPlayers();
            }
        }
        return new Return<>(atomicInteger, atomicInteger1);
    }

    private String initPatterns(final String x, final ServerInfo serverInfo) {
        return x.replace("%server%", serverInfo.getServiceId().getServerId()).replace("%id%",
            serverInfo.getServiceId().getId() +
            NetworkUtils.EMPTY_STRING).replace("%host%",
            serverInfo
                .getHost())
            .replace("%port%", serverInfo.getPort() + NetworkUtils.EMPTY_STRING).replace("%memory%", serverInfo.getMemory() + "MB")
            .replace("%online_players%", serverInfo.getOnlineCount() + NetworkUtils.EMPTY_STRING).replace("%max_players%",
                serverInfo.getMaxPlayers() +
                NetworkUtils.EMPTY_STRING)
            .replace("%motd%", ChatColor.translateAlternateColorCodes('&', serverInfo.getMotd())).replace("%state%",
                serverInfo.getServerState()
                    .name() +
                NetworkUtils.EMPTY_STRING)
            .replace("%wrapper%", serverInfo.getServiceId().getWrapperId() + NetworkUtils.EMPTY_STRING).replace("%extra%",
                serverInfo
                    .getServerConfig()
                    .getExtra())
            .replace("%template%", serverInfo.getTemplate().getName()).replace("%group%", serverInfo.getServiceId().getGroup());
    }

    @Deprecated
    public void shutdown() {
        for (final MobImpl mobImpl : this.mobs.values()) {
            if (mobImpl.displayMessage != null) {
                final Entity entity = mobImpl.displayMessage;
                if (entity.getPassenger() != null) {
                    entity.getPassenger().remove();
                }
                mobImpl.displayMessage.remove();
            }
            mobImpl.entity.remove();
        }

        mobs.clear();
    }

    public Location toLocation(final MobPosition position) {
        return new Location(Bukkit.getWorld(position.getWorld()),
            position.getX(),
            position.getY(),
            position.getZ(),
            position.getYaw(),
            position.getPitch());
    }

    public MobPosition toPosition(final String group, final Location location) {
        return new MobPosition(group,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch());
    }

    public Inventory create(final MobConfig mobConfig, final ServerMob mob) {
        final Inventory inventory = Bukkit.createInventory(null,
            mobConfig.getInventorySize(),
            ChatColor.translateAlternateColorCodes('&',
                mob.getDisplay() +
                NetworkUtils.SPACE_STRING));

        for (final Map.Entry<Integer, MobItemLayout> mobItem : mobConfig.getDefaultItemInventory().entrySet()) {
            inventory.setItem(mobItem.getKey() - 1, transform(mobItem.getValue()));
        }
        return inventory;
    }

    private ItemStack transform(final MobItemLayout mobItemLayout) {
        final Material material = ItemStackBuilder.getMaterialIgnoreVersion(mobItemLayout.getItemName(), mobItemLayout.getItemId());
        return material == null ? null : ItemStackBuilder.builder(material, 1, mobItemLayout.getSubId()).lore(new ArrayList<>(
            CollectionWrapper.transform(mobItemLayout.getLore(), new Catcher<String, String>() {
                @Override
                public String doCatch(final String key) {
                    return ChatColor.translateAlternateColorCodes('&', key);
                }
            }))).displayName(ChatColor.translateAlternateColorCodes('&', mobItemLayout.getDisplay())).build();
    }

    private List<ServerInfo> getServers(final String group) {

        return new ArrayList<>(CollectionWrapper.filterMany(this.servers.values(), new Acceptable<ServerInfo>() {
            @Override
            public boolean isAccepted(final ServerInfo serverInfo) {
                return serverInfo.getServiceId().getGroup() != null && serverInfo.getServiceId().getGroup().equalsIgnoreCase(group);
            }
        }));
    }

    @Deprecated
    public void unstableEntity(final Entity entity) {
        try {
            final Class<?> nbt = ReflectionUtil.reflectNMSClazz(".NBTTagCompound");
            final Class<?> entityClazz = ReflectionUtil.reflectNMSClazz(".Entity");
            final Object object = nbt.newInstance();

            final Object nmsEntity = entity.getClass().getMethod("getHandle").invoke(entity);
            try {
                entityClazz.getMethod("e", nbt).invoke(nmsEntity, object);
            } catch (final Exception ex) {
                entityClazz.getMethod("save", nbt).invoke(nmsEntity, object);
            }

            object.getClass().getMethod("setInt", String.class, int.class).invoke(object, "NoAI", 1);
            object.getClass().getMethod("setInt", String.class, int.class).invoke(object, "Silent", 1);
            entityClazz.getMethod("f", nbt).invoke(nmsEntity, object);
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            System.out.println("[CLOUD] Disabling NoAI and Silent support for " + entity.getEntityId());
            ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100));
        }
    }

    public Collection<Inventory> inventories() {
        return CollectionWrapper.getCollection(this.mobs, new Catcher<Inventory, MobImpl>() {
            @Override
            public Inventory doCatch(final MobImpl key) {
                return key.getInventory();
            }
        });
    }

    public MobImpl find(final Inventory inventory) {
        return CollectionWrapper.filter(this.mobs.values(), new Acceptable<MobImpl>() {
            @Override
            public boolean isAccepted(final MobImpl value) {
                return value.getInventory().equals(inventory);
            }
        });
    }

    //MobImpl
    public static class MobImpl {

        private UUID uniqueId;

        private ServerMob mob;

        private Entity entity;

        private Inventory inventory;

        private Map<Integer, String> serverPosition;

        private Entity displayMessage;

        public MobImpl(final UUID uniqueId,
                       final ServerMob mob,
                       final Entity entity,
                       final Inventory inventory,
                       final Map<Integer, String> serverPosition,
                       final Entity displayMessage) {
            this.uniqueId = uniqueId;
            this.mob = mob;
            this.entity = entity;
            this.inventory = inventory;
            this.serverPosition = serverPosition;
            this.displayMessage = displayMessage;
        }

        public UUID getUniqueId() {
            return uniqueId;
        }

        public void setUniqueId(final UUID uniqueId) {
            this.uniqueId = uniqueId;
        }

        public Entity getEntity() {
            return entity;
        }

        public void setEntity(final Entity entity) {
            this.entity = entity;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(final Inventory inventory) {
            this.inventory = inventory;
        }

        public Map<Integer, String> getServerPosition() {
            return serverPosition;
        }

        public void setServerPosition(final Map<Integer, String> serverPosition) {
            this.serverPosition = serverPosition;
        }

        public Entity getDisplayMessage() {
            return displayMessage;
        }

        public void setDisplayMessage(final Entity displayMessage) {
            this.displayMessage = displayMessage;
        }

        public ServerMob getMob() {
            return mob;
        }

        public void setMob(final ServerMob mob) {
            this.mob = mob;
        }
    }

    private class NetworkHandlerAdapterImplx extends NetworkHandlerAdapter {

        @Override
        public void onServerAdd(final ServerInfo serverInfo) {
            servers.put(serverInfo.getServiceId().getServerId(), serverInfo);
            handleUpdate(serverInfo);
        }

        @Override
        public void onServerInfoUpdate(final ServerInfo serverInfo) {
            servers.put(serverInfo.getServiceId().getServerId(), serverInfo);
            handleUpdate(serverInfo);
        }

        @Override
        public void onServerRemove(final ServerInfo serverInfo) {
            servers.remove(serverInfo.getServiceId().getServerId());
            handleUpdate(serverInfo);
        }
    }

    private class ListenrImpl implements Listener {

        @EventHandler
        public void handleRightClick(final PlayerInteractEntityEvent e) {
            final MobImpl mobImpl = CollectionWrapper.filter(mobs.values(), new Acceptable<MobImpl>() {
                @Override
                public boolean isAccepted(final MobImpl value) {
                    return value.getEntity().getUniqueId().equals(e.getRightClicked().getUniqueId());
                }
            });

            if (mobImpl != null) {
                e.setCancelled(true);
                if (!CloudAPI.getInstance().getServerGroupData(mobImpl.getMob().getTargetGroup()).isMaintenance()) {
                    if (mobImpl.getMob().getAutoJoin() != null && mobImpl.getMob().getAutoJoin()) {
                        final ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();
                        byteArrayDataOutput.writeUTF("Connect");

                        final List<ServerInfo> serverInfos = getServers(mobImpl.getMob().getTargetGroup());

                        for (final ServerInfo serverInfo : serverInfos) {
                            if (serverInfo.getOnlineCount() < serverInfo.getMaxPlayers() &&
                                serverInfo.getServerState().equals(ServerState.LOBBY)) {
                                byteArrayDataOutput.writeUTF(serverInfo.getServiceId().getServerId());
                                e.getPlayer().sendPluginMessage(CloudServer.getInstance().getPlugin(),
                                    "BungeeCord",
                                    byteArrayDataOutput.toByteArray());
                                return;
                            }
                        }
                    } else {
                        e.getPlayer().openInventory(mobImpl.getInventory());
                    }
                } else {
                    e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        CloudAPI.getInstance().getCloudNetwork().getMessages()
                            .getString("mob-selector-maintenance-message")));
                }
            }
        }

        @EventHandler
        public void entityDamage(final EntityDamageEvent e) {
            final MobImpl mob = CollectionWrapper.filter(mobs.values(), new Acceptable<MobImpl>() {
                @Override
                public boolean isAccepted(final MobImpl value) {
                    return e.getEntity().getUniqueId().equals(value.getEntity().getUniqueId());
                }
            });
            if (mob != null) {
                e.getEntity().setFireTicks(0);
                e.setCancelled(true);
            }
        }

        @EventHandler
        public void handleInventoryClick(final InventoryClickEvent e) {
            if (!(e.getWhoClicked() instanceof Player)) {
                return;
            }

            if (inventories().contains(e.getInventory()) && e.getCurrentItem() != null && e.getSlot() == e.getRawSlot()) {
                e.setCancelled(true);
                if (ItemStackBuilder.getMaterialIgnoreVersion(mobConfig.getItemLayout().getItemName(),
                    mobConfig.getItemLayout().getItemId()) == e.getCurrentItem().getType()) {
                    final MobImpl mob = find(e.getInventory());
                    if (mob.getServerPosition().containsKey(e.getSlot())) {
                        if (CloudAPI.getInstance().getServerId().equalsIgnoreCase(mob.getServerPosition().get(e.getSlot()))) {
                            return;
                        }
                        final ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();
                        byteArrayDataOutput.writeUTF("Connect");
                        byteArrayDataOutput.writeUTF(mob.getServerPosition().get(e.getSlot()));
                        ((Player) e.getWhoClicked()).sendPluginMessage(CloudServer.getInstance().getPlugin(),
                            "BungeeCord",
                            byteArrayDataOutput.toByteArray());
                    }
                }
            }
        }

        @EventHandler
        public void onSave(final WorldSaveEvent e) {
            final Map<UUID, ServerMob> filteredMobs = MapWrapper.transform(MobSelector.this.mobs, new Catcher<UUID, UUID>() {
                @Override
                public UUID doCatch(final UUID key) {
                    return key;
                }
            }, new Catcher<ServerMob, MobImpl>() {
                @Override
                public ServerMob doCatch(final MobImpl key) {
                    return key.getMob();
                }
            });

            MobSelector.getInstance().shutdown();


            Bukkit.getScheduler().runTaskLater(CloudServer.getInstance().getPlugin(), new Runnable() {
                @Override
                public void run() {
                    MobSelector.getInstance().setMobs(MapWrapper.transform(filteredMobs, new Catcher<UUID, UUID>() {
                        @Override
                        public UUID doCatch(final UUID key) {
                            return key;
                        }
                    }, new Catcher<MobImpl, ServerMob>() {
                        @Override
                        public MobImpl doCatch(final ServerMob key) {
                            MobSelector.getInstance().toLocation(key.getPosition()).getChunk().load();
                            final Entity entity = MobSelector.getInstance().toLocation(key.getPosition()).getWorld().spawnEntity(MobSelector
                                    .getInstance()
                                    .toLocation(
                                        key.getPosition()),
                                EntityType
                                    .valueOf(
                                        key.getType()));
                            final Entity armorStand = ReflectionUtil.armorstandCreation(MobSelector.getInstance()
                                                                                                   .toLocation(key.getPosition()),
                                entity,
                                key);

                            if (armorStand != null) {
                                MobSelector.getInstance().updateCustom(key, armorStand);
                                if (armorStand.getPassenger() == null && key.getItemId() != null) {
                                    final Material material = ItemStackBuilder.getMaterialIgnoreVersion(key.getItemName(), key.getItemId());
                                    if (material != null) {
                                        final Item item = Bukkit.getWorld(key.getPosition().getWorld()).dropItem(armorStand.getLocation(),
                                            new ItemStack(material));
                                        item.setPickupDelay(Integer.MAX_VALUE);
                                        item.setTicksLived(Integer.MAX_VALUE);
                                        armorStand.setPassenger(item);
                                    }
                                }
                            }

                            if (entity instanceof Villager) {
                                ((Villager) entity).setProfession(Villager.Profession.FARMER);
                            }

                            MobSelector.getInstance().unstableEntity(entity);
                            entity.setCustomNameVisible(true);
                            entity.setCustomName(ChatColor.translateAlternateColorCodes('&', key.getDisplay()));
                            final MobImpl mob = new MobImpl(key.getUniqueId(),
                                key,
                                entity,
                                MobSelector.getInstance().create(mobConfig, key),
                                new HashMap<>(),
                                armorStand);
                            Bukkit.getPluginManager().callEvent(new BukkitMobInitEvent(mob));
                            return mob;
                        }
                    }));
                    Bukkit.getScheduler().runTaskAsynchronously(CloudServer.getInstance().getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            for (final ServerInfo serverInfo : getServers().values()) {
                                MobSelector.getInstance().handleUpdate(serverInfo);
                            }
                        }
                    });
                }
            }, 40);
        }
    }
}
