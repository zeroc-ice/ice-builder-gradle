// **********************************************************************
//
// Copyright (c) 2014-2016 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import org.junit.Test

import static org.junit.Assert.assertTrue

class Slice2JavaPluginTest extends TestCase {

    @Test
    public void testSlice2JavaWithDefaults() {
        // Where builder checks for slice files by default
        pathToFile([project.rootDir, 'src', 'main', 'slice']).mkdirs()

        writeTestSliceToFile(pathToFile([project.rootDir, 'src', 'main', 'slice', 'Test.ice']))

        project.tasks.compileSlice.execute()

        assertTrue(pathToFile([project.rootDir, 'build', 'generated-src', 'Test']).exists())
        assertTrue(pathToFile([project.rootDir, 'build', 'generated-src', 'Test', 'Hello.java']).exists())
    }

    @Test
    public void testSlice2JavaSliceSrcDir() {
        def sliceDir = pathToFile([project.rootDir, 'src', 'other', 'slice'])
        sliceDir.mkdirs()

        project.slice.java {
            srcDir = sliceDir.toString()
        }

        writeTestSliceToFile(pathToFile([project.rootDir, 'src', 'other', 'slice', 'Test.ice']))

        project.tasks.compileSlice.execute()

        assertTrue(pathToFile([project.rootDir, 'build', 'generated-src', 'Test']).exists())
        assertTrue(pathToFile([project.rootDir, 'build', 'generated-src', 'Test', 'Hello.java']).exists())

    }

    private void writeTestSliceToFile(file) {
        file << """
            |module Test
            |{
            |
            |interface Hello
            |{
            |    idempotent void sayHello(int delay);
            |    void shutdown();
            |};
            |
            |};
        """.stripMargin()
    }
}
