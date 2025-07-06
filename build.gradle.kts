import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta6"
    id("maven-publish")
}

val git : String = versionBanner()
val builder : String = builder()
ext["git_version"] = git
ext["builder"] = builder
// 配置 Shadow 插件
configure<com.github.jengelman.gradle.plugins.shadow.ShadowExtension> {
    relocate("com.google.gson", "shadowed.com.google.gson")
    relocate("org.apache", "shadowed.org.apache")
}

// 注册 sourcesJar 任务
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// 正确配置 Shadow Jar 任务
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    minimize()
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    from(project.file("src/main/resources"))
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest {
        attributes(
            "Main-Class" to "net.momirealms.customfishing.CustomFishing",
            "Implementation-Version" to project.version
        )
    }
}
subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenCentral()
    }

    tasks.processResources {
        filteringCharset = "UTF-8"

        filesMatching(arrayListOf("custom-fishing.properties")) {
            expand(rootProject.properties)
        }

        filesMatching(arrayListOf("*.yml", "*/*.yml")) {
            expand(
                Pair("project_version", rootProject.properties["project_version"]),
                Pair("config_version", rootProject.properties["config_version"])
            )
        }
    }
}

fun versionBanner(): String {
    val os = ByteArrayOutputStream()
    try {
        project.exec {
            commandLine = "git rev-parse --short=8 HEAD".split(" ")
            standardOutput = os
        }
    } catch (e: ExecException) {
        return "Unknown"
    }
    return String(os.toByteArray()).trim()
}

fun builder(): String {
    val os = ByteArrayOutputStream()
    try {
        project.exec {
            commandLine = "git config user.name".split(" ")
            standardOutput = os
        }
    } catch (e: ExecException) {
        return "Unknown"
    }
    return String(os.toByteArray()).trim()
}
publishing {
    repositories {
        mavenLocal()
        }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "net.momirealms"
            artifactId = "custom-fishing"
            version = rootProject.properties["project_version"].toString()
            artifact(tasks["Jar"]) 
            }
            pom {
                name = "CustomFishing"
                url = "https://github.com/Xiao-MoMi/Custom-Fishing"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}
// 修复任务依赖声明
tasks.named("publishMavenJavaPublicationToMavenLocal") {
    dependsOn(tasks.named("shadowJar"))
    dependsOn(tasks.named("sourcesJar"))
}

// 修复任务名称大小写问题
tasks.named("jar") {
    enabled = false
}

// 确保编译任务在资源处理之前运行
tasks.named("processResources") {
    dependsOn(tasks.named("classes"))
}


