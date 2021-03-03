plugins {
//    java
    application
    kotlin("jvm") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.dunctebot"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()

    maven {
        url = uri("https://duncte123.jfrog.io/artifactory/maven")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "com.dunctebot", name = "dunctebot-models", version = "0.1.20")

    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.3")

    implementation(group = "com.sparkjava", name = "spark-core", version = "2.9.2")
    implementation(group = "org.apache.velocity", name = "velocity-engine-core", version = "2.2")

    implementation(group = "com.github.ben-manes.caffeine", name = "caffeine", version = "2.8.5")

    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.10.1")

    implementation(group = "net.sf.trove4j", name = "trove4j", version = "3.0.3")
    implementation(group = "com.jagrosh", name = "jda-utilities-oauth2", version = "3.0.4")
    implementation(group = "net.dv8tion", name = "JDA", version = "4.2.0_227") {
        exclude(module = "opus-java")
    }

    // Yes, this is JDA
    // We're running this PR https://github.com/DV8FromTheWorld/JDA/pull/1178
    // but it is broken atm
    /*implementation(group = "com.github.dv8fromtheworld", name = "JDA", version = "68f4c4b") {
        exclude(module = "opus-java")
    }*/
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15

}

application {
    mainClassName = "com.dunctebot.dashboard.MainKt"
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "15"
        }
    }
    wrapper {
        gradleVersion = "6.7.1"
        distributionType = Wrapper.DistributionType.ALL
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
