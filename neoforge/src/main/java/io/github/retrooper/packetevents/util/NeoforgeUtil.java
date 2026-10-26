/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.PacketSide;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
@ApiStatus.Internal
public final class NeoforgeUtil {

    private static final String VIA_DECODER_NAME = "via-decoder";
    private static final String VIA_ENCODER_NAME = "via-encoder";

    private NeoforgeUtil() {
    }

    public static void reorderHandlers(ChannelPipeline pipeline) {
        reorderPipeline(pipeline, PacketEvents.DECODER_NAME, "decompress", VIA_DECODER_NAME, "inbound_config", "decoder");
        reorderPipeline(pipeline, PacketEvents.ENCODER_NAME, "compress", VIA_ENCODER_NAME, "outbound_config", "encoder");
    }

    private static void reorderPipeline(ChannelPipeline pipeline, String peName, String compressionName,
                                        String viaName, String configName, String vanillaName) {
        ChannelHandler peHandler = pipeline.get(peName);
        if (peHandler == null) {
            return;
        }

        List<String> names = pipeline.names();
        String compression = names.contains(compressionName) ? compressionName : null;
        String vanilla = names.contains(configName) ? configName : vanillaName;
        boolean hasVia = names.contains(viaName);

        boolean needsReorder = (compression != null && !isAfter(names, peName, compression))
                || (hasVia && !isAfter(names, peName, viaName))
                || !isBefore(names, peName, vanilla);
        if (!needsReorder) {
            return;
        }

        pipeline.remove(peName);

        names = pipeline.names();
        hasVia = names.contains(viaName);
        compression = names.contains(compressionName) ? compressionName : null;
        vanilla = names.contains(configName) ? configName : vanillaName;

        String addAfter = findLatestHandler(names, compression, hasVia ? viaName : null);
        if (addAfter != null) {
            pipeline.addAfter(addAfter, peName, peHandler);
        } else {
            pipeline.addBefore(vanilla, peName, peHandler);
        }
    }

    private static boolean isAfter(List<String> names, String handler, String target) {
        int handlerIdx = names.indexOf(handler);
        int targetIdx = names.indexOf(target);
        if (handlerIdx == -1 || targetIdx == -1) return true;
        return handlerIdx > targetIdx;
    }

    private static boolean isBefore(List<String> names, String handler, String target) {
        int handlerIdx = names.indexOf(handler);
        int targetIdx = names.indexOf(target);
        if (handlerIdx == -1 || targetIdx == -1) return true;
        return handlerIdx < targetIdx;
    }

    private static @Nullable String findLatestHandler(List<String> names, @Nullable String... handlers) {
        String latest = null;
        int latestIdx = -1;
        for (String handler : handlers) {
            if (handler != null) {
                int idx = names.indexOf(handler);
                if (idx > latestIdx) {
                    latestIdx = idx;
                    latest = handler;
                }
            }
        }
        return latest;
    }

    /**
     * Tests whether this connection should be handled by this packetevents api instance.
     */
    public static boolean isOurConnection(Connection connection) {
        return isOurConnection(connection.getReceiving());
    }

    /**
     * Tests whether this connection should be handled by this packetevents api instance.
     */
    public static boolean isOurConnection(PacketFlow flow) {
        PacketSide connectionSide = switch (flow) {
            case CLIENTBOUND -> PacketSide.CLIENT;
            case SERVERBOUND -> PacketSide.SERVER;
        };
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        return api != null && api.getInjector().getPacketSide() == connectionSide;
    }
}
