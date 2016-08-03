package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.Rule
import org.junit.Test


import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class SlicePluginPropertyTest {

    def os = null
    def autoDetectedIceHome = null

    @Before
    public void initialize() {
        os = System.properties['os.name']
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void pluginAddsCompileSliceTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'
        assertTrue(project.tasks.compileSlice instanceof SliceTask)
    }

    @Test
    public void testAutoDetectIceHome() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

        assertTrue(project.slice.iceHome != "")
        assertTrue(project.slice.srcDist == false)
        assertTrue(project.slice.iceVersion != "" && project.slice.iceVersion != null)
        assertTrue(new File(project.slice.slice2java).exists())
        assertTrue(new File(project.slice.sliceDir).exists())

        autoDetectedIceHome = project.slice.iceHome
    }

    @Test
    public void testManualBinDistIceHome() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

        project.slice.iceHome = autoDetectedIceHome
        assertTrue(project.slice.iceHome != "")
        assertTrue(project.slice.srcDist == false)
        assertTrue(project.slice.iceVersion != "" && project.slice.iceVersion != null)
        assertTrue(new File(project.slice.slice2java).exists())
        assertTrue(new File(project.slice.sliceDir).exists())
    }

    @Test
    public void testInvalidIceHome() {
        //
        // Test an bogus iceHome (non srcDist)
        //
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

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
        // initialize the configuration without failure
        //
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

        // Create temporary iceHome with fake structure that build expects
        def tmpIceHome = File.createTempDir("iceHome", "dir")
        tmpIceHome.deleteOnExit()
        def tmpBuildGralde = new File([tmpIceHome.toString(), "java", "build.gradle"].join(File.separator))
        tmpBuildGralde.mkdirs()
        assertTrue(tmpBuildGralde.exists())

        project.slice.iceHome = tmpIceHome.toString()

        assertTrue(project.slice.iceHome == tmpIceHome.toString())
        assertTrue(project.slice.srcDist == true)
        assertTrue(project.slice.iceVersion == null)
    }


    @Test
    public void testCppPlatformAndConfigurationFromEnvironment() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

        environmentVariables.set("CPP_CONFIGURATION", "Release");
        environmentVariables.set("CPP_PLATFORM", "Win32");

        assertTrue(project.slice.cppConfiguration == "Release")
        assertTrue(project.slice.cppPlatform == "Win32")
    }

    @Test
    public void testCppPlatformAndConfiguration() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

        project.slice {
            cppConfiguration = "Debug"
            cppPlatform = "x64"
        }

        assertTrue(project.slice.cppConfiguration == "Debug")
        assertTrue(project.slice.cppPlatform == "x64")
    }

    @Test
    public void testCppPlatformAndConfigurationOverrideEnvironment() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'

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
