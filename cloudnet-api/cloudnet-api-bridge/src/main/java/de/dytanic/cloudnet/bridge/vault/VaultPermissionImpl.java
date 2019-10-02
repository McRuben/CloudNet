package de.dytanic.cloudnet.bridge.vault;

import de.dytanic.cloudnet.api.CloudAPI;
import de.dytanic.cloudnet.api.player.PermissionProvider;
import de.dytanic.cloudnet.bridge.CloudServer;
import de.dytanic.cloudnet.lib.player.OfflinePlayer;
import de.dytanic.cloudnet.lib.player.permission.GroupEntityData;
import de.dytanic.cloudnet.lib.player.permission.PermissionEntity;
import de.dytanic.cloudnet.lib.player.permission.PermissionGroup;
import net.milkbowl.vault.permission.Permission;

/**
 * Created by Tareko on 25.11.2017.
 */
public class VaultPermissionImpl extends Permission {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @Override
    public String getName() {
        return "CloudNet-Permission";
    }

    @Override
    public boolean isEnabled() {
        return CloudAPI.getInstance().getPermissionPool() != null;
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean playerHas(final String world, final String player, final String permission) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        final PermissionEntity permissionEntity = offlinePlayer.getPermissionEntity();
        final boolean hasPermission = permissionEntity.hasPermission(CloudAPI.getInstance().getPermissionPool(), permission, null);
        CloudAPI.getInstance().getLogger().finest(player + " hasPermission \"" + permission + "\": " + hasPermission);
        return hasPermission;
    }

    @Override
    public boolean playerAdd(final String world, final String player, final String permission) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        PermissionProvider.addPlayerPermission(offlinePlayer, permission);
        CloudAPI.getInstance().getLogger().finest(player + " added permission \"" + permission + '"');
        return true;
    }

    @Override
    public boolean playerRemove(final String world, final String player, final String permission) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        PermissionProvider.removePlayerPermission(offlinePlayer, permission);
        CloudAPI.getInstance().getLogger().finest(player + " removed permission \"" + permission + '"');
        return true;
    }

    @Override
    public boolean groupHas(final String world, final String group, final String permission) {
        final PermissionGroup permissionGroup = CloudAPI.getInstance().getPermissionGroup(group);
        if (permissionGroup != null) {
            return permissionGroup.getPermissions().getOrDefault(permission, false);
        } else {
            return false;
        }
    }

    @Override
    public boolean groupAdd(final String world, final String group, final String permission) {
        PermissionProvider.addPermission(group, permission);
        CloudAPI.getInstance().getLogger().finest(group + " added permission \"" + permission + '"');
        return true;
    }

    @Override
    public boolean groupRemove(final String world, final String group, final String permission) {
        PermissionProvider.removePermission(group, permission);
        CloudAPI.getInstance().getLogger().finest(group + " removed permission \"" + permission + '"');
        return true;
    }

    @Override
    public boolean playerInGroup(final String world, final String player, final String group) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        return PermissionProvider.isInGroup(group, offlinePlayer);
    }

    @Override
    public boolean playerAddGroup(final String world, final String player, final String group) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        PermissionProvider.addPlayerGroup(offlinePlayer, group, 0);
        CloudAPI.getInstance().getLogger().finest(player + " added to group \"" + group + '"');
        return true;
    }

    @Override
    public boolean playerRemoveGroup(final String world, final String player, final String group) {
        final OfflinePlayer offlinePlayer = getPlayer(player);
        PermissionProvider.removePlayerGroup(group, offlinePlayer);
        CloudAPI.getInstance().getLogger().finest(player + " removed from group \"" + group + '"');
        return true;
    }

    @Override
    public String[] getPlayerGroups(final String world, final String player) {
        final PermissionEntity permissionEntity = getPlayer(player).getPermissionEntity();
        return permissionEntity.getGroups().stream().map(GroupEntityData::getGroup).toArray(String[]::new);
    }

    @Override
    public String getPrimaryGroup(final String world, final String player) {
        return PermissionProvider.getGroupName(getPlayer(player));
    }

    @Override
    public String[] getGroups() {
        return CloudAPI.getInstance().getPermissionPool().getGroups().keySet().toArray(EMPTY_STRING_ARRAY);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    private static OfflinePlayer getPlayer(final String name) {
        OfflinePlayer offlinePlayer = CloudServer.getInstance().getCachedPlayer(name);

        if (offlinePlayer == null) {
            offlinePlayer = CloudAPI.getInstance().getOfflinePlayer(name);
        }

        return offlinePlayer;
    }
}
