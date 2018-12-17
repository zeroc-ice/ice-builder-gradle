// **********************************************************************
//
// Copyright (c) 2014-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.logging.Logging
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

class SlicePlugin implements Plugin<Project> {
    private static final def LOGGER = Logging.getLogger(SliceTask)

    void apply(Project project) {
        project.task('compileSlice', type: SliceTask) {
            group = "Slice"
        }

        // Create and install the extension object.
        project.extensions.create("slice", SliceExtension, project.container(Java))
        project.extensions.slice.extensions.create("freezej",
                                                   Freezej,
                                                   project.container(Dict),
                                                   project.container(Index))

        project.slice.output = project.file("${project.buildDir}/generated-src")

        project.afterEvaluate {
            if (isAndroidProject(project)) {
                // Android projects do not define a 'compileJava' task. We wait until the project is evaluated
                // and add our dependency to the variant's javaCompiler task.
                getAndroidVariants(project).all { variant ->
                    variant.registerJavaGeneratingTask(project.tasks.getByName('compileSlice'), project.slice.output)
                }
            } else {
                project.tasks.getByName("compileJava").dependsOn('compileSlice');
                project.sourceSets.main.java.srcDir project.slice.output
            }
        }
    }

    def isAndroidProject(Project project) {
        return project.hasProperty('android') && project.android.sourceSets
    }

    def getAndroidVariants(Project project) {
        // https://sites.google.com/a/android.com/tools/tech-docs/new-build-system/user-guide
        return project.android.hasProperty('libraryVariants') ?
            project.android.libraryVariants : project.android.applicationVariants
    }
}
