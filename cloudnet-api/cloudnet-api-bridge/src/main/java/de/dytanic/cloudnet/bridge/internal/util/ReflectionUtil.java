/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.internal.util;

import de.dytanic.cloudnet.lib.serverselectors.mob.ServerMob;
import org.bukkit.Location;
import org.bukkit.entity.*;

/**
 * Created by Tareko on 17.08.2017.
 */
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    public static Class<?> reflectCraftClazz(final String suffix) {
        try {
            final String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return Class.forName("org.bukkit.craftbukkit." + version + suffix);
        } catch (final Exception ex) {
            try {
                return Class.forName("org.bukkit.craftbukkit." + suffix);
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Class<?> forName(final String path) {
        try {
            return Class.forName(path);
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> reflectNMSClazz(final String suffix) {
        try {
            final String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return Class.forName("net.minecraft.server." + version + suffix);
        } catch (final Exception ex) {
            try {
                return Class.forName("net.minecraft.server." + suffix);
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Entity armorstandCreation(final Location location, final Entity entity, final ServerMob serverMob) {
        final ArmorStand armorStand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation().clone().add(0,
            ((LivingEntity) entity).getEyeHeight() - (entity instanceof Wither ? 0.15 : 0.3),
            0), EntityType.ARMOR_STAND);

        armorStand.setVisible(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setSmall(true);
        armorStand.setCanPickupItems(false);
        return armorStand;
    }
}
