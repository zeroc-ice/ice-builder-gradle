// Copyright (c) ZeroC, Inc. All rights reserved.

package com.zeroc.gradle.icebuilder.slice

import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before

import static org.junit.Assume.assumeNoException
import static org.junit.Assume.assumeNotNull

class TestCase {
    def project = null

    @Before
    void createProject() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'java'
        project.pluginManager.apply 'slice'
    }

    @Before
    void checkIceInstalled() {
        assumeNotNull(project.slice.iceHome)
        assumeNotNull(project.slice.slice2java)
    }

    @After
    public void cleanupProject() {
        project.delete()
        project = null
    }

    def newProjectWithProjectDir() {
        def p = ProjectBuilder.builder().withProjectDir(project.rootDir).build()
        p.pluginManager.apply 'java'
        p.pluginManager.apply 'slice'
        return p
    }

    void forceReinitialization() {
        // Setting any property will trigger the plug-in initialization in the next property read.
        //
        // Set cppConfiguration and cppPlatform to null is required to force reading CPP_PLATFORM and
        // CPP_CONFIGURATION enviroment variables during re-intialization
        project.slice.cppConfiguration = null
        project.slice.cppPlatform = null
    }

    def pathToFile(pathList) {
        return new File(pathList.join(File.separator))
    }
}
