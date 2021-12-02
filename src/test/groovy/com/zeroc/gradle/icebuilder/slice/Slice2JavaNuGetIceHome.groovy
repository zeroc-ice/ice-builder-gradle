// Copyright (c) ZeroC, Inc. All rights reserved.

package com.zeroc.gradle.icebuilder.slice

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assume.assumeTrue

class Slice2JavaNuGetIceHome extends TestCase {
    @Before
    public void checkIsWindows() {
        // For what we are testing our ice version must be >= 3.7 on Windows os

        assumeTrue(project.slice.compareIceVersion('3.7') >= 0)
        assumeTrue(System.getProperty('os.name').toLowerCase().contains('windows'))
    }

    @Test void testCppNugetIceHome() {
        def nuget = File.createTempDir()
        def tools = new File([nuget.toString(), "tools"].join(File.separator))
        tools.mkdirs()
        def copyFileBytes = { src, dst ->
           def srcFile = new File(src)
           def dstFile = new File(dst)
           dstFile << srcFile.bytes
           return dstFile
        }

        def dst =  [nuget.toString(), "tools", new File(project.slice.slice2java).getName()].join(File.separator)
        copyFileBytes(project.slice.slice2java, dst).setExecutable(true)

        project.slice.iceHome = nuget.toString()
        assertTrue(project.slice.slice2java == dst)
    }
}
