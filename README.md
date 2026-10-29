<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
    <h2><i>PacketEventsModern (PEM)</i></h2>
    <h3>PacketEventsModern is a fork of <a href="https://github.com/retrooper/packetevents">PacketEvents</a>, a protocol library tailored to Minecraft Java Edition, kept up to date with modern platforms and libraries.</h3>
    <a href="https://github.com/Flectone/PacketEventsModern/actions"><img src="https://img.shields.io/github/actions/workflow/status/Flectone/PacketEventsModern/gradle-publish.yml?style=for-the-badge&logo=github"></a>
    <a href="https://central.sonatype.com/namespace/net.flectone"><img src="https://img.shields.io/maven-central/v/net.flectone/packeteventsmodern-api?style=for-the-badge"></a>
    <img src="https://img.shields.io/github/license/Flectone/PacketEventsModern?style=for-the-badge&logo=github">
</div>
<h3>What's different</h3>

- Adventure 5.x, Java 21
- Adventure is not shaded, the platform provides it
- NeoForge platform support from 1.21.1 to latest
- Fabric from 1.14 to latest in one jar (intermediary and official mappings)
- Published to Maven Central as `net.flectone:packeteventsmodern-*`

The API and packages are the same as in PacketEvents, so the [original documentation](https://docs.packetevents.com/) still applies.

<h3>Usage</h3>

```groovy
repositories {
    mavenCentral()
}

dependencies {
    compileOnly "net.flectone:packeteventsmodern-spigot:2.14.2"
}
```

<h3>Resources</h3>

- [PacketEvents documentation](https://docs.packetevents.com/)
    - [Getting Started](https://docs.packetevents.com/development-setup)
    - [JavaDocs](https://javadocs.packetevents.com)
- [Releases](https://github.com/Flectone/PacketEventsModern/releases/)
- [Maven Central](https://central.sonatype.com/namespace/net.flectone)

<h3>Credits</h3>

<i>All credit for the original library goes to retrooper and the PacketEvents contributors.</i>

[PacketEvents](https://github.com/retrooper/packetevents)\
[Grim's PacketEvents fork](https://github.com/GrimAnticheat/packetevents)\
[Protocol Documentation for the Minecraft Java Edition](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol)\
[ViaVersion](https://github.com/ViaVersion/ViaVersion)\
[ProtocolSupport](https://github.com/ProtocolSupport/ProtocolSupport)\
[adventure](https://github.com/KyoriPowered/adventure)\
[MCProtocolLib](https://github.com/GeyserMC/MCProtocolLib/)