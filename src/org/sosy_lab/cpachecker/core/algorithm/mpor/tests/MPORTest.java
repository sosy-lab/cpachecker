// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.mpor.DirectedGraph;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORTest {

  public MPORTest() {}

  @Test
  public void testSeqCompilable() {
    ImmutableList<String> programPaths =
        ImmutableList.of(
            "test/programs/mpor_seq/compilable_test/fib_safe-5.i",
            "test/programs/mpor_seq/compilable_test/queue_longest.i",
            "test/programs/mpor_seq/compilable_test/singleton-b.i",
            "test/programs/mpor_seq/compilable_test/ring_2w1r-2.i",
            "test/programs/mpor_seq/compilable_test/divinefifo-bug_1w1r.i");
    for (String programPath : programPaths) {
      testCompilable(programPath);
    }
    // delete the seqs again after testing (only files, not folders)
    deleteFilesInFolder("test/programs/mpor_seq/");
  }

  private void testCompilable(String pProgramPath) {
    String command =
        "./scripts/cpa.sh --predicateAnalysis --option analysis.algorithm.MPOR=true " + pProgramPath;
    try {
      // run the command with ProcessBuilder
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true); // Combine stdout and stderr
      Process process = processBuilder.start();
      // wait for command to complete and make sure that MPOR succeeds
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        fail();
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      fail();
    }
  }

  private void deleteFilesInFolder(String pFolderPath) {
    File directory = new File(pFolderPath);
    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          // check if it's a file (not a directory) and delete it
          if (file.isFile()) {
            if (!file.delete()) {
              fail("Failed to delete file: " + file.getName());
            }
          }
        }
      } else {
        fail("Failed to list files in directory: " + pFolderPath);
      }
    } else {
      fail("The provided path is not a directory.");
    }
  }

  @Test
  public void testDirectedGraphSccs() {
    DirectedGraph<Integer> directedGraph = new DirectedGraph<>();
    directedGraph.addNode(0);
    directedGraph.addNode(1);
    directedGraph.addNode(2);
    directedGraph.addNode(3);
    directedGraph.addNode(4);
    directedGraph.addEdge(0, 1);
    directedGraph.addEdge(0, 2);
    directedGraph.addEdge(1, 2);
    directedGraph.addEdge(2, 3);
    directedGraph.addEdge(3, 4);
    directedGraph.addEdge(4, 3);
    ImmutableSet<ImmutableSet<Integer>> sccs = directedGraph.computeSCCs();
    ImmutableSet<Integer> maximalScc = sccs.iterator().next();
    assertTrue(maximalScc.contains(3) && maximalScc.contains(4));
  }

  @Test
  public void testDirectedGraphTwoNodeCycle() {
    DirectedGraph<Integer> directedGraphA = new DirectedGraph<>();
    directedGraphA.addNode(0);
    directedGraphA.addNode(1);
    directedGraphA.addNode(2);
    directedGraphA.addEdge(0, 1);
    directedGraphA.addEdge(1, 0);
    assertTrue(directedGraphA.containsCycle());
  }

  @Test
  public void testDirectedGraphMultipleNodeCycle() {
    DirectedGraph<Integer> directedGraphB = new DirectedGraph<>();
    directedGraphB.addNode(0);
    directedGraphB.addNode(1);
    directedGraphB.addNode(2);
    directedGraphB.addNode(3);
    directedGraphB.addNode(4);
    directedGraphB.addEdge(0, 1);
    directedGraphB.addEdge(0, 2);
    directedGraphB.addEdge(1, 2);
    directedGraphB.addEdge(2, 3);
    directedGraphB.addEdge(2, 4);
    directedGraphB.addEdge(4, 0);
    assertTrue(directedGraphB.containsCycle());
  }

  @Test
  public void testDirectedGraphNoCycles() {
    DirectedGraph<Integer> directedGraphC = new DirectedGraph<>();
    directedGraphC.addNode(0);
    directedGraphC.addNode(1);
    directedGraphC.addNode(2);
    directedGraphC.addNode(3);
    directedGraphC.addNode(4);
    directedGraphC.addEdge(0, 1);
    directedGraphC.addEdge(0, 2);
    directedGraphC.addEdge(1, 2);
    directedGraphC.addEdge(2, 3);
    directedGraphC.addEdge(2, 4);
    assertFalse(directedGraphC.containsCycle());
  }
}
