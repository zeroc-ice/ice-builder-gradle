//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

package com.zeroc.gradle.icebuilder.slice

import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assume.assumeTrue

class SlicePluginWarningTest extends TestCase {

    @Test
    public void testWarning() {

        pathToFile([project.rootDir, 'src', 'main', 'slice']).mkdirs()

        writeTestSliceToFile(pathToFile([project.rootDir, 'src', 'main', 'slice', 'Test.ice']))

        project.tasks.compileSlice.execute()
    }

    private void writeTestSliceToFile(file) {
        file << """
           |module Test
           |{
           |
           |["java:buffer"] sequence<string> TestSeq;
           |
           |};
        """.stripMargin()
    }
}
