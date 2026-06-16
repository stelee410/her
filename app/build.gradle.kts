plugins {
    id("com.android.application")
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val syncFullscreenAvatarAssets by tasks.registering(org.gradle.api.tasks.Copy::class) {
    from(rootProject.layout.projectDirectory.dir("assets")) {
        include("standby.mp4", "talking.mp4")
    }
    into(layout.buildDirectory.dir("generated/fullscreen-avatar-assets"))
}

val syncTabletDemoAssets by tasks.registering(org.gradle.api.tasks.Sync::class) {
    (1..5).forEach { index ->
        val sourceDir = "assets/404 star/$index"
        val targetDir = "tablet_demo/star_$index"
        from(rootProject.layout.projectDirectory.file("$sourceDir/打招呼.mp4")) {
            into(targetDir)
            rename { "greeting.mp4" }
        }
        from(rootProject.layout.projectDirectory.file("$sourceDir/待机.mp4")) {
            into(targetDir)
            rename { "idle.mp4" }
        }
        from(rootProject.layout.projectDirectory.file("$sourceDir/说话.mp4")) {
            into(targetDir)
            rename { "speaking.mp4" }
        }
        from(rootProject.layout.projectDirectory.file("$sourceDir/agent.md")) {
            into(targetDir)
        }
    }
    from(rootProject.layout.projectDirectory.file("assets/jess/res/jess-smile.mp4")) {
        into("tablet_demo/jess")
        rename { "greeting.mp4" }
    }
    from(rootProject.layout.projectDirectory.file("assets/jess/res/jess-stay.mp4")) {
        into("tablet_demo/jess")
        rename { "idle.mp4" }
    }
    from(rootProject.layout.projectDirectory.file("assets/jess/res/jess-speak-loop.mp4")) {
        into("tablet_demo/jess")
        rename { "speaking.mp4" }
    }
    from(rootProject.layout.projectDirectory.file("assets/jess/agent.md")) {
        into("tablet_demo/jess")
    }
    into(layout.buildDirectory.dir("generated/tablet-demo-assets"))
}

android {
    namespace = "com.linkyun.her"
    compileSdk = 35

    flavorDimensions += "device"

    defaultConfig {
        applicationId = "com.linkyun.her"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val apiKey = localProperties.getProperty("AGENTVOICE_API_KEY")
            ?: providers.gradleProperty("AGENTVOICE_API_KEY").orNull
            ?: providers.environmentVariable("AGENTVOICE_API_KEY").orNull
            ?: ""
        val llmKey = localProperties.getProperty("AGENTLLM_API_KEY")
            ?: providers.gradleProperty("AGENTLLM_API_KEY").orNull
            ?: providers.environmentVariable("AGENTLLM_API_KEY").orNull
            ?: ""
        buildConfigField("String", "AGENTVOICE_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "AGENTLLM_API_KEY", "\"$llmKey\"")
        buildConfigField("String", "AGENTLLM_BASE_URL", "\"https://agentllm.linkyun.co\"")
        buildConfigField("String", "AGENTVOICE_BASE_URL", "\"https://agentvoice.linkyun.co\"")
        buildConfigField("String", "AGENTVOICE_REALTIME_URL", "\"wss://agentvoice.linkyun.co/v1/realtime\"")
        buildConfigField("String", "AGENTVOICE_CLONED_VOICE", "\"S_VCQjam1U1\"")
    }

    productFlavors {
        create("phone") {
            dimension = "device"
            buildConfigField("boolean", "DIGITAL_AVATAR_ENABLED", "false")
            buildConfigField("boolean", "TABLET_DEMO_MODE", "false")
        }
        create("tablet") {
            dimension = "device"
            buildConfigField("boolean", "DIGITAL_AVATAR_ENABLED", "true")
            buildConfigField("boolean", "TABLET_DEMO_MODE", "false")
        }
        create("tabletDemo") {
            dimension = "device"
            applicationIdSuffix = ".tabletdemo"
            buildConfigField("boolean", "DIGITAL_AVATAR_ENABLED", "true")
            buildConfigField("boolean", "TABLET_DEMO_MODE", "true")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("tablet") {
            assets.srcDir(layout.buildDirectory.dir("generated/fullscreen-avatar-assets"))
        }
        getByName("tabletDemo") {
            res.srcDir("src/tablet/res")
            assets.srcDir(layout.buildDirectory.dir("generated/tablet-demo-assets"))
        }
    }
}

tasks.matching { task ->
    task.name.startsWith("preTablet") && task.name.endsWith("Build")
}.configureEach {
    if (name.startsWith("preTabletDemo")) {
        dependsOn(syncTabletDemoAssets)
    } else {
        dependsOn(syncFullscreenAvatarAssets)
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
