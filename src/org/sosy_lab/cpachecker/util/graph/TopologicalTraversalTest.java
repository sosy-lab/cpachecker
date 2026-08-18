// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.SuccessorsFunction;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;

/** Unit tests for {@link TopologicalTraversal}. */
public final class TopologicalTraversalTest {

  private static List<String> traverseToList(String root, SuccessorsFunction<String> edges) {
    return ImmutableList.copyOf(TopologicalTraversal.traverse(root, edges));
  }

  @Test
  public void singleNodeWithoutSuccessors_returnsOnlyRoot() {
    Multimap<String, String> adjacency = ImmutableListMultimap.of();

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void linearChain_isReturnedInTopologicalOrder() {
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "B", "C",
            "C", "D");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A", "B", "C", "D").inOrder();
  }

  @Test
  public void diamond_nodeIsVisitedOnceAfterBothPredecessors() {
    // A -> B, A -> C, B -> D, C -> D
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "A", "C",
            "B", "D",
            "C", "D");

    List<String> result = traverseToList("A", adjacency::get);

    // B and C are not ordered relative to each other by the graph, so only assert what the
    // topological contract guarantees: A first, D last, both B and C in between.
    assertThat(result).containsExactly("A", "B", "C", "D");
    assertThat(result).containsAtLeast("A", "B").inOrder();
    assertThat(result).containsAtLeast("A", "C").inOrder();
    assertThat(result).containsAtLeast("B", "D").inOrder();
    assertThat(result).containsAtLeast("C", "D").inOrder();
  }

  @Test
  public void triangle_nodeWaitsUntilAllPredecessorsAreVisited() {
    // A -> B, A -> C, B -> C  (C has two predecessors on different levels)
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "A", "C",
            "B", "C");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A", "B", "C").inOrder();
  }

  @Test
  public void nodesNotReachableFromRoot_areExcluded() {
    // A -> B, and a separate, unreachable component C -> D
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "C", "D");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A", "B");
  }

  @Test
  public void duplicateSuccessors_multiEdgeToSameNode_visitsNodeOnce() {
    // A -> B twice (parallel edge)
    Multimap<String, String> adjacency = ImmutableListMultimap.of("A", "B", "A", "B");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A", "B").inOrder();
  }

  @Test
  public void selfLoopOnRoot_visitsRootExactlyOnce() {
    // A -> A
    Multimap<String, String> adjacency = ImmutableListMultimap.of("A", "A");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void twoCycleIncludingRoot_onlyRootIsVisited() {
    // A -> B, B -> A : the cycle prevents B from ever reaching zero unvisited incoming edges
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "B", "A");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void cycleNotIncludingRoot_onlyPrefixBeforeCycleIsVisited() {
    // A -> B, B -> C, C -> B : B and C form a cycle downstream of root A
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "B", "C",
            "C", "B");

    List<String> result = traverseToList("A", adjacency::get);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void iteratorThrowsAfterExhaustion() {
    Multimap<String, String> adjacency = ImmutableListMultimap.of();

    Iterator<String> iterator = TopologicalTraversal.traverse("A", adjacency::get).iterator();

    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("A");
    assertThat(iterator.hasNext()).isFalse();
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  public void widerTree_allNodesVisitedExactlyOnceRespectingDependencies() {
    // A -> B, A -> C, B -> D, B -> E, C -> F
    Multimap<String, String> adjacency =
        ImmutableListMultimap.of(
            "A", "B",
            "A", "C",
            "B", "D",
            "B", "E",
            "C", "F");

    List<String> result = traverseToList("A", adjacency::get);

    // Several interleavings of the B- and C-subtrees are valid topological orders, so only
    // assert the node set plus the precedence constraints implied by the edges.
    assertThat(result).containsExactly("A", "B", "C", "D", "E", "F");
    assertThat(result).containsAtLeast("A", "B").inOrder();
    assertThat(result).containsAtLeast("A", "C").inOrder();
    assertThat(result).containsAtLeast("B", "D").inOrder();
    assertThat(result).containsAtLeast("B", "E").inOrder();
    assertThat(result).containsAtLeast("C", "F").inOrder();
  }
}
