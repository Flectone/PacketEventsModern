plugins {
    packetevents.`patching-conventions`
}

dependencies {
    api(libs.adventure.text.serializer.gson)
    api(libs.adventure.text.serializer.json.legacy)
    api(libs.adventure.text.serializer.commons)
}

tasks {
    shadowJar {
        dependencies {
            exclude(dependency("net.kyori:adventure-api:.*"))
            exclude(dependency("net.kyori:adventure-key:.*"))
            exclude(dependency("net.kyori:adventure-nbt:.*"))
            exclude(dependency("com.google.code.gson:gson:.*"))
            exclude("META-INF/services/**")
        }
    }
}
