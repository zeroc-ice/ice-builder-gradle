// **********************************************************************
//
// Copyright (c) 2014-2016 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

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
    //     try {
            assumeNotNull(project.slice.iceHome)
            assumeNotNull(project.slice.slice2java)
    //     } catch (org.gradle.api.GradleException e) {
    //         assumeNoException(e);
    //     }
    }

    @After
    public void cleanupProject() {
        project.delete()
        project = null
    }

    void forceReinitialization() {
        // forces re-initialization
        def iceHome = project.slice.iceHome
        project.slice.iceHome = iceHome
    }
}
