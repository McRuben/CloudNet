package de.dytanic.cloudnet.bridge.vault;

import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.api.player.PermissionProvider;
import de.dytanic.cloudnet.bridge.CloudServer;
import de.dytanic.cloudnet.lib.player.OfflinePlayer;
import de.dytanic.cloudnet.lib.player.permission.PermissionGroup;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

/**
 * Created by Tareko on 21.12.2017.
 */
public class VaultChatImpl extends Chat {

    public VaultChatImpl(final Permission perms) {
        super(perms);
    }

    @Override
    public String getName() {
        return "CloudNet-Chat";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPlayerPrefix(final String world, final String player) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        if (offlinePlayer.getPermissionEntity().getPrefix() != null) {
            return offlinePlayer.getPermissionEntity().getPrefix();
        } else {
            // Get prefix from highest permission group
            return PermissionProvider.getPrefix(offlinePlayer);
        }
    }

    @Override
    public void setPlayerPrefix(final String world, final String player, final String prefix) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        offlinePlayer.getPermissionEntity().setPrefix(prefix);
        update(offlinePlayer);
    }

    @Override
    public String getPlayerSuffix(final String world, final String player) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        if (offlinePlayer.getPermissionEntity().getSuffix() != null) {
            return offlinePlayer.getPermissionEntity().getSuffix();
        } else {
            return PermissionProvider.getSuffix(offlinePlayer);
        }
    }

    @Override
    public void setPlayerSuffix(final String world, final String player, final String suffix) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        offlinePlayer.getPermissionEntity().setSuffix(suffix);
        update(offlinePlayer);
    }

    @Override
    public String getGroupPrefix(final String world, final String group) {
        final PermissionGroup permissionGroup = CloudAPI.getInstance().getPermissionPool().getGroups().get(group);
        if (permissionGroup != null) {
            return permissionGroup.getPrefix();
        } else {
            return null;
        }
    }

    @Override
    public void setGroupPrefix(final String world, final String group, final String prefix) {
        final PermissionGroup permissionGroup = CloudAPI.getInstance().getPermissionPool().getGroups().get(group);
        if (permissionGroup != null) {
            permissionGroup.setPrefix(prefix);
            CloudAPI.getInstance().updatePermissionGroup(permissionGroup);
        }
    }

    @Override
    public String getGroupSuffix(final String world, final String group) {
        final PermissionGroup permissionGroup = CloudAPI.getInstance().getPermissionPool().getGroups().get(group);
        if (permissionGroup != null) {
            return permissionGroup.getSuffix();
        } else {
            return null;
        }
    }

    @Override
    public void setGroupSuffix(final String world, final String group, final String suffix) {
        final PermissionGroup permissionGroup = CloudAPI.getInstance().getPermissionPool().getGroups().get(group);
        if (permissionGroup != null) {
            permissionGroup.setSuffix(suffix);
            CloudAPI.getInstance().updatePermissionGroup(permissionGroup);
        }
    }

    @Override
    public int getPlayerInfoInteger(final String world, final String player, final String node, final int defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoInteger(final String world, final String player, final String node, final int value) {

    }

    @Override
    public int getGroupInfoInteger(final String world, final String group, final String node, final int defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoInteger(final String world, final String group, final String node, final int value) {

    }

    @Override
    public double getPlayerInfoDouble(final String world, final String player, final String node, final double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoDouble(final String world, final String player, final String node, final double value) {

    }

    @Override
    public double getGroupInfoDouble(final String world, final String group, final String node, final double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoDouble(final String world, final String group, final String node, final double value) {

    }

    @Override
    public boolean getPlayerInfoBoolean(final String world, final String player, final String node, final boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoBoolean(final String world, final String player, final String node, final boolean value) {

    }

    @Override
    public boolean getGroupInfoBoolean(final String world, final String group, final String node, final boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoBoolean(final String world, final String group, final String node, final boolean value) {

    }

    @Override
    public String getPlayerInfoString(final String world, final String player, final String node, final String defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoString(final String world, final String player, final String node, final String value) {

    }

    @Override
    public String getGroupInfoString(final String world, final String group, final String node, final String defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoString(final String world, final String group, final String node, final String value) {

    }

    private void update(final OfflinePlayer offlinePlayer) {
        CloudAPI.getInstance().updatePlayer(offlinePlayer);
    }

    private OfflinePlayer getPlayer(final String name) {
        OfflinePlayer offlinePlayer = CloudServer.getInstance().getCachedPlayer(name);

        if (offlinePlayer == null) {
            offlinePlayer = CloudAPI.getInstance().getOfflinePlayer(name);
        }

        return offlinePlayer;
    }

}
