[![Build Status](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle.svg?token=icxd1yE9Nf6WLivZz2vF&branch=master)](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle)

# Ice Builder for Gradle

The Ice Builder for Gradle provides a gradle plugin to manage the compilation
of [Slice](https://doc.zeroc.com/display/Ice/The+Slice+Language) files to
Java. You can configure the builder to compile your Slice files with [`slice2java`](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options) and/or [`slice2freezej`](https://doc.zeroc.com/display/Ice/Using+a+Freeze+Map+in+Java).

## Contents

- [Build Instructions](#build-instructions)
- [Using the Gradle Plugin](#using-the-gradle-plugin)
  - [Gradle Task](#gradle-task)
  - [Project Layout](#project-layout)
  - [Convention Properties](#convention-properties)
  - [Configuring Slice-to-Java Projects](#configuring-slice-to-java-projects)
    - [java Properties](#java-properties)
    - [java Examples](#java-examples)
  - [Configuring Slice-to-FreezeJ Projects](#configuring-slice-to-freezej-projects)
    - [freezej Properties](#freezej-properties)
    - [dict Source Set](#dict-source-set)
      - [dict Properties](#dict-properties)
      - [dict Examples](#dict-examples)
    - [dict Source Set](#dict-source-set)
      - [index Properties](#index-properties)
      - [index Example](#index-example)

## Build Instructions

To build the plugin run:

```shell
$ ./gradlew build
```

## Using the Gradle Plugin

To use the plugin, include the following in your build script:

```gradle
buildscript {
    repositories {
        maven {
            url 'https://repo.zeroc.com/nexus/content/repositories/releases'
        }
    }
    dependencies {
        classpath group: 'com.zeroc.gradle', name: 'ice-builder', version: '1.0.0'
    }
}
apply plugin: 'ice-builder'
```

It is important that the `ice-builder` plugin is applied after the `java` plugin in
order for task dependencies to be properly setup.

### Gradle Task

The Ice Builder plugin adds a task to your project, as shown below:

| Task name    | Type      | Description                             |
| ------------ | --------- | --------------------------------------- |
| compileSlice | SliceTask | Generates Java source from Slice files. |

The Ice Builder plugin adds the following dependency to tasks added by the Java
plugin:

| Task name   | Depends On   |
| ----------- | ------------ |
| compileJava | compileSlice |

In addition, it adds the following dependency to tasks added by
the Android plugin:

| Task name | Depends On   |
| --------- | ------------ |
| preBuild  | compileSlice |

### Project Layout

The Ice Builder plugin assumes the following project layout:

| Directory      | Meaning                                     |
| -------------- | ------------------------------------------- |
| src/main/slice | Location of your project's Slice files.     |

This default layout can be changed with the property `srcDir`, described below.

### Convention Properties

The Ice Builder plugin defines the following convention properties:

| Property name | Type   | Default value          | Description                                   |
| ------------- | ------ | ---------------------- | --------------------------------------------- |
| iceHome       | String | (see below)            | The location of the Ice installation.         |
| output        | File   | buildDir/generated-src | The location to place generated source files. |

If `iceHome` is not set, the builder will check the `ICE_HOME` environment
variable to determine the location of the Ice installation. If `ICE_HOME` is
not set either, the builder uses the following defaults on Linux and OS X:

| OS         | Default Ice Installation Directory     |
| ---------- | -------------------------------------- |
| Linux      | /usr                                   |
| OS X       | /usr/local                             |

On Windows, or when Ice is installed in a non-standard location, you need to set
`iceHome` or `ICE_HOME`.

You can set `iceHome` in your build script as shown below:

```gradle
slice.iceHome = '/opt/Ice'
```

### Configuring Slice-to-Java Projects

Use the `java` sub-section to configure the compilation of Slice files with `slice2java`.
The `java` sub-section can contain one or more source sets, each with its own set of flags 
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

#### `java` Properties

Each source set in the `java` sub-section defines the following convention properties:

| Property name | Type           | Default value  | Description                                             |
| ------------- | -------------- | :------------: | ------------------------------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.                        |
| args          | String         | -              | The arguments to slice2java                             |
| files         | FileCollection | -              | The Slice files in this source set. Overrides `srcDir`. |
| include       | Set<File>      | -              | Slice include file search path.                         |

Refer to [slice2java Command-Line Options](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options)
for a description of the options you can provide through the `args` property.

Note: the location of the Ice Slice files is automatically added to `include` by the Ice Builder.

#### `java` Examples

Build all the Slice files in `src/main/slice` with the --tie argument:

```gradle
slice {
  java {
     args = "--tie"
  }
}
```

Build `a.ice` with the argument --stream, and all Slice files in `b` with all given
include directories:

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
       files = filetree(dir: "b", includes: ['**.ice'])
     }
  }
}
```

### Configuring Slice-to-FreezeJ Projects

Use the `freezej` sub-section to compile Slice files with `slice2freezej`. 
`freezej` contains source sets for dictionaries and indices.

#### `freezej` Properties

Each source set defines the following convention properties:

| Property name | Type           | Default value  | Description                                             |
| ------------- | -------------- | :------------: | ------------------------------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.                        |
| args          | String         | -              | The arguments to slice2freezej.                         |
| files         | FileCollection | -              | The Slice files in this source set. Overrides `srcDir`. |
| include       | Set<File>      | -              | Slice include file search path.                         |

Refer to [slice2freezej Command-Line Options](https://doc.zeroc.com/display/Ice/slice2freezej+Command-Line+Options)
for a description of the options you can provide through the `args` property.

Note: the location of the Ice Slice files is automatically added to `include` by the Ice Builder.

#### `dict` Source Set

A `dict` source set describes one ore more Freeze dictionaries generated by `slice2freezej`. 
Each dictionary in such a source set must have a unique name.

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

| Property name | Type                        | Default value | Description                                |
| ------------- | --------------------------- | :-----------: | ------------------------------------------ |
| javaType      | String                      | -             | The name of the generated Java type.       |
| key           | String                      | -             | The Slice type of the key.                 |
| value         | String                      | -             | The Slice type of the value.               |
| index         | List\<Map\<String,String>>  | -             | A list of dictionary values used for keys. |

##### dict Examples

Given the following Slice definitions in Test.ice:

```
// Slice
module Test {
struct Foo
{
    string s;
    Struct1 s1;
};
};
```

Generate a Freeze dictionary `StringFooMap` that maps a `string` to the Slice type `Test::Foo`:

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

Generate the same dictionary, but this time with an index on the data member `s` of the `Foo` structure:

```gradle
slice {
  freezej {
     files = [file("Test.ice")]
     dict {
       IntIntDict {
          javaType = "Test.StringFooMap"
          key = "string"
          value = "Test::Foo"
          index = [["member" : "s"]]
          // Example: case insensitive
          // index = [["member" : "s", case: 'false']]
          // Example: two indices.
          // index = [["member" : "s"], ['member': 's1']
       }
     }
  }
}
```

#### `index` Source Set

An `index` source set describes one ore more Freeze Evictor indices generated by `slice2freezej`.
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

Given the following Slice type defined in `Test.ice`:

```
// Slice
module Test {
struct Foo
{
    string s;
    Struct1 s1;
};
};
```

This generates a Freeze Evictor index `Test.SIndex` for the data member `s`:

```gradle
freezej {
  files = [file("Test.ice")]
  index {
    NameIndex {
      javaType = "Test.SIndex"
      type = "Test::Foo"
      member = "s"
      caseSensitive = false
    }
  }
}
```
