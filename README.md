[![Build Status](https://travis-ci.org/zeroc-ice/ice-builder-gradle.svg)](https://travis-ci.org/zeroc-ice/ice-builder-gradle)

# Ice Builder for Gradle

The Ice Builder for Gradle provides a Gradle plug-in named `slice`. This plug-in manages the compilation
of [Slice](https://doc.zeroc.com/display/Ice/The+Slice+Language) files to Java. It compiles your Slice files with [`slice2java`](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options), and it is also capable of generating Freeze maps and indices with [`slice2freezej`](https://doc.zeroc.com/display/Ice/Using+a+Freeze+Map+in+Java).

An [Ice](https://github.com/zeroc-ice/ice) installation with `slice2java` and `slice2freezej` version 3.5.1 or higher is required.

## Contents

- [Build Instructions](#build-instructions)
- [Using the `slice` Plug-In](#using-the-slice-plug-in)
  - [Gradle Task](#gradle-task)
  - [Project Layout](#project-layout)
  - [Convention Properties](#convention-properties)
  - [Configuring Slice-to-Java Projects](#configuring-slice-to-java-projects)
    - [java Properties](#java-properties)
    - [java Examples](#java-examples)
  - [Configuring Slice-to-FreezeJ Projects](#configuring-slice-to-freezej-projects)
    - [freezej Properties](#freezej-properties)
    - [dict Block](#dict-block)
      - [dict Properties](#dict-properties)
      - [dict Examples](#dict-examples)
    - [index Block](#index-block)
      - [index Properties](#index-properties)
      - [index Example](#index-example)
- [When does the Plug-In Recompile Slice Files?](#when-does-the-plug-in-recompile-slice-files)

## Build Instructions

To build the `slice` plug-in run:

```shell
./gradlew build
```

## Using the `slice` Plug-in

Build script snippet for use in all Gradle versions:
```gradle
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "gradle.plugin.com.zeroc.gradle.ice-builder:slice:1.4.2"
    }
}

apply plugin: "com.zeroc.gradle.ice-builder.slice"
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```
plugins {
    id "com.zeroc.gradle.ice-builder.slice" version "1.4.2"
}
```

It is important that the `slice` plug-in is applied after the `java` plug-in in
order for task dependencies to be properly setup.

### Gradle Task

The `slice` plug-in adds a task to your project, as shown below:

| Task name    | Type      | Description                             |
| ------------ | --------- | --------------------------------------- |
| compileSlice | SliceTask | Generates Java source from Slice files. |

The plug-in adds the following dependency to tasks added by the `java` plug-in:

| Task name   | Depends On   |
| ----------- | ------------ |
| compileJava | compileSlice |

On Android, it adds the following dependency to each build variant of `project.android.libraryVariants`,
`project.android.applicationVariants`, and `project.android.testVariants`:

| Task name    | Depends On   |
| ------------ | ------------ |
| javaCompiler | compileSlice |

### Project Layout

The plug-in assumes the following project layout:

| Directory      | Meaning                                     |
| -------------- | ------------------------------------------- |
| src/main/slice | Location of your project's Slice files.     |

This default layout can be changed with the property `srcDir`, described below.

### Convention Properties

The `slice` plug-in defines the following convention properties:

| Property name     | Type    | Default value                                    | Description                                                                                                                                                                                                          |
| ----------------- | ------- | ------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| iceHome           | String  | (see below)                                      | The root directory of the Ice installation.                                                                                                                                                                          |
| freezeHome        | String  | _iceHome_                                        | The root directory of the Freeze installation.                                                                                                                                                                       |
| output            | File    | _buildDir_/generated-src                         | The directory that contains the generated source files.                                                                                                                                                              |
| iceVersion        | String  | Ice version                                      | The Ice version returned by slice2java -v (read only)                                                                                                                                                                |
| iceArtifactVersion| String  | Ice jar artifact version                         | The Ice jar artifact version version. This is often the same as `iceVersion`                                                                                                                                         |
| srcDist           | Boolean | (platform dependent)                             | True when using a source distribution, false otherwise (read only)                                                                                                                                                   |
| sliceDir          | String  | (platform dependent)                             | The Ice Slice installation directory (read only)                                                                                                                                                                     |
| jarDir            | String  | (platform dependent)                             | The Ice JARs installation directory (read only)                                                                                                                                                                      |
| slice2java        | String  | (platform dependent)                             | Full path of the slice2java compiler (read only)                                                                                                                                                                     |
| slice2freezej     | String  | (platform dependent)                             | Full path of the slice2freezej compiler (read only)                                                                                                                                                                  |
| cppPlatform       | String  | CPP\_PLATFORM env variable, if set               | On Windows, when _srcDist_ is `true` and _iceVersion_ >= 3.7, the plug-in finds slice2java and slice2freezej in _iceHome_\bin\\_cppPlatform_\\_cppConfiguration_. _cppPlatform_ can be `Win32` or `x64`.             |
| cppConfiguration  | String  | CPP\_CONFIGURATION env variable, if set          | On Windows, when _srcDist_ is `true` and _iceVersion_ >= 3.7, the plug-in finds slice2java and slice2freezej in in _iceHome_\bin\\_cppPlatform_\\_cppConfiguration_. _cppConfiguration_ can be `Debug` or `Release`. |
| compat            | Boolean | `false` if _iceVersion_ >= 3.7, otherwise `null` | When _iceVersion_ >= 3.7, adds `--compat` to the _slice2java_ arguments.                                                                                                                                             |

If `iceHome` is not set, the plug-in will check the `ICE_HOME` environment
variable to determine the location of the Ice installation. If `ICE_HOME` is
not set either, the plug-in uses the following defaults on Linux and OS X:

| OS         | Default Ice Installation Directory     |
| ---------- | -------------------------------------- |
| Linux      | /usr                                   |
| macOS      | /usr/local                             |

On Windows, when neither `iceHome` nor `ICE_HOME` are set, the builder queries the Windows
registry to find Ice installations and selects the newest version.

You can set `iceHome` in your build script as shown below:

```gradle
slice.iceHome = '/opt/Ice'
```

### Slice Plugin Methods

| Method Name       | Arugment(s) | Description                                                                                                                                                             |
| ----------------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| compareIceVersion | `String`    | Compares _iceVersion_ with given string. Returns `-1`, `0`, or `1`, depending on whether _iceVersion_ is respectfuly less than, equal to, or greater than given string. |

### Configuring Slice-to-Java Projects

Use the `java` block to configure the compilation of Slice files with `slice2java`.
The `java` block can contain one or more source sets, each with its own set of flags
for compiling Slice files. You can omit the source set name when you have a single set:

```gradle
slice {
    java {
        ...
    }
}
```

Otherwise, the source sets must have unique names, for example:

```gradle
slice {
    java {
        set1 {
            ...
        }
        set2 {
            ...
        }
    }
}
```
Each source set triggers a separate execution of the `slice2java` compiler.

#### `java` Properties

Each source set in the `java` block defines the following convention properties:

| Property name | Type           | Default value  | Description                                             |
| ------------- | -------------- | :------------: | ------------------------------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.                        |
| args          | String         | -              | The arguments to slice2java                             |
| files         | FileCollection | -              | The Slice files in this source set. Overrides `srcDir`. |
| include       | Set<File>      | -              | Slice include file search path.                         |

Refer to the [slice2java Command-Line Options](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options)
for a description of the options you can provide through the `args` property.

Note: the `slice` directory of your Ice installation (`${slice.iceHome}/slice`) is automatically added to `include` by the plug-in.

#### `java` Examples

Compile all Slice files in `src/main/slice` with the `--tie` argument:

```gradle
slice {
    java {
        args = "--tie"
    }
}
```

Compile `a.ice` with the argument `--stream`, and compile all Slice files in `b` without `--stream`. Both compilations add `${projectDir}` to the Slice include search path:

```gradle
slice {
    java {
        set1 {
            include = ["${projectDir}"]
            args = "--stream"
            files = [file("a.ice")]
        }
        set2 {
            include = ["${projectDir}"]
            files = fileTree(dir: "b", includes: ['**.ice'])
        }
    }
}
```

### Configuring Slice-to-FreezeJ Projects

Use the `freezej` block to generate Freeze maps and indices with `slice2freezej`.

The plug-in currently supports a single unnamed source set within `freezej`:
```
slice {
  freezej {
      srcDir = "src/main/slice"
      ...
  }
}
```

#### `freezej` Properties

Each `freezej` block defines the following convention properties:

| Property name | Type           | Default value  | Description                                             |
| ------------- | -------------- | :------------: | ------------------------------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.                        |
| args          | String         | -              | The arguments to `slice2freezej`.                       |
| files         | FileCollection | -              | The Slice files in this source set. Overrides `srcDir`. |
| include       | Set<File>      | -              | Slice include file search path.                         |

Refer to the [slice2freezej Command-Line Options](https://doc.zeroc.com/display/Ice/slice2freezej+Command-Line+Options)
for a description of the options you can provide through the `args` property.

Note: the `slice` directory of your Ice installation (`${slice.iceHome}/slice`) is automatically added to `include` by the plug-in.

#### `dict` Block

A `dict` block describes one ore more Freeze maps (also known as dictionaries) generated by `slice2freezej`.
Each map in such a block must have a unique name.

```gradle
slice {
    freezej {
        dict {
            Dict1 {
                ...
            }
            Dict2 {
                ...
            }
        }
    }
}
```

##### `dict` Properties

`dict` defines the following convention properties:

| Property name | Type           | Default value | Description                                |
| ------------- | -------------- | :-----------: | ------------------------------------------ |
| javaType      | String         | -             | The name of the generated Java type.       |
| key           | String         | -             | The Slice type of the key.                 |
| value         | String         | -             | The Slice type of the value.               |
| index         | List\<Map\<>>  | -             | Secondary index or indices.                |

The `index` is a list of maps. Valid entries in each map are as follows:

| Key (String)  | Value Type    | Default value | Description                                                                     |
| ------------- | ------------- | :-----------: | ------------------------------------------------------------------------------- |
| member        | String        | -             | The name of a data member in the Freeze map value type.                         |
| caseSensitive | boolean       | true          | If member is a string, this specifies whether the comparison is case sensitive. |

##### dict Examples

Given the following Slice definitions in `Test.ice`:

```
// Slice
module Test
{

struct Foo
{
    string s;
    Struct1 s1;
}

}
```

Generate a Freeze map named `StringFooMap` that maps a `string` to the Slice type `Test::Foo`:

```gradle
slice {
    freezej {
        files = [file("Test.ice")]
        dict {
            StringFooMap {
                javaType = "Test.StringFooMap"
                key = "string"
                value = "Test::Foo"
            }
        }
    }
}
```

Generate the same Freeze map, but this time with an index on the data member `s` of the `Foo` structure:

```gradle
slice {
    freezej {
        files = [file("Test.ice")]
        dict {
            StringFooMap {
                javaType = "Test.StringFooMap"
                key = "string"
                value = "Test::Foo"
                index = [["member" : "s"]]
                // Example: case insensitive
                // index = [["member" : "s", "caseSensitive": false]]
                // Example: two indices.
                // index = [["member" : "s"], ['member': 's1']]
            }
        }
    }
}
```

Generate an `int` to `string` map, and create an index on the `string` value:

```gradle
slice {
    freezej {
        dict {
            IntToStringMap {
                javaType = "Test.IntToStringMap"
                key = "int"
                value = "string"
                index = [[]] // list with one empty map to create an index on the full value
                // Same index but case insensitive:
                // index = [["caseSensitive": false]]
            }
        }
    }
}
```
#### `index` Block

An `index` block describes one ore more Freeze Evictor indices generated by `slice2freezej`.
Each index must have a unique name.

```gradle
slice {
    freezej {
        index {
            Index1 {
                ...
            }
            Index2 {
                ...
            }
        }
    }
}
```

##### `index` Properties

`index` defines the following convention properties:

| Property name | Type    | Default value | Description                                                                         |
| ------------- | ------- | :-----------: | ----------------------------------------------------------------------------------- |
| javaType      | String  | -             | The name of the generated Java type.                                                |
| type          | String  | -             | The Slice type of the type to be indexed.                                           |
| member        | String  | -             | The name of the data member in the type to index.                                   |
| caseSensitive | boolean | true          | If the member is a string, this specifies whether the comparison is case sensitive. |

##### index Example

Given the following Slice type defined in `Phonebook.ice`:

```
// Slice
module Demo
{

class Contact
{
    string name;
    string address;
    string phone;
}

}
```

Generate a Freeze Evictor index `NameIndex` for the data member `name`:

```gradle
freezej {
    files = [file("PhoneBook.ice")]
    index {
        NameIndex {
            javaType = "NameIndex"
            type = "Demo::Contact"
            member = "name"
            caseSensitive = false
        }
    }
}
```

## When does the Plug-in Recompile Slice Files?

Slice files will be recompiled if either of the following are true:
 * This Slice file or a Slice file included directly or indirectly by this Slice file was updated after the last compilation of the Slice file through the plug-in.
 * The options used to compile this Slice file have changed.

Removing a Slice file from a source set will trigger the removal of the corresponding generated `.java` files the next time the source set is built.
