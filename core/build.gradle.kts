// Pure Kotlin/JVM: no Android imports allowed here, so it unit-tests on any PC in seconds.
plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.bouncycastle)
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
