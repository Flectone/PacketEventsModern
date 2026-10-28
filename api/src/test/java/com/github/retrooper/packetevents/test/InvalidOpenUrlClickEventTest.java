package com.github.retrooper.packetevents.test;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvalidOpenUrlClickEventTest extends BaseDummyAPITest {

    private static final String INVALID_URL = "http://as.link}";

    @Test
    @DisplayName("Invalid open_url click event is dropped when parsing json")
    public void testInvalidOpenUrlJson() {
        String json = "{\"text\":\"" + INVALID_URL + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + INVALID_URL + "\"}}";

        Component component = assertDoesNotThrow(() -> AdventureSerializer.serializer(ClientVersion.V_1_12_2).fromJson(json));

        assertInstanceOf(TextComponent.class, component);
        assertEquals(INVALID_URL, ((TextComponent) component).content());
        assertNull(component.clickEvent());
    }

    @Test
    @DisplayName("Invalid open_url click event is dropped when parsing nbt")
    public void testInvalidOpenUrlNbt() {
        NBTCompound clickEvent = new NBTCompound();
        clickEvent.setTag("action", new NBTString("open_url"));
        clickEvent.setTag("url", new NBTString(INVALID_URL));

        NBTCompound compound = new NBTCompound();
        compound.setTag("text", new NBTString(INVALID_URL));
        compound.setTag("click_event", clickEvent);

        Component component = assertDoesNotThrow(() -> AdventureSerializer.serializer(ClientVersion.V_1_21_6).fromNbtTag(compound));

        assertInstanceOf(TextComponent.class, component);
        assertEquals(INVALID_URL, ((TextComponent) component).content());
        assertNull(component.clickEvent());
    }

    @Test
    @DisplayName("Valid open_url click event is kept")
    public void testValidOpenUrl() {
        String json = "{\"text\":\"link\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://example.com/page\"}}";

        Component component = AdventureSerializer.serializer(ClientVersion.V_1_12_2).fromJson(json);

        assertEquals(ClickEvent.openUrl("https://example.com/page"), component.clickEvent());
    }
}
