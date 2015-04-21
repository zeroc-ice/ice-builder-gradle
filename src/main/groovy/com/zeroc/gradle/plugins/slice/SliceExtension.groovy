// **********************************************************************
//
// Copyright (c) 2014-2015 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.plugins.slice;

import org.gradle.api.NamedDomainObjectContainer

class SliceExtension {
    final NamedDomainObjectContainer<Java> java;
    def iceHome = null
    def srcDist = false
    def output

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
