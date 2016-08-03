package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assume.assumeNotNull

class Slice2JavaPluginTest {

    def project = null

    @Before
    public void applySlicePlugin() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'
        assumeNotNull(project.slice.iceHome)
        assumeNotNull(project.slice.slice2java)
    }

    @After
    public void cleanup() {
        project.delete()
        project = null
    }

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

    private File pathToFile(pathList) {
        return new File(pathList.join(File.separator))
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
