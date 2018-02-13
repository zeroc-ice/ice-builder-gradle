// **********************************************************************
//
// Copyright (c) 2014-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables

import static org.junit.Assert.*
import static org.junit.Assume.*

class SlicePluginPropertyTest extends TestCase {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void pluginAddsCompileSliceTaskToProject() {
        assertTrue(project.tasks.compileSlice instanceof SliceTask)
    }

    @Test
    public void testAutoDetectIceHome() {
        // This test only works if ICE_HOME is not set
        assumeTrue(System.getenv()['ICE_HOME'] == null)
        assertTrue(project.slice.iceHome != "")
        assertTrue(project.slice.srcDist == false)
        assertTrue(project.slice.iceVersion != "" && project.slice.iceVersion != null)
        assertTrue(new File(project.slice.slice2java).exists())
        assertTrue(new File(project.slice.sliceDir).exists())
    }

    @Test
    public void testManualBinDistIceHome() {
        // This test only works if ICE_HOME is not set
        assumeTrue(System.getenv()['ICE_HOME'] == null)
        def iceHome = project.slice.iceHome
        project.slice.iceHome = iceHome
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
        def tmpIceHome = File.createTempDir()
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
        def tmpIceHome = File.createTempDir()
        tmpIceHome.deleteOnExit()
        def tmpBuildGralde = new File([tmpIceHome.toString(), "java", "build.gradle"].join(File.separator))
        tmpBuildGralde.mkdirs()
        tmpBuildGralde.deleteOnExit()
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
        forceReinitialization()

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
        forceReinitialization()

        project.slice {
            cppConfiguration = "Debug"
            cppPlatform = "x64"
        }

        assertTrue(project.slice.cppConfiguration == "Debug")
        assertTrue(project.slice.cppPlatform == "x64")
    }
}
