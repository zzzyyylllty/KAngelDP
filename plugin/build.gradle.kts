taboolib {
    description {
        name("KAngelDungeon")
        desc("ChoTen RPG Plugin.")
        contributors {
            // 作者名称
            name("AkaCandyKAngel")
        }
        dependencies {
            name("MythicMobs").optional(true)
            name("Chemdah").optional(true)
            name("ItemsAdder").optional(true)
            name("Oxaren").optional(true)
            name("Nexo").optional(true)
            name("CraftEngine").optional(true)
            name("PlaceholderAPI").optional(true)
            // name("ProtocolLib").optional(true)
            // 可选依赖.
            // name("XXX").optional(true)
        }
    }


    // Relocate 必须与META-INF/dependencies里面的relocate一致。
    // 同时，还需要修改io.github.zzzyyylllty.kangeldungeon.util.kangeldungeonLocalDependencyHelper.replaceTestTexts
    relocate("top.maplex.arim","io.github.zzzyyylllty.kangeldungeon.library.arim")
    relocate("ink.ptms.um","io.github.zzzyyylllty.kangeldungeon.library.um")
    // relocate("com.google", "io.github.zzzyyylllty.kangeldungeon.library.google")
    relocate("com.alibaba", "io.github.zzzyyylllty.kangeldungeon.library.alibaba")
    relocate("kotlinx.serialization", "kotlinx.serialization170")
    // relocate("de.tr7zw.changeme.nbtapi","io.github.zzzyyylllty.kangeldungeon.library.nbtapi")
    relocate("io.github.projectunified.uniitem","io.github.zzzyyylllty.kangeldungeon.library.uniitem")
    relocate("com.fasterxml.jackson","io.github.zzzyyylllty.kangeldungeon.library.jackson")
    relocate("com.mojang.datafixers","io.github.zzzyyylllty.kangeldungeon.library.datafixers")
    relocate("io.netty.handler.codec.http", "io.github.zzzyyylllty.kangeldungeon.library.http")
    relocate("io.netty.handler.codec.rtsp", "io.github.zzzyyylllty.kangeldungeon.library.rtsp")
    relocate("io.netty.handler.codec.spdy", "io.github.zzzyyylllty.kangeldungeon.library.spdy")
    relocate("io.netty.handler.codec.http2", "io.github.zzzyyylllty.kangeldungeon.library.http2")
    relocate("org.tabooproject.fluxon","io.github.zzzyyylllty.kangeldungeon.library.fluxon")
    relocate("com.github.benmanes.caffeine","io.github.zzzyyylllty.kangeldungeon.library.caffeine")
    relocate("org.kotlincrypto","io.github.zzzyyylllty.kangeldungeon.library.kotlincrypto")
//    relocate("com.oracle.truffle","io.github.zzzyyylllty.kangeldungeon.library.truffle")
//    relocate("org.graalvm.polyglot","io.github.zzzyyylllty.kangeldungeon.library.polyglot")
}

//tasks {
//    jar {
//        archiveFileName.set("${rootProject.name}-${archiveFileName.get().substringAfter('-')}")
//        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
//    }
//}

tasks {

    val taboolibMainTask = named("taboolibMainTask")

    val baseJarFile = layout.buildDirectory.file("libs/${rootProject.name}-${rootProject.version}-Premium.jar")

    val freeJar by registering(Jar::class) {
        group = "build"
        description = "Generate FREE version jar by filtering premium classes"

        dependsOn(taboolibMainTask)

        archiveFileName.set("${rootProject.name}-${version}-Free.jar")

        // 从taboolibMainTask产物复制并过滤premium包
        from(zipTree(baseJarFile)) {
            exclude("io/github/zzzyyylllty/kangeldungeon/premium/*")
        }
    }

    named("build") {
        dependsOn(freeJar)
    }


    jar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}-Premium.jar")
        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
    }
}
