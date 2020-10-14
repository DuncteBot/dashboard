/*
 * MIT License
 *
 * Copyright (c) 2020 Duncan Sterken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
//    java
    application
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.dunctebot"
version = "1.0"

repositories {
    jcenter()

    maven {
        url = uri("https://dl.bintray.com/duncte123/maven")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "com.dunctebot", name = "dunctebot-models", version = "0.0.8")

    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.3")
    implementation(group = "io.github.cdimascio", name = "java-dotenv", version = "5.2.1")

    implementation(group = "com.sparkjava", name = "spark-core", version = "2.9.2")
    implementation(group = "org.apache.velocity", name = "velocity-engine-core", version = "2.2")

    implementation(group = "com.github.ben-manes.caffeine", name = "caffeine", version = "2.8.5")

    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.10.1")

    implementation(group = "net.sf.trove4j", name = "trove4j", version = "3.0.3")
    implementation(group = "com.jagrosh", name = "jda-utilities-oauth2", version = "3.0.4")
    implementation(group = "net.dv8tion", name = "JDA", version = "4.2.0_198") {
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
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

}

application {
    mainClassName = "com.dunctebot.dashboard.MainKt"
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
    wrapper {
        gradleVersion = "6.1.1"
        distributionType = Wrapper.DistributionType.ALL
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
