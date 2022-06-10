// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.Test;

/** Tests for dominator trees ({@link DomTree}) and dominance frontiers ({@link DomFrontiers}). */
public final class DominanceTest {

  private static final String ENTRY_NODE = "ENTRY";
  private static final String EXIT_NODE = "EXIT";
  private static final String UNKNOWN_NODE = "UNKNOWN";

  private static <N> void assertDirectedAndNoSelfLoops(Graph<N> pGraph) {
    assertWithMessage("Expected the graph to be directed.").that(pGraph.isDirected()).isTrue();
    assertWithMessage("Expected the graph to forbid self-loops.")
        .that(pGraph.allowsSelfLoops())
        .isFalse();
  }

  private static <N> void assertHasNextElement(Iterator<N> pIterator) {
    assertWithMessage("Expected that the iterator has a next element.")
        .that(pIterator.hasNext())
        .isTrue();
  }

  private static <N> void assertNoNextElement(Iterator<N> pIterator) {
    assertWithMessage("Expected that the iterator has no more elements.")
        .that(pIterator.hasNext())
        .isFalse();
    assertThrows(NoSuchElementException.class, () -> pIterator.next());
  }

  private static <N> void assertRemoveUnsupported(Iterator<N> pIterator) {
    assertThrows(UnsupportedOperationException.class, () -> pIterator.remove());
  }

  @Test
  public void testEmptyDomTree() {
    DomTree<String> emptyDomTree = DomTree.empty();
    var tester = new DomTreeTester<>(emptyDomTree);

    tester.assertNodeCountIs(0);
    tester.assertHasNoRoot();
    tester.assertNodeIsUnknown(UNKNOWN_NODE);
  }

  @Test
  public void testEmptyDomTreeIterator() {
    DomTree<String> emptyDomTree = DomTree.empty();
    Iterator<String> emptyDomTreeIterator = emptyDomTree.iterator();

    assertNoNextElement(emptyDomTreeIterator);
    assertRemoveUnsupported(emptyDomTreeIterator);
  }

  @Test
  public void testEmptyDomTreeGraph() {
    DomTree<String> emptyDomTree = DomTree.empty();
    Graph<String> emptyDomTreeGraph = emptyDomTree.asGraph();

    assertDirectedAndNoSelfLoops(emptyDomTreeGraph);
    assertThat(emptyDomTreeGraph.nodes()).isEmpty();
    assertThat(emptyDomTreeGraph.edges()).isEmpty();
  }

  @Test
  public void testEmptyDomFrontiers() {
    DomTree<String> emptyDomTree = DomTree.empty();
    DomFrontiers<String> emptyDomTreeFrontiers = DomFrontiers.forDomTree(emptyDomTree);

    assertThrows(
        IllegalArgumentException.class, () -> emptyDomTreeFrontiers.getFrontier(UNKNOWN_NODE));
    assertThat(emptyDomTreeFrontiers.getIteratedFrontier(ImmutableSet.of())).isEmpty();
    assertThrows(
        IllegalArgumentException.class,
        () -> emptyDomTreeFrontiers.getIteratedFrontier(ImmutableSet.of(UNKNOWN_NODE)));
  }

  /** Returns a graph consisting of the node {@link #ENTRY_NODE}. */
  private static Graph<String> createSingleNodeGraph() {
    MutableGraph<String> singleNodeGraph = GraphBuilder.directed().build();
    singleNodeGraph.addNode(ENTRY_NODE);
    return singleNodeGraph;
  }

  @Test
  public void testSingleNodeDomTree() {
    DomTree<String> singleNodeDomTree = DomTree.forGraph(createSingleNodeGraph(), ENTRY_NODE);
    var tester = new DomTreeTester<>(singleNodeDomTree);

    tester.assertNodeCountIs(1);
    tester.assertRootIs(ENTRY_NODE);
    tester.assertThat(ENTRY_NODE).hasNoParent();
    tester.assertThat(ENTRY_NODE).hasNoAncestors();
    tester.assertThat(ENTRY_NODE).isNotAncestorOf(ENTRY_NODE);
    tester.assertNodeIsUnknown(UNKNOWN_NODE);
  }

  @Test
  public void testSingleNodeDomTreeIterator() {
    DomTree<String> singleNodeDomTree = DomTree.forGraph(createSingleNodeGraph(), ENTRY_NODE);
    Iterator<String> singleNodeDomTreeIterator = singleNodeDomTree.iterator();

    assertHasNextElement(singleNodeDomTreeIterator);
    assertRemoveUnsupported(singleNodeDomTreeIterator);
    assertThat(singleNodeDomTreeIterator.next()).isEqualTo(ENTRY_NODE);
    assertRemoveUnsupported(singleNodeDomTreeIterator);
    assertNoNextElement(singleNodeDomTreeIterator);
  }

  @Test
  public void testSingleNodeDomTreeGraph() {
    DomTree<String> singleNodeDomTree = DomTree.forGraph(createSingleNodeGraph(), ENTRY_NODE);
    Graph<String> singleNodeTreeGraph = singleNodeDomTree.asGraph();

    assertDirectedAndNoSelfLoops(singleNodeTreeGraph);
    assertThat(singleNodeTreeGraph.nodes()).containsExactly(ENTRY_NODE);
    assertThat(singleNodeTreeGraph.edges()).isEmpty();
  }

  @Test
  public void testSingleNodeDomFrontiers() {
    DomTree<String> singleNodeDomTree = DomTree.forGraph(createSingleNodeGraph(), ENTRY_NODE);
    DomFrontiers<String> singleNodeDomTreeFrontiers = DomFrontiers.forDomTree(singleNodeDomTree);

    assertThat(singleNodeDomTreeFrontiers.getFrontier(ENTRY_NODE)).isEmpty();
    assertThrows(
        IllegalArgumentException.class, () -> singleNodeDomTreeFrontiers.getFrontier(UNKNOWN_NODE));
    assertThat(singleNodeDomTreeFrontiers.getIteratedFrontier(ImmutableSet.of())).isEmpty();
    assertThat(singleNodeDomTreeFrontiers.getIteratedFrontier(ImmutableSet.of(ENTRY_NODE)))
        .isEmpty();
    assertThrows(
        IllegalArgumentException.class,
        () -> singleNodeDomTreeFrontiers.getIteratedFrontier(ImmutableSet.of(UNKNOWN_NODE)));
  }

  /**
   * Returns the following graph:
   *
   * <pre>{@code
   *        ---------> { A } ----------
   *       /                           \
   * { ENTRY }                          ---> { EXIT }
   *       \                           /
   *        ----> { B } ---> { C } ----
   *
   * }</pre>
   */
  private static Graph<String> createBranchingGraph() {
    MutableGraph<String> branchingGraph = GraphBuilder.directed().build();
    branchingGraph.addNode(ENTRY_NODE);
    branchingGraph.addNode("A");
    branchingGraph.addNode("B");
    branchingGraph.addNode("C");
    branchingGraph.addNode(EXIT_NODE);
    branchingGraph.putEdge(ENTRY_NODE, "A");
    branchingGraph.putEdge("A", EXIT_NODE);
    branchingGraph.putEdge(ENTRY_NODE, "B");
    branchingGraph.putEdge("B", "C");
    branchingGraph.putEdge("C", EXIT_NODE);
    return branchingGraph;
  }

  @Test
  public void testBranchingDomTree() {
    Graph<String> branchingGraph = createBranchingGraph();
    DomTree<String> branchingDomTree = DomTree.forGraph(branchingGraph, ENTRY_NODE);
    var tester = new DomTreeTester<>(branchingDomTree);

    tester.assertNodeCountIs(branchingGraph.nodes().size());
    tester.assertRootIs(ENTRY_NODE);

    tester.assertThat(ENTRY_NODE).hasNoParent();
    tester.assertThat("A").isChildOf(ENTRY_NODE);
    tester.assertThat("B").isChildOf(ENTRY_NODE);
    tester.assertThat("C").isChildOf("B");
    tester.assertThat(EXIT_NODE).isChildOf(ENTRY_NODE);

    tester.assertThat(ENTRY_NODE).hasNoAncestors();
    tester.assertThat("A").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("A").isDescendantOf(ENTRY_NODE);
    tester.assertThat("B").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("B").isDescendantOf(ENTRY_NODE);
    tester.assertThat("C").hasTheseAncestors(ENTRY_NODE, "B");
    tester.assertThat("C").isDescendantOf(ENTRY_NODE);
    tester.assertThat("C").isDescendantOf("B");
    tester.assertThat(EXIT_NODE).hasTheseAncestors(ENTRY_NODE);
    tester.assertThat(EXIT_NODE).isDescendantOf(ENTRY_NODE);

    tester.assertThat(ENTRY_NODE).isNotAncestorOf(ENTRY_NODE);
    for (String node : branchingGraph.nodes()) {
      tester.assertThat("A").isNotAncestorOf(node);
      if (!node.equals("C")) {
        tester.assertThat("B").isNotAncestorOf(node);
      }
      tester.assertThat("C").isNotAncestorOf(node);
      tester.assertThat(EXIT_NODE).isNotAncestorOf(node);
    }
  }

  @Test
  public void testBranchingDomTreeIterator() {
    Graph<String> branchingGraph = createBranchingGraph();
    DomTree<String> branchingDomTree = DomTree.forGraph(branchingGraph, ENTRY_NODE);

    assertThat(branchingDomTree).containsExactly(branchingGraph.nodes().toArray());
  }

  @Test
  public void testBranchingDomTreeGraph() {
    Graph<String> branchingGraph = createBranchingGraph();
    DomTree<String> branchingDomTree = DomTree.forGraph(branchingGraph, ENTRY_NODE);
    Graph<String> branchingDomTreeGraph = branchingDomTree.asGraph();

    assertDirectedAndNoSelfLoops(branchingDomTreeGraph);
    assertThat(branchingDomTreeGraph.nodes()).containsExactly(branchingGraph.nodes().toArray());
    assertThat(branchingDomTreeGraph.edges())
        .containsExactly(
            EndpointPair.ordered(ENTRY_NODE, "A"),
            EndpointPair.ordered(ENTRY_NODE, "B"),
            EndpointPair.ordered("B", "C"),
            EndpointPair.ordered(ENTRY_NODE, EXIT_NODE));
  }

  @Test
  public void testBranchingDomFrontiers() {
    Graph<String> branchingGraph = createBranchingGraph();
    DomTree<String> branchingDomTree = DomTree.forGraph(branchingGraph, ENTRY_NODE);
    DomFrontiers<String> branchingDomFrontiers = DomFrontiers.forDomTree(branchingDomTree);

    assertThat(branchingDomFrontiers.getFrontier(ENTRY_NODE)).isEmpty();
    assertThat(branchingDomFrontiers.getFrontier("A")).containsExactly(EXIT_NODE);
    assertThat(branchingDomFrontiers.getFrontier("B")).containsExactly(EXIT_NODE);
    assertThat(branchingDomFrontiers.getFrontier("C")).containsExactly(EXIT_NODE);
    assertThat(branchingDomFrontiers.getFrontier(EXIT_NODE)).isEmpty();

    assertThat(branchingDomFrontiers.getIteratedFrontier(branchingGraph.nodes()))
        .containsExactly(EXIT_NODE);
    assertThat(branchingDomFrontiers.getIteratedFrontier(ImmutableSet.of(ENTRY_NODE, EXIT_NODE)))
        .isEmpty();
    assertThat(
            branchingDomFrontiers.getIteratedFrontier(ImmutableSet.of(ENTRY_NODE, EXIT_NODE, "A")))
        .containsExactly(EXIT_NODE);
  }

  /**
   * Returns the following graph:
   *
   * <pre>{@code
   * { ENTRY }
   *     |
   *     |  ----- { C } <---
   *     | /                \
   *     |/                  \
   *     v                    |
   *   { A }                  |
   *     |\                  /
   *     | \                /
   *     |  ----> { B } ----
   *     |
   *     v
   * { EXIT }
   *
   * }</pre>
   */
  private static Graph<String> createLoopGraph() {
    MutableGraph<String> loopGraph = GraphBuilder.directed().build();
    loopGraph.addNode(ENTRY_NODE);
    loopGraph.addNode("A");
    loopGraph.addNode("B");
    loopGraph.addNode("C");
    loopGraph.addNode(EXIT_NODE);
    loopGraph.putEdge(ENTRY_NODE, "A");
    loopGraph.putEdge("A", EXIT_NODE);
    loopGraph.putEdge("A", "B");
    loopGraph.putEdge("B", "C");
    loopGraph.putEdge("C", "A");
    return loopGraph;
  }

  @Test
  public void testLoopDomTree() {
    Graph<String> loopGraph = createLoopGraph();
    DomTree<String> loopDomTree = DomTree.forGraph(loopGraph, ENTRY_NODE);
    var tester = new DomTreeTester<>(loopDomTree);

    tester.assertNodeCountIs(loopGraph.nodes().size());
    tester.assertRootIs(ENTRY_NODE);

    tester.assertThat(ENTRY_NODE).hasNoParent();
    tester.assertThat(ENTRY_NODE).isParentOf("A");
    tester.assertThat("A").isParentOf("B");
    tester.assertThat("A").isParentOf(EXIT_NODE);
    tester.assertThat("B").isParentOf("C");

    tester.assertThat(ENTRY_NODE).hasNoAncestors();
    tester.assertThat("A").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("B").hasTheseAncestors(ENTRY_NODE, "A");
    tester.assertThat("C").hasTheseAncestors(ENTRY_NODE, "A", "B");
    tester.assertThat(EXIT_NODE).hasTheseAncestors(ENTRY_NODE, "A");
  }

  @Test
  public void testLoopDomTreeIterator() {
    Graph<String> loopGraph = createLoopGraph();
    DomTree<String> loopDomTree = DomTree.forGraph(loopGraph, ENTRY_NODE);

    assertThat(loopDomTree).containsExactly(loopGraph.nodes().toArray());
  }

  @Test
  public void testLoopDomTreeGraph() {
    Graph<String> loopGraph = createLoopGraph();
    DomTree<String> loopDomTree = DomTree.forGraph(loopGraph, ENTRY_NODE);
    Graph<String> loopDomTreeGraph = loopDomTree.asGraph();

    assertDirectedAndNoSelfLoops(loopDomTreeGraph);
    assertThat(loopDomTreeGraph.nodes()).containsExactly(loopGraph.nodes().toArray());
    assertThat(loopDomTreeGraph.edges())
        .containsExactly(
            EndpointPair.ordered(ENTRY_NODE, "A"),
            EndpointPair.ordered("A", "B"),
            EndpointPair.ordered("B", "C"),
            EndpointPair.ordered("A", EXIT_NODE));
  }

  @Test
  public void testLoopDomFrontiers() {
    Graph<String> loopGraph = createLoopGraph();
    DomTree<String> loopDomTree = DomTree.forGraph(loopGraph, ENTRY_NODE);
    DomFrontiers<String> loopDomFrontiers = DomFrontiers.forDomTree(loopDomTree);

    assertThat(loopDomFrontiers.getFrontier(ENTRY_NODE)).isEmpty();
    assertThat(loopDomFrontiers.getFrontier("A")).containsExactly("A");
    assertThat(loopDomFrontiers.getFrontier("B")).containsExactly("A");
    assertThat(loopDomFrontiers.getFrontier("C")).containsExactly("A");
    assertThat(loopDomFrontiers.getFrontier(EXIT_NODE)).isEmpty();

    assertThat(loopDomFrontiers.getIteratedFrontier(loopGraph.nodes())).containsExactly("A");
    assertThat(loopDomFrontiers.getIteratedFrontier(ImmutableSet.of(ENTRY_NODE, EXIT_NODE)))
        .isEmpty();
    assertThat(loopDomFrontiers.getIteratedFrontier(ImmutableSet.of("A", "B", "C")))
        .containsExactly("A");
  }

  /**
   * Returns the following graph:
   *
   * <pre>{@code
   * { ENTRY }
   *     |
   *     |
   *     |
   *     |
   *     v            -------
   *   { A }         /       \
   *     |\         |        |
   *     | \        v       /
   *     |  ----> { B } ----
   *     |
   *     v
   * { EXIT }
   *
   * }</pre>
   */
  private static Graph<String> createInfiniteLoopGraph() {
    MutableGraph<String> infiniteLoopGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    infiniteLoopGraph.addNode(ENTRY_NODE);
    infiniteLoopGraph.addNode("A");
    infiniteLoopGraph.addNode("B");
    infiniteLoopGraph.addNode(EXIT_NODE);
    infiniteLoopGraph.putEdge(ENTRY_NODE, "A");
    infiniteLoopGraph.putEdge("A", "B");
    infiniteLoopGraph.putEdge("B", "B");
    infiniteLoopGraph.putEdge("A", EXIT_NODE);
    return infiniteLoopGraph;
  }

  @Test
  public void testInfiniteLoopPostDomTree() {
    Graph<String> transposedInfiniteLoopGraph = Graphs.transpose(createInfiniteLoopGraph());
    DomTree<String> infiniteLoopPostDomTree =
        DomTree.forGraph(transposedInfiniteLoopGraph, EXIT_NODE);
    var tester = new DomTreeTester<>(infiniteLoopPostDomTree);

    tester.assertNodeCountIs(
        Sets.difference(transposedInfiniteLoopGraph.nodes(), ImmutableSet.of("B")).size());
    tester.assertRootIs(EXIT_NODE);

    tester.assertThat(EXIT_NODE).hasNoParent();
    tester.assertThat(EXIT_NODE).isParentOf("A");
    tester.assertThat("A").isParentOf(ENTRY_NODE);
    tester.assertNodeIsUnknown("B");

    tester.assertThat(EXIT_NODE).hasNoAncestors();
    tester.assertThat("A").hasTheseAncestors(EXIT_NODE);
    tester.assertThat(ENTRY_NODE).hasTheseAncestors(EXIT_NODE, "A");
  }

  @Test
  public void testInfiniteLoopPostDomTreeIterator() {
    Graph<String> transposedInfiniteLoopGraph = Graphs.transpose(createInfiniteLoopGraph());
    DomTree<String> infiniteLoopPostDomTree =
        DomTree.forGraph(transposedInfiniteLoopGraph, EXIT_NODE);

    assertThat(infiniteLoopPostDomTree)
        .containsExactly(
            Sets.difference(transposedInfiniteLoopGraph.nodes(), ImmutableSet.of("B")).toArray());
  }

  @Test
  public void testInfiniteLoopPostDomTreeGraph() {
    Graph<String> transposedInfiniteLoopGraph = Graphs.transpose(createInfiniteLoopGraph());
    DomTree<String> infiniteLoopPostDomTree =
        DomTree.forGraph(transposedInfiniteLoopGraph, EXIT_NODE);
    Graph<String> infiniteLoopPostDomTreeGraph = infiniteLoopPostDomTree.asGraph();

    assertDirectedAndNoSelfLoops(infiniteLoopPostDomTreeGraph);
    assertThat(infiniteLoopPostDomTreeGraph.nodes())
        .containsExactly(Iterables.toArray(infiniteLoopPostDomTree, Object.class));
    assertThat(infiniteLoopPostDomTreeGraph.edges())
        .containsExactly(
            EndpointPair.ordered(EXIT_NODE, "A"), EndpointPair.ordered("A", ENTRY_NODE));
  }

  @Test
  public void testInfiniteLoopPostDomFrontiers() {
    Graph<String> transposedInfiniteLoopGraph = Graphs.transpose(createInfiniteLoopGraph());
    DomTree<String> infiniteLoopPostDomTree =
        DomTree.forGraph(transposedInfiniteLoopGraph, EXIT_NODE);
    DomFrontiers<String> infiniteLoopPostDomFrontiers =
        DomFrontiers.forDomTree(infiniteLoopPostDomTree);

    assertThat(infiniteLoopPostDomFrontiers.getFrontier(ENTRY_NODE)).isEmpty();
    assertThat(infiniteLoopPostDomFrontiers.getFrontier("A")).isEmpty();
    assertThat(infiniteLoopPostDomFrontiers.getFrontier(EXIT_NODE)).isEmpty();
    assertThat(
            infiniteLoopPostDomFrontiers.getIteratedFrontier(
                ImmutableSet.copyOf(infiniteLoopPostDomTree)))
        .isEmpty();
    assertThrows(
        IllegalArgumentException.class, () -> infiniteLoopPostDomFrontiers.getFrontier("B"));
  }

  /** Returns a more complex graph with multiple branchings and loops. */
  private static Graph<String> createExampleGraph() {

    // DOT file representation for graph visualization
    /*
    digraph EXAMPLE {
    ENTRY [shape="circle" label="ENTRY"]
    A [shape="circle" label="A"]
    B [shape="circle" label="B"]
    C [shape="circle" label="C"]
    D [shape="circle" label="D"]
    E [shape="circle" label="E"]
    F [shape="circle" label="F"]
    G [shape="circle" label="G"]
    H [shape="circle" label="H"]
    I [shape="circle" label="I"]
    J [shape="circle" label="J"]
    K [shape="circle" label="K"]
    L [shape="circle" label="L"]
    M [shape="circle" label="M"]
    N [shape="circle" label="N"]
    O [shape="circle" label="O"]
    P [shape="circle" label="P"]
    Q [shape="circle" label="Q"]
    EXIT [shape="circle" label="EXIT"]
    ENTRY -> D
    ENTRY -> H
    ENTRY -> A
    A -> B
    B -> B
    B -> C
    C -> Q
    D -> E
    D -> F
    E -> C
    E -> G
    F -> L
    F -> G
    G -> D
    G -> Q
    H -> I
    H -> M
    I -> J
    J -> K
    K -> L
    L -> Q
    M -> N
    N -> P
    N -> O
    O -> O
    P -> L
    Q -> EXIT
    }
    */

    // keep this graph and the DOT file representation in sync!
    MutableGraph<String> exampleGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    exampleGraph.addNode(ENTRY_NODE);
    exampleGraph.addNode("A");
    exampleGraph.addNode("A");
    exampleGraph.addNode("B");
    exampleGraph.addNode("C");
    exampleGraph.addNode("D");
    exampleGraph.addNode("E");
    exampleGraph.addNode("F");
    exampleGraph.addNode("G");
    exampleGraph.addNode("H");
    exampleGraph.addNode("I");
    exampleGraph.addNode("J");
    exampleGraph.addNode("K");
    exampleGraph.addNode("L");
    exampleGraph.addNode("M");
    exampleGraph.addNode("N");
    exampleGraph.addNode("O");
    exampleGraph.addNode("P");
    exampleGraph.addNode("Q");
    exampleGraph.addNode(EXIT_NODE);

    exampleGraph.putEdge(ENTRY_NODE, "A");
    exampleGraph.putEdge("A", "B");
    exampleGraph.putEdge("B", "B");
    exampleGraph.putEdge("B", "C");
    exampleGraph.putEdge("C", "Q");
    exampleGraph.putEdge(ENTRY_NODE, "D");
    exampleGraph.putEdge("D", "E");
    exampleGraph.putEdge("D", "F");
    exampleGraph.putEdge("E", "C");
    exampleGraph.putEdge("E", "G");
    exampleGraph.putEdge("F", "G");
    exampleGraph.putEdge("F", "L");
    exampleGraph.putEdge("G", "D");
    exampleGraph.putEdge("G", "Q");
    exampleGraph.putEdge(ENTRY_NODE, "H");
    exampleGraph.putEdge("H", "I");
    exampleGraph.putEdge("I", "J");
    exampleGraph.putEdge("J", "K");
    exampleGraph.putEdge("K", "L");
    exampleGraph.putEdge("L", "Q");
    exampleGraph.putEdge("H", "M");
    exampleGraph.putEdge("M", "N");
    exampleGraph.putEdge("N", "O");
    exampleGraph.putEdge("O", "O");
    exampleGraph.putEdge("N", "P");
    exampleGraph.putEdge("P", "L");
    exampleGraph.putEdge("Q", EXIT_NODE);

    return exampleGraph;
  }

  @Test
  public void testExampleDomTree() {
    Graph<String> exampleGraph = createExampleGraph();
    DomTree<String> exampleDomTree = DomTree.forGraph(exampleGraph, ENTRY_NODE);
    var tester = new DomTreeTester<>(exampleDomTree);

    tester.assertNodeCountIs(exampleGraph.nodes().size());
    tester.assertRootIs(ENTRY_NODE);

    tester.assertThat(ENTRY_NODE).hasNoParent();
    tester.assertThat("A").isChildOf(ENTRY_NODE);
    tester.assertThat("B").isChildOf("A");
    tester.assertThat("C").isChildOf(ENTRY_NODE);
    tester.assertThat("D").isChildOf(ENTRY_NODE);
    tester.assertThat("E").isChildOf("D");
    tester.assertThat("F").isChildOf("D");
    tester.assertThat("G").isChildOf("D");
    tester.assertThat("H").isChildOf(ENTRY_NODE);
    tester.assertThat("I").isChildOf("H");
    tester.assertThat("J").isChildOf("I");
    tester.assertThat("K").isChildOf("J");
    tester.assertThat("L").isChildOf(ENTRY_NODE);
    tester.assertThat("M").isChildOf("H");
    tester.assertThat("N").isChildOf("M");
    tester.assertThat("O").isChildOf("N");
    tester.assertThat("P").isChildOf("N");
    tester.assertThat("Q").isChildOf(ENTRY_NODE);
    tester.assertThat(EXIT_NODE).isChildOf("Q");

    tester.assertThat(ENTRY_NODE).hasNoAncestors();
    tester.assertThat("A").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("B").hasTheseAncestors(ENTRY_NODE, "A");
    tester.assertThat("C").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("D").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("E").hasTheseAncestors(ENTRY_NODE, "D");
    tester.assertThat("F").hasTheseAncestors(ENTRY_NODE, "D");
    tester.assertThat("G").hasTheseAncestors(ENTRY_NODE, "D");
    tester.assertThat("H").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("I").hasTheseAncestors(ENTRY_NODE, "H");
    tester.assertThat("J").hasTheseAncestors(ENTRY_NODE, "H", "I");
    tester.assertThat("K").hasTheseAncestors(ENTRY_NODE, "H", "I", "J");
    tester.assertThat("L").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat("M").hasTheseAncestors(ENTRY_NODE, "H");
    tester.assertThat("N").hasTheseAncestors(ENTRY_NODE, "H", "M");
    tester.assertThat("O").hasTheseAncestors(ENTRY_NODE, "H", "M", "N");
    tester.assertThat("P").hasTheseAncestors(ENTRY_NODE, "H", "M", "N");
    tester.assertThat("Q").hasTheseAncestors(ENTRY_NODE);
    tester.assertThat(EXIT_NODE).hasTheseAncestors(ENTRY_NODE, "Q");
  }

  @Test
  public void testExampleDomFrontiers() {
    Graph<String> exampleGraph = createExampleGraph();
    DomTree<String> exampleDomTree = DomTree.forGraph(exampleGraph, ENTRY_NODE);
    DomFrontiers<String> exampleDomFrontiers = DomFrontiers.forDomTree(exampleDomTree);

    assertThat(exampleDomFrontiers.getFrontier(ENTRY_NODE)).isEmpty();
    assertThat(exampleDomFrontiers.getFrontier("A")).containsExactly("C");
    assertThat(exampleDomFrontiers.getFrontier("B")).containsExactly("B", "C");
    assertThat(exampleDomFrontiers.getFrontier("C")).containsExactly("Q");
    assertThat(exampleDomFrontiers.getFrontier("D")).containsExactly("D", "C", "L", "Q");
    assertThat(exampleDomFrontiers.getFrontier("E")).containsExactly("C", "G");
    assertThat(exampleDomFrontiers.getFrontier("F")).containsExactly("G", "L");
    assertThat(exampleDomFrontiers.getFrontier("G")).containsExactly("Q", "D");
    assertThat(exampleDomFrontiers.getFrontier("H")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("I")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("J")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("K")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("L")).containsExactly("Q");
    assertThat(exampleDomFrontiers.getFrontier("M")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("N")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("O")).containsExactly("O");
    assertThat(exampleDomFrontiers.getFrontier("P")).containsExactly("L");
    assertThat(exampleDomFrontiers.getFrontier("Q")).isEmpty();
    assertThat(exampleDomFrontiers.getFrontier(EXIT_NODE)).isEmpty();

    assertThat(exampleDomFrontiers.getIteratedFrontier(ImmutableSet.of("B", "I")))
        .containsExactly("B", "C", "L", "Q");
    assertThat(exampleDomFrontiers.getIteratedFrontier(ImmutableSet.of("E")))
        .containsExactly("C", "G", "Q", "D", "L");
  }

  @Test
  public void testExamplePostDomTree() {
    Graph<String> transposedExampleGraph = Graphs.transpose(createExampleGraph());
    DomTree<String> examplePostDomTree = DomTree.forGraph(transposedExampleGraph, EXIT_NODE);
    var tester = new DomTreeTester<>(examplePostDomTree);

    tester.assertNodeCountIs(
        Sets.difference(transposedExampleGraph.nodes(), ImmutableSet.of("O")).size());
    tester.assertRootIs(EXIT_NODE);

    tester.assertThat(ENTRY_NODE).isChildOf("Q");
    tester.assertThat("A").isChildOf("B");
    tester.assertThat("B").isChildOf("C");
    tester.assertThat("C").isChildOf("Q");
    tester.assertThat("D").isChildOf("Q");
    tester.assertThat("E").isChildOf("Q");
    tester.assertThat("F").isChildOf("Q");
    tester.assertThat("G").isChildOf("Q");
    tester.assertThat("H").isChildOf("L");
    tester.assertThat("I").isChildOf("J");
    tester.assertThat("J").isChildOf("K");
    tester.assertThat("K").isChildOf("L");
    tester.assertThat("L").isChildOf("Q");
    tester.assertThat("M").isChildOf("N");
    tester.assertThat("N").isChildOf("P");
    tester.assertNodeIsUnknown("O");
    tester.assertThat("P").isChildOf("L");
    tester.assertThat("Q").isChildOf(EXIT_NODE);
    tester.assertThat(EXIT_NODE).hasNoParent();

    tester.assertThat(ENTRY_NODE).hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("A").hasTheseAncestors(EXIT_NODE, "Q", "B", "C");
    tester.assertThat("B").hasTheseAncestors(EXIT_NODE, "Q", "C");
    tester.assertThat("C").hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("D").hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("E").hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("F").hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("G").hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("H").hasTheseAncestors(EXIT_NODE, "Q", "L");
    tester.assertThat("I").hasTheseAncestors(EXIT_NODE, "Q", "J", "K", "L");
    tester.assertThat("J").hasTheseAncestors(EXIT_NODE, "Q", "K", "L");
    tester.assertThat("K").hasTheseAncestors(EXIT_NODE, "Q", "L");
    tester.assertThat("L").hasTheseAncestors(EXIT_NODE, "Q");
    tester.assertThat("M").hasTheseAncestors(EXIT_NODE, "Q", "N", "P", "L");
    tester.assertThat("N").hasTheseAncestors(EXIT_NODE, "Q", "P", "L");
    tester.assertNodeIsUnknown("O");
    tester.assertThat("P").hasTheseAncestors(EXIT_NODE, "Q", "L");
    tester.assertThat("Q").hasTheseAncestors(EXIT_NODE);
    tester.assertThat(EXIT_NODE).hasNoAncestors();
  }

  @Test
  public void testExamplePostDomFrontiers() {
    Graph<String> transposedExampleGraph = Graphs.transpose(createExampleGraph());
    DomTree<String> examplePostDomTree = DomTree.forGraph(transposedExampleGraph, EXIT_NODE);
    DomFrontiers<String> examplePostDomFrontiers = DomFrontiers.forDomTree(examplePostDomTree);

    assertThat(examplePostDomFrontiers.getFrontier(ENTRY_NODE)).isEmpty();
    assertThat(examplePostDomFrontiers.getFrontier("A")).containsExactly(ENTRY_NODE);
    assertThat(examplePostDomFrontiers.getFrontier("B")).containsExactly("B", ENTRY_NODE);
    assertThat(examplePostDomFrontiers.getFrontier("C")).containsExactly("E", ENTRY_NODE);
    assertThat(examplePostDomFrontiers.getFrontier("D")).containsExactly("G", ENTRY_NODE);
    assertThat(examplePostDomFrontiers.getFrontier("E")).containsExactly("D");
    assertThat(examplePostDomFrontiers.getFrontier("F")).containsExactly("D");
    assertThat(examplePostDomFrontiers.getFrontier("G")).containsExactly("F", "E");
    assertThat(examplePostDomFrontiers.getFrontier("H")).containsExactly(ENTRY_NODE);
    assertThat(examplePostDomFrontiers.getFrontier("I")).containsExactly("H");
    assertThat(examplePostDomFrontiers.getFrontier("J")).containsExactly("H");
    assertThat(examplePostDomFrontiers.getFrontier("K")).containsExactly("H");
    assertThat(examplePostDomFrontiers.getFrontier("L")).containsExactly(ENTRY_NODE, "F");
    assertThat(examplePostDomFrontiers.getFrontier("M")).containsExactly("H");
    assertThat(examplePostDomFrontiers.getFrontier("N")).containsExactly("H");
    assertThrows(IllegalArgumentException.class, () -> examplePostDomFrontiers.getFrontier("O"));
    assertThat(examplePostDomFrontiers.getFrontier("P")).containsExactly("H");
    assertThat(examplePostDomFrontiers.getFrontier("Q")).isEmpty();
    assertThat(examplePostDomFrontiers.getFrontier(EXIT_NODE)).isEmpty();
  }

  private static final class DomTreeTester<N> {

    private final DomTree<N> domTree;

    private DomTreeTester(DomTree<N> pDomTree) {
      domTree = pDomTree;
    }

    private void assertNodeCountIs(int pNodeCount) {
      assertWithMessage(
              "Expected the node count of the dominator tree to be '%s', but it is '%s'.",
              pNodeCount, domTree.getNodeCount())
          .that(domTree.getNodeCount())
          .isEqualTo(pNodeCount);
    }

    private void assertHasNoRoot() {
      assertWithMessage(
              "Expected the dominator tree to have no root (i.e., be empty), but it is '%s'.",
              domTree.getRoot().orElse(null))
          .that(domTree.getRoot())
          .isEqualTo(Optional.empty());
    }

    private void assertRootIs(N pRootNode) {
      assertWithMessage(
              "Expected the root of the dominator tree to be '%s', but it is '%s'.",
              pRootNode, domTree.getRoot().orElse(null))
          .that(domTree.getRoot())
          .isEqualTo(Optional.of(pRootNode));
    }

    private void assertNodeIsUnknown(N pUnknownNode) {
      assertWithMessage(
              "Expected '%s' to be an unknown node, but the dominator tree contains it.",
              pUnknownNode)
          .that(Iterables.contains(domTree, pUnknownNode))
          .isFalse();
      assertThrows(IllegalArgumentException.class, () -> domTree.getParent(pUnknownNode));
      assertThrows(IllegalArgumentException.class, () -> domTree.getAncestors(pUnknownNode));
      N otherNode = Iterables.tryFind(domTree, element -> true).or(pUnknownNode);
      assertThrows(
          IllegalArgumentException.class, () -> domTree.isAncestorOf(otherNode, pUnknownNode));
      assertThrows(
          IllegalArgumentException.class, () -> domTree.isAncestorOf(pUnknownNode, otherNode));
    }

    private NodeTester assertThat(N pNode) {
      return new NodeTester(pNode);
    }

    private final class NodeTester {

      private final N node;

      private NodeTester(N pNode) {
        node = pNode;
      }

      private void hasNoParent() {
        assertWithMessage(
                "Expected '%s' to have no parent, but parent is '%s'.",
                node, domTree.getParent(node).orElse(null))
            .that(domTree.getParent(node))
            .isEqualTo(Optional.empty());
      }

      private void isParentOf(N pChildNode) {
        assertWithMessage(
                "Expected '%s' to be the parent of '%s', but parent is '%s'.",
                node, pChildNode, domTree.getParent(pChildNode).orElse(null))
            .that(domTree.getParent(pChildNode))
            .isEqualTo(Optional.of(node));
      }

      private void isChildOf(N pParentNode) {
        assertWithMessage(
                "Expected '%s' to be a child of '%s', but the parent of '%s' is '%s'.",
                node, pParentNode, node, domTree.getParent(node).orElse(null))
            .that(domTree.getParent(node))
            .isEqualTo(Optional.of(pParentNode));
      }

      private void isNotAncestorOf(N pDescendantNode) {
        assertWithMessage(
                "Expected '%s' to _not_ be an ancestor of '%s', but it is.", node, pDescendantNode)
            .that(domTree.isAncestorOf(node, pDescendantNode))
            .isFalse();
      }

      private void isDescendantOf(N pAncestorNode) {
        assertWithMessage(
                "Expected '%s' to be a descendant of '%s', but it isn't.", node, pAncestorNode)
            .that(domTree.isAncestorOf(pAncestorNode, node))
            .isTrue();
      }

      private void hasNoAncestors() {
        assertWithMessage(
                "Expected '%s' to have no ancestors, but ancestors are '%s'.",
                node, domTree.getAncestors(node))
            .that(domTree.getAncestors(node))
            .isEmpty();
      }

      private void hasTheseAncestors(Object... pAncestorNodes) {
        assertWithMessage(
                "Expected '%s' to have '%s' as ancestors, but ancestors are '%s'.",
                node, ImmutableSet.copyOf(pAncestorNodes), domTree.getAncestors(node))
            .that(domTree.getAncestors(node))
            .containsExactly(pAncestorNodes);
      }
    }
  }
}
