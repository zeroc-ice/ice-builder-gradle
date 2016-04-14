// **********************************************************************
//
// Copyright (c) 2014-2016 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.GradleException

class SliceExtension {
    final NamedDomainObjectContainer<Java> java;
    def iceHome = null
    def iceVersion = null
    def srcDist = false
    def freezeHome = null
    def sliceDir = null
    def slice2java = null
    def slice2freezej = null
    def jarDir = null
    def cppPlatform = null
    def cppConfiguration = null
    def env = []
    def output

    static Configuration configuration = null

    class Configuration {
        def iceHome = null
        def iceVersion = null
        def srcDist = false
        def freezeHome = null
        def sliceDir = null
        def slice2java = null
        def slice2freezej = null
        def jarDir = null
        def cppPlatform = null
        def cppConfiguration = null
        def env = []

        Configuration(iceHome = null) {
            this.iceHome = iceHome ? iceHome : getIceHome();

            def os = System.properties['os.name']

            if(this.iceHome != null) {
                srcDist = new File([this.iceHome, "java", "build.gradle"].join(File.separator)).exists()
                slice2java = getSlice2java(this.iceHome)

                //
                // If freezeHome is not set we assume slice2freezej resides in the same location than slice2java
                // otherwise slice2freezej will be located in the freeze home bin directory.
                //
                if (freezeHome == null) {
                    slice2freezej = [new File(slice2java).getParent(), "slice2freezej"].join(File.separator)
                } else {
                    if(new File([freezeHome, "bin"].join(File.separator)).exists()) {
                        slice2freezej = [freezeHome, "bin", "slice2freezej"].join(File.separator)
                    } else {
                        slice2freezej = [freezeHome, "cpp", "bin", "slice2freezej"].join(File.separator)
                    }
                }

                //
                // Setup the environment required to run slice2java/slice2freezej commands
                //
                if (os == "Mac OS X") {
                    def libdir = "${this.iceHome}/lib"
                    env = ["DYLD_LIBRARY_PATH=${[libdir, System.env.DYLD_LIBRARY_PATH].join(File.pathSeparator)}"]
                } else if (!os.contains("Windows")) {
                    def libdir = new File("${this.iceHome}/lib/i386-linux-gnu").exists() ?
                        "${this.iceHome}/lib/i386-linux-gnu" : "${this.iceHome}/lib"
                    def lib64dir = new File("${this.iceHome}/lib/x86_64-linux-gnu").exists() ?
                        "${this.iceHome}/lib/x86_64-linux-gnu" : "${this.iceHome}/lib64"
                    env = ["LD_LIBRARY_PATH=${[libdir, lib64dir, System.env.LD_LIBRARY_PATH].join(File.pathSeparator)}"]
                }

                //
                // Retrieve the version of the Ice distribution being used
                //
                iceVersion = getIceVersion(this.iceHome)

                //
                // Guess the slice and jar directories of the Ice distribution we are using
                //
                if(this.iceHome in ["/usr", "/usr/local"]) {
                    sliceDir = [this.iceHome, "share", "Ice-${iceVersion}", "slice"].join(File.separator)
                    jarDir = [this.iceHome, "share", "java"].join(File.separator)
                } else {
                    sliceDir = [this.iceHome, "slice"].join(File.separator)
                    jarDir = srcDist ?
                        [this.iceHome, "java", "lib"].join(File.separator) :
                        [this.iceHome, "lib"].join(File.separator)
                }
            }
        }

        def getIceHome() {
            if(System.env.ICE_HOME != null) {
                return System.env.ICE_HOME
            }

            def os = System.properties['os.name']
            if (os == "Mac OS X") {
                return "/usr/local"
            } else if (os.contains("Windows")) {
                return getWin32IceHome()
            } else {
                return "/usr"
            }
        }

        //
        // Query Win32 registry key and return the InstallDir value for the given key
        //
        def getWin32InstallDir(key) {
            def sout = new StringBuffer()
            def serr = new StringBuffer()
            def p = ["reg", "query", key, "/v", "InstallDir"].execute()
            p.waitForProcessOutput(sout, serr)
            if (p.exitValue() != 0) {
                return null
            }
            return sout.toString().split("    ")[3].trim()
        }

        //
        // Query Win32 registry and return the path of the latest Ice version available.
        //
        def getWin32IceHome() {
            def sout = new StringBuffer()
            def serr = new StringBuffer()

            def p = ["reg", "query", "HKLM\\Software\\ZeroC"].execute()
            p.waitForProcessOutput(sout, serr)
            if (p.exitValue() != 0) {
                println serr.toString()
                throw new GradleException("reg command failed: ${p.exitValue()}")
            }

            def iceInstallDir = null
            def iceVersion = null

            sout.toString().split("\\r?\\n").each {
                if (it.indexOf("HKEY_LOCAL_MACHINE\\Software\\ZeroC\\Ice") != -1) {
                    def installDir = getWin32InstallDir(it)
                    if (installDir != null) {
                        def version = getIceVersion(installDir).split("\\.")
                        if (version.length == 3) {
                            //
                            // Check if version is greater than current version
                            //
                            if (iceVersion == null || version[0] > iceVersion[0] ||
                                (version[0] == iceVersion[0] && version[1] > iceVersion[1]) ||
                                (version[0] == iceVersion[0] && version[1] == iceVersion[1] &&
                                 version[2] > iceVersion[2])) {
                                iceInstallDir = installDir
                                iceVersion = version
                            }
                        }
                    }
                }
            }
            return iceInstallDir
        }

        def getIceVersion(iceHome) {
            def command = [getSlice2java(iceHome), "--version"]
            def sout = new StringBuffer()
            def serr = new StringBuffer()
            def p = command.execute(env, null)
            p.waitForProcessOutput(sout, serr)
            if (p.exitValue() != 0) {
                println serr.toString()
                throw new GradleException("${command[0]} command failed: ${p.exitValue()}")
            }
            return serr.toString().trim()
        }

        def getSlice2java(iceHome) {
            def os = System.properties['os.name']
            //
            // Check if we are using a Slice source distribution
            //
            def srcDist = new File([iceHome, "java", "build.gradle"].join(File.separator)).exists()
            def slice2java = null
            //
            // Set the location of the slice2java executable
            //
            if (os.contains("Windows")) {
                if (srcDist) {
                    //
                    // Guess the cpp platform to use with Windows source builds
                    //
                    if (cppPlatform == null) {
                        cppPlatform = System.getenv("CPP_PLATFORM")
                    }

                    //
                    // Gues the cpp configuration to use with Windows source builds
                    //
                    if (cppConfiguration == null) {
                        cppConfiguration = System.getenv("CPP_CONFIGURATION")
                    }

                    //
                    // Ice >= 3.7 Windows source distribution, the slice2java compiler is located in the platform
                    // configuration depend directory. Otherwise cppPlatform and cppConfiguration will be null and
                    // it will fallback to the common bin directory used with Ice < 3.7.
                    //
                    if (cppPlatform != null && cppConfiguration != null) {
                        slice2java = [iceHome, "bin", cppPlatform, cppConfiguration, "slice2java.exe"].join(File.separator)
                    }
                } else {
                    //
                    // With Ice >= 3.7 Windows binary distribution we use the slice2java compiler Win32/Release
                    // bin directory. We assume that if the file exists at this location we are using Ice >= 3.7
                    // distribution otherwise it will fallback to the common bin directory used with Ice < 3.7.
                    //
                    def path = [iceHome, "build", "native", "bin", "Win32", "Release", "slice2java.exe"].join(File.separator)
                    if (new File(path).exists()) {
                        slice2java = path
                    }
                }
            }

            if (slice2java == null) {
                slice2java = srcDist ?
                    [iceHome, "cpp", "bin", "slice2java"].join(File.separator) :
                    [iceHome, "bin", "slice2java"].join(File.separator)
            }

            return slice2java
        }
    }

    SliceExtension(java) {
        this.java = java
        Configuration c = null
        if (iceHome != null) {
            if (configuration == null) {
                configuration = new Configuration()
            }
            c = configuration
        } else {
            c = new Configuration(iceHome)
        }

        iceHome = c.iceHome
        iceVersion = c.iceVersion
        srcDist = c.srcDist
        freezeHome = c.freezeHome
        sliceDir = c.sliceDir
        slice2java = c.slice2java
        slice2freezej = c.slice2freezej
        jarDir = c.jarDir
        cppPlatform = c.cppPlatform
        cppConfiguration = c.cppConfiguration
        env = c.env
    }

    def java(Closure closure) {
        try {
            java.configure(closure)
        } catch(MissingPropertyException ex) {
            java.create('default', closure)
        }
    }
}
