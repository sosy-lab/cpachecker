// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public final class SegmentedPathsTest {

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

  /**
   * An edge whose predecessor has no other exit, so it must be dropped by {@code addEdgesToFront}.
   */
  private static CFAEdge straightEdge(int predecessorNumber, int successorNumber) {
    CFANode predecessor = mockNode(predecessorNumber);
    CFAEdge edge = mockEdge(predecessor, mockNode(successorNumber));
    setLeavingEdges(predecessor, edge);
    return edge;
  }

  /** An edge whose predecessor has a sibling exit, i.e. a real decision edge. */
  private static CFAEdge decisionEdge(int predecessorNumber, int successorNumber) {
    CFANode predecessor = mockNode(predecessorNumber);
    CFAEdge edge = mockEdge(predecessor, mockNode(successorNumber));
    CFAEdge sibling = mockEdge(predecessor, mockNode(successorNumber + 1000));
    setLeavingEdges(predecessor, edge, sibling);
    return edge;
  }

  @Test
  public void addEdgesToFront_keepsOnlyDecisionEdges() {
    CFAEdge decision = decisionEdge(1, 2);
    CFAEdge straight = straightEdge(2, 3);

    SegmentedPaths result =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decision, straight));

    assertThat(result.paths)
        .containsExactly(ImmutableSet.of(ImmutableList.of(SegmentedPaths.edgeToString(decision))));
  }

  @Test
  public void addEdgesToFront_withNoDecisionEdges_yieldsSegmentWithEmptyPath() {
    CFAEdge straight = straightEdge(1, 2);

    SegmentedPaths result = SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(straight));

    assertThat(result.paths).containsExactly(ImmutableSet.of(ImmutableList.of()));
  }

  @Test
  public void equals_isStructuralNotReference() {
    // Two separately-constructed instances built from equivalent input must be equal, since equal
    // witnesses can legitimately be produced by different code paths (e.g. once via
    // addEdgesToFront and once after being deserialized from a message).
    SegmentedPaths first =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decisionEdge(1, 2)));
    SegmentedPaths second =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decisionEdge(1, 2)));

    assertThat(first).isNotSameInstanceAs(second);
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void equals_detectsDifferentContent() {
    SegmentedPaths first =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decisionEdge(1, 2)));
    SegmentedPaths second =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decisionEdge(1, 3)));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  public void equals_distinguishesEmptySegmentsFromNoSegments() {
    // A segment produced for a straight-line path (no decision edges) still exists (it contains a
    // single empty path), so it must not collapse into "no segments at all" (SegmentedPaths.EMPTY).
    SegmentedPaths noSegments = SegmentedPaths.EMPTY;
    SegmentedPaths oneEmptySegment =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(straightEdge(1, 2)));

    assertThat(noSegments).isNotEqualTo(oneEmptySegment);
  }

  @Test
  public void merge_withSingleElement_returnsItUnchanged() {
    SegmentedPaths single =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decisionEdge(1, 2)));

    assertThat(SegmentedPaths.merge(ImmutableList.of(single))).isSameInstanceAs(single);
  }

  @Test
  public void merge_withEmptyCollection_throws() {
    assertThrows(IllegalArgumentException.class, () -> SegmentedPaths.merge(ImmutableList.of()));
  }

  @Test
  public void merge_unionsFrontSegmentsAndKeepsRemainingSegmentsFromFirst() {
    SegmentedPaths tail =
        SegmentedPaths.EMPTY.addEdgesToFront(ImmutableList.of(decisionEdge(1, 2)));
    CFAEdge branchA = decisionEdge(10, 11);
    CFAEdge branchB = decisionEdge(20, 21);
    SegmentedPaths first = tail.addEdgesToFront(ImmutableList.of(branchA));
    SegmentedPaths second = tail.addEdgesToFront(ImmutableList.of(branchB));

    SegmentedPaths merged = SegmentedPaths.merge(ImmutableList.of(first, second));

    assertThat(merged.paths.getFirst())
        .containsExactly(
            ImmutableList.of(SegmentedPaths.edgeToString(branchA)),
            ImmutableList.of(SegmentedPaths.edgeToString(branchB)));
    assertThat(merged.paths.subList(1, merged.paths.size())).isEqualTo(tail.paths);
  }

  @Test
  public void transformEdges_replacesMappedEdgesAndLeavesOthersUnchanged() {
    CFAEdge mapped = decisionEdge(1, 2);
    CFAEdge unmapped = decisionEdge(3, 4);
    CFAEdge replacement = decisionEdge(5, 6);
    SegmentedPaths original =
        SegmentedPaths.EMPTY
            .addEdgesToFront(ImmutableList.of(unmapped))
            .addEdgesToFront(ImmutableList.of(mapped));

    SegmentedPaths transformed = original.transformEdges(ImmutableMap.of(mapped, replacement));

    assertThat(transformed.paths)
        .containsExactly(
            ImmutableSet.of(ImmutableList.of(SegmentedPaths.edgeToString(replacement))),
            ImmutableSet.of(ImmutableList.of(SegmentedPaths.edgeToString(unmapped))))
        .inOrder();
  }
}
