package com.github.retrooper.packetevents.test;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UnknownAttributeTest extends BaseDummyAPITest {

    private static final int UNKNOWN_ID = 4096;

    @Test
    @DisplayName("Unknown attribute ids are read and written back unchanged")
    public void testUnknownAttributeIdRoundTrip() {
        ServerVersion version = ServerVersion.V_1_21_4;
        int scaleId = Attributes.SCALE.getId(version.toClientVersion());

        ByteBuf input = Unpooled.buffer();
        PacketWrapper<?> writer = PacketWrapper.createUniversalPacketWrapper(input, version);
        writer.writeVarInt(42);
        writer.writeVarInt(2);
        writer.writeVarInt(scaleId);
        writer.writeDouble(2.0);
        writer.writeVarInt(0);
        writer.writeVarInt(UNKNOWN_ID);
        writer.writeDouble(1.5);
        writer.writeVarInt(0);
        byte[] expected = ByteBufUtil.getBytes(input);

        TestWrapper wrapper = new TestWrapper(version, input);
        assertDoesNotThrow(wrapper::read);

        List<WrapperPlayServerUpdateAttributes.Property> properties = wrapper.getProperties();
        assertEquals(2, properties.size());
        assertSame(Attributes.SCALE, properties.get(0).getAttribute());
        assertEquals(2.0, properties.get(0).getValue());

        Attribute unknown = properties.get(1).getAttribute();
        assertEquals(UNKNOWN_ID, unknown.getId(version.toClientVersion()));
        assertEquals(unknown, Attributes.getByIdOrCreate(version.toClientVersion(), UNKNOWN_ID));
        assertEquals(1.5, properties.get(1).getValue());

        ByteBuf output = Unpooled.buffer();
        TestWrapper rewriter = new TestWrapper(version, output);
        rewriter.setEntityId(wrapper.getEntityId());
        rewriter.setProperties(properties);
        rewriter.write();
        assertArrayEquals(expected, ByteBufUtil.getBytes(output));
    }

    private static final class TestWrapper extends WrapperPlayServerUpdateAttributes {

        private TestWrapper(ServerVersion version, ByteBuf buffer) {
            super(0, new ArrayList<>());
            this.serverVersion = version;
            this.clientVersion = ClientVersion.UNKNOWN;
            this.buffer = buffer;
        }
    }
}
