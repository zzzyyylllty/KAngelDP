# KAngelDungeon

# WORK ON PROGRESS...

[Documentation](docs)

超天地牢副本管理系统服务 (Liminal Skyline v4.0 服务)

ChoTen Dungeon management system service (Liminal Skyline v4.0 Service)

## As dependency

```Gradle kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.zzzyyylllty:KAngelDP:VERSION")
}
```

## Build Runtime Version

Required Java 21.

Runtime version for normal use.

Build artifact is in `plugin/build/libs` folder.

```
./gradlew clean build
```

## Build Api Version

The api version includes the TabooLib core, intended for developers' use but not runnable.

```
./gradlew clean taboolibBuildApi -PDeleteCode
```

> The parameter `-PDeleteCode` indicates the removal of all logic code to reduce size.