// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.DirectedGraph;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORTest {

  // TODO these trigger an error where the return value assignment is empty
  // "singleton-b.i",
  // "fib_safe-5.i",
  // TODO this triggers a substitute not found because the pthread_create call passes
  //  a parameter to the start routine and the thread reads it
  // "ring_2w1r-2.i",
  // TODO this triggers a pthread_create loop error, even though its outside the loop
  // "divinefifo-bug_1w1r.i"

  public MPORTest() {}

  // TODO add more compile tests

  @Test
  public void testCompileSeqQueueLongest() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable_test/queue_longest.i");
    testCompile(path);
    deleteDir("./output");
  }

  @Test
  public void testCompileSeqStack() throws Exception {
    Path path = Path.of("./test/programs/mpor_seq/seq_compilable_test/stack-1.i");
    testCompile(path);
    deleteDir("./output");
  }

  private void testCompile(Path pInputFilePath) throws Exception {
    // create cfa for test program pFileName
    LogManager logger = LogManager.createTestLogManager();
    CFACreator creator =
        new CFACreator(Configuration.builder().build(), logger, ShutdownNotifier.createDummy());
    String program = Files.readString(pInputFilePath);
    CFA inputCfa = creator.parseSourceAndCreateCFA(program);

    // create seq with mpor algorithm
    MPORAlgorithm algorithm = new MPORAlgorithm(logger, inputCfa);
    String seq = algorithm.createSeq();

    // test that seq can be parsed and cfa created ==> code compiles
    CFA seqCfa = creator.parseSourceAndCreateCFA(seq);
    assertThat(seqCfa != null).isTrue();

    // "anti" test: just remove the last 100 chars from the seq, it probably won't compile
    String faultySeq = seq.substring(0, seq.length() - 100);

    // test that we get an exception while parsing the new "faulty" program
    Exception e = null;
    try {
      creator.parseSourceAndCreateCFA(faultySeq);
    } catch (Exception pE) {
      e = pE;
    }
    assertThat(e != null).isTrue();
  }

  private void deleteDir(String pDirPath) throws IOException {
    try (Stream<Path> paths = Files.walk(Path.of(pDirPath))) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException pE) {
                  throw new RuntimeException(pE);
                }
              });
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
    assertThat(maximalScc.contains(3) && maximalScc.contains(4)).isTrue();
  }

  @Test
  public void testDirectedGraphTwoNodeCycle() {
    DirectedGraph<Integer> directedGraphA = new DirectedGraph<>();
    directedGraphA.addNode(0);
    directedGraphA.addNode(1);
    directedGraphA.addNode(2);
    directedGraphA.addEdge(0, 1);
    directedGraphA.addEdge(1, 0);
    assertThat(directedGraphA.containsCycle()).isTrue();
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
    assertThat(directedGraphB.containsCycle()).isTrue();
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
    assertThat(directedGraphC.containsCycle()).isFalse();
  }
}
