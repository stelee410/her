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

android {
    namespace = "com.linkyun.her"
    compileSdk = 35

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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
}
