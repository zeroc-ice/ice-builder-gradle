// **********************************************************************
//
// Copyright (c) 2014-present ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.icebuilder.slice;

class Dict {
    def name
    def javaType
    def key
    def value
    // list of dictionary values
    def index

    Dict(String n) {
        name = n
    }
}
