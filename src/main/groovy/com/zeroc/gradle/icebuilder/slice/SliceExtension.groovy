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

    private def iceHome = null
    private def iceVersion = null
    private def srcDist = false
    private def freezeHome = null
    private def sliceDir = null
    private def slice2java = null
    private def slice2freezej = null
    private def jarDir = null
    private def cppPlatform = null
    private def cppConfiguration = null

    private def env = []
    def output

    private static Configuration configuration = null

    class Configuration {

        def _iceHome = null
        def _iceVersion = null
        def _srcDist = false
        def _freezeHome = null
        def _sliceDir = null
        def _slice2java = null
        def _slice2freezej = null
        def _jarDir = null
        def _cppPlatform = null
        def _cppConfiguration = null
        def _env = []

        Configuration(iceHome = null, cppPlatform = null, cppConfiguration = null) {
            _iceHome = iceHome ? iceHome : getIceHome();
            _cppPlatform = cppPlatform
            _cppConfiguration = cppConfiguration

            def os = System.properties['os.name']

            if(this.iceHome != null) {
                _srcDist = new File([_iceHome, "java", "build.gradle"].join(File.separator)).exists()
                _slice2java = getSlice2java(_iceHome)

                //
                // If freezeHome is not set we assume slice2freezej resides in the same location than slice2java
                // otherwise slice2freezej will be located in the freeze home bin directory.
                //
                if (freezeHome == null) {
                    _slice2freezej = [new File(_slice2java).getParent(), "slice2freezej"].join(File.separator)
                } else {
                    if(new File([_freezeHome, "bin"].join(File.separator)).exists()) {
                        _slice2freezej = [_freezeHome, "bin", "slice2freezej"].join(File.separator)
                    } else {
                        _slice2freezej = [_freezeHome, "cpp", "bin", "slice2freezej"].join(File.separator)
                    }
                }

                //
                // Setup the environment required to run slice2java/slice2freezej commands
                //
                if (os == "Mac OS X") {
                    def libdir = "${_iceHome}/lib"
                    _env = ["DYLD_LIBRARY_PATH=${[libdir, System.env.DYLD_LIBRARY_PATH].join(File.pathSeparator)}"]
                } else if (!os.contains("Windows")) {
                    def libdir = new File("${_iceHome}/lib/i386-linux-gnu").exists() ?
                        "${_iceHome}/lib/i386-linux-gnu" : "${_iceHome}/lib"
                    def lib64dir = new File("${_iceHome}/lib/x86_64-linux-gnu").exists() ?
                        "${_iceHome}/lib/x86_64-linux-gnu" : "${_iceHome}/lib64"
                    _env = ["LD_LIBRARY_PATH=${[libdir, lib64dir, System.env.LD_LIBRARY_PATH].join(File.pathSeparator)}"]
                }

                //
                // Retrieve the version of the Ice distribution being used
                //
                _iceVersion = getIceVersion(_iceHome)

                //
                // Guess the slice and jar directories of the Ice distribution we are using
                //
                if(_iceHome in ["/usr", "/usr/local"]) {
                    _sliceDir = [_iceHome, "share", "Ice-${_iceVersion}", "slice"].join(File.separator)
                    _jarDir = [_iceHome, "share", "java"].join(File.separator)
                } else {
                    _sliceDir = [_iceHome, "slice"].join(File.separator)
                    _jarDir = _srcDist ?
                        [_iceHome, "java", "lib"].join(File.separator) :
                        [_iceHome, "lib"].join(File.separator)
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
            def p = command.execute(_env, null)
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
                    if (_cppPlatform == null) {
                        _cppPlatform = System.getenv("CPP_PLATFORM")
                    }

                    //
                    // Gues the cpp configuration to use with Windows source builds
                    //
                    if (_cppConfiguration == null) {
                        _cppConfiguration = System.getenv("CPP_CONFIGURATION")
                    }

                    //
                    // Ice >= 3.7 Windows source distribution, the slice2java compiler is located in the platform
                    // configuration depend directory. Otherwise cppPlatform and cppConfiguration will be null and
                    // it will fallback to the common bin directory used with Ice < 3.7.
                    //
                    if (_cppPlatform != null && _cppConfiguration != null) {
                        slice2java = [iceHome, "bin", _cppPlatform, _cppConfiguration, "slice2java.exe"].join(File.separator)
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
        init()
    }

    def java(Closure closure) {
        try {
            java.configure(closure)
        } catch(MissingPropertyException ex) {
            java.create('default', closure)
        }
    }

    private void init() {
        Configuration c = null
        if (iceHome) {
            c = new Configuration(iceHome)
        } else {
            if (configuration == null) {
                configuration = new Configuration()
            }
            c = configuration
        }

        iceHome = c._iceHome

        iceVersion = c._iceVersion
        srcDist = c._srcDist
        freezeHome = c._freezeHome
        sliceDir = c._sliceDir
        slice2java = c._slice2java
        slice2freezej = c._slice2freezej
        jarDir = c._jarDir
        cppPlatform = c._cppPlatform
        cppConfiguration = c._cppConfiguration
        env = c._env
    }

    def getIceHome() {
        return iceHome
    }

    def setIceHome(value) {
        iceHome = value
        init()
    }

    def getIceVersion() {
        return iceVersion
    }

    def getSrcDist() {
        return srcDist
    }

    def getFreezeHome() {
        return freezeHome
    }

    def getSliceDir() {
        return sliceDir
    }

    def getSlice2java() {
        return slice2java
    }

    def getSlice2freezej() {
        return slice2freezej
    }

    def getJarDir() {
        return jarDir
    }

    def getCppPlatform() {
        return cppPlatform
    }

    def setCppPlatform(value) {
        cppPlatform = value
        init()
    }

    def getCppConfiguration() {
        return cppConfiguration
    }

    def setCppConfiguration(value) {
        cppConfiguration = value
        init()
    }

    def getEnv() {
        return env
    }
}
