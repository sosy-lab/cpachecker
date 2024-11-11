// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.directed_graph;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class DirectedGraphTest {

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
