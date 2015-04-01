// **********************************************************************
//
// Copyright (c) 2014-2015 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.plugins.slice;

class Java {
    def name
    def args = ""
    def files
    def srcDir = "src/main/slice"
    def include

    Java(String n) {
        name = n;
    }
}
