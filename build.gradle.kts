plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.16.0"
    id("org.jetbrains.kotlinx.kover") version "0.7.3"
}

group = "com.alibabacloud"
version = "0.0.9"

repositories {
    mavenCentral()
    maven("https://repo1.maven.org/maven2/com/aliyun/")
}

dependencies {
    implementation("com.aliyun:tea:1.2.8") {
        exclude(group = "org.slf4j")
    }
    implementation("com.aliyun:credentials-java:0.3.0")
    implementation("com.aliyun:tea-xml:0.0.1") {
        exclude(group = "org.slf4j")
    }
    implementation("com.aliyun:tea-util:0.2.21")
    implementation("com.aliyun:openapiutil:0.2.1")
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

intellij {
    val ideName = System.getenv("INTELLIJ_TYPE") ?: "IU"
    val ideVersion = System.getenv("INTELLIJ_VERSION") ?: "2022.2.5"
    val pyVersion = System.getenv("PY_VERSION") ?: "222.4554.10"
    val goVersion = System.getenv("GO_VERSION") ?: "222.4459.24"
    version.set(ideVersion)
    type.set(ideName)
    plugins.set(listOf("com.intellij.java", "Pythonid:$pyVersion", "org.jetbrains.plugins.go:$goVersion"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    test {
        useJUnitPlatform()
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
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
