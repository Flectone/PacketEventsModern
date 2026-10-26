package io.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.FakeChannelUtil;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.handler.PacketDecoder;
import io.github.retrooper.packetevents.handler.PacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.util.List;

public class FabricInjectionUtil {
    private static final String VIA_DECODER_NAME = "via-decoder";
    private static final String VIA_ENCODER_NAME = "via-encoder";

    public static void injectAtPipelineBuilder(ChannelPipeline pipeline, PacketSide pipelineSide) {
        FabricPacketEventsAPI fabricPacketEventsAPI = FabricPacketEventsAPI.getAPI(pipelineSide);

        Channel channel = pipeline.channel();

        User existing = fabricPacketEventsAPI.getProtocolManager().getUser(channel);
        if (existing != null) {
            return;
        }

        fabricPacketEventsAPI.getLogManager().debug("Game connected!");
        User user = new User(channel, ConnectionState.HANDSHAKING,
            null, new UserProfile(null, null));

        fabricPacketEventsAPI.getProtocolManager().setUser(channel, user);

        UserConnectEvent connectEvent = new UserConnectEvent(user);
        fabricPacketEventsAPI.getEventManager().callEvent(connectEvent);
        if (connectEvent.isCancelled()) {
            channel.unsafe().closeForcibly();
            return;
        }

        String decoderName = channel.pipeline().names().contains("inbound_config") ? "inbound_config" : "decoder";
        channel.pipeline().addBefore(decoderName, PacketEvents.DECODER_NAME, new PacketDecoder(pipelineSide, user));
        String encoderName = channel.pipeline().names().contains("outbound_config") ? "outbound_config" : "encoder";
        channel.pipeline().addBefore(encoderName, PacketEvents.ENCODER_NAME, new PacketEncoder(pipelineSide, user));
        channel.closeFuture().addListener((ChannelFutureListener) future ->
            PacketEventsImplHelper.handleDisconnection(user.getChannel(), user.getUUID()));
    }

    // Shared method to remove handlers if they exist
    public static void removeIfExists(ChannelPipeline pipeline, String handlerName) {
        if (pipeline.get(handlerName) != null) {
            pipeline.remove(handlerName);
        }
    }

    /**
     * Reorders PacketEvents handlers to maintain correct relative positions after
     * compression handlers are added or ViaVersion triggers a reorder.
     * <p>
     * Required decoder order: decompress -> via-decoder -> pe-decoder -> decoder
     * Required encoder order: compress -> via-encoder -> pe-encoder -> encoder
     * <p>
     * Note: Other handlers may exist between these, but the relative ordering must be preserved.
     * This method is resilient to being called multiple times - it will only move handlers
     * if they are actually in the wrong position.
     */
    public static void reorderHandlers(ChannelPipeline pipeline, PacketSide side) {
        FabricPacketEventsAPI api = FabricPacketEventsAPI.getAPI(side);

        if (api.getSettings().isDebugEnabled()) {
            api.getLogManager().debug("Pipeline before reorder: " + pipeline.names());
        }

        // Reorder decoders
        reorderPipeline(pipeline, PacketEvents.DECODER_NAME, "decompress", VIA_DECODER_NAME, "inbound_config", "decoder");

        // Reorder encoders
        reorderPipeline(pipeline, PacketEvents.ENCODER_NAME, "compress", VIA_ENCODER_NAME, "outbound_config", "encoder");

        if (api.getSettings().isDebugEnabled()) {
            api.getLogManager().debug("Pipeline after reorder: " + pipeline.names());
        }
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

    /**
     * Checks if handler is positioned AFTER target in the pipeline.
     * Returns true if target doesn't exist (constraint is trivially satisfied).
     */
    private static boolean isAfter(List<String> names, String handler, String target) {
        int handlerIdx = names.indexOf(handler);
        int targetIdx = names.indexOf(target);
        if (handlerIdx == -1 || targetIdx == -1) return true;
        return handlerIdx > targetIdx;
    }

    /**
     * Checks if handler is positioned BEFORE target in the pipeline.
     * Returns true if target doesn't exist (constraint is trivially satisfied).
     */
    private static boolean isBefore(List<String> names, String handler, String target) {
        int handlerIdx = names.indexOf(handler);
        int targetIdx = names.indexOf(target);
        if (handlerIdx == -1 || targetIdx == -1) return true;
        return handlerIdx < targetIdx;
    }

    /**
     * Finds the handler with the highest index (latest in pipeline) among the given handler names.
     * Returns null if none of the handlers exist in the pipeline.
     */
    private static String findLatestHandler(List<String> names, String... handlers) {
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

    public static void fireUserLoginEvent(Object player) {
        FabricPacketEventsAPI api = FabricPacketEventsAPI.getServerAPI();

        User user = api.getPlayerManager().getUser(player);
        if (user == null) {
            Object channelObj = api.getPlayerManager().getChannel(player);

            if (!FakeChannelUtil.isFakeChannel(channelObj) &&
                    (!api.isTerminated() || api.getSettings().isKickIfTerminated())) {
                FabricPacketEventsAPI.getServerAPI().getPlayerManager().disconnectPlayer(player, "PacketEvents failed to inject into a channel.");
            }
            return;
        }

        api.getEventManager().callEvent(new UserLoginEvent(user, player));
    }
}
