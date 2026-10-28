/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
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

package com.github.retrooper.packetevents.util.adventure;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTByteArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTDouble;
import com.github.retrooper.packetevents.protocol.nbt.NBTEnd;
import com.github.retrooper.packetevents.protocol.nbt.NBTFloat;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTIntArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTLong;
import com.github.retrooper.packetevents.protocol.nbt.NBTLongArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.nbt.NBTShort;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import com.github.retrooper.packetevents.protocol.nbt.codec.NBTCodec;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.util.NbtCodecs;
import com.github.retrooper.packetevents.protocol.util.NbtDecoder;
import com.github.retrooper.packetevents.util.UniqueIdUtil;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.google.gson.JsonElement;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.StorageNBTComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class AdventureNBTSerializer implements ComponentSerializer<Component, Component, NBT> {

    private static final Set<TextDecoration> DECORATIONS = TextDecoration.NAMES.values();

    private final ClientVersion version;
    private final boolean downsampleColor;

    public AdventureNBTSerializer(ClientVersion version, boolean downsampleColor) {
        this.version = version;
        this.downsampleColor = downsampleColor;
    }

    @Deprecated
    public AdventureNBTSerializer(boolean downsampleColor) {
        this(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), downsampleColor);
    }

    @Contract(value = "!null -> !null")
    @Deprecated
    @Override
    public @Nullable Component deserializeOrNull(@Nullable NBT input) {
        return this.deserializeOrNull(input, PacketWrapper.createDummyWrapper(this.version));
    }

    @Contract(value = "!null, _ -> !null")
    public @Nullable Component deserializeOrNull(@Nullable NBT input, PacketWrapper<?> wrapper) {
        return input != null ? this.deserialize(input, wrapper) : null;
    }

    @Contract(value = "_, !null -> !null")
    @Deprecated
    @Override
    public @Nullable Component deserializeOr(@Nullable NBT input, @Nullable Component fallback) {
        return this.deserializeOr(input, fallback, PacketWrapper.createDummyWrapper(this.version));
    }

    @Contract(value = "_, !null, _ -> !null")
    public @Nullable Component deserializeOr(@Nullable NBT input, @Nullable Component fallback, PacketWrapper<?> wrapper) {
        return input != null ? this.deserialize(input, wrapper) : fallback;
    }

    @Contract(value = "!null -> !null")
    @Deprecated
    @Override
    public @Nullable NBT serializeOrNull(@Nullable Component component) {
        return this.serializeOrNull(component, PacketWrapper.createDummyWrapper(this.version));
    }

    @Contract(value = "!null, _ -> !null")
    public @Nullable NBT serializeOrNull(@Nullable Component component, PacketWrapper<?> wrapper) {
        return component != null ? this.serialize(component, wrapper) : null;
    }

    @Contract(value = "_, !null -> !null")
    @Deprecated
    @Override
    public @Nullable NBT serializeOr(@Nullable Component component, @Nullable NBT fallback) {
        return this.serializeOr(component, fallback, PacketWrapper.createDummyWrapper(this.version));
    }

    @Contract(value = "_, !null, _ -> !null")
    public @Nullable NBT serializeOr(@Nullable Component component, @Nullable NBT fallback, PacketWrapper<?> wrapper) {
        return component != null ? this.serialize(component, wrapper) : fallback;
    }

    @Deprecated
    @Override
    public @NotNull Component deserialize(@NotNull NBT input) {
        return this.deserialize(input, PacketWrapper.createDummyWrapper(this.version));
    }

    public @NotNull Component deserialize(@NotNull NBT input, PacketWrapper<?> wrapper) {
        if (input instanceof NBTString string) { // Serialized as string
            return Component.text(string.getValue());
        }

        if (input instanceof NBTByte bool && bool.getAsByte() < 2) { // Serialized as boolean
            return Component.text(bool.getAsByte() == 1);
        }

        if (input instanceof NBTNumber number) { // Serialized as number
            return Component.text(number.getAsNumber().toString());
        }

        // Serialized as tree
        NBTCompound compound = requireType(input, NBTType.COMPOUND);
        NBTReader reader = new NBTReader(wrapper, compound);

        Function<NBT, String> textFunction = nbt -> switch (nbt) {
            case NBTString string -> string.getValue();
            case NBTByte bool when bool.getAsByte() < 2 -> String.valueOf(bool.getAsByte() == 1);
            case NBTNumber number -> number.getAsNumber().toString();
            default -> throw new IllegalStateException("Don't know how to deserialize " + nbt.getType() + " to text");
        };

        String text = reader.read("text", textFunction);
        if (text == null) text = reader.read("", textFunction);

        String translate = reader.readUTF("translate", Function.identity());
        String translateFallback = reader.readUTF("fallback", Function.identity());
        NBT with = reader.read("with", Function.identity());
        List<TranslationArgument> translateWith = switch (with) {
            case null -> null;
            case NBTIntArray array -> Arrays.stream(array.getValue())
                    .mapToObj(TranslationArgument::numeric)
                    .toList();
            case NBTLongArray array -> Arrays.stream(array.getValue())
                    .mapToObj(TranslationArgument::numeric)
                    .toList();
            case NBTByteArray array -> {
                byte[] params = array.getValue();
                List<TranslationArgument> args = new ArrayList<>(params.length);
                for (byte param : params) {
                    args.add(TranslationArgument.bool(param != (byte) 0));
                }
                yield args;
            }
            default -> this.deserializeTranslationArgumentList(requireType(with, NBTType.LIST).getTags(), wrapper);
        };
        NBTReader score = reader.child("score");
        String selector = reader.readUTF("selector", Function.identity());
        String keybind = reader.readUTF("keybind", Function.identity());
        String nbt = reader.readUTF("nbt", Function.identity());
        boolean nbtInterpret = Optional.ofNullable(reader.readBoolean("interpret", Function.identity())).orElse(false);
        BlockNBTComponent.Pos nbtBlock = reader.readUTF("block", BlockNBTComponent.Pos::fromString);
        String nbtEntity = reader.readUTF("entity", Function.identity());
        Key nbtStorage = reader.readUTF("storage", Key::key);
        List<Component> extra = reader.readList("extra", tag -> this.deserializeComponentList(tag, wrapper));
        Component separator = reader.read("separator", tag -> this.deserialize(tag, wrapper));
        NBT player = reader.read("player", Function.identity());
        String sprite = reader.readUTF("sprite", Function.identity());
        Style style = this.deserializeStyle(compound, wrapper);

        // build component from read values
        ComponentBuilder<?, ?> builder;
        if (text != null) {
            builder = Component.text().content(text);
        } else if (translate != null) {
            TranslatableComponent.Builder i18nBuilder;
            builder = i18nBuilder = Component.translatable().key(translate);
            if (translateWith != null) {
                i18nBuilder.arguments(translateWith);
            }
            i18nBuilder.fallback(translateFallback);
        } else if (score != null) {
            builder = Component.score()
                    .name(score.readUTF("name", Function.identity()))
                    .objective(score.readUTF("objective", Function.identity()));
        } else if (selector != null) {
            builder = Component.selector().pattern(selector).separator(separator);
        } else if (keybind != null) {
            builder = Component.keybind().keybind(keybind);
        } else if (nbt != null) {
            if (nbtBlock != null) {
                builder = Component.blockNBT()
                        .nbtPath(nbt).interpret(nbtInterpret).separator(separator)
                        .pos(nbtBlock);
            } else if (nbtEntity != null) {
                builder = Component.entityNBT()
                        .nbtPath(nbt).interpret(nbtInterpret).separator(separator)
                        .selector(nbtEntity);
            } else if (nbtStorage != null) {
                builder = Component.storageNBT()
                        .nbtPath(nbt).interpret(nbtInterpret).separator(separator)
                        .storage(nbtStorage);
            } else {
                throw new IllegalStateException("Illegal nbt component, block/entity/storage is missing");
            }
        } else if (player != null) {
            ItemProfile profile = ItemProfile.decode(player, wrapper);
            PlayerHeadObjectContents playerHead = ObjectContents.playerHead()
                    .id(profile.getId()).name(profile.getName())
                    .profileProperties(profile.getAdventureProperties())
                    .hat(Optional.ofNullable(reader.readBoolean("hat", Function.identity())).orElse(true))
                    .build();
            builder = Component.object().contents(playerHead);
        } else if (sprite != null) {
            Key spriteKey = Key.key(sprite);
            Key atlasKey = reader.readUTF("atlas", atlas -> Key.key(atlas));
            builder = Component.object().contents(atlasKey != null
                    ? ObjectContents.sprite(atlasKey, spriteKey)
                    : ObjectContents.sprite(spriteKey));
        } else {
            throw new IllegalStateException("Illegal nbt component, component type could not be determined");
        }

        builder.style(style);

        if (extra != null) {
            builder.append(extra);
        }

        return builder.build();
    }

    @Deprecated
    @Override
    public @NotNull NBT serialize(@NotNull Component component) {
        return this.serialize(component, PacketWrapper.createDummyWrapper(this.version));
    }

    public @NotNull NBT serialize(@NotNull Component component, PacketWrapper<?> wrapper) {
        if (component instanceof TextComponent text && !component.hasStyling() && component.children().isEmpty()) {
            return new NBTString(text.content());
        }

        return this.serializeComponent(component, wrapper);
    }

    private @NotNull NBTCompound serializeComponent(Component component, PacketWrapper<?> wrapper) {
        NBTWriter writer = new NBTWriter(new NBTCompound());

        // component parts
        switch (component) {
            case TextComponent text -> {
                // text content
                writer.writeUTF("text", text.content());
            }
            case TranslatableComponent translatable -> {
                // translation key
                writer.writeUTF("translate", translatable.key());

                // translation fallback
                String fallback = translatable.fallback();
                if (fallback != null) {
                    writer.writeUTF("fallback", fallback);
                }

                // translation arguments
                List<TranslationArgument> args = translatable.arguments();
                if (!args.isEmpty()) {
                    writer.writeList("with", NBTType.COMPOUND, this.serializeTranslationArgumentList(args, wrapper));
                }
            }
            case ScoreComponent scoreComponent -> {
                // nested compound
                NBTWriter score = writer.child("score");

                // score name
                score.writeUTF("name", scoreComponent.name());

                // score objective
                score.writeUTF("objective", scoreComponent.objective());
            }
            case SelectorComponent selector -> {
                // selector
                writer.writeUTF("selector", selector.pattern());

                // separator
                Component separator = selector.separator();
                if (separator != null) writer.write("separator", this.serialize(separator, wrapper));
            }
            case KeybindComponent keybind -> {
                // keybind
                writer.writeUTF("keybind", keybind.keybind());
            }
            case NBTComponent<?> nbtComponent -> {
                // nbt path
                writer.writeUTF("nbt", nbtComponent.nbtPath());

                // interpret
                if (nbtComponent.interpret()) {
                    writer.writeBoolean("interpret", true);
                }

                // separator
                Component separator = nbtComponent.separator();
                if (separator != null) writer.write("separator", this.serialize(separator, wrapper));

                switch (nbtComponent) {
                    case BlockNBTComponent block -> {
                        // nbt block
                        writer.writeUTF("block", block.pos().asString());
                    }
                    case EntityNBTComponent entity -> {
                        // nbt entity
                        writer.writeUTF("entity", entity.selector());
                    }
                    case StorageNBTComponent storage -> {
                        // nbt storage key
                        writer.writeUTF("storage", storage.storage().asString());
                    }
                    default -> {
                    }
                }
            }
            case ObjectComponent object -> {
                if (this.version.isNewerThanOrEquals(ClientVersion.V_1_21_9)) {
                    // object contents
                    switch (object.contents()) {
                        case PlayerHeadObjectContents playerHead -> {
                            // player head profile
                            ItemProfile profile = ItemProfile.fromAdventure(playerHead);
                            writer.write("player", ItemProfile.encode(wrapper, profile));

                            // player head hat
                            if (playerHead.hat() != PlayerHeadObjectContents.DEFAULT_HAT) {
                                writer.writeBoolean("hat", playerHead.hat());
                            }
                        }
                        case SpriteObjectContents sprite -> {
                            // atlas
                            if (!sprite.atlas().equals(SpriteObjectContents.DEFAULT_ATLAS)) {
                                writer.writeUTF("atlas", sprite.atlas().toString());
                            }

                            // sprite
                            writer.writeUTF("sprite", sprite.sprite().toString());
                        }
                        default -> {
                        }
                    }
                } else {
                    // skip
                    writer.writeUTF("text", "");
                }
            }
            default -> {
            }
        }

        if (component.hasStyling()) {
            this.serializeStyle(component.style(), wrapper).getTags().forEach(writer::write);
        }

        // component children
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            writer.writeList("extra", NBTType.COMPOUND, this.serializeComponentList(children, wrapper));
        }

        return writer.compound;
    }

    // -------------------- Style --------------------
    @Deprecated
    public @NotNull Style deserializeStyle(NBTCompound input) {
        return this.deserializeStyle(input, PacketWrapper.createDummyWrapper(this.version));
    }

    @SuppressWarnings("PatternValidation")
    public @NotNull Style deserializeStyle(NBTCompound input, PacketWrapper<?> wrapper) {
        if (input.isEmpty()) return Style.empty();

        Style.Builder style = Style.style();
        NBTReader reader = new NBTReader(wrapper, input);

        String font = reader.getUTF("font");
        if (font != null) style.font(Key.key(font));

        String colorName = reader.getUTF("color");
        if (colorName != null) {
            TextColor color = this.deserializeColor(colorName);
            if (color != null) style.color(color);
        }

        Number shadowColor = reader.getNumber("shadow_color");
        if (shadowColor != null) style.shadowColor(ShadowColor.shadowColor(shadowColor.intValue()));

        for (TextDecoration decoration : DECORATIONS) {
            Boolean value = reader.getBoolean(decoration.toString());
            if (value != null) style.decoration(decoration, TextDecoration.State.byBoolean(value));
        }

        String insertion = reader.getUTF("insertion");
        if (insertion != null) style.insertion(insertion);

        boolean modernEvents = this.version.isNewerThanOrEquals(ClientVersion.V_1_21_5);
        NBTReader clickEvent = reader.child(modernEvents ? "click_event" : "clickEvent");
        if (clickEvent != null) {
            ClickEvent.Action<?> action = clickEvent.readUTF("action", ClickEvent.Action.NAMES::value);
            ClickEvent<?> value = switch (action.name()) {
                case "open_url" -> {
                    try {
                        yield ClickEvent.openUrl(clickEvent.readUTF(modernEvents ? "url" : "value", Function.identity()));
                    } catch (IllegalArgumentException ignored) {
                        yield null;
                    }
                }
                case "open_file" -> ClickEvent.openFile(clickEvent.readUTF(modernEvents ? "path" : "value", Function.identity()));
                case "run_command" -> ClickEvent.runCommand(clickEvent.readUTF(modernEvents ? "command" : "value", Function.identity()));
                case "suggest_command" -> ClickEvent.suggestCommand(clickEvent.readUTF(modernEvents ? "command" : "value", Function.identity()));
                case "change_page" -> ClickEvent.changePage(modernEvents
                        ? clickEvent.readNumber("page", Number::intValue)
                        : clickEvent.readUTF("value", Integer::parseInt));
                case "copy_to_clipboard" -> ClickEvent.copyToClipboard(clickEvent.readUTF("value", Function.identity()));
                case "show_dialog" -> {
                    NBT dialogTag = clickEvent.read("dialog", Function.identity());
                    yield ClickEvent.showDialog(Dialog.decode(dialogTag, wrapper));
                }
                case "custom" -> {
                    Key key = clickEvent.readUTF("id", Key::key);
                    NBT payload = clickEvent.read("payload", Function.identity());
                    yield ClickEvent.custom(key, new NbtTagHolder(payload != null ? payload : NBTEnd.INSTANCE));
                }
                default -> throw new UnsupportedOperationException("Unsupported clickevent: " + action);
            };
            style.clickEvent(value);
        }

        NBTReader hoverEvent = reader.child(modernEvents ? "hover_event" : "hoverEvent");
        if (hoverEvent != null) {
            HoverEvent.Action<?> action = hoverEvent.readUTF("action", HoverEvent.Action.NAMES::value);
            switch (action.toString()) {
                case "show_text" -> style.hoverEvent(HoverEvent.showText(hoverEvent.read(modernEvents ? "value" : "contents",
                        tag -> this.deserialize(tag, wrapper))));
                case "show_item" -> {
                    if (!modernEvents && hoverEvent.type("contents") == NBTType.STRING) {
                        style.hoverEvent(HoverEvent.showItem(hoverEvent.readUTF("contents", Key::key), 1));
                    } else {
                        NBTReader item = modernEvents ? hoverEvent : hoverEvent.child("contents");
                        if (item != null) {
                            style.hoverEvent(this.deserializeShowItem(item));
                        }
                    }
                }
                case "show_entity" -> {
                    NBTReader entity = modernEvents ? hoverEvent : hoverEvent.child("contents");
                    if (entity != null) {
                        style.hoverEvent(HoverEvent.showEntity(
                                entity.readUTF(modernEvents ? "id" : "type", Key::key),
                                entity.read(modernEvents ? "uuid" : "id", NbtCodecs.LENIENT_UUID),
                                entity.read("name", name -> this.deserialize(name, wrapper))));
                    }
                }
                default -> {
                }
            }
        }

        return style.build();
    }

    @SuppressWarnings("PatternValidation")
    private HoverEvent<HoverEvent.ShowItem> deserializeShowItem(NBTReader item) {
        Key itemId = item.readUTF("id", Key::key);
        Integer count = item.readNumber("count", Number::intValue);
        int nonNullCount = count == null ? 1 : count;

        BinaryTagHolder tag = item.readUTF("tag", BinaryTagHolder::binaryTagHolder);
        if (tag != null) {
            return HoverEvent.showItem(itemId, nonNullCount, tag);
        }

        Map<Key, DataComponentValue> components = item.readCompound("components", nbt -> {
            Map<Key, DataComponentValue> map = new HashMap<>(nbt.size());
            for (Map.Entry<String, NBT> entry : nbt.getTags().entrySet()) {
                if (entry.getKey().startsWith("!")) { // removed component
                    map.put(Key.key(entry.getKey().substring(1)), DataComponentValue.removed());
                } else {
                    map.put(Key.key(entry.getKey()), new NbtTagHolder(entry.getValue()));
                }
            }
            return map;
        });
        return HoverEvent.showItem(itemId, nonNullCount, components == null ? Map.of() : components);
    }

    @Deprecated
    public @NotNull NBTCompound serializeStyle(Style style) {
        return this.serializeStyle(style, PacketWrapper.createDummyWrapper(this.version));
    }

    public @NotNull NBTCompound serializeStyle(Style style, PacketWrapper<?> wrapper) {
        if (style.isEmpty()) return new NBTCompound();

        NBTWriter writer = new NBTWriter(new NBTCompound());

        Key font = style.font();
        if (font != null) writer.writeUTF("font", font.asString());

        TextColor color = style.color();
        if (color != null) writer.writeUTF("color", this.serializeColor(color));

        ShadowColor shadowColor = style.shadowColor();
        if (shadowColor != null) writer.writeInt("shadow_color", shadowColor.value());

        for (TextDecoration decoration : DECORATIONS) {
            TextDecoration.State state = style.decoration(decoration);
            if (state != TextDecoration.State.NOT_SET) {
                writer.writeBoolean(decoration.toString(), state == TextDecoration.State.TRUE);
            }
        }

        String insertion = style.insertion();
        if (insertion != null) writer.writeUTF("insertion", insertion);

        ClickEvent<?> clickEvent = style.clickEvent();
        if (clickEvent != null) {
            boolean modern = this.version.isNewerThanOrEquals(ClientVersion.V_1_21_5);
            NBTWriter child = writer.child(modern ? "click_event" : "clickEvent");
            child.writeUTF("action", clickEvent.action().toString());
            if (!modern) {
                child.writeUTF("value", switch (clickEvent.payload()) {
                    case ClickEvent.Payload.Int page -> Integer.toString(page.integer());
                    case ClickEvent.Payload.Text text -> text.value();
                    default -> throw new UnsupportedOperationException("Unsupported clickevent: " + clickEvent);
                });
            } else {
                switch (clickEvent.payload()) {
                    case ClickEvent.Payload.Text text -> child.writeUTF(switch (clickEvent.action().name()) {
                        case "open_url" -> "url";
                        case "open_file" -> "path";
                        case "run_command", "suggest_command" -> "command";
                        case "copy_to_clipboard" -> "value";
                        default -> throw new UnsupportedOperationException("Unsupported clickevent: " + clickEvent);
                    }, text.value());
                    case ClickEvent.Payload.Int page -> child.writeInt("page", page.integer());
                    case ClickEvent.Payload.Dialog dialog -> child.write("dialog", Dialog.encode(wrapper, (Dialog) dialog.dialog()));
                    case ClickEvent.Payload.Custom customPayload -> {
                        child.writeUTF("id", customPayload.key().asString());
                        BinaryTagHolder nbtHolder = customPayload.nbt();
                        if (nbtHolder instanceof NbtTagHolder holder) {
                            NBT payloadTag = holder.getTag();
                            // nbt end tag means there is no payload
                            if (!(payloadTag instanceof NBTEnd)) {
                                child.write("payload", payloadTag);
                            }
                        } else if (nbtHolder != null) {
                            String nbtString = nbtHolder.string();
                            // as adventure doesn't want to make the nbt payload nullable (though it actually is),
                            // we use an empty nbt string to mark a null payload
                            if (!nbtString.isEmpty()) {
                                child.write("payload", AdventureNbtUtil.fromString(nbtString));
                            }
                        }
                    }
                }
            }
        }

        HoverEvent<?> hoverEvent = style.hoverEvent();
        if (hoverEvent != null) {
            boolean modern = this.version.isNewerThanOrEquals(ClientVersion.V_1_21_5);
            NBTWriter child = writer.child(modern ? "hover_event" : "hoverEvent");
            child.writeUTF("action", hoverEvent.action().toString());
            switch (hoverEvent.value()) {
                case Component text -> child.write(modern ? "value" : "contents", this.serialize(text, wrapper));
                case HoverEvent.ShowItem item -> this.serializeShowItem(item, child, modern);
                case HoverEvent.ShowEntity showEntity -> {
                    NBTWriter entity = modern ? child : child.child("contents");
                    entity.writeUTF(modern ? "id" : "type", showEntity.type().asString());
                    entity.writeIntArray(modern ? "uuid" : "id", UniqueIdUtil.toIntArray(showEntity.id()));
                    Component name = showEntity.name();
                    if (name != null) {
                        entity.write("name", this.serialize(name, wrapper));
                    }
                }
                default -> {
                }
            }
        }

        return writer.compound;
    }

    private void serializeShowItem(HoverEvent.ShowItem item, NBTWriter child, boolean modern) {
        Key itemId = item.item();
        int count = item.count();
        BinaryTagHolder nbt = item.nbt();
        boolean emptyComps = item.dataComponents().isEmpty();

        // "modern" item stacks are no longer allowed to be inlined
        if (!modern && count == 1 && nbt == null && emptyComps) {
            child.writeUTF("contents", itemId.asString());
            return;
        }

        NBTWriter itemNBT = modern ? child : child.child("contents");
        itemNBT.writeUTF("id", itemId.asString());
        if (!modern || count != 1) {
            itemNBT.writeInt("count", count);
        }
        if (nbt != null) {
            itemNBT.writeUTF("tag", nbt.string());
        }
        if (emptyComps) {
            return;
        }

        NBTWriter compsNbt = itemNBT.child("components");
        for (Map.Entry<Key, DataComponentValue> entry : item.dataComponents().entrySet()) {
            String key = entry.getKey().toString();
            DataComponentValue value = entry.getValue();
            if (value == DataComponentValue.removed()) {
                // I don't think value matters here
                compsNbt.writeCompound("!" + key, new NBTCompound());
                continue;
            }
            switch (value) {
                case NbtTagHolder holder -> compsNbt.write(key, holder.getTag());
                case DataComponentValue.TagSerializable tagSerializable ->
                        compsNbt.write(key, AdventureNbtUtil.fromString(tagSerializable.asBinaryTag().string()));
                case GsonDataComponentValue gsonValue -> {
                    // there is no way to recover 100% of the actual information
                    // without implementing JSON data serialization for every
                    // available component
                    JsonElement element = gsonValue.element();
                    compsNbt.write(key, NBTCodec.jsonToNBT(element));
                }
                default -> {
                    // else: unsupported entry component value, skip
                }
            }
        }
    }
    // -------------------------------------------------

    // ------------------- TextColor -------------------
    private @Nullable TextColor deserializeColor(final @NotNull String value) {
        final TextColor color;
        if (value.startsWith(TextColor.HEX_PREFIX)) {
            color = TextColor.fromHexString(value);
        } else {
            color = NamedTextColor.NAMES.value(value);
        }

        if (color == null) return null;

        return this.downsampleColor ? NamedTextColor.nearestTo(color) : color;
    }

    @SuppressWarnings("ConstantConditions")
    private @NotNull String serializeColor(final @NotNull TextColor value) {
        if (value instanceof NamedTextColor named) {
            return NamedTextColor.NAMES.key(named);
        } else if (this.downsampleColor) {
            return NamedTextColor.NAMES.key(NamedTextColor.nearestTo(value));
        } else {
            return String.format(Locale.ROOT, "%c%06X", TextColor.HEX_CHARACTER, value.value());
        }
    }
    // -------------------------------------------------

    // ---------------- Component List -----------------
    private @NotNull List<Component> deserializeComponentList(List<?> value, PacketWrapper<?> wrapper) {
        if (value.isEmpty()) return List.of();

        List<Component> components = new ArrayList<>(value.size());
        for (Object nbt : value) {
            components.add(this.deserialize((NBT) nbt, wrapper));
        }

        return components;
    }

    private List<NBTCompound> serializeComponentList(List<Component> value, PacketWrapper<?> wrapper) {
        List<NBTCompound> components = new ArrayList<>(value.size());
        for (Component component : value) {
            components.add(this.serializeComponent(component, wrapper));
        }
        return components;
    }
    // -------------------------------------------------

    // ------------ TranslationArgument List ------------
    private @NotNull List<TranslationArgument> deserializeTranslationArgumentList(List<?> value, PacketWrapper<?> wrapper) {
        if (value.isEmpty()) return List.of();

        List<TranslationArgument> arguments = new ArrayList<>(value.size());
        for (Object nbt : value) {
            arguments.add(switch (nbt) {
                case NBTByte bool -> TranslationArgument.bool(bool.getAsByte() != (byte) 0);
                case NBTNumber number -> TranslationArgument.numeric(number.getAsNumber());
                case NBTString string -> TranslationArgument.component(Component.text(string.getValue()));
                default -> TranslationArgument.component(this.deserialize(requireType((NBT) nbt, NBTType.COMPOUND), wrapper));
            });
        }

        return arguments;
    }

    private List<NBTCompound> serializeTranslationArgumentList(List<TranslationArgument> value, PacketWrapper<?> wrapper) {
        List<NBTCompound> arguments = new ArrayList<>(value.size());
        for (TranslationArgument argument : value) {
            arguments.add(this.serializeComponent(argument.asComponent(), wrapper));
        }
        return arguments;
    }
    // -------------------------------------------------

    static class NBTReader {

        private final PacketWrapper<?> wrapper;
        private final NBTCompound compound;

        public NBTReader(PacketWrapper<?> wrapper, NBTCompound compound) {
            this.wrapper = wrapper;
            this.compound = compound;
        }

        public void useBoolean(String key, Consumer<Boolean> consumer) {
            useNumber(key, num -> consumer.accept(num.byteValue() != 0));
        }

        public <R> R readBoolean(String key, Function<Boolean, R> function) {
            return readNumber(key, num -> function.apply(num.byteValue() != 0));
        }

        public void useNumber(String key, Consumer<Number> consumer) {
            useTag(key, tag -> {
                if (tag instanceof NBTNumber number) {
                    consumer.accept(number.getAsNumber());
                } else {
                    throw new IllegalArgumentException("Expected number but got " + tag.getType());
                }
            });
        }

        public <R> R readNumber(String key, Function<Number, R> function) {
            return withTag(key, tag -> {
                if (tag instanceof NBTNumber number) {
                    return function.apply(number.getAsNumber());
                } else {
                    throw new IllegalArgumentException("Expected number but got " + tag.getType());
                }
            });
        }

        public void useUTF(String key, Consumer<String> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.STRING).getValue()));
        }

        public <R> R readUTF(String key, Function<String, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.STRING).getValue()));
        }

        public @Nullable String getUTF(String key) {
            NBT tag = compound.getTagOrNull(key);
            return tag == null ? null : requireType(tag, NBTType.STRING).getValue();
        }

        public @Nullable Number getNumber(String key) {
            NBT tag = compound.getTagOrNull(key);
            if (tag == null) return null;
            if (tag instanceof NBTNumber number) return number.getAsNumber();
            throw new IllegalArgumentException("Expected number but got " + tag.getType());
        }

        public @Nullable Boolean getBoolean(String key) {
            Number number = getNumber(key);
            return number == null ? null : number.byteValue() != 0;
        }

        public void useByteArray(String key, Consumer<byte[]> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.BYTE_ARRAY).getValue()));
        }

        public <R> R readByteArray(String key, Function<byte[], R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.BYTE_ARRAY).getValue()));
        }

        public void useIntArray(String key, Consumer<int[]> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.INT_ARRAY).getValue()));
        }

        public <R> R readIntArray(String key, Function<int[], R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.INT_ARRAY).getValue()));
        }

        public void useLongArray(String key, Consumer<long[]> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.LONG_ARRAY).getValue()));
        }

        public <R> R readLongArray(String key, Function<long[], R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.LONG_ARRAY).getValue()));
        }

        public void useCompound(String key, Consumer<NBTCompound> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.COMPOUND)));
        }

        public <R> R readCompound(String key, Function<NBTCompound, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.COMPOUND)));
        }

        public void useList(String key, Consumer<List<?>> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.LIST).getTags()));
        }

        public <R> R readList(String key, Function<List<?>, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.LIST).getTags()));
        }

        public void use(String key, Consumer<NBT> consumer) {
            useTag(key, consumer);
        }

        public <R> R read(String key, NbtDecoder<R> decoder) {
            return withTag(key, tag -> decoder.decode(tag, this.wrapper));
        }

        public <R> R read(String key, Function<NBT, R> function) {
            return withTag(key, function);
        }

        public NBTReader child(String key) {
            return withTag(key, tag -> {
                NBTCompound compound = requireType(tag, NBTType.COMPOUND);
                return new NBTReader(this.wrapper, compound);
            });
        }

        public NBTType<?> type(String key) {
            return withTag(key, NBT::getType);
        }

        private void useTag(String key, Consumer<NBT> consumer) {
            NBT tag = compound.getTagOrNull(key);
            if (tag != null) {
                consumer.accept(tag);
            }
        }

        private <R> R withTag(String key, Function<NBT, R> function) {
            NBT tag = compound.getTagOrNull(key);
            return tag == null ? null : function.apply(tag);
        }
    }

    static class NBTWriter {
        private final NBTCompound compound;

        public NBTWriter(NBTCompound compound) {
            this.compound = compound;
        }

        public void writeBoolean(String key, boolean value) {
            compound.setTag(key, new NBTByte(value ? (byte) 1 : (byte) 0));
        }

        public void writeByte(String key, byte value) {
            compound.setTag(key, new NBTByte(value));
        }

        public void writeShort(String key, short value) {
            compound.setTag(key, new NBTShort(value));
        }

        public void writeInt(String key, int value) {
            compound.setTag(key, new NBTInt(value));
        }

        public void writeLong(String key, long value) {
            compound.setTag(key, new NBTLong(value));
        }

        public void writeFloat(String key, float value) {
            compound.setTag(key, new NBTFloat(value));
        }

        public void writeDouble(String key, double value) {
            compound.setTag(key, new NBTDouble(value));
        }

        public void writeUTF(String key, String value) {
            compound.setTag(key, new NBTString(value));
        }

        public void writeByteArray(String key, byte[] value) {
            compound.setTag(key, new NBTByteArray(value));
        }

        public void writeIntArray(String key, int[] value) {
            compound.setTag(key, new NBTIntArray(value));
        }

        public void writeLongArray(String key, long[] value) {
            compound.setTag(key, new NBTLongArray(value));
        }

        public <T extends NBT> void writeList(String key, NBTType<T> innerType, List<T> value) {
            compound.setTag(key, new NBTList<>(innerType, value));
        }

        public void writeCompound(String key, NBTCompound value) {
            compound.setTag(key, value);
        }

        public void write(String key, NBT value) {
            compound.setTag(key, value);
        }

        public NBTWriter child(String key) {
            NBTCompound child = new NBTCompound();
            compound.setTag(key, child);
            return new NBTWriter(child);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends NBT> T requireType(NBT nbt, NBTType<T> required) {
        if (nbt.getType() != required) {
            throw new IllegalArgumentException("Expected " + required + " but got " + nbt.getType());
        }
        return (T) nbt;
    }
}