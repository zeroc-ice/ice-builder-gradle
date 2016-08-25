package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assume.assumeNotNull
import static org.junit.Assume.assumeTrue

class Slice2JavaCompatDefine {

    def project = null

    @Before
    public void applySlicePlugin() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'
        assumeNotNull(project.slice.iceHome)
        assumeNotNull(project.slice.slice2java)
        // For what we are testing our ice version must be < 3.7
        assumeTrue(project.slice.compareIceVersion('3.7') == -1)
    }

    @After
    public void cleanup() {
        project.delete()
        project = null
    }

    @Test
    public void testSlice2JavaCompatDefine() {
        // Where builder checks for slice files by default
        pathToFile([project.rootDir, 'src', 'main', 'slice']).mkdirs()

        writeTestSliceToFile(pathToFile([project.rootDir, 'src', 'main', 'slice', 'Test.ice']))

        project.tasks.compileSlice.execute()

        assertTrue(pathToFile([project.rootDir,
                               'build',
                               'generated-src',
                               'com',
                               'zeroc',
                               'Test',
                               'Hello.java']).exists())
    }

    private File pathToFile(pathList) {
        return new File(pathList.join(File.separator))
    }

    private void writeTestSliceToFile(file) {
        file << """
            |#ifdef __SLICE2JAVA_COMPAT__
            |[[\"java:package:com.zeroc\"]]        [[\"java:package:com.zeroc\"]]
            |#endif
            |
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
