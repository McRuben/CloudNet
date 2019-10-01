/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.internal.listener.v18_112;

import de.dytanic.cloudnet.bridge.internal.serverselectors.MobSelector;
import de.dytanic.cloudnet.lib.utility.Acceptable;
import de.dytanic.cloudnet.lib.utility.CollectionWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

/**
 * Created by Tareko on 14.09.2017.
 */
public final class ArmorStandListener implements Listener {

    @EventHandler
    public void handle(final PlayerArmorStandManipulateEvent e) {
        final MobSelector.MobImpl mob = CollectionWrapper.filter(MobSelector.getInstance().getMobs().values(),
            new Acceptable<MobSelector.MobImpl>() {
                @Override
                public boolean isAccepted(final MobSelector.MobImpl value) {
                    return e.getRightClicked().getUniqueId().equals(value.getDisplayMessage().getUniqueId());
                }
            });
        if (mob != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(final ItemDespawnEvent e) {
        final MobSelector.MobImpl mob = CollectionWrapper.filter(MobSelector.getInstance().getMobs().values(),
            new Acceptable<MobSelector.MobImpl>() {
                @Override
                public boolean isAccepted(final MobSelector.MobImpl value) {
                    return
                        value.getDisplayMessage().getPassenger() != null
                        && e.getEntity().getEntityId() == value.getDisplayMessage().getPassenger().getEntityId();
                }
            });
        if (mob != null) {
            e.setCancelled(true);
        }
    }
}
