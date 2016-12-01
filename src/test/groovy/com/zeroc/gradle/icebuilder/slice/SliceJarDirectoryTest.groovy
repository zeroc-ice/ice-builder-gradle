package com.zeroc.gradle.icebuilder.slice

import java.nio.file.Files
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.Rule
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail
import static org.junit.Assume.assumeNotNull

class SliceJarDirectoryTest {

    def project = null
    def iceHome = null

    @Before
    public void applySlicePlugin() {
        def isWindows = System.getProperty('os.name').toLowerCase().contains('windows')
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'
        assumeNotNull(project.slice.iceHome)
        assumeNotNull(project.slice.slice2java)

        iceHome = File.createTempDir()
        createIceHomePath(["bin"])

        def copyFileBytes = { src, dst ->
           def srcFile = new File(src)
           def dstFile = new File(dst)
           dstFile << srcFile.bytes
           return dstFile
        }

        def dst =  [iceHome.toString(), "bin", new File(project.slice.slice2java).getName()].join(File.separator)
        copyFileBytes(project.slice.slice2java, dst).setExecutable(true)

        // For Ice 3.6 we also copy slice2java dependencies.
        // This is unnecessary in Ice 3.7 as slice2java is statically linked
        if(isWindows && project.slice.compareIceVersion("3.6") == 1) {
           ['slice36.dll', 'iceutil36.dll'].each {
                def src = [new File(project.slice.slice2java).getParent(), it].join(File.separator)
                copyFileBytes(src, [iceHome.toString(), "bin", it].join(File.separator)).setExecutable(true)
           }
        }
    }

    @After
    public void cleanup() {
        project.delete()
        project = null
        iceHome.deleteDir()
        iceHome.deleteOnExit()
    }

    def createIceHomePath(path) {
        def newPath = new File([iceHome.toString(), path.join(File.separator)].join(File.separator))
        newPath.mkdirs()
        newPath.toString()
    }

    @Test
    public void testSliceCommon() {
        def tmpSliceDir = createIceHomePath(["share", "slice"])
        project.slice.iceHome = iceHome.toString()
        assertNotNull(project.slice.sliceDir)
        assertTrue(project.slice.sliceDir == tmpSliceDir)
    }

    @Test
    public void testIce37SliceDir() {
        def tmpSliceDir = createIceHomePath(["share", "ice", "slice"])
        project.slice.iceHome = iceHome.toString()
        assertNotNull(project.slice.sliceDir)
        assertTrue(project.slice.sliceDir == tmpSliceDir)
    }

    @Test
    public void testIce36SliceDir() {
        def tmpSliceDir = createIceHomePath(["share", "Ice-${project.slice.iceVersion}", "slice"])
        project.slice.iceHome = iceHome.toString()
        println(project.slice.iceHome)
        assertNotNull(project.slice.sliceDir)
        assertTrue(project.slice.sliceDir == tmpSliceDir)
    }

    @Test
    public void testOptSourceSliceDir() {
        def tmpSliceDir = createIceHomePath(["slice"])
        project.slice.iceHome = iceHome.toString()
        assertNotNull(project.slice.sliceDir)
        assertTrue(project.slice.sliceDir == tmpSliceDir)
    }

}
