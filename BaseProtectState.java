package com.groundone.baseprotect;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BaseProtectState extends SavedData {
    private boolean enabled = true;
    private final BlockPos[] corners = new BlockPos[4];
    private final Set<UUID> safePlayers = new HashSet<>();

    public static final SavedData.Factory<BaseProtectState> FACTORY = new SavedData.Factory<>(
        BaseProtectState::new,
        BaseProtectState::load
    );

    public static BaseProtectState getServerState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "baseprotect");
    }

    public static BaseProtectState load(CompoundTag tag) {
        BaseProtectState state = new BaseProtectState();
        state.enabled = tag.getBoolean("enabled");

        for (int i = 0; i < 4; i++) {
            if (tag.contains("corner" + i)) {
                CompoundTag cTag = tag.getCompound("corner" + i);
                state.corners[i] = new BlockPos(cTag.getInt("x"), cTag.getInt("y"), cTag.getInt("z"));
            }
        }

        ListTag list = tag.getList("safePlayers", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                state.safePlayers.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {}
        }

        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("enabled", enabled);

        for (int i = 0; i < 4; i++) {
            if (corners[i] != null) {
                CompoundTag cTag = new CompoundTag();
                cTag.putInt("x", corners[i].getX());
                cTag.putInt("y", corners[i].getY());
                cTag.putInt("z", corners[i].getZ());
                tag.put("corner" + i, cTag);
            }
        }

        ListTag list = new ListTag();
        for (UUID uuid : safePlayers) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("safePlayers", list);

        return tag;
    }

    public void setCorner(int index, BlockPos pos) {
        if (index >= 0 && index < 4) {
            corners[index] = pos;
            setDirty();
        }
    }

    public BlockPos getCorner(int index) {
        return (index >= 0 && index < 4) ? corners[index] : null;
    }

    public boolean areCornersSet() {
        for (BlockPos c : corners) {
            if (c == null) return false;
        }
        return true;
    }

    public void clearCorners() {
        for (int i = 0; i < 4; i++) {
            corners[i] = null;
        }
        setDirty();
    }

    public boolean isSafe(UUID uuid) {
        return safePlayers.contains(uuid);
    }

    public boolean addSafePlayer(UUID uuid) {
        boolean added = safePlayers.add(uuid);
        if (added) setDirty();
        return added;
    }

    public boolean removeSafePlayer(UUID uuid) {
        boolean removed = safePlayers.remove(uuid);
        if (removed) setDirty();
        return removed;
    }

    public Set<UUID> getSafePlayers() {
        return safePlayers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setDirty();
    }

    public boolean isInside(double x, double z) {
        if (!areCornersSet()) return false;

        boolean inside = false;
        for (int i = 0, j = 3; i < 4; j = i++) {
            double xi = corners[i].getX();
            double zi = corners[i].getZ();
            double xj = corners[j].getX();
            double zj = corners[j].getZ();

            boolean intersect = ((zi > z) != (zj > z)) &&
                    (x < (xj - xi) * (z - zi) / (zj - zi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }
}
