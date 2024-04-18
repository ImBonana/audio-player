package de.maxhenkel.audioplayer;

import de.maxhenkel.admiral.permissions.PermissionManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AudioPlayerPermissionManager implements PermissionManager<ServerCommandSource> {

    public static final AudioPlayerPermissionManager INSTANCE = new AudioPlayerPermissionManager();

    private static final Permission PLAY_COMMAND_PERMISSION = new Permission("audioplayer.play_command", PermissionType.OPS);

    private static final List<Permission> PERMISSIONS = List.of(
            PLAY_COMMAND_PERMISSION
    );

    @Override
    public boolean hasPermission(ServerCommandSource stack, String permission) {
        for (Permission p : PERMISSIONS) {
            if (!p.permission.equals(permission)) continue;

            if (stack.isExecutedByPlayer()) return p.hasPermission(stack.getPlayer());

            if (p.type().equals(PermissionType.OPS)) return stack.hasPermissionLevel(2);

            return p.hasPermission(null);
        }
        return false;
    }

    private static Boolean loaded;

    private static boolean isFabricPermissionsAPILoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
            if (loaded) {
                AudioPlayer.LOGGER.info("Using Fabric Permissions API");
            }
        }
        return loaded;
    }

    private record Permission(String permission, PermissionType type) {

        public boolean hasPermission(@Nullable ServerPlayerEntity player) {
                if (isFabricPermissionsAPILoaded()) {
                    return checkFabricPermission(player);
                }
                return type.hasPermission(player);
            }

            private boolean checkFabricPermission(@Nullable ServerPlayerEntity player) {
                if (player == null) {
                    return false;
                }
                TriState permissionValue = Permissions.getPermissionValue(player, permission);
                return switch (permissionValue) {
                    case DEFAULT -> type.hasPermission(player);
                    case TRUE -> true;
                    default -> false;
                };
            }
        }

    private enum PermissionType {

        EVERYONE, NO_ONE, OPS;

        boolean hasPermission(@Nullable ServerPlayerEntity player) {
            return switch (this) {
                case EVERYONE -> true;
                case NO_ONE -> false;
                case OPS -> player != null && player.hasPermissionLevel(player.server.getOpPermissionLevel());
            };
        }

    }
}
