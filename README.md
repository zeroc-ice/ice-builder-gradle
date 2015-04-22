[![Build Status](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle.svg?token=icxd1yE9Nf6WLivZz2vF&branch=master)](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle)

- [Ice Builder for Gradle](#ice-builder-for-gradle)
- [Build Instructions](#build-instructions)
- [Using the Gradle Plugin](#using-the-gradle-plugin)
  - [Gradle Task](#Gradle Task)
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

# Ice Builder for Gradle

The Ice Builder for Gradle provides a gradle plugin to manage compilation
of [Slice](https://doc.zeroc.com/display/Ice/The+Slice+Language) files to
Java.

# Build Instructions

To build the plugin run:

```shell
$ ./gradlew build
```

# Using the Gradle Plugin

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

## Gradle Task

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

## Project Layout

The Ice Builder plugin assumes the project layout shown below.

| Directory      | Meaning                             |
| -------------- | ----------------------------------- |
| src/main/slice | Location of production Slice files. |

The default layout can be changed using the source set `srcDir` property
documented in proceeding sections.

## Convention Properties

The Ice Builder plugin defines the following convention properties:

| Property name | Type   | Default value          | Description                                   |
| ------------- | ------ | ---------------------- | --------------------------------------------- |
| iceHome       | String | (see below)            | The location of the Ice installation.         |
| output        | File   | buildDir/generated-src | The location to place generated source files. |

If `iceHome` is not set, the plugin will check the `ICE_HOME` environment
variable to determine the location of the Ice installation. If `ICE_HOME` is
also not set, on Linux or OS X it will look in the default install location of
the Ice binary distribution.

| OS         | Default Ice Install Directory          |
| ---------- | -------------------------------------- |
| Linux      | /usr                                   |
| OS X       | /usr/local                             |

On Windows or if Ice is installed in a non-standard location, then either
`iceHome` or `ICE_HOME` must be set.

To set `iceHome` in your build script you would do the following:

```gradle
slice.iceHome = '/opt/Ice'
```

## Configuring Slice-to-Java Projects

The sub-section java is used to configure files compiled with slice2java.
Contained within the java sub-section are a number of source sets, each
representing a set of common flags for Slice files. If there is only a single
source set, the source set name can be omitted.

```gradle
slice {
  java {
     ...
  }
}
```

Otherwise the source sets must have unique names.

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

### java Properties

Each source set defines the following convention properties:

| Property name | Type           | Default value  | Description                         |
| ------------- | -------------- | -------------- | ----------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.    |
| args          | String         | -              | The arguments to slice2java         |
| files         | FileCollection | -              | The Slice files in this source set. |
| include       | Set<File>      | -              | Slice include file search path.     |

For more information on the `args` that can be set see the [slice2java Command-Line Options](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options) documentation.

Also note that the location of the Ice Slice files is automatically added to `include` by the plugin.

### java Examples

Build all the Slice files contained in src/main/slice with the --tie argument:

```gradle
slice {
  java {
     args = "--tie"
  }
}
```

Build a.ice with the argument --stream, and all Slice files in b with all given
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

## Configuring Slice-to-FreezeJ Projects

The sub-section freezej is used to configure files compiled with slice2freezej.
Contained within the freezej sub-section is a source-set for dictionaries and a
source-set for indices.

### freezej Properties

Each source set defines the following convention properties:

| Property name | Type           | Default value  | Description                         |
| ------------- | -------------- | -------------- | ----------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.    |
| args          | String         | -              | The arguments to slice2freezej.     |
| files         | FileCollection | -              | The Slice files in this source set. |
| include       | Set<File>      | -              | Slice include file search path.     |

For more information on the `args` that can be set see the [slice2freezej Command-Line Options](https://doc.zeroc.com/display/Ice36/slice2freezej+Command-Line+Options) documentation.

Also note that the location of the Ice Slice files is automatically added to `include` by the plugin.

### dict Source Set

The dict source-set specifies the set of Freeze dictionary data types to
generate.

Each dictionary defined within the source set must have a unique name.

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

#### dict Properties

Each dictionary defines the following convention properties:

| Property name | Type                        | Default value | Description                                |
| ------------- | --------------------------- | ------------- | ------------------------------------------ |
| javaType      | String                      | -             | The name of the generated Java type.       |
| key           | String                      | -             | The Slice type of the key.                 |
| value         | String                      | -             | The Slice type of the value.               |
| index         | List\<Map\<String, String>> | -             | A list of dictionary values used for keys. |

#### dict Examples

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

Generate a dictionary mapping a string to the slice type Foo:

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

Generate the same dictionary, but this time with an index on the members:

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

### index Source Set

The index source-set specifies the set of Freeze evictor index types to
generate.

Each index defined within the source set must have a unique name.

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

#### index Properties

Each index defines the following convention properties:

| Property name | Type    | Default value | Description |
| ------------- | ------- | ------------- | ----------- |
| javaType      | String  | -             | The name of the generated Java type. |
| type          | String  | -             | The Slice type of the type to be indexed. |
| member        | String  | -             | The name of the data member in the type to index. |
| caseSensitive | boolean | true          | If the member is a string, this specifies whether the comparison is case sensitive. |

#### index Example

Given the following Slice types in Test.ice:

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

This generates an index called Test.SIndex on the member s:

```gradle
freezej {
  files = [file("Test.ice")]
  index {
    NameIndex {
      javaType = "Test.SIndex"
      type = "Test::Foo"
      member = "s"
      casesensitive = false
    }
  }
}
```
