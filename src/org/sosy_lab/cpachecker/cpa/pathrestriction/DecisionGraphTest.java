// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph.PathPosition;

public final class DecisionGraphTest {

  /** The two edges leaving a branching node. */
  private record Branch(CFAEdge taken, CFAEdge other) {}

  private static CFANode mockNode(int nodeNumber) {
    CFANode node = mock(CFANode.class);
    when(node.getNodeNumber()).thenReturn(nodeNumber);
    return node;
  }

  private static CFAEdge mockEdge(CFANode predecessor, CFANode successor) {
    CFAEdge edge = mock(CFAEdge.class);
    when(edge.getPredecessor()).thenReturn(predecessor);
    when(edge.getSuccessor()).thenReturn(successor);
    return edge;
  }

  /** Makes {@code predecessor} leave via exactly {@code outgoing}, e.g. to model branching. */
  private static void setLeavingEdges(CFANode predecessor, CFAEdge... outgoing) {
    when(predecessor.getAllLeavingEdges())
        .thenReturn(FluentIterable.from(ImmutableList.copyOf(outgoing)));
  }

  /** An edge whose predecessor has no other exit, so it is not a decision. */
  private static CFAEdge straightEdge(int predecessorNumber, int successorNumber) {
    CFANode predecessor = mockNode(predecessorNumber);
    CFAEdge edge = mockEdge(predecessor, mockNode(successorNumber));
    setLeavingEdges(predecessor, edge);
    return edge;
  }

  /** The two exits of a branching node, i.e. two decision edges. */
  private static Branch branch(int predecessorNumber, int successorNumber) {
    CFANode predecessor = mockNode(predecessorNumber);
    CFAEdge taken = mockEdge(predecessor, mockNode(successorNumber));
    CFAEdge other = mockEdge(predecessor, mockNode(successorNumber + 1000));
    setLeavingEdges(predecessor, taken, other);
    return new Branch(taken, other);
  }

  /** Whether the graph permits taking {@code edges} in this order. */
  private static boolean permits(DecisionGraph graph, CFAEdge... edges) {
    PathPosition position = graph.start();
    for (CFAEdge edge : edges) {
      position = position.advance(edge);
      if (position.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static DecisionGraph path(CFAEdge... edges) {
    return DecisionGraph.EMPTY.prepend(ImmutableList.copyOf(edges));
  }

  @Test
  public void empty_permitsEverything() {
    Branch branch = branch(1, 2);

    assertThat(permits(DecisionGraph.EMPTY, branch.taken())).isTrue();
    assertThat(permits(DecisionGraph.EMPTY, branch.other())).isTrue();
    assertThat(DecisionGraph.EMPTY.size()).isEqualTo(0);
  }

  @Test
  public void prepend_keepsOnlyDecisionEdges() {
    Branch branch = branch(1, 2);
    CFAEdge straight = straightEdge(2, 3);

    DecisionGraph graph = path(branch.taken(), straight);

    assertThat(graph.size()).isEqualTo(1);
    assertThat(permits(graph, branch.taken())).isTrue();
    assertThat(permits(graph, branch.other())).isFalse();
  }

  @Test
  public void prepend_withNoDecisionEdges_permitsEverything() {
    DecisionGraph graph = path(straightEdge(1, 2));

    assertThat(graph).isEqualTo(DecisionGraph.EMPTY);
    assertThat(graph.size()).isEqualTo(0);
  }

  @Test
  public void advance_ignoresEdgesThatAreNoDecision() {
    Branch branch = branch(1, 2);
    CFAEdge straight = straightEdge(5, 6);

    assertThat(permits(path(branch.taken()), straight, branch.taken())).isTrue();
  }

  @Test
  public void advance_permitsEverythingAfterThePathEnds() {
    Branch first = branch(1, 2);
    Branch second = branch(3, 4);

    DecisionGraph graph = path(first.taken());

    assertThat(permits(graph, first.taken(), second.taken())).isTrue();
    assertThat(permits(graph, first.taken(), second.other())).isTrue();
  }

  @Test
  public void prepend_putsNewDecisionsBeforeExistingOnes() {
    Branch first = branch(1, 2);
    Branch second = branch(3, 4);

    DecisionGraph graph = path(second.taken()).prepend(ImmutableList.of(first.taken()));

    assertThat(graph.size()).isEqualTo(2);
    assertThat(permits(graph, first.taken(), second.taken())).isTrue();
    assertThat(permits(graph, first.taken(), second.other())).isFalse();
    assertThat(permits(graph, second.taken())).isFalse();
  }

  @Test
  public void then_appendsTheSuffixAtEveryEnd() {
    Branch first = branch(1, 2);
    Branch second = branch(3, 4);
    DecisionGraph prefix =
        DecisionGraph.union(ImmutableList.of(path(first.taken()), path(first.other())));

    DecisionGraph graph = prefix.then(path(second.taken()));

    assertThat(graph.size()).isEqualTo(2);
    assertThat(permits(graph, first.taken(), second.taken())).isTrue();
    assertThat(permits(graph, first.other(), second.taken())).isTrue();
    assertThat(permits(graph, first.taken(), second.other())).isFalse();
  }

  @Test
  public void union_withSingleElement_returnsItUnchanged() {
    DecisionGraph single = path(branch(1, 2).taken());

    assertThat(DecisionGraph.union(ImmutableList.of(single))).isSameInstanceAs(single);
  }

  @Test
  public void union_withEmptyCollection_throws() {
    assertThrows(IllegalArgumentException.class, () -> DecisionGraph.union(ImmutableList.of()));
  }

  @Test
  public void union_permitsThePathsOfEveryChoice() {
    Branch first = branch(1, 2);
    Branch second = branch(3, 4);
    Branch unrelated = branch(5, 6);
    // Different continuations after the first decision, e.g. from different successor blocks.
    DecisionGraph viaTaken = path(first.taken(), second.taken());
    DecisionGraph viaOther = path(first.other(), second.other());

    DecisionGraph union = DecisionGraph.union(ImmutableList.of(viaTaken, viaOther));

    assertThat(permits(union, first.taken(), second.taken())).isTrue();
    assertThat(permits(union, first.other(), second.other())).isTrue();
    assertThat(permits(union, first.taken(), second.other())).isFalse();
    assertThat(permits(union, first.other(), second.taken())).isFalse();
    assertThat(permits(union, unrelated.taken())).isFalse();
  }

  @Test
  public void equals_isStructuralNotReference() {
    // Equal witnesses can be produced by different code paths, e.g. once via prepend and once
    // after being deserialized from a message.
    Branch branch = branch(1, 2);
    DecisionGraph first = path(branch.taken());
    DecisionGraph second = path(branch.taken());

    assertThat(first).isNotSameInstanceAs(second);
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void equals_detectsDifferentContent() {
    Branch branch = branch(1, 2);

    assertThat(path(branch.taken())).isNotEqualTo(path(branch.other()));
  }

  @Test
  public void serialize_roundTrips() {
    Branch first = branch(1, 2);
    Branch second = branch(3, 4);
    DecisionGraph shared = path(second.taken());
    List<DecisionGraph> graphs =
        ImmutableList.of(
            DecisionGraph.EMPTY,
            path(first.taken(), second.other()),
            DecisionGraph.union(
                ImmutableList.of(
                    shared.prepend(ImmutableList.of(first.taken())),
                    shared.prepend(ImmutableList.of(first.other())),
                    DecisionGraph.EMPTY)));

    for (DecisionGraph graph : graphs) {
      DecisionGraph deserialized = DecisionGraph.deserialize(graph.serialize());

      assertThat(deserialized).isEqualTo(graph);
      assertThat(deserialized.size()).isEqualTo(graph.size());
    }
  }

  @Test
  public void transformEdges_replacesMappedEdgesAndLeavesOthersUnchanged() {
    Branch mapped = branch(1, 2);
    Branch unmapped = branch(3, 4);
    Branch replacement = branch(5, 6);
    DecisionGraph original = path(mapped.taken(), unmapped.taken());

    DecisionGraph transformed =
        original.transformEdges(ImmutableMap.of(mapped.taken(), replacement.taken()));

    assertThat(transformed).isEqualTo(path(replacement.taken(), unmapped.taken()));
  }

  @Test
  public void transferRelation_followsTheGraph() {
    Branch first = branch(1, 2);
    Branch second = branch(3, 4);
    PathRestrictionTransferRelation transfer = new PathRestrictionTransferRelation();
    Precision precision = mock(Precision.class);
    AbstractState initial = path(first.taken(), second.taken()).start();

    assertThat(transfer.getAbstractSuccessorsForEdge(initial, precision, first.other())).isEmpty();
    PathPosition next =
        Iterables.getOnlyElement(
            transfer.getAbstractSuccessorsForEdge(initial, precision, first.taken()));
    assertThat(transfer.getAbstractSuccessorsForEdge(next, precision, second.taken())).hasSize(1);
    assertThat(transfer.getAbstractSuccessorsForEdge(next, precision, second.other())).isEmpty();
  }
}
