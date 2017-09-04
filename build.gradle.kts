import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val VERTX_VER = "3.4.2"
val KOTLIN_VER = "1.1.4-3"
val KOTLINX_COROUTINES_VER = "0.18"
val JUNIT_VER = "4.12"

group = "org.digieng"
version = "0.1-SNAPSHOT"

plugins {
    kotlin(module = "jvm", version = "1.1.4-3")
}

configure<KotlinProjectExtension> {
    experimental.coroutines = Coroutines.ENABLE
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compile("io.vertx:vertx-core:$VERTX_VER")
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$KOTLIN_VER")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLINX_COROUTINES_VER")
    testCompile("junit:junit:$JUNIT_VER")
    testCompile("io.vertx:vertx-unit:$VERTX_VER")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VER")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions.jvmTarget = "1.8"
task(name = "createSourceJar", type = Jar::class) {
    dependsOn("classes")
    classifier = "sources"
    from("src/main/kotlin")
}

task("createAllJarFiles") {
    dependsOn("jar", "createSourceJar")
}