/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.bridge.internal.serverselectors.packet.in;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.bridge.event.bukkit.BukkitUpdateSignLayoutsEvent;
import de.dytanic.cloudnet.bridge.internal.serverselectors.SignSelector;
import de.dytanic.cloudnet.lib.network.protocol.packet.PacketInHandler;
import de.dytanic.cloudnet.lib.network.protocol.packet.PacketSender;
import de.dytanic.cloudnet.lib.serverselectors.sign.Sign;
import de.dytanic.cloudnet.lib.serverselectors.sign.SignLayoutConfig;
import de.dytanic.cloudnet.lib.utility.Acceptable;
import de.dytanic.cloudnet.lib.utility.MapWrapper;
import de.dytanic.cloudnet.lib.utility.document.Document;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Tareko on 23.08.2017.
 */
public class PacketInSignSelector extends PacketInHandler {

    @Override
    public void handleInput(final Document data, final PacketSender packetSender) {
        final Map<UUID, Sign> signMap = data.getObject("signs", new TypeToken<Map<UUID, Sign>>() {}.getType());
        final SignLayoutConfig signLayoutConfig = data.getObject("signLayoutConfig", new TypeToken<SignLayoutConfig>() {}.getType());

        final Map<UUID, Sign> values = MapWrapper.filter(signMap, new Acceptable<Sign>() {
            @Override
            public boolean isAccepted(final Sign value) {
                return value.getPosition().getGroup().equals(CloudAPI.getInstance().getGroup());
            }
        });

        Bukkit.getPluginManager().callEvent(new BukkitUpdateSignLayoutsEvent(signLayoutConfig));

        if (SignSelector.getInstance() != null) {
            SignSelector.getInstance().setSignLayoutConfig(signLayoutConfig);

            final Collection<UUID> collection = new HashSet<>();

            for (final Sign sign : SignSelector.getInstance().getSigns().values()) {
                if (!values.containsKey(sign.getUniqueId())) {
                    collection.add(sign.getUniqueId());
                }
            }

            for (final UUID x : collection) {
                SignSelector.getInstance().getSigns().remove(x);
            }

            for (final Sign sign : values.values()) {
                if (!SignSelector.getInstance().getSigns().containsKey(sign.getUniqueId())) {
                    SignSelector.getInstance().getSigns().put(sign.getUniqueId(), sign);
                }
            }
        } else {
            final SignSelector signSelector = new SignSelector(values, signLayoutConfig);
            signSelector.start();
        }
    }
}
