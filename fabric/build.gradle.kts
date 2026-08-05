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

plugins {
    packetevents.`library-conventions`
    packetevents.`publish-conventions`
    net.fabricmc.`fabric-loom`
}

dependencies {
    // include dependencies
    include(libs.bundles.adventure)
    api(libs.adventure.text.logger.slf4j)
    include(libs.adventure.text.logger.slf4j)
    include(project(":api", "shadow"))
    include(project(":netty-common"))

    // include fabric-specific modules
    include(project(":fabric-common"))
    include(project(":fabric-official"))
    include(project(":fabric-intermediary"))

    // dummy dependency
    minecraft(libs.fabric.minecraft.official)
}

tasks {
    named<Jar>("jar") {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
    }
}