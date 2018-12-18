// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.GradleException
import java.io.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.util.XmlSlurper
import groovy.xml.MarkupBuilder

class SliceTask extends DefaultTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(SliceTask)

    SliceTask() {
        // This forces the task to run on each build rather than relying on a hash
        // of the input files.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    def action() {
        if(!project.slice.output.isDirectory()) {
            if(!project.slice.output.mkdirs()) {
                throw new GradleException("could not create slice output directory: ${project.slice.output}")
            }
        }

        //
        // In case the slice output directory is not inside the buildDir, as we still store
        // dependency files here
        //
        if(!project.buildDir.isDirectory()) {
            if(!project.buildDir.mkdirs()) {
                throw new GradleException("could not create build output directory: ${project.buildDir}")
            }
        }

        // Make sure default source set is present
        if(project.slice.java.isEmpty()) {
            project.slice.java.create("default")
        }

        processJava()
        processFreezeJ()
    }

    @InputFiles
    def getInputFiles() {
        return []
    }

    @OutputDirectory
    def getOutputDirectory() {
        return project.slice.output
    }

    def processFreezeJ() {
        def freezej = project.slice.freezej

        // Set of source files and all dependencies.
        Set files = []
        if(freezej.files) {
            files.addAll(freezej.files)
            getS2FDependencies(freezej).values().each({ files.addAll(it) })
        }

        def state = new FreezeJBuildState()
        def stateFile = new File(project.slice.output, "slice2freezej.df.xml")
        state.read(stateFile)

        def rebuild = false

        def args = buildS2FCommandLine(freezej)
        // If the command line changes or the slice2freezej compiler (always the first argument)
        // has been updated then rebuild.
        if(args != state.args || getTimestamp(new File(args[0])) > state.timestamp) {
            rebuild = true
        }

        // Rebuild if the set of slice files has changed, or if one of the slice files has a timestamp newer
        // than the last build.
        if(!rebuild && state.slice.size() != files.size()) {
            rebuild = true
        }

        // If the build timestamp is older than the slice file timestamp then we need to build the source file.
        if(!rebuild) {
            rebuild = files.any { getTimestamp(it) > state.timestamp }
        }

        // Check if the prior set of slice files has changed.
        if(!rebuild) {
            rebuild = state.slice.any { !files.contains(it) }
        }

        // Check that any previously generated files still exist
        if(!rebuild) {
            rebuild = state.generated.any { generatedFile ->
                !generatedFile.isFile() || getTimestamp(generatedFile) > state.timestamp
            }
        }

        // Bail out if there is nothing to do (in theory this should not occur).
        if(!rebuild) {
            LOGGER.info("nothing to do")
            return
        }

        if(freezej.files) {
            LOGGER.info("running slice2freezej on the following slice files")
            freezej.files.each { LOGGER.info("    ${it}") }
        } else if (!stateFile.isFile()) {
            // The state file has never been written and and we have no
            // files to process. No reason to continue as it will only
            // write an empty dependency file.
            LOGGER.info("nothing to do")
            return
        }

        // List of generated java source files.
        def generated = executeS2F(freezej)

        // Gather up the list of source files that we previously built for those files which are building.
        Set oldGenerated = state.generated

        // Remove all source files that we have generated from the list of previously built sources.
        oldGenerated.removeAll(generated)

        // Update the build state.
        def newState = new FreezeJBuildState()
        newState.timestamp = System.currentTimeMillis()
        newState.slice = files
        newState.generated = generated
        newState.args = args

        // Write the new dependencies file.
        newState.write(stateFile)

        deleteFiles(oldGenerated)
    }

    def getS2FGenerated(freezej) {
        def files = []
        freezej.dict.each {
            files.add(it.javaType)
        }

        freezej.index.each {
            files.add(it.javaType)
        }

        // Convert each file name from java package convention to file name.
        files = files.collect {
            it.tr('.', '/') + ".java"
        }
        // Convert to a file with the generated path.
        files = files.collect {
            new File(project.slice.output, it)
        }

        return files
    }

    // Executes slice2freezej to determine the slice file dependencies.
    // Returns a dictionary of A  -> [B] where A depends on B.
    def getS2FDependencies(freezej) {
        if((freezej.dict == null || freezej.dict.isEmpty()) && (freezej.index == null || freezej.index.isEmpty())) {
            // No build artifacts.
            return [:]
        }

        def command = buildS2FCommandLine(freezej)
        command.add("--depend-xml")

        freezej.files.each {
            command.add(it.getAbsolutePath() )
        }

        LOGGER.info("processing dependencies:\n${command}")

        def sout = new StringBuffer()
        def serr = new StringBuffer()

        def p = command.execute(project.slice.env, null)
        p.waitForProcessOutput(sout, serr)

        printWarningsAndErrors(serr)

        if (p.exitValue() != 0) {
            throw new GradleException("${command[0]} failed with exit code: ${p.exitValue()}")
        }

        return parseSliceDependencyXML(new XmlSlurper().parseText(sout.toString()))
    }

    // Run slice2freezej. Returns a dictionary of A -> [B] where A is a slice file, and B is the list of
    // generated java source files.
    def executeS2F(freezej) {
        if((freezej.dict == null || freezej.dict.isEmpty()) && (freezej.index == null || freezej.index.isEmpty())) {
            // No build artifacts.
            return [:]
        }

        def command = buildS2FCommandLine(freezej)
        freezej.files.each {
            command.add(it.getAbsolutePath() )
        }

        LOGGER.info("processing slice:\n${command}")

        def sout = new StringBuffer()
        def serr = new StringBuffer()

        def p = command.execute(project.slice.env, null)
        p.waitForProcessOutput(sout, serr)

        printWarningsAndErrors(serr)

        if (p.exitValue() != 0) {
            throw new GradleException("${command[0]} failed with exit code: ${p.exitValue()}")
        }
        return getS2FGenerated(freezej)
    }

    def buildS2FCommandLine(freezej) {
        def command = []
        command.add(project.slice.slice2freezej)
        command.add("--output-dir=" + project.slice.output.getAbsolutePath())
        if (project.slice.sliceDir) {
            command.add("-I${project.slice.sliceDir}")
        }

        freezej.include.each {
            command.add('-I' + it)
        }

        freezej.args.split().each {
            command.add(it)
        }

        freezej.dict.each {
            def javaType = it.javaType
            command.add("--dict")
            command.add(javaType + "," + it.key + "," + it.value)
            it.index.each {
                command.add("--dict-index")
                def buf = new StringBuffer()
                buf << javaType
                if(it.containsKey('member')) {
                    buf << ','
                    buf << it['member']
                }
                if(it.containsKey('caseSensitive')) {
                    buf << ','
                    if(it['caseSensitive']) {
                        buf << "case-sensitive"
                    }else {
                        buf << "case-insensitive"
                    }
                }
                command.add(buf.toString())
            }
        }

        freezej.index.each {
            command.add("--index")
            def buf = new StringBuffer()
            buf << it.javaType
            buf << ','
            buf << it.type
            buf << ','
            buf << it.member
            buf << ','
            if(it.caseSensitive) {
                buf << "case-sensitive"
            }else {
                buf << "case-insensitive"
            }
            command.add(buf.toString())
        }
        return command
    }

    // Process the written slice file which is of the format:
    //
    // <state>
    //   <timestamp value="YYYY"/>
    //   <slice name="A.ice"/>
    //   <slice name="B.ice"/>
    //   <generated name="B.java"/>
    //   <generated name="B.ice"/>
    // </state>

    class FreezeJBuildState {
        // Timestamp of last build in milliseconds.
        def timestamp

        // List of slice files.
        def slice = []

        // List of generated source files.
        def generated = []

        // List of command line arguments.
        def args = []

        def write(stateFile) {
            def writer = new StringWriter()
            def xml = new MarkupBuilder(writer)
            xml.state {
                xml.timestamp("value": timestamp)
                args.each {
                    xml.arg("value": it)
                }
                slice.each {
                    xml.slice("name": it)
                }
                generated.each {
                    xml.generated("name": it)
                }
            }
            stateFile.write(writer.toString())
        }

        def read(stateFile) {
            if(!stateFile.isFile()) {
                return
            }

            try {
                def xml = new XmlSlurper().parse(stateFile)

                if(xml.name() != "state") {
                    throw new GradleException("malformed XML: expected `state'")
                }

                xml.children().each {
                    if(it.name() == "arg") {
                        args.add(it.attributes().get("value"))
                    } else if(it.name() == "timestamp") {
                        timestamp = it.attributes().get("value").toLong()
                    } else if(it.name() == "slice") {
                        slice.add(new File(it.attributes().get("name")))
                    } else if(it.name() == "generated") {
                        generated.add(new File(it.attributes().get("name")))
                    }
                }
            }
            catch(Exception ex) {
                LOGGER.info("invalid XML: ${stateFile}")
                println ex
            }
        }
    }

    def processJava() {
        // Dictionary of A->[B] where A is a slice file and B is the list of generated
        // source files.
        def generated = [:]

        // Set of slice files processed.
        Set built = []

        // Complete set of slice files.
        Set files = []

        def stateFile = new File(project.slice.output, "slice2java.df.xml")

        // Dictionary of A->[B] where A is the source set name and B is
        // the JavaSourceSet
        def sourceSet = [:]

        // Dictionary of A->[B] where B depends on A for the java task.
        def s2jDependencies = [:]

        def state = new JavaBuildState()
        state.read(stateFile)

        project.slice.java.each {
            it.args = it.args.stripIndent()

            if (it.files == null) {
                it.files = project.fileTree(dir: it.srcDir).include('**/*.ice')
            }

            it.files.each {
                if(files.contains(it)) {
                    throw new GradleException("${it}: input file specified in multiple source sets")
                }
                files.add(it)
            }

            if(!it.files.isEmpty()) {
                s2jDependencies << getS2JDependencies(it)
            }
        }

        project.slice.java.each {
            processJavaSet(it, s2jDependencies, state, generated, built, sourceSet)
        }

        if(built.isEmpty() && !stateFile.isFile()) {
            // The state file has never been written and and we have no
            // files to process. No reason to continue as it will only
            // write an empty dependency file.
            LOGGER.info("nothing to do")
            return
        }

        // The set of generated files to remove.
        Set oldGenerated = []

        // Add all of the previously generated files from the slice files that were
        // just built in processJavaSet.
        built.each {
            def d = state.slice[it]
            if(d != null) {
                oldGenerated.addAll(d)
            }
        }

        // Add to the oldGenerated list the generated files for those slice files
        // no longer are in any source set.
        state.slice.each {
            if(!files.contains(it.key)) {
                oldGenerated.addAll(it.value)
            }
        }

        // Remove all generated files that were just generated in processJavaSet.
        generated.values().each {
            oldGenerated.removeAll(it)
        }

        def newState = new JavaBuildState()
        newState.timestamp = System.currentTimeMillis()
        newState.sourceSet = sourceSet
        // Update the dependencies.
        built.each {
            newState.slice[it] = generated[it]
        }
        files.each {
            if(!built.contains(it)) {
                newState.slice[it] = state.slice[it]
            }
        }

        // Write the new dependencies file.
        newState.write(stateFile)

        deleteFiles(oldGenerated)
    }

    def processJavaSet(java, s2jDependencies, state, generated, built, sourceSet) {
        def ss = new JavaSourceSet()
        ss.args = buildS2JCommandLine(java)
        java.files.each {
            ss.slice.add(it)
        }
        sourceSet[java.name] = ss

        // The JavaSourceSet from the previous build.
        def prevSS = state.sourceSet[java.name]

        Set toBuild = []

        // If the source set is new, the sourceSet arguments are different,
        // or the slice2java compiler (always the first argument) has been updated,
        // then rebuild all slice files.
        if(prevSS == null || ss.args != prevSS.args || getTimestamp(new File(ss.args[0])) > state.timestamp) {
            java.files.each {
                toBuild.add(it)
            }
        } else {
            // s2jDependencies is populated in getInputFiles.
            java.files.each { sliceFile ->
                // Build the slice file if it wasn't built before in this source set,
                // or its timestamp is newer than the last build time,
                // or any of its dependencies have a timestamp newer than the last build time.
                if(!prevSS.slice.contains(sliceFile) || (getTimestamp(sliceFile) > state.timestamp) ||
                    s2jDependencies[sliceFile].any { dependency ->
                        getTimestamp(dependency) > state.timestamp
                    }) {
                        toBuild.add(sliceFile)
                }
                //
                // Check that any previously generated files still exist
                //
                else if(state.slice[sliceFile] != null && state.slice[sliceFile].any { generatedFile ->
                        !generatedFile.isFile() || getTimestamp(generatedFile) > state.timestamp
                    }) {
                    toBuild.add(sliceFile)
                }
            }
        }

        // Bail out if there is nothing to do (in theory this should not occur)
        if(toBuild.isEmpty()) {
            LOGGER.info("nothing to do")
            return
        }

        LOGGER.info("running slice2java on the following slice files")
        toBuild.each {
            LOGGER.info("    ${it}")
        }

        // Update the set of java source files generated and the slice files processed.
        generated << executeS2J(java, toBuild)
        built.addAll(toBuild)
    }

    def buildS2JCommandLine(java) {
        def command = []
        command.add(project.slice.slice2java)
        if (project.slice.sliceDir) {
            command.add("-I${project.slice.sliceDir}")
        }

        java.include.each {
            command.add('-I' + it)
        }

        java.args.split().each {
            command.add(it)
        }

        if(project.slice.compat) {
            command.add('--compat')
        }

        //
        // If the ice version is less than 3.7 it is still possible that we are compiling
        // 3.7 slice files.
        //
        // compareIceVersion returns -1 if iceVersion < 3.7
        //
        if(project.slice.compareIceVersion('3.7') == -1) {
            command.add('-D__SLICE2JAVA_COMPAT__')
        }

        return command
    }

    // Run slice2java. Returns a dictionary of A -> [B] where A is a slice file,
    // and B is the list of produced java source files.
    def executeS2J(java, files) {
        def command = buildS2JCommandLine(java)
        command.add("--list-generated")
        command.add("--output-dir=" + project.slice.output.getAbsolutePath())
        files.each {
            command.add(it.getAbsolutePath())
        }

        LOGGER.info("processing slice:\n${command}")

        def sout = new StringBuffer()
        def serr = new StringBuffer()

        def p = command.execute(project.slice.env, null)
        p.waitForProcessOutput(sout, serr)

        printWarningsAndErrors(serr, sout)

        if (p.exitValue() != 0) {
            throw new GradleException("${command[0]} failed with exit code: ${p.exitValue()}")
        }
        return parseGeneratedXML(new XmlSlurper().parseText(sout.toString()))
    }

    // Executes slice2java to determine the slice file dependencies.
    // Returns a dictionary of A  -> [B] where A depends on B.
    def getS2JDependencies(java) {
        def command = buildS2JCommandLine(java)
        command.add("--depend-xml")
        java.files.each {
            command.add(it.getAbsolutePath() )
        }

        LOGGER.info("processing dependencies:\n${command}")

        def sout = new StringBuffer()
        def serr = new StringBuffer()

        def p = command.execute(project.slice.env, null)
        p.waitForProcessOutput(sout, serr)

        printWarningsAndErrors(serr)

        if (p.exitValue() != 0) {
            throw new GradleException("${command[0]} failed with exit code: ${p.exitValue()}")
        }

        LOGGER.info("processing dependencies:\n${command}")

        return parseSliceDependencyXML(new XmlSlurper().parseText(sout.toString()))
    }

    // Cache of file -> timestamp.
    def timestamps = [:]

    // Get the last modified time for the file. Note that this time is in ms.
    def getTimestamp(file) {
        if(timestamps.containsKey(file)) {
            return timestamps[file]
        }

        if(!file.isFile()) {
            throw new GradleException("${file}: cannot stat")
        }

        def t = file.lastModified()
        timestamps[file] = t
        return t
    }

    class JavaSourceSet {
        // List of slice files.
        def slice = []

        // List of arguments.
        def args = []
    }

    // Process the written slice file which is of the format:
    //
    // <state>
    //   <timestamp value="xxxx"/>
    //   <sourceSet name="default">
    //     <source name="A.ice">
    //       <file name="Demo/Foo.java"/>
    //     </source>
    //     <source name="Hello.ice"/>
    //     <arg value="-I."/>
    //   </sourceSet>
    // </state>
    //
    class JavaBuildState {
        // Timestamp of last build in milliseconds.
        def timestamp

        // List of slice files. Dictionary of A -> [B] where B is the list of generated java
        // source files.
        def slice = [:]

        // Dictionary of source set -> JavaSourceSet
        def sourceSet = [:]

        def write(stateFile) {

            def writer = new StringWriter()
            def xml = new MarkupBuilder(writer)
            xml.state {
                xml.timestamp("value": timestamp)
                sourceSet.each {
                    def key = it.key
                    def value = it.value
                    xml.sourceSet("name": key) {
                        value.slice.each {
                            def file = it
                            xml.source("name": file) {
                                slice[file].each {
                                    xml.file("name": it)
                                }
                            }
                        }
                        value.args.each {
                            xml.arg("value": it)
                        }
                    }
                }
            }

            stateFile.write(writer.toString())
        }

        def read(stateFile) {
            if(!stateFile.isFile()) {
                return
            }

            try {
                def xml = new XmlSlurper().parse(stateFile)
                if(xml.name() != "state") {
                    throw new GradleException("malformed XML: expected `state'")
                }

                xml.children().each {
                    if(it.name() == "timestamp") {
                        timestamp = it.attributes().get("value").toLong()
                    } else if(it.name() == "sourceSet") {
                        def ss = new JavaSourceSet()
                        def name = it.attributes().get("name")
                        it.children().each {
                            if(it.name() == "arg") {
                                ss.args.add(it.attributes().get("value"))
                            } else if(it.name() == "source") {

                                def source = new File(it.attributes().get("name"))
                                def files = []
                                it.children().each {
                                    if(it.name() == "file") {
                                        files.add(new File(it.attributes().get("name")))
                                    }
                                }
                                slice[source] = files
                                ss.slice.add(source)
                            }
                        }
                        sourceSet[name] = ss
                    }
                }
            }
            catch(Exception ex) {
                LOGGER.error("invalid XML: ${stateFile}")
                LOGGER.error(ex)
            }
        }
    }

    // Process the generated XML which is of the format:
    //
    // <generated>
    //   <source name="A.ice">
    //     <file name="Demo/_HelloOperations.java"/>
    //   </source>
    // </generated>
    def parseGeneratedXML(xml) {
        if(xml.name() != "generated") {
            throw new GradleException("malformed XML: expected `generated'")
        }

        def generated =[:]
        xml.children().each {
            if(it.name() == "source") {
                def source = it.attributes().get("name")
                def files = []
                it.children().each {
                    if(it.name() == "file") {
                        files.add(new File(it.attributes().get("name")))
                    }
                }
                generated.put(new File(source), files)

            }
        }
        return generated
    }

    // Parse the dependency XML which is of the format:
    //
    // <dependencies>
    //   <source name="A.ice">
    //     <dependsOn name="Hello.ice"/>
    //   </source>
    //   <source name="Hello.ice">
    //   </source>
    // </dependencies>
    def parseSliceDependencyXML(xml) {
        if(xml.name() != "dependencies") {
            throw new GradleException("malformed XML")
        }

        def dependencies =[:]
        xml.children().each {
            if(it.name() == "source") {
                def source = it.attributes().get("name")
                def files = []
                it.children().each {
                    if(it.name() == "dependsOn") {
                        def dependsOn = new File(it.attributes().get("name"))
                        files.add(dependsOn)
                    }
                }
                dependencies.put(new File(source), files)
            }
        }
        return dependencies
    }

    def deleteFiles(files) {
        if(!files.isEmpty()) {
            LOGGER.info("the following generated java source files will be deleted")
            files.each {
                LOGGER.info("    ${it}")
            }

            String buildDirPath = project.slice.output.getPath()
            files.each {
                String parent = it.getParent()
                if(!parent.startsWith(buildDirPath)) {
                    LOGGER.info("not removing ${it} as it is outside the build dir ${buildDirPath}")
                } else {
                    it.delete()
                }
            }
        }
    }

    def printWarningsAndErrors(serr, sout = null) {
        def lineSep = System.getProperty("line.separator")
        def errLines = serr.toString().split(lineSep).findAll { !it.trim().isEmpty() }
        def outLines = []

        //
        // stdout is always xml
        //
        if(sout) {
            def xml = new XmlSlurper().parseText(sout.toString())
            if(xml.name() != "generated") {
                throw new GradleException("malformed XML: expected `generated'")
            }

            xml.children().each {
                if(it.name() == "source") {
                    it.children().each {
                        if(it.name() == "output") {
                            def lines = it.text().split(lineSep).findAll { !it.trim().isEmpty() }
                            outLines.addAll(lines)
                        }
                    }
                }
            }
        }

        def warningMatch = /(.*):[0-9]+:\s+warning:(.*)/

        for(line in errLines + outLines) {
            switch(line) {
                case ~warningMatch:
                    LOGGER.warn(line.trim())
                    break
                default:
                    // These should all be errors
                    LOGGER.error(line.trim())
                    break
            }
        }
    }
}
