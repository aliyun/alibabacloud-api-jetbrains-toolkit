import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij.platform") version "2.0.1"
    id("org.jetbrains.kotlinx.kover") version "0.7.3"
}

group = "com.alibabacloud"
version = "0.0.12-242"

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    maven("https://repo1.maven.org/maven2/com/aliyun/")
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {
    intellijPlatform {
        val ideVersion = System.getenv("INTELLIJ_VERSION") ?: "2024.2.0.1"
        val goVersion = System.getenv("GO_VERSION") ?: "242.20224.300"
        val pyCoreVersion = System.getenv("PY_CORE_VERSION") ?: "242.20224.300"
        intellijIdeaUltimate(ideVersion, false)
        jetbrainsRuntime()
        bundledPlugins("com.intellij.java")
        plugins(listOf("org.jetbrains.plugins.go:$goVersion", "PythonCore:$pyCoreVersion"))
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }

    implementation("com.aliyun:tea:1.2.8") {
        exclude(group = "org.slf4j")
    }
    implementation("com.aliyun:credentials-java:0.3.0")
    implementation("com.aliyun:tea-xml:0.0.1") {
        exclude(group = "org.slf4j")
    }
    implementation("com.aliyun:tea-util:0.2.21")
    implementation("com.aliyun:openapiutil:0.2.1")
    testRuntimeOnly("junit:junit:4.13.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    testImplementation("org.assertj:assertj-core:3.20.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.1")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "242.*"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}

koverReport {
    defaults {
        xml {
            onCheck = true
        }
        html {
            onCheck = true
        }
    }
}