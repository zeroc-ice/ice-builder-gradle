// **********************************************************************
//
// Copyright (c) 2014-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder

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

    @Test
    public void testRemovingGeneratedFiles() {
        // Where builder checks for slice files by default
        pathToFile([project.rootDir, 'src', 'main', 'slice']).mkdirs()

        writeTestSliceToFile(pathToFile([project.rootDir, 'src', 'main', 'slice', 'Test.ice']))

        project.tasks.compileSlice.execute()

        def geneatedHello = pathToFile([project.rootDir, 'build', 'generated-src', 'Test', 'Hello.java'])

        assertTrue(geneatedHello.exists())
        geneatedHello.delete()
        assertTrue(!geneatedHello.exists())

        // Project tasks should not be re-executed and we are not using a tool like
        // GradleConnector/GradleRunner. So instead we make a new project and run it.
        def p = newProjectWithProjectDir()
        p.pluginManager.apply 'java'
        p.pluginManager.apply 'slice'
        p.tasks.compileSlice.execute()
        assertTrue(geneatedHello.exists())
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
