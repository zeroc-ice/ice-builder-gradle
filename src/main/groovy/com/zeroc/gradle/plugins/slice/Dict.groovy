// **********************************************************************
//
// Copyright (c) 2014-2015 ZeroC, Inc. All rights reserved.
//
// **********************************************************************

package com.zeroc.gradle.plugins.slice;

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
