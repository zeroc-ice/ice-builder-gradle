[![Build Status](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle.svg?token=icxd1yE9Nf6WLivZz2vF&branch=master)](https://magnum.travis-ci.com/zeroc-ice/ice-builder-gradle)

# Ice Builder for Gradle

The Ice Builder for Gradle provides a gradle plugin to manage compilation
of [Slice](https://doc.zeroc.com/display/Ice/The+Slice+Language) files to
Java.

## Build Instructions

To build the plugin run:

```
  $ ./gradlew build
```

## Using the Gradle Plugin

To use the plugin, include the following in your build script:

```
buildscript {
    repositories {
        maven {
            url 'https://repo.zeroc.com/nexus/content/repositories/releases'
        }
    }
    dependencies {
        classpath group: 'com.zeroc.gradle.plugins', name: 'ice-gradle-plugin', version: '1.0'
    }
}
apply plugin: 'slice'
```

It is important that the `slice` plugin is applied after the `java` plugin in
order for task dependencies to be properly setup.

## Gradle Tasks

The Ice Builder plugin adds a task to your project, as shown below:

| Task name         | Type      | Description                             |
| ----------------- | --------- | --------------------------------------- |
| generateSliceTask | SliceTask | Generates Java source from Slice files. |

The Ice Builder plugin adds the following dependency to tasks added by the Java
plugin:

| Task name   | Depends On        |
| ----------- | ----------------- |
| compileJava | generateSliceTask |

In addition, it adds the following dependency to tasks added by
the Android plugin:

| Task name | Depends On        |
| --------- | ----------------- |
| preBuild  | generateSliceTask |

## Project Layout

| Directory      | Meaning                 |
| -------------- | ----------------------- |
| src/main/slice | Production Slice files. |

## Convention Properties

The Ice Builder plugin defines the following convention properties:

| Property name | Type   | Default value                        | Description                                  |
| ------------- | ------ | ------------------------------------ | -------------------------------------------- |
| iceHome       | String | null                                 | The location of the Ice installation         |
| iceVersion    | String | Latest Ice version at plugin release | The Ice version                              |
| output        | File   | buildDir/generated-src               | The location to place generated source files |

If `iceHome` is not set, the plugin will check the `ICE_HOME` environment
variable to determine the location of the Ice installation. If `ICE_HOME` is not
set, it will look in the default install location of the Ice binary
distribution. If Ice is installed in a non-standard location, then either
`iceHome` or `ICE_HOME` must be set.

## Configuring Slice-to-Java Projects

The sub-section java is used to configure files compiled with slice2java.
Contained within the java sub-section are a number of source sets, each
representing a set of common flags for Slice files. If there is only a single
source set, the source set can be omitted.

### Java Properties

Each source set defines the following convention properties:

| Property name | Type | Default value | Description |
| ------------- | ---- | ------------- | ----------- |
| args | String | -  | The arguments to slice2java |
| files  | FileCollection | -  | The Slice files in this source set. Contains only .ice files found in the source directories, and excludes all other files. |
| include | Set<File>  | null | Locations to be included. |
| name | String (read-only) | Not null | The name of the set. |
| output.classesDir | File | _buildDir_/classes/main | The directory in which to generate the classes of this source set. |
| srcDir | File | src/main/slice | The Slice file source directory. |

### Java Examples

Build all the Slice files contained in src/main/slice with the --tie argument:

```
slice {
  java {
     args = "--tie"
  }
}
```

Build a.ice with the argument --stream, and all Slice files in b with all given
include directories:

```
slice {
  java {
     set1 {
       include = ["${projectDir}", "${sliceDir}"]
       args = "--stream"
       files = [file("a.ice")]
     }
     set2 {
       include = ["${projectDir}", "${sliceDir}"]
       files = filetree(dir: "b", includes: ['**.ice'])
     }
  }
}
```

## Configuring Slice-to-Freeze Projects

The sub-section freezej is used to configure files compiled with slice2freezej.
Contained within the freezej sub-section is a source-set for dictionaries and a
source-set for indices.

### Freeze Properties

Each source set defines the following convention properties:

| Property name | Type | Default value | Description |
| ------------- | ---- | ------------- | ----------- |
| name | String (read-only) | Not null | The name of the set. |
| args | String | -  | The arguments to slice2freezej |
| files  | FileCollection | -  | The Slice files in this source set. Contains only .ice files found in the source directories, and excludes all other files. |
| include | Set<File>  | null | Locations to be included. |
| name | String (read-only) | Not null | The name of the set. |
| output.classesDir | File | _buildDir_/classes/main | The directory in which to generate the classes of this source set. |
| srcDir | File | src/main/slice | The Slice file source directory. |

### Dictionary Source Set

The dict source-set specifies the set of Freeze dictionary data types to
generate.

#### Dictionary Properties

Each dictionary defines the following convention properties:

| Property name | Type | Default value | Description |
| ------------- | ---- | ------------- | ----------- |
| index | List\<Map\<String, String>> | Not null | A list of dictionary values used for keys. |
| javaType | String | - | The name of the generated Java type. |
| key | String | - | The Slice type of the key. |
| name | String (read-only) | Not null | The name of the dictionary. |
| value | String | - | The Slice type of the value. |

#### Dictionary Examples

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

```
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

```
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

More examples are embedded in the gradle definitions.

### Index Source Set

The index source-set specifies the set of Freeze evictor index types to
generate.

#### Index Properties

Each index defines the following convention properties:

| Property name | Type | Default value | Description |
| ------------- | ---- | ------------- | ----------- |
| name | String (read-only) | Not null | The name of the index. |
| javaType | String | - | The name of the generated Java type. |
| type | String | - | The Slice type of the type to be indexed. |
| member | String | - | The name of the data member in the type to index. |
| casesensitive | boolean | true | If the member is a string, this specifies whether the comparison is case sensitive. |

#### Index Example

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

```
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
```
