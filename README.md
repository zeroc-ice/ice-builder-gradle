[![Build Status](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle.svg?token=icxd1yE9Nf6WLivZz2vF&branch=master)](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle)

# Ice Builder for Gradle

The Ice Builder for Gradle provides a gradle plug-in named `slice`. This plug-in manages the compilation
of [Slice](https://doc.zeroc.com/display/Ice/The+Slice+Language) files to Java. It compiles your Slice files with [`slice2java`](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options), and it is also capable of generating Freeze maps and indices with [`slice2freezej`](https://doc.zeroc.com/display/Ice/Using+a+Freeze+Map+in+Java).

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
    - [dict Source Set](#dict-source-set)
      - [dict Properties](#dict-properties)
      - [dict Examples](#dict-examples)
    - [index Source Set](#index-source-set)
      - [index Properties](#index-properties)
      - [index Example](#index-example)
  - [Dependencies](#dependencies)

## Build Instructions

To build the `slice` plug-in run:

```shell
$ ./gradlew build
```

## Using the `slice` Plug-In

Include the following in your build script:

```gradle
buildscript {
    repositories {
        maven {
            url 'https://repo.zeroc.com/nexus/content/repositories/releases'
        }
    }
    dependencies {
        classpath group: 'com.zeroc.gradle.ice-builder', name: 'slice', version: '1.0.0'
    }
}
apply plugin: 'slice'
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

In addition, it adds the following dependency to tasks added by
the Android plug-in:

| Task name | Depends On   |
| --------- | ------------ |
| preBuild  | compileSlice |

### Project Layout

The plug-in assumes the following project layout:

| Directory      | Meaning                                     |
| -------------- | ------------------------------------------- |
| src/main/slice | Location of your project's Slice files.     |

This default layout can be changed with the property `srcDir`, described below.

### Convention Properties

The plug-in defines the following convention properties:

| Property name | Type   | Default value            | Description                                   |
| ------------- | ------ | ------------------------ | --------------------------------------------- |
| iceHome       | String | (see below)              | The location of the Ice installation.         |
| output        | File   | _buildDir_/generated-src | The location to place generated source files. |

If `iceHome` is not set, the plug-in will check the `ICE_HOME` environment
variable to determine the location of the Ice installation. If `ICE_HOME` is
not set either, the plug-in uses the following defaults on Linux and OS X:

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

Refer to the [slice2java Command-Line Options](https://doc.zeroc.com/display/Ice/slice2java+Command-Line+Options)
for a description of the options you can provide through the `args` property.

Note: the location of the Ice Slice files is automatically added to `include` by the plug-in.

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
       files = filetree(dir: "b", includes: ['**.ice'])
     }
  }
}
```

### Configuring Slice-to-FreezeJ Projects

Use the `freezej` sub-section to generate Freeze maps and indices with `slice2freezej`.

#### `freezej` Properties

Each `freezej` source set defines the following convention properties:

| Property name | Type           | Default value  | Description                                             |
| ------------- | -------------- | :------------: | ------------------------------------------------------- |
| srcDir        | File           | src/main/slice | The Slice file source directory.                        |
| args          | String         | -              | The arguments to `slice2freezej`.                       |
| files         | FileCollection | -              | The Slice files in this source set. Overrides `srcDir`. |
| include       | Set<File>      | -              | Slice include file search path.                         |

Refer to the [slice2freezej Command-Line Options](https://doc.zeroc.com/display/Ice/slice2freezej+Command-Line+Options)
for a description of the options you can provide through the `args` property.

Note: the location of the Ice Slice files is automatically added to `include` by the plug-in.

#### `dict` Source Set

A `dict` source set describes one ore more Freeze maps (also known as dictionaries) generated by `slice2freezej`.
Each map in such a source set must have a unique name.

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
module Test {
struct Foo
{
    string s;
    Struct1 s1;
};
};
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

Given the following Slice type defined in `Phonebook.ice`:

```
// Slice
module Demo {
class Contact
{
    string name;
    string address;
    string phone;
};
};
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

### Dependenicies

The plugin minimizes recompilation by maintaining dependencies between Slice files. The task stores this information in
the `build` directory and updates these dependencies after each invocation.

The plugin compiles a Slice file when any of the following conditions are true:

- no dependency information is found for the Slice file
- the modification time of the Slice file is later than the modification time of the dependency file
- the Slice file includes another Slice file that is eligible for compilation
