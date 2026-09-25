# DBuild

Gradle plugin for building Minecraft mods for multiple loaders from a single project.

DBuild is designed for projects that support Fabric, Forge, NeoForge or Quilt, but don't want to maintain a separate Gradle project for every loader.

The same Java sources can be shared between loaders, while loader-specific code and resources are kept in their own source sets.

*If you dont want read all ts check [template](https://github.com/Akfz/DBuild-TEMPLATE)*

## Supported loaders

* Fabric
* Forge
* NeoForge
* Quilt

## Installation

Add DBuild to your `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'io.github.Akfz.dbg' version '1.0.1'

    id 'fabric-loom' apply false
    id 'net.minecraftforge.gradle' apply false
    id 'net.neoforged.moddev' apply false
}
```

The loader plugins are declared with `apply false`. DBuild applies the selected loader when the project is configured for it.

## Basic setup

A simple configuration looks like this:

```groovy
dbuild {
    defaultLoader = 'fabric'

    useLoaderSourceSet()
    filterForeignMetadata()
    dbuildannotations()

    registryCommand 'runClient', 'runServer', 'build'

    fabric {
        devJar()
        obfJar()

        configure {
            apply plugin: 'fabric-loom'

            dependencies {
                minecraft "com.mojang:minecraft:${project.minecraft_version}"
                mappings "net.fabricmc:yarn:${project.minecraft_version}+build.3:v2"
                modImplementation "net.fabricmc:fabric-loader:${project.fabric_loader_version}"
            }
        }
    }

    forge {
        devJar()
        obfJar()

        configure {
            apply plugin: 'net.minecraftforge.gradle'

            dependencies {
                minecraft "net.minecraftforge:forge:${project.forge_version}"
            }
        }
    }
}
```

The `configure` block is passed directly to the selected loader configuration, so you can still use the normal Fabric Loom, ForgeGradle or other loader APIs.

## Selecting a loader

DBuild has a default loader:

```groovy
dbuild {
    defaultLoader = 'fabric'
}
```

The default is mainly useful for IDE imports and normal Gradle configuration.

For a different loader, use:

```text
./gradlew build -Pdbuild.loader=forge
```

For example:

```text
./gradlew runClient -Pdbuild.loader=fabric
./gradlew build -Pdbuild.loader=forge
```

`-Pdbuild.loader` takes priority over `defaultLoader`.

## Source sets

`useLoaderSourceSet()` changes the normal `main` source set into common and loader-specific source sets.

```text
src/main/common/java
src/main/common/resources

src/main/fabric/java
src/main/fabric/resources

src/main/forge/java
src/main/forge/resources

src/main/neoforge/java
src/main/neoforge/resources

src/main/quilt/java
src/main/quilt/resources
```

`common` is always compiled.

The directory for the currently selected loader is compiled in addition to it.

For example, when using Fabric:

```text
src/main/common/java       -> compiled
src/main/fabric/java       -> compiled
src/main/forge/java        -> ignored
src/main/neoforge/java     -> ignored
src/main/quilt/java        -> ignored
```

The loader directories are created automatically for configured loaders.

## Resources

`filterForeignMetadata()` can be used when the project contains metadata files for several loaders.

For example:

```text
fabric.mod.json
mods.toml
neoforge.mods.toml
quilt.mod.json
```

Only the metadata belonging to the active loader is included in the resulting jar.

This allows all loader metadata to live in the same project without having to maintain separate resource directories manually.

## Development and production jars

DBuild provides helpers for creating loader-specific jars:

```groovy
fabric {
    devJar()
    obfJar()
}
```

This creates:

```text
libs/<base>-<version>-fabric-dev.jar
libs/<base>-<version>-fabric.jar
```

The same applies to the other configured loaders.

`devJar()` creates an unobfuscated development jar.

`obfJar()` creates the production jar using the loader's normal remapping/obfuscation process.

## Dependencies

Dependencies can be embedded into the resulting jar with `embed`:

```groovy
dbuild {
    embed 'com.google.code.gson:gson:2.11.0'
}
```

Helpers are applied to all configured loaders when called directly on `dbuild`.

A helper inside a loader block only affects that loader:

```groovy
dbuild {
    embed 'com.google.code.gson:gson:2.11.0'

    fabric {
        devJar()
    }

    forge {
        devJar()
    }
}
```

## Annotations

DBuild can remove classes from a particular build using annotations.

Add the annotation library as a compile-only dependency:

```groovy
dependencies {
    compileOnly 'v.akfz:DBuildAnnotations:1.0.0'
}
```

Available annotations include:

```java
@DevOnly
@ProdOnly
@OnlyLoader(OnlyLoader.Loader.FABRIC)
```

For example:

```java
@DevOnly
public class DevelopmentHelper {
}
```

This class will be available in the development jar but won't be included in the production jar.

```java
@OnlyLoader(OnlyLoader.Loader.FABRIC)
public class FabricHelper {
}
```

This class is only compiled when Fabric is the active loader.

The annotations are processed while building the project. They don't add a runtime dependency and don't modify the compiled bytecode.

## Loader runs

Loader-specific run configurations can be created inside `configure`.

For Fabric, for example:

```groovy
fabric {
    configure {
        apply plugin: 'fabric-loom'

        loom {
            runs {
                client {
                    client()
                    runDir 'run'
                }

                server {
                    server()
                    runDir 'run-server'
                }
            }
        }
    }
}
```

DBuild does not replace the loader's run configuration. It only makes it possible to configure it from the corresponding loader block.

## Command registry

`registryCommand` creates tasks that run another Gradle invocation with the selected loader.

```groovy
dbuild {
    registryCommand 'runClient', 'runServer', 'build'
}
```

With Fabric and Forge configured, this can create tasks such as:

```text
fabricRunClient
fabricRunServer
fabricBuild

forgeRunClient
forgeRunServer
forgeBuild
```

Running:

```text
./gradlew forgeRunClient
```

is equivalent to starting:

```text
./gradlew runClient -Pdbuild.loader=forge
```

The wrapper runs the command in a separate Gradle process.

Loader-specific commands can also be registered:

```groovy
dbuild {
    fabric {
        registryCommand 'runServer'
    }
}
```

## Properties

The following properties are used by DBuild:

| Property             | Description                                          |
| -------------------- | ---------------------------------------------------- |
| `dbuild.loader`      | Loader used for the current Gradle invocation        |
| `mod_id`             | Mod ID                                               |
| `mod_version`        | Mod version                                          |
| `archives_base_name` | Base name used for generated jars                    |
| `defaultLoader`      | Default loader when `dbuild.loader` is not specified |

The mod properties can either be placed in `gradle.properties`:

```properties
mod_id=mymod
mod_version=1.0.0
archives_base_name=mymod
```

or configured directly:

```groovy
dbuild {
    modId = 'mymod'
    modVersion = '1.0.0'
    archivesBaseName = 'mymod'
}
```

If a required property is missing, DBuild reports it during configuration.

## Helpers

Most DBuild functionality is implemented as helpers.

For example:

```groovy
dbuild {
    useLoaderSourceSet()
    filterForeignMetadata()
    dbuildannotations()

    dbuild '1.0-SNAPSHOT'

    embed 'com.google.code.gson:gson:2.11.0'

    devJar()
    obfJar()
}
```

Helpers can be provided by DBuild itself or by another jar.

A custom helper implements:

```java
public class MyHelper implements Helper {

    @Override
    public String name() {
        return "myHelper";
    }

    @Override
    public Set<String> loaders() {
        return Set.of("fabric");
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void apply(LoaderContext ctx) {
        Project project = ctx.project();

        // ...
    }
}
```

Register the helper by adding its class name to:

```text
META-INF/dbuildmf
```

For example:

```text
com.example.MyHelper
```

This makes it possible to keep project-specific DBuild helpers in a separate jar instead of modifying DBuild itself.

## Tasks

Some of the tasks created by DBuild are:

```text
build
fabricDevJar
forgeDevJar
fabricRunClient
forgeRunClient
fabricRunServer
forgeRunServer
fabricBuild
forgeBuild
```

The exact tasks depend on the configured loaders and registered commands.

## Example project

A complete example project is available here:

[DBuild Template](https://github.com/Akfz/DBuild-TEMPLATE)

## JNIHelper

DBuild can also be used together with [JNIHelper](https://github.com/Akfz/JNIHelper) for projects that need native libraries.

JNIHelper is a separate Gradle plugin that handles JNI headers, native library packaging and preparing native libraries for local runs.

This is useful for Minecraft mods that have native code which needs to be built for different platforms.

## Why DBuild?

The main reason for DBuild is keeping a multiloader mod in one Gradle project.

Instead of:

```text
project-fabric/
project-forge/
project-neoforge/
```

the project can look like:

```text
src/main/common/
src/main/fabric/
src/main/forge/
src/main/neoforge/
```

and the loader can be selected when Gradle is started:

```text
./gradlew build -Pdbuild.loader=fabric
./gradlew build -Pdbuild.loader=forge
./gradlew build -Pdbuild.loader=neoforge
```

Loader-specific Gradle configuration remains inside its loader block, while common Java code only needs to be written once.
