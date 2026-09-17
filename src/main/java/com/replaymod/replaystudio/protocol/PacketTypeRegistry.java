/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.protocol;

import com.replaymod.replaystudio.lib.guava.collect.Lists;
import com.replaymod.replaystudio.lib.viaversion.api.Via;
import com.replaymod.replaystudio.lib.viaversion.api.connection.UserConnection;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.AbstractProtocol;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolPathEntry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.ClientboundPacketType;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.PacketWrapper;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.Protocol;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.mapping.PacketMapping;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.mapping.PacketMappings;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.provider.PacketTypeMap;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.VersionType;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_17to1_17_1.Protocol1_17To1_17_1;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_20to1_20_2.Protocol1_20To1_20_2;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import com.replaymod.replaystudio.lib.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9;
import com.replaymod.replaystudio.viaversion.CustomViaManager;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketTypeRegistry {
    /**
     * Protocol version of Minecraft 26.3.
     * <p>
     * ViaVersion (as of the snapshot used by ReplayStudio) does not know about this version yet, so it is added
     * here and the packet ids for it are taken from {@link #MC_26_3_PACKET_IDS}.
     */
    public static final ProtocolVersion MC_26_3 = new ProtocolVersion(VersionType.RELEASE, 777, -1, "26.3", null);

    /**
     * Real packet ids of the vanilla 26.3 client per protocol state (clientbound).
     * <p>
     * These are the ids the vanilla client registers in {@code GameProtocols}, {@code ConfigurationProtocols}
     * and {@code LoginProtocols} (id = registration index, with the bundle delimiter registered first). They
     * have been extracted from the 26.3 client jar and verified against actual recorded 26.3 replays.
     */
    private static final Map<State, Map<Integer, PacketType>> MC_26_3_PACKET_IDS = new EnumMap<>(State.class);

    static {
        Map<Integer, PacketType> play = new HashMap<>();
        play.put(0, PacketType.Bundle);
        play.put(1, PacketType.SpawnObject);
        play.put(2, PacketType.EntityAnimation);
        play.put(3, PacketType.Statistics);
        play.put(5, PacketType.BlockBreakAnim);
        play.put(6, PacketType.UpdateTileEntity);
        play.put(7, PacketType.BlockValue);
        play.put(8, PacketType.BlockChange);
        play.put(10, PacketType.Difficulty);
        play.put(15, PacketType.TabComplete);
        play.put(17, PacketType.CloseWindow);
        play.put(18, PacketType.WindowItems);
        play.put(19, PacketType.WindowProperty);
        play.put(20, PacketType.SetSlot);
        play.put(24, PacketType.PluginMessage);
        play.put(32, PacketType.Disconnect);
        play.put(34, PacketType.EntityStatus);
        play.put(35, PacketType.EntityTeleport);
        play.put(36, PacketType.Explosion);
        play.put(38, PacketType.UnloadChunk);
        play.put(39, PacketType.NotifyClient);
        play.put(42, PacketType.OpenHorseWindow);
        play.put(45, PacketType.KeepAlive);
        play.put(46, PacketType.ChunkData);
        play.put(47, PacketType.PlayEffect);
        play.put(48, PacketType.SpawnParticle);
        play.put(49, PacketType.UpdateLight);
        play.put(50, PacketType.JoinGame);
        play.put(52, PacketType.MapData);
        play.put(53, PacketType.TradeList);
        play.put(54, PacketType.EntityPosition);
        play.put(55, PacketType.EntityPositionRotation);
        play.put(57, PacketType.EntityRotation);
        play.put(61, PacketType.OpenTileEntityEditor);
        play.put(65, PacketType.PlayerAbilities);
        play.put(67, PacketType.CombatEnd);
        play.put(68, PacketType.CombatEnter);
        play.put(69, PacketType.CombatEntityDead);
        play.put(70, PacketType.PlayerListEntryRemove);
        play.put(71, PacketType.PlayerListEntry);
        play.put(73, PacketType.PlayerPositionRotation);
        play.put(78, PacketType.DestroyEntities);
        play.put(79, PacketType.EntityRemoveEffect);
        play.put(80, PacketType.ResetScore);
        play.put(84, PacketType.Respawn);
        play.put(85, PacketType.EntityHeadLook);
        play.put(86, PacketType.MultiBlockChange);
        play.put(95, PacketType.SwitchCamera);
        play.put(96, PacketType.UpdateViewPosition);
        play.put(97, PacketType.UpdateViewDistance);
        play.put(99, PacketType.SpawnPosition);
        play.put(100, PacketType.DisplayScoreboard);
        play.put(101, PacketType.EntityMetadata);
        play.put(102, PacketType.EntityAttach);
        play.put(103, PacketType.EntityVelocity);
        play.put(104, PacketType.EntityEquipment);
        play.put(105, PacketType.SetExperience);
        play.put(106, PacketType.UpdateHealth);
        play.put(107, PacketType.ChangeHeldItem);
        play.put(108, PacketType.ScoreboardObjective);
        play.put(109, PacketType.SetPassengers);
        play.put(111, PacketType.Team);
        play.put(112, PacketType.UpdateScore);
        play.put(113, PacketType.UpdateSimulationDistance);
        play.put(115, PacketType.UpdateTime);
        play.put(118, PacketType.EntitySoundEffect);
        play.put(119, PacketType.PlaySound);
        play.put(120, PacketType.Reconfigure);
        play.put(124, PacketType.Chat);
        play.put(127, PacketType.EntityCollectItem);
        play.put(134, PacketType.EntityProperties);
        play.put(135, PacketType.EntityEffect);
        play.put(137, PacketType.Tags);
        MC_26_3_PACKET_IDS.put(State.PLAY, play);

        Map<Integer, PacketType> configuration = new HashMap<>();
        configuration.put(1, PacketType.ConfigCustomPayload);
        configuration.put(2, PacketType.ConfigDisconnect);
        configuration.put(3, PacketType.ConfigFinish);
        configuration.put(4, PacketType.ConfigKeepAlive);
        configuration.put(5, PacketType.ConfigPing);
        configuration.put(7, PacketType.ConfigRegistries);
        configuration.put(13, PacketType.ConfigFeatures);
        configuration.put(14, PacketType.ConfigTags);
        configuration.put(15, PacketType.ConfigSelectKnownPacks);
        MC_26_3_PACKET_IDS.put(State.CONFIGURATION, configuration);

        Map<Integer, PacketType> login = new HashMap<>();
        login.put(2, PacketType.LoginSuccess);
        MC_26_3_PACKET_IDS.put(State.LOGIN, login);
    }

    private static Map<ProtocolVersion, EnumMap<State, PacketTypeRegistry>> forVersionAndState = new HashMap<>();
    private static Field clientbound;

    static {
        CustomViaManager.initialize();
        // Copy the regular ViaVersion list and extend it by our own 26.3 version
        List<ProtocolVersion> protocols = new ArrayList<>(ProtocolVersion.getProtocols());
        protocols.add(MC_26_3);
        for (ProtocolVersion version : protocols) {
            EnumMap<State, PacketTypeRegistry> forState = new EnumMap<>(State.class);
            for (State state : State.values()) {
                forState.put(state, new PacketTypeRegistry(version, state));
            }
            forVersionAndState.put(version, forState);
        }
    }

    public static PacketTypeRegistry get(ProtocolVersion version, State state) {
        EnumMap<State, PacketTypeRegistry> forState = forVersionAndState.get(version);
        return forState != null ? forState.get(state) : new PacketTypeRegistry(version, state);
    }

    private final ProtocolVersion version;
    private final State state;
    private final PacketType unknown;
    private final Map<Integer, PacketType> typeForId = new HashMap<>();
    private final Map<PacketType, Integer> idForType = new HashMap<>();

    private PacketTypeRegistry(ProtocolVersion version, State state) {
        this.version = version;
        this.state = state;

        if (MC_26_3.equals(version)) {
            // ViaVersion has no native 26.3 converter, so the ids cannot be derived by walking the ViaVersion
            // protocol chain. Use the real ids registered by the vanilla 26.3 client instead.
            PacketType unknown26_3 = null;
            for (PacketType packetType : PacketType.values()) {
                if (packetType.isUnknown() && packetType.getState() == state) {
                    unknown26_3 = packetType;
                    break;
                }
            }
            this.unknown = unknown26_3;
            Map<Integer, PacketType> ids26_3 = MC_26_3_PACKET_IDS.get(state);
            if (ids26_3 != null) {
                for (Map.Entry<Integer, PacketType> entry : ids26_3.entrySet()) {
                    typeForId.put(entry.getKey(), entry.getValue());
                    idForType.put(entry.getValue(), entry.getKey());
                }
            }
            return;
        }

        PacketType unknown = null;
        packets: for (PacketType packetType : PacketType.values()) {
            if (packetType.getState() != state) {
                continue; // incorrect protocol state (e.g. LOGIN vs PLAY)
            }

            if (packetType.isUnknown()) {
                unknown = packetType;
                continue; // "unknown" type exists for all versions
            }

            if (packetType.getInitialVersion().newerThan(version)) {
                continue; // packet didn't yet exist in this version
            }

            List<ProtocolPathEntry> protocolPath = getProtocolPath(version.getVersion(), packetType.getInitialVersion().getVersion());
            if (protocolPath == null) {
                continue; // no path from packet version to current version (current version is not supported)
            }

            protocolPath = Lists.reverse(protocolPath);

            List<ProtocolVersion> inputProtocols = new ArrayList<>();
            for (ProtocolPathEntry entry : protocolPath) {
                inputProtocols.add(entry.outputProtocolVersion());
            }
            inputProtocols.add(version);
            inputProtocols.remove(0);

            int id = packetType.getInitialId();
            for (int i = 0; i < protocolPath.size(); i++) {
                ProtocolPathEntry entry = protocolPath.get(i);
                Protocol<?, ?, ?, ?> protocol = entry.protocol();
                boolean wasReplaced = false;
                for (Pair<Integer, Integer> idMapping : getIdMappings(protocol, state)) {
                    int oldId = idMapping.getKey();
                    int newId = idMapping.getValue();
                    if (oldId == id) {
                        if (newId == -1) {
                            // Packet no longer exists in this version.

                            // Special case: Minecraft replaces the DestroyEntities packet in 1.17 with a singe-entity
                            //               variant, only to revert that change in 1.17.1. So let's keep that around
                            //               if we are not stopping at 1.17.
                            if (protocol instanceof Protocol1_16_4To1_17 && packetType == PacketType.DestroyEntities && version != ProtocolVersion.v1_17) {
                                // ViaVersion maps the newly introduced DestroyEntity back to the good old
                                // DestroyEntities in 1.17.1, but not the other way around (cause it has to emit many
                                // packets for one), so if we just manually map to DestroyEntity here, it'll map back
                                // in 1.17.1 for us.
                                id = PacketType.DestroyEntity.getInitialId();
                                wasReplaced = false;
                                break;
                            }

                            continue packets;
                        }
                        id = newId;
                        wasReplaced = false;
                        break;
                    }
                    if (newId == id) {
                        wasReplaced = true;
                    }
                }

                // Special case: Multiple packets get merged into Spawn Object in 1.19, we want to drop those and
                //               preserve the original type.
                if (protocol instanceof Protocol1_18_2To1_19) {
                    switch (packetType) {
                        case SpawnPainting:
                        case SpawnMob:
                            continue packets;
                        case SpawnObject:
                            wasReplaced = false;
                            id = 0;
                            break;
                    }
                }

                // Special case: ViaVersion remaps the Spawn Global Entity packet into a Spawn Entity, though they're
                //               logically distinct packets for us.
                if (protocol instanceof Protocol1_15_2To1_16 && packetType == PacketType.SpawnGlobalEntity) {
                    wasReplaced = true;
                }

                // Special case: ViaVersion remaps the Use Bed packet into a Entity Metadata, though they're logically
                //               distinct packets for us.
                if (protocol instanceof Protocol1_13_2To1_14 && packetType == PacketType.PlayerUseBed) {
                    wasReplaced = true;
                }

                // Special case: ViaVersion cancels the Update Entity NBT packets unconditionally, instead of setting
                //               their newId to -1.
                if (protocol instanceof Protocol1_8To1_9 && packetType == PacketType.EntityNBTUpdate) {
                    wasReplaced = true;
                }

                // Special case: ViaVersion remaps the DestroyEntity packet into a DestroyEntities. thought they're
                //               logically distinct packets for us.
                if (protocol instanceof Protocol1_17To1_17_1 && packetType == PacketType.DestroyEntity) {
                    wasReplaced = true;
                }

                // Special case: Spawn Player is finally merged into Spawn Object
                if (protocol instanceof Protocol1_20To1_20_2 && packetType == PacketType.SpawnPlayer) {
                    wasReplaced = true;
                }

                // Special case: Spawn Exp Orb is finally merged into Spawn Object
                if (protocol instanceof Protocol1_21_4To1_21_5 && packetType == PacketType.SpawnExpOrb) {
                    wasReplaced = true;
                }

                if (wasReplaced) {
                    ProtocolVersion expected = packetType.getRemovedVersion();
                    ProtocolVersion actual = inputProtocols.get(i);
                    if (expected == null) {
                        throw new RuntimeException("Packet " + packetType + " unexpectedly removed in " + version);
                    }
                    if (!expected.equalTo(actual)) {
                        throw new RuntimeException("Packet " + packetType + " unexpectedly removed in " + actual + " should have been removed in " + expected);
                    }
                    continue packets; // packet no longer exists in this version
                }
            }

            ProtocolVersion expectedRemoval = packetType.getRemovedVersion();
            if (expectedRemoval != null && expectedRemoval.olderThanOrEqualTo(version)) {
                throw new RuntimeException("Packet " + packetType + " unexpectedly still present in " + version);
            }

            PacketType existingType = typeForId.get(id);
            if (existingType != null) {
                throw new RuntimeException("Id " + id + " in " + version + " to be assigned to " + packetType + " but already assigned to " + existingType);
            }

            typeForId.put(id, packetType);
            idForType.put(packetType, id);
        }
        this.unknown = unknown;
    }

    private static List<ProtocolPathEntry> getProtocolPath(int clientVersion, int serverVersion) {
        // ViaVersion doesn't officially support 1.7.6 but luckily there weren't any (client-bound) packet id changes
        if (serverVersion == ProtocolVersion.v1_7_6.getVersion()) {
            return getProtocolPath(clientVersion, ProtocolVersion.v1_8.getVersion());
        }
        if (clientVersion == ProtocolVersion.v1_7_6.getVersion()) {
            return getProtocolPath(ProtocolVersion.v1_8.getVersion(), serverVersion);
        }
        // The trivial case
        if (clientVersion == serverVersion) {
            return Collections.emptyList();
        }
        // otherwise delegate to ViaVersion
        return Via.getManager().getProtocolManager().getProtocolPath(clientVersion, serverVersion);
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public State getState() {
        return state;
    }

    public Integer getId(PacketType type) {
        return idForType.get(type);
    }

    public PacketType getType(int id) {
        return typeForId.getOrDefault(id, unknown);
    }

    public boolean atLeast(ProtocolVersion protocolVersion) {
        return version.getVersion() >= protocolVersion.getVersion();
    }

    public boolean atMost(ProtocolVersion protocolVersion) {
        return version.getVersion() <= protocolVersion.getVersion();
    }

    public boolean olderThan(ProtocolVersion protocolVersion) {
        return version.getVersion() < protocolVersion.getVersion();
    }

    public PacketTypeRegistry withState(State state) {
        return PacketTypeRegistry.get(version, state);
    }

    public PacketTypeRegistry withLoginSuccess() {
        return withState(atLeast(ProtocolVersion.v1_20_2) ? State.CONFIGURATION : State.PLAY);
    }

    private static List<Pair<Integer, Integer>> getIdMappings(Protocol<?, ?, ?, ?> protocol, State state) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        try {
            if (clientbound == null) {
                clientbound = AbstractProtocol.class.getDeclaredField("clientboundMappings");
                clientbound.setAccessible(true);
            }
            PacketMappings mappings = (PacketMappings) clientbound.get(protocol);

            PacketTypeMap<? extends ClientboundPacketType> packetTypeMap =
                    protocol.getPacketTypesProvider().unmappedClientboundPacketTypes().get(state);
            if (packetTypeMap == null) {
                return result;
            }

            PacketWrapper dummyPacketWrapper = PacketWrapper.create(null, (UserConnection) null);
            for (ClientboundPacketType unmappedPacketType : packetTypeMap.types()) {
                PacketMapping packetMapping = mappings.mappedPacket(state, unmappedPacketType.getId());
                if (packetMapping == null) {
                    continue;
                }

                dummyPacketWrapper.setPacketType(null);
                packetMapping.applyType(dummyPacketWrapper);

                int oldId = unmappedPacketType.getId();
                int newId = dummyPacketWrapper.getId();
                result.add(Pair.of(oldId, newId));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
