/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.internal.serverselectors.packet.in;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.api.network.packet.PacketInHandlerDefault;
import de.dytanic.cloudnet.bridge.CloudServer;
import de.dytanic.cloudnet.bridge.event.bukkit.BukkitMobInitEvent;
import de.dytanic.cloudnet.bridge.internal.serverselectors.MobSelector;
import de.dytanic.cloudnet.bridge.internal.util.ItemStackBuilder;
import de.dytanic.cloudnet.bridge.internal.util.ReflectionUtil;
import de.dytanic.cloudnet.lib.network.protocol.packet.PacketSender;
import de.dytanic.cloudnet.lib.serverselectors.mob.MobConfig;
import de.dytanic.cloudnet.lib.serverselectors.mob.ServerMob;
import de.dytanic.cloudnet.lib.utility.MapWrapper;
import de.dytanic.cloudnet.lib.utility.document.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Tareko on 25.08.2017.
 */
public class PacketInMobSelector extends PacketInHandlerDefault {

    @Override
    public void handleInput(final Document data, final PacketSender packetSender) {
        final Map<UUID, ServerMob> mobMap = data.getObject("mobs", new TypeToken<Map<UUID, ServerMob>>() {}.getType());
        final MobConfig mobConfig = data.getObject("mobConfig", new TypeToken<MobConfig>() {}.getType());

        final Map<UUID, ServerMob> filteredMobs = mobMap.entrySet().stream()
                                                        .filter((entry) -> entry.getValue().getPosition().getGroup()
                                                                                .equalsIgnoreCase(CloudAPI.getInstance().getGroup()))
                                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (MobSelector.getInstance() != null) {
            MobSelector.getInstance().shutdown();
            MobSelector.getInstance().setMobConfig(mobConfig);
            MobSelector.getInstance().setMobs(new HashMap<>());
            Bukkit.getScheduler().runTask(CloudServer.getInstance().getPlugin(), () -> {

                MobSelector.getInstance().setMobs(MapWrapper.transform(filteredMobs, key -> key, key -> spawnMob(key, mobConfig)));
                Bukkit.getScheduler().runTaskAsynchronously(CloudServer.getInstance().getPlugin(), () -> {
                    MobSelector.getInstance().getServers().values().forEach(MobSelector.getInstance()::handleUpdate);
                });
            });

        } else {
            final MobSelector mobSelector = new MobSelector(mobConfig);
            MobSelector.getInstance().setMobs(new HashMap<>());
            Bukkit.getScheduler().runTask(CloudServer.getInstance().getPlugin(),
                () -> MobSelector.getInstance().setMobs(MapWrapper.transform(filteredMobs, key -> key, key -> spawnMob(key, mobConfig))));
            mobSelector.init();
        }
    }

    private MobSelector.MobImpl spawnMob(final ServerMob key, final MobConfig mobConfig) {
        MobSelector.getInstance().toLocation(key.getPosition()).getChunk().load();
        final Entity entity = MobSelector.getInstance().toLocation(key.getPosition()).getWorld().spawnEntity(
            MobSelector.getInstance().toLocation(key.getPosition()),
            EntityType.valueOf(key.getType()));
        final Entity armorStand = ReflectionUtil.armorstandCreation(
            MobSelector.getInstance().toLocation(key.getPosition()),
            entity,
            key);

        MobSelector.getInstance().updateCustom(key, armorStand);
        if (armorStand.getPassenger() == null && key.getItemId() != null) {
            final Material material = ItemStackBuilder.getMaterialIgnoreVersion(key.getItemName(), key.getItemId());
            if (material != null) {
                final Item item = Bukkit.getWorld(key.getPosition().getWorld()).dropItem(armorStand.getLocation(),
                    new ItemStack(material));
                item.setTicksLived(Integer.MAX_VALUE);
                item.setPickupDelay(Integer.MAX_VALUE);
                armorStand.setPassenger(item);
            }
        }

        if (entity instanceof Villager) {
            ((Villager) entity).setProfession(Villager.Profession.FARMER);
        }

        MobSelector.getInstance().unstableEntity(entity);
        entity.setCustomNameVisible(true);
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', key.getDisplay()));

        final MobSelector.MobImpl mob = new MobSelector.MobImpl(key.getUniqueId(),
            key,
            entity,
            MobSelector.getInstance().create(mobConfig, key),
            new HashMap<>(),
            armorStand);
        Bukkit.getPluginManager().callEvent(new BukkitMobInitEvent(mob));
        return mob;
    }
}
