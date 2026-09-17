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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.packets.PacketEntityMovement;
import com.replaymod.replaystudio.protocol.packets.PacketEntityTeleport;
import com.replaymod.replaystudio.protocol.packets.PacketJoinGame;
import com.replaymod.replaystudio.protocol.packets.PacketRespawn;
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.protocol.registry.Registries;
import com.replaymod.replaystudio.util.DPosition;
import com.replaymod.replaystudio.util.Location;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Minecraft26_3ProtocolTest {
    private final PacketTypeRegistry play = PacketTypeRegistry.get(PacketTypeRegistry.MC_26_3, State.PLAY);
    private final PacketTypeRegistry configuration = PacketTypeRegistry.get(PacketTypeRegistry.MC_26_3, State.CONFIGURATION);
    private final PacketTypeRegistry login = PacketTypeRegistry.get(PacketTypeRegistry.MC_26_3, State.LOGIN);

    @Test
    public void expected26_3PacketIds() {
        // The real 26.3 ids, as registered by the vanilla 26.3 client
        assertEquals(50, play.getId(PacketType.JoinGame).intValue());
        assertEquals(84, play.getId(PacketType.Respawn).intValue());
        assertEquals(54, play.getId(PacketType.EntityPosition).intValue());
        assertEquals(55, play.getId(PacketType.EntityPositionRotation).intValue());
        assertEquals(57, play.getId(PacketType.EntityRotation).intValue());
        assertEquals(120, play.getId(PacketType.Reconfigure).intValue());
        assertEquals(35, play.getId(PacketType.EntityTeleport).intValue()); // Entity Position Sync
        assertEquals(85, play.getId(PacketType.EntityHeadLook).intValue());
        assertEquals(103, play.getId(PacketType.EntityVelocity).intValue());
        assertEquals(15, configuration.getId(PacketType.ConfigSelectKnownPacks).intValue());
        assertEquals(2, login.getId(PacketType.LoginSuccess).intValue());
    }

    @Test
    public void linear26_3Movement() throws IOException {
        Packet packet = PacketEntityMovement.write(
                play, 42, new DPosition(0.5, -0.25, 1.0), Pair.of(90f, 45f), false);
        Triple<DPosition, Pair<Float, Float>, Boolean> movement = PacketEntityMovement.getMovement(packet);
        assertNotNull(movement.getLeft());
        assertEquals(0.5, movement.getLeft().getX(), 1e-9);
        assertEquals(-0.25, movement.getLeft().getY(), 1e-9);
        assertEquals(1.0, movement.getLeft().getZ(), 1e-9);
        assertNotNull(movement.getMiddle());
        assertEquals(90f, movement.getMiddle().getKey(), 1e-6);
        assertEquals(45f, movement.getMiddle().getValue(), 1e-6);
        assertFalse(movement.getRight());
    }

    @Test
    public void stepped26_3MovementSumsSteps() throws IOException {
        Packet packet = new Packet(play, PacketType.EntityPosition);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(7); // entity id
            out.writeVarInt(1 | (2 << 1)); // on ground, 2 steps
            out.writeVarInt(1); // tick offset
            out.writeShort(100);
            out.writeShort(-200);
            out.writeShort(300);
            out.writeVarInt(2); // tick offset
            out.writeShort(50);
            out.writeShort(50);
            out.writeShort(-100);
        }
        Triple<DPosition, Pair<Float, Float>, Boolean> movement = PacketEntityMovement.getMovement(packet);
        assertNotNull(movement.getLeft());
        assertEquals(150 / 4096.0, movement.getLeft().getX(), 1e-9);
        assertEquals(-150 / 4096.0, movement.getLeft().getY(), 1e-9);
        assertEquals(200 / 4096.0, movement.getLeft().getZ(), 1e-9);
        assertTrue(movement.getRight());
    }

    @Test
    public void rotationOnlyWriterAndFieldOrder() throws IOException {
        // Rotation-only packets no longer contain the ground flag
        Packet rotationOnly = PacketEntityMovement.write(play, 5, null, Pair.of(90f, 45f), false);
        try (Packet.Reader in = rotationOnly.reader()) {
            assertEquals(5, in.readVarInt());
            assertEquals(64, in.readByte()); // 90 / 360 * 256
            assertEquals(32, in.readByte()); // 45 / 360 * 256
            assertEquals(0, in.asBuf().readableBytes());
        }

        // For position (+ rotation) packets the properties varint precedes the delta data
        Packet positionRotation = PacketEntityMovement.write(
                play, 9, new DPosition(1 / 4096.0, 2 / 4096.0, 3 / 4096.0), Pair.of(0f, 0f), true);
        try (Packet.Reader in = positionRotation.reader()) {
            assertEquals(9, in.readVarInt());
            assertEquals(1, in.readVarInt()); // properties: on ground, 0 steps
            assertEquals(1, in.readShort());
            assertEquals(2, in.readShort());
            assertEquals(3, in.readShort());
            assertEquals(0, in.readByte());
            assertEquals(0, in.readByte());
            assertEquals(0, in.asBuf().readableBytes());
        }
    }

    @Test(expected = IOException.class)
    public void truncatedStepDataIsRejected() throws IOException {
        Packet packet = new Packet(play, PacketType.EntityPosition);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(3); // entity id
            out.writeVarInt(1 | (5 << 1)); // on ground, claims 5 steps
            out.writeVarInt(1);
            out.writeShort(10);
            out.writeShort(10);
            out.writeShort(10);
            // only one of the five steps is present
        }
        PacketEntityMovement.getMovement(packet);
    }

    @Test
    public void legacy26_2MovementEncodingIsPreserved() throws IOException {
        PacketTypeRegistry play26_2 = PacketTypeRegistry.get(ProtocolVersion.v26_2, State.PLAY);
        Packet packet = PacketEntityMovement.write(
                play26_2, 11, new DPosition(0.5, -0.25, 1.0), Pair.of(90f, 45f), true);
        // Old encoding: entity id, three shorts, rotation, ground flag
        try (Packet.Reader in = packet.reader()) {
            assertEquals(11, in.readVarInt());
            assertEquals((int) (0.5 * 4096), in.readShort());
            assertEquals((int) (-0.25 * 4096), in.readShort());
            assertEquals((int) (1.0 * 4096), in.readShort());
            assertEquals(64, in.readByte());
            assertEquals(32, in.readByte());
            assertTrue(in.readBoolean());
            assertEquals(0, in.asBuf().readableBytes());
        }
    }

    @Test
    public void respawnOptionalPreviousGameMode() throws IOException {
        for (int previousGameMode : new int[]{-1, 0, 3}) {
            PacketRespawn respawn = new PacketRespawn();
            respawn.dimension = "minecraft:overworld";
            respawn.dimensionType = new DimensionType(new CompoundTag(), "minecraft:overworld", 0);
            respawn.seed = 42L;
            respawn.gameMode = 1;
            respawn.prevGameMode = (byte) previousGameMode;
            respawn.portalCooldown = 0;
            respawn.seaLevel = 63;

            Packet packet = respawn.write(play);
            PacketRespawn read = PacketRespawn.read(packet, new Registries());
            assertEquals("previous game mode " + previousGameMode, previousGameMode, read.prevGameMode);
            assertEquals(1, read.gameMode);
        }
    }

    @Test
    public void joinGameAbsentPreviousGameMode() throws IOException {
        PacketJoinGame joinGame = new PacketJoinGame();
        joinGame.entityId = 123;
        joinGame.hardcore = false;
        joinGame.gameMode = 0;
        joinGame.prevGameMode = -1;
        joinGame.dimensions = Collections.singletonList("minecraft:overworld");
        joinGame.registries = new Registries();
        joinGame.dimensionType = new DimensionType(new CompoundTag(), "minecraft:overworld", 0);
        joinGame.dimension = "minecraft:overworld";
        joinGame.seed = 7L;
        joinGame.maxPlayers = 20;
        joinGame.viewDistance = 10;
        joinGame.simulationDistance = 10;
        joinGame.respawnScreen = true;
        joinGame.seaLevel = 63;
        joinGame.onlineMode = true;
        joinGame.enforcesSecureChat = true;

        Packet packet = joinGame.write(play);
        PacketJoinGame read = PacketJoinGame.read(packet, new Registries());
        assertEquals(-1, read.prevGameMode);
        assertEquals(0, read.gameMode);
    }

    @Test
    public void teleportSteppedPathUsesLastPosition() throws IOException {
        Packet packet = new Packet(play, PacketType.EntityTeleport);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(99); // entity id
            out.writeVarInt(1); // path type: stepped
            out.writeVarInt(2); // two steps
            out.writeDouble(1.0);
            out.writeDouble(65.0);
            out.writeDouble(1.0);
            out.writeVarInt(1); // tick offset
            out.writeDouble(4.0);
            out.writeDouble(66.5);
            out.writeDouble(-3.0);
            out.writeVarInt(2); // tick offset
            out.writeFloat(90f);
            out.writeFloat(45f);
            out.writeBoolean(true); // on ground
        }
        Location location = PacketEntityTeleport.getLocation(packet);
        assertEquals(4.0, location.getX(), 1e-9);
        assertEquals(66.5, location.getY(), 1e-9);
        assertEquals(-3.0, location.getZ(), 1e-9);
        assertEquals(90f, location.getYaw(), 1e-6);
        assertEquals(45f, location.getPitch(), 1e-6);
    }

    @Test
    public void teleportLinearWriteReadRoundTrip() throws IOException {
        Location original = new Location(12.5, 70.25, -8.75, 123.5f, -15f);
        Packet packet = PacketEntityTeleport.write(play, 42, original, true);
        Location read = PacketEntityTeleport.getLocation(packet);
        assertEquals(original.getX(), read.getX(), 1e-9);
        assertEquals(original.getY(), read.getY(), 1e-9);
        assertEquals(original.getZ(), read.getZ(), 1e-9);
        assertEquals(original.getYaw(), read.getYaw(), 1e-6);
        assertEquals(original.getPitch(), read.getPitch(), 1e-6);
    }
}
