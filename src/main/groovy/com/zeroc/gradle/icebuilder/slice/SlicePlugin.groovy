// **********************************************************************
//
// Copyright (c) 2014-2016 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

class SlicePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('compileSlice', type: SliceTask) {
            group = "Slice"
        }

        // Create and install the extension object.
        project.extensions.create("slice", SliceExtension, project.container(Java))

        project.extensions.slice.extensions.create("freezej", Freezej,
             project.container(Dict), project.container(Index))

        project.slice.output = project.file("${project.buildDir}/generated-src")


        if(!project.getTasksByName("compileJava", false).isEmpty()) {
            project.getTasks().getByName("compileJava").dependsOn('compileSlice');
            project.sourceSets.main.java.srcDir project.slice.output
        } else if(!project.getTasksByName("preBuild", false).isEmpty())  {
            project.getTasks().getByName("preBuild").dependsOn('compileSlice');
        }
    }
}
