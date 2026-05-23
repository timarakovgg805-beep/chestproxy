plugins {
    id("com.gtnewhorizons.retrofuturagradle") version("1.3.+")
    id("java")
    id("eclipse")
}

val propMcVersion: String by extra
val propForgeVersion: String by extra
val propMappingsVersion: String by extra
val propModId: String by extra
val propModName: String by extra
val propModJavaVersion: String by extra
val propModGroup: String by extra
val propSpecVersion: String by extra

group = propModGroup
base {
    archivesName.set("${propModId}_${propMcVersion}")
}

var buildNumber = project.findProperty("BUILD_NUMBER") ?: "9999"
version = "${propSpecVersion}.${buildNumber}"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(propModJavaVersion))
    }
}

repositories {
    maven {
        name = "GTNH Maven"
        setUrl("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven {
        name = "BlameJared"
        setUrl("https://maven.blamejared.com")
    }
    maven {
        name = "CurseMaven"
        setUrl("https://www.cursemaven.com")
    }
    mavenCentral()
}

dependencies {
    implementation("mezz.jei:jei_1.12.2:4.16.1.1013:api")
    runtimeOnly("mezz.jei:jei_1.12.2:4.16.1.1013")
}

minecraft {
    mcVersion.set(propMcVersion)
    mcpMappingChannel.set("stable")
    mcpMappingVersion.set(propMappingsVersion)

    injectedTags.set(mapOf("VERSION" to project.version))
}

tasks.injectTags.configure {
    outputClassName.set("com.chestproxy.Tags")
}

tasks.processResources {
    filesMatching("mcmod.info") {
        expand(mapOf("version" to project.version, "minecraftVersion" to propMcVersion))
    }
}
