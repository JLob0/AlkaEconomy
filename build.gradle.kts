import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    `maven-publish`
}

group = "com.alkacode"
version = "1.0.6"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    // banco/HikariCP vem do AlkaCore (DatabaseProvider) - AlkaEconomy nao abre
    // conexao JDBC propria nem embarca driver, so usa java.sql.* sobre o que o
    // Core ja disponibiliza em runtime (publicado via
    // `./gradlew publishToMavenLocal` no projeto AlkaCore).
    compileOnly("com.alkacode:AlkaCore:1.0.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // expand() so pode rodar no plugin.yml (que tem o placeholder ${version}) - o
    // config.yml tem simbolos de moeda tipo "$" que o template engine do Gradle
    // (Groovy SimpleTemplateEngine) tenta interpretar como inicio de expressao e
    // quebra o build.
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// publica o jar "puro" (classes do plugin) no repositorio Maven local, para outros
// plugins Alka* consumirem a API via compileOnly.
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "AlkaEconomy"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
