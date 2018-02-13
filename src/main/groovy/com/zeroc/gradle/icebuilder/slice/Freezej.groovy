// **********************************************************************
//
// Copyright (c) 2014-2018 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

import org.gradle.api.NamedDomainObjectContainer

class Freezej {
    final NamedDomainObjectContainer<Dict> dict;
    final NamedDomainObjectContainer<Index> index;

    def name
    def args = ""
    def files
    def srcDir
    def include

    Freezej(dict, index) {
        this.dict = dict
        this.index = index
    }

    def dict(Closure closure) {
        dict.configure(closure)
    }
    def index(Closure closure) {
        index.configure(closure)
    }
}
