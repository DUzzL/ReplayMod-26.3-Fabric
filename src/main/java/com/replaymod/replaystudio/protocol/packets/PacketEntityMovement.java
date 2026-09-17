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
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.util.DPosition;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;

public class PacketEntityMovement {
    public static Triple<DPosition, Pair<Float, Float>, Boolean> getMovement(Packet packet) throws IOException {
        PacketType type = packet.getType();
        boolean hasPos = type == PacketType.EntityPosition || type == PacketType.EntityPositionRotation;
        boolean hasRot = type == PacketType.EntityRotation || type == PacketType.EntityPositionRotation;
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(PacketTypeRegistry.MC_26_3)) {
                in.readVarInt(); // entity id
                DPosition pos = null;
                Pair<Float, Float> yawPitch = null;
                boolean onGround = true;
                if (hasPos) {
                    int properties = in.readVarInt();
                    onGround = (properties & 1) != 0; // bit 0: on ground
                    int stepCount = properties >>> 1; // remaining bits: number of steps
                    if (stepCount > 0) {
                        // Each step consists of at least a VarInt tick offset and three shorts.
                        if (stepCount > in.asBuf().readableBytes() / 7) {
                            throw new IOException("Truncated stepped movement data");
                        }
                        int dx = 0, dy = 0, dz = 0;
                        for (int i = 0; i < stepCount; i++) {
                            in.readVarInt(); // tick offset
                            dx += in.readShort();
                            dy += in.readShort();
                            dz += in.readShort();
                        }
                        pos = new DPosition(dx / 4096.0, dy / 4096.0, dz / 4096.0);
                    } else {
                        pos = new DPosition(
                                in.readShort() / 4096.0,
                                in.readShort() / 4096.0,
                                in.readShort() / 4096.0
                        );
                    }
                }
                if (hasRot) {
                    yawPitch = Pair.of(
                            in.readByte() / 256f * 360,
                            in.readByte() / 256f * 360
                    );
                }
                return Triple.of(pos, yawPitch, onGround);
            }
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                in.readVarInt(); // entity id
            } else {
                in.readInt(); // entity id
            }
            DPosition pos = null;
            if (hasPos) {
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    pos = new DPosition(
                            in.readShort() / 4096.0,
                            in.readShort() / 4096.0,
                            in.readShort() / 4096.0
                    );
                } else {
                    pos = new DPosition(
                            in.readByte() / 32.0,
                            in.readByte() / 32.0,
                            in.readByte() / 32.0
                    );
                }
            }
            Pair<Float, Float> yawPitch = null;
            if (hasRot) {
                yawPitch = Pair.of(
                        in.readByte() / 256f * 360,
                        in.readByte() / 256f * 360
                );
            }
            boolean onGround = true;
            if (packet.atLeast(ProtocolVersion.v1_8) && (hasPos || hasRot)) {
                onGround = in.readBoolean();
            }
            return Triple.of(pos, yawPitch, onGround);
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId, DPosition deltaPos, Pair<Float, Float> yawPitch, boolean onGround) throws IOException {
        boolean hasPos = deltaPos != null;
        boolean hasRot = yawPitch != null;
        PacketType type;
        if (hasPos) {
            if (hasRot) {
                type = PacketType.EntityPositionRotation;
            } else {
                type = PacketType.EntityPosition;
            }
        } else {
            if (hasRot) {
                type = PacketType.EntityRotation;
            } else {
                type = PacketType.EntityMovement;
            }
        }
        Packet packet = new Packet(registry, type);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(PacketTypeRegistry.MC_26_3)) {
                out.writeVarInt(entityId);
                if (hasPos) {
                    // bit 0: on ground, remaining bits: number of steps (this writer only produces linear deltas)
                    out.writeVarInt(onGround ? 1 : 0);
                    out.writeShort((int) (deltaPos.getX() * 4096));
                    out.writeShort((int) (deltaPos.getY() * 4096));
                    out.writeShort((int) (deltaPos.getZ() * 4096));
                }
                if (hasRot) {
                    out.writeByte((int) (yawPitch.getKey() / 360 * 256));
                    out.writeByte((int) (yawPitch.getValue() / 360 * 256));
                }
                return packet;
            }
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeVarInt(entityId);
            } else {
                out.writeInt(entityId);
            }
            if (hasPos) {
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    out.writeShort((int) (deltaPos.getX() * 4096));
                    out.writeShort((int) (deltaPos.getY() * 4096));
                    out.writeShort((int) (deltaPos.getZ() * 4096));
                } else {
                    out.writeByte((int) (deltaPos.getX() * 32));
                    out.writeByte((int) (deltaPos.getY() * 32));
                    out.writeByte((int) (deltaPos.getZ() * 32));
                }
            }
            if (hasRot) {
                out.writeByte((int) (yawPitch.getKey() / 360 * 256));
                out.writeByte((int) (yawPitch.getValue() / 360 * 256));
            }
            if (packet.atLeast(ProtocolVersion.v1_8) && (hasPos || hasRot)) {
                out.writeBoolean(onGround);
            }
        }
        return packet;
    }
}
