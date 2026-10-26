/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2025 retrooper and contributors
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

package com.github.retrooper.packetevents.protocol.chat.clickevent;

import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTEnd;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.adventure.NbtTagHolder;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import net.kyori.adventure.text.event.ClickEvent.Payload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ClickEvent {

    static ClickEvent decode(NBTCompound compound, PacketWrapper<?> wrapper) {
        String actionName = compound.getStringTagValueOrThrow("action");
        ClickEventAction<?> action = ClickEventActions.getRegistry().getByNameOrThrow(actionName);
        return action.decode(compound, wrapper);
    }

    @SuppressWarnings("unchecked") // not unchecked
    static void encode(NBTCompound compound, PacketWrapper<?> wrapper, ClickEvent clickEvent) {
        compound.set("action", clickEvent.getAction().getName(), ResourceLocation::encode, wrapper);
        ((ClickEventAction<? super ClickEvent>) clickEvent.getAction()).encode(compound, wrapper, clickEvent);
    }

    ClickEventAction<?> getAction();

    static ClickEvent fromAdventure(net.kyori.adventure.text.event.ClickEvent<?> clickEvent) {
        return switch (clickEvent.payload()) {
            case Payload.Text text -> switch (clickEvent.action().name()) {
                case "open_url" -> new OpenUrlClickEvent(text.value());
                case "open_file" -> new OpenFileClickEvent(text.value());
                case "run_command" -> new RunCommandClickEvent(text.value());
                case "suggest_command" -> new SuggestCommandClickEvent(text.value());
                case "copy_to_clipboard" -> new CopyToClipboardClickEvent(text.value());
                default -> throw new UnsupportedOperationException("Unsupported clickevent: " + clickEvent);
            };
            case Payload.Int page -> new ChangePageClickEvent(page.integer());
            case Payload.Dialog dialog -> new ShowDialogClickEvent((Dialog) dialog.dialog());
            case Payload.Custom payload -> {
                NbtTagHolder nbtTag = (NbtTagHolder) payload.nbt();
                yield new CustomClickEvent(
                        new ResourceLocation(payload.key()),
                        nbtTag == null || nbtTag.getTag() instanceof NBTEnd ? null : nbtTag.getTag()
                );
            }
        };
    }

    net.kyori.adventure.text.event.ClickEvent asAdventure();
}
