//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

package com.zeroc.gradle.icebuilder.slice

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assume.assumeNotNull

//
// Test version comparsions
//
// 1 is a > b
// 0 if a == b
// -1 if a < b
//

class SlicePluginVersionTest extends TestCase {

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
