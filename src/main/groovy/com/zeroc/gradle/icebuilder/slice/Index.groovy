// Copyright (c) ZeroC, Inc. All rights reserved.

package com.zeroc.gradle.icebuilder.slice;

class Index {
    def name
    def javaType
    def type
    def member
    def caseSensitive = true

    Index(String n) {
        name = n
    }
}
