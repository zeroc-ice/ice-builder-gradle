package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static org.junit.Assume.assumeNotNull

import static com.zeroc.gradle.icebuilder.slice.SliceExtension.compareVersions

//
// Test version comparsions
//
// 1 is a > b
// 0 if a == b
// -1 if a < b
//

class SlicePluginVersionTest {

    def project = null

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
    public void testIceVersionEquals() {
        assumeNotNull(project.slice.iceVersion)
        assertEquals(0, project.slice.compareIceVersion(project.slice.iceVersion))
    }

    @Test
    public void testCompareVersionEquals() {
        assertEquals(0, SliceExtension.compareVersions("3.7.0", "3.7.0"))
    }

    @Test
    public void testCompareMajorVersionEquals() {
        assertEquals(0, SliceExtension.compareVersions("3.7", "3.7"))
    }

    @Test
    public void testCompareVersionPointRelase() {
        assertEquals(-1, SliceExtension.compareVersions("3.7.0", "3.7.1"))
    }

    @Test
    public void testCompareVersionLess() {
        assertEquals(-1, SliceExtension.compareVersions("3.6.0", "3.7.0"))
    }

    @Test
    public void testCompareVersionGreater() {
        assertEquals(1, SliceExtension.compareVersions("3.7.0", "3.6.0"))
    }

    // Common indices match. Assume the longest version is the most recent
    @Test
    public void testCompareVersionLength() {
        assertEquals(-1, SliceExtension.compareVersions("3.7", "3.7.0"))
    }

    @Test
    public void testCompareVersionLengthGreater() {
        assertEquals(1, SliceExtension.compareVersions("3.7", "3.6.0"))
    }

    @Test
    public void testCompareVersionLengthLess() {
        assertEquals(-1, SliceExtension.compareVersions("3.6.0", "3.7"))
    }

}
