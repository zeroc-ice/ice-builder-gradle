// **********************************************************************
//
// Copyright (c) 2014-2016 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.NamedDomainObjectContainer

class SliceExtension {
    final NamedDomainObjectContainer<Java> java;
    def iceHome = null
    def freezeHome = null
    def srcDist = false
    def output

    def cppPlatform = null
    def cppConfiguration = null

    SliceExtension(java) {
        this.java = java
    }

    def java(Closure closure) {
        try {
            java.configure(closure)
        }
        catch(MissingPropertyException ex) {
            java.create('default', closure)
        }
    }
}
