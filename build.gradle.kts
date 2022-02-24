plugins {
//    java
    application
    kotlin("jvm") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.dunctebot"
version = "1.0"

repositories {
    mavenCentral()

    maven {
        url = uri("https://duncte123.jfrog.io/artifactory/maven")
    }
    maven("https://m2.dv8tion.net/releases")

    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "com.dunctebot", name = "dunctebot-models", version = "0.1.21")

    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.3")

    implementation(group = "io.javalin", name = "javalin", version = "4.3.0")
    implementation(group = "org.apache.velocity", name = "velocity-engine-core", version = "2.2")

    implementation(group = "com.github.ben-manes.caffeine", name = "caffeine", version = "2.8.5")

    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.10.1")

    implementation(group = "net.sf.trove4j", name = "trove4j", version = "3.0.3")
   // implementation(group = "com.jagrosh", name = "jda-utilities-oauth2", version = "3.0.5")
    implementation(group = "com.github.JDA-Applications", name = "JDA-Utilities", version = "804d58a") {
        // This is fine
        exclude(module = "jda-utilities-examples")
        exclude(module = "jda-utilities-doc")
        exclude(module = "jda-utilities-command")
        exclude(module = "jda-utilities-menu")
    }
    implementation(group = "net.dv8tion", name = "JDA", version = "4.3.0_298") {
        exclude(module = "opus-java")
    }

    // webjars
    implementation(group = "org.webjars.npm", name = "vue", version = "2.6.14")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

}

application {
    mainClass.set("com.dunctebot.dashboard.MainKt")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "16"
        }
    }
    wrapper {
        gradleVersion = "7.0.1"
        distributionType = Wrapper.DistributionType.ALL
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
