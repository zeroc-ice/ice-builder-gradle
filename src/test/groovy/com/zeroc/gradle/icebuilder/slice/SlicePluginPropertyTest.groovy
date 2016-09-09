package com.zeroc.gradle.icebuilder.slice

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

class SlicePluginPropertyTest {

    def project = null
    def static os = null

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeClass
    public static void initialize() {
        os = System.properties['os.name']
    }

    @Before
    public void applySlicePlugin() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'
    }

    @After
    public void cleanup() {
        project.delete()
        project = null
    }

    @Test
    public void pluginAddsCompileSliceTaskToProject() {
        assertTrue(project.tasks.compileSlice instanceof SliceTask)
    }

    @Test
    public void testAutoDetectIceHome() {
        assertTrue(project.slice.iceHome != "")
        assertTrue(project.slice.srcDist == false)
        assertTrue(project.slice.iceVersion != "" && project.slice.iceVersion != null)
        assertTrue(new File(project.slice.slice2java).exists())
        assertTrue(new File(project.slice.sliceDir).exists())
    }

    @Test
    public void testManualBinDistIceHome() {
        def iceHome = project.slice.iceHome
        project.slice.iceHome = iceHome // forces re-initialization
        assertTrue(project.slice.iceHome != "")
        assertNotNull(project.slice.iceHome)
        assertTrue(project.slice.srcDist == false)
        assertTrue(project.slice.iceVersion != "")
        assertNotNull(project.slice.iceVersion)
        assertTrue(new File(project.slice.slice2java).exists())
        assertTrue(new File(project.slice.sliceDir).exists())
    }

    @Test
    public void testInvalidIceHome() {
        //
        // Test an bogus iceHome (non srcDist)
        //
        def tmpIceHome = File.createTempDir("iceHome", "dir")
        tmpIceHome.deleteOnExit()
        project.slice.iceHome = tmpIceHome.toString()
        try {
            def iceHome = project.slice.iceHome
            fail()
        } catch (e) {
            // This should throw an exception since /dev/null is not a valid iceHome location. slice2java is missing
        }
    }

    @Test
    public void testIceHomeWithNoSlice2Java() {
        //
        // Test that if iceHome is a srcDist and slice2java is missing that we can still
        // initialize (at least partially) the configuration without failure
        //

        // Create temporary iceHome with fake structure that slice extension requires
        def tmpIceHome = File.createTempDir("iceHome", "dir")
        tmpIceHome.deleteOnExit()
        def tmpBuildGralde = new File([tmpIceHome.toString(), "java", "build.gradle"].join(File.separator))
        tmpBuildGralde.mkdirs()
        assertTrue(tmpBuildGralde.exists())

        project.slice.iceHome = tmpIceHome.toString()

        assertTrue(project.slice.iceHome == tmpIceHome.toString())
        assertTrue(project.slice.srcDist == true)
        assertTrue(project.slice.iceVersion == null)
        assertTrue(project.slice.sliceDir == null)
        assertTrue(project.slice.jarDir == null)
    }


    @Test
    public void testCppPlatformAndConfigurationFromEnvironment() {
        environmentVariables.set("CPP_CONFIGURATION", "Release");
        environmentVariables.set("CPP_PLATFORM", "Win32");

        assertTrue(project.slice.cppConfiguration == "Release")
        assertTrue(project.slice.cppPlatform == "Win32")
    }

    @Test
    public void testCppPlatformAndConfiguration() {
        project.slice {
            cppConfiguration = "Debug"
            cppPlatform = "x64"
        }

        assertTrue(project.slice.cppConfiguration == "Debug")
        assertTrue(project.slice.cppPlatform == "x64")
    }

    @Test
    public void testCppPlatformAndConfigurationOverrideEnvironment() {
        environmentVariables.set("CPP_CONFIGURATION", "Release");
        environmentVariables.set("CPP_PLATFORM", "Win32");

        project.slice {
            cppConfiguration = "Debug"
            cppPlatform = "x64"
        }

        assertTrue(project.slice.cppConfiguration == "Debug")
        assertTrue(project.slice.cppPlatform == "x64")
    }
}
