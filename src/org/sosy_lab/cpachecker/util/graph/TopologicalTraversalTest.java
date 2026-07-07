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
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.SuccessorsFunction;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.Test;

/** Unit tests for {@link TopologicalTraversal}. */
public final class TopologicalTraversalTest {

  /** Builds a {@link SuccessorsFunction} from an adjacency map, defaulting to no successors. */
  private static SuccessorsFunction<String> successorsOf(Map<String, List<String>> adjacency) {
    return node -> adjacency.getOrDefault(node, ImmutableList.of());
  }

  private static List<String> traverseToList(String root, SuccessorsFunction<String> edges) {
    return ImmutableList.copyOf(TopologicalTraversal.traverse(root, edges));
  }

  /**
   * Asserts only what the topological-order contract actually guarantees: that {@code predecessor}
   * appears before {@code successor}. Does not assume any particular order among nodes that are not
   * related by a path.
   */
  private static void assertPrecedes(List<String> order, String predecessor, String successor) {
    int predecessorIndex = order.indexOf(predecessor);
    int successorIndex = order.indexOf(successor);
    assertThat(predecessorIndex).isAtLeast(0);
    assertThat(successorIndex).isAtLeast(0);
    assertThat(predecessorIndex).isLessThan(successorIndex);
  }

  @Test
  public void singleNodeWithoutSuccessors_returnsOnlyRoot() {
    SuccessorsFunction<String> edges = successorsOf(ImmutableMap.of());

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void linearChain_isReturnedInTopologicalOrder() {
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B"),
            "B", ImmutableList.of("C"),
            "C", ImmutableList.of("D"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A", "B", "C", "D").inOrder();
  }

  @Test
  public void diamond_nodeIsVisitedOnceAfterBothPredecessors() {
    // A -> B, A -> C, B -> D, C -> D
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B", "C"),
            "B", ImmutableList.of("D"),
            "C", ImmutableList.of("D"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    // B and C are not ordered relative to each other by the graph, so only assert what the
    // topological contract guarantees: A first, D last, both B and C in between.
    assertThat(result).containsExactly("A", "B", "C", "D");
    assertPrecedes(result, "A", "B");
    assertPrecedes(result, "A", "C");
    assertPrecedes(result, "B", "D");
    assertPrecedes(result, "C", "D");
  }

  @Test
  public void triangle_nodeWaitsUntilAllPredecessorsAreVisited() {
    // A -> B, A -> C, B -> C  (C has two predecessors on different levels)
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B", "C"),
            "B", ImmutableList.of("C"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A", "B", "C").inOrder();
  }

  @Test
  public void nodesNotReachableFromRoot_areExcluded() {
    // A -> B, and a separate, unreachable component C -> D
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B"),
            "C", ImmutableList.of("D"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A", "B");
  }

  @Test
  public void duplicateSuccessors_multiEdgeToSameNode_visitsNodeOnce() {
    // A -> B twice (parallel edge)
    Map<String, List<String>> adjacency = ImmutableMap.of("A", ImmutableList.of("B", "B"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A", "B").inOrder();
  }

  @Test
  public void selfLoopOnRoot_visitsRootExactlyOnce() {
    // A -> A
    Map<String, List<String>> adjacency = ImmutableMap.of("A", ImmutableList.of("A"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void twoCycleIncludingRoot_onlyRootIsVisited() {
    // A -> B, B -> A : the cycle prevents B from ever reaching zero unvisited incoming edges
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B"),
            "B", ImmutableList.of("A"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void cycleNotIncludingRoot_onlyPrefixBeforeCycleIsVisited() {
    // A -> B, B -> C, C -> B : B and C form a cycle downstream of root A
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B"),
            "B", ImmutableList.of("C"),
            "C", ImmutableList.of("B"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    assertThat(result).containsExactly("A");
  }

  @Test
  public void traverseReturnsFreshIterableEachTime() {
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B"),
            "B", ImmutableList.of("C"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    Iterable<String> traversal = TopologicalTraversal.traverse("A", edges);

    assertThat(ImmutableList.copyOf(traversal)).containsExactly("A", "B", "C").inOrder();
    // Iterating a second time must produce the same, complete sequence again.
    assertThat(ImmutableList.copyOf(traversal)).containsExactly("A", "B", "C").inOrder();
  }

  @Test
  public void iteratorThrowsAfterExhaustion() {
    SuccessorsFunction<String> edges = successorsOf(ImmutableMap.of());

    Iterator<String> iterator = TopologicalTraversal.traverse("A", edges).iterator();

    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("A");
    assertThat(iterator.hasNext()).isFalse();
    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  public void widerTree_allNodesVisitedExactlyOnceRespectingDependencies() {
    // A -> B, A -> C, B -> D, B -> E, C -> F
    Map<String, List<String>> adjacency =
        ImmutableMap.of(
            "A", ImmutableList.of("B", "C"),
            "B", ImmutableList.of("D", "E"),
            "C", ImmutableList.of("F"));
    SuccessorsFunction<String> edges = successorsOf(adjacency);

    List<String> result = traverseToList("A", edges);

    // Several interleavings of the B- and C-subtrees are valid topological orders, so only
    // assert the node set plus the precedence constraints implied by the edges.
    assertThat(result).containsExactly("A", "B", "C", "D", "E", "F");
    assertPrecedes(result, "A", "B");
    assertPrecedes(result, "A", "C");
    assertPrecedes(result, "B", "D");
    assertPrecedes(result, "B", "E");
    assertPrecedes(result, "C", "F");
  }
}
