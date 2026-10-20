// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DssBlockPathGraphTest {

  private record State(CFANode node) implements AbstractStateWithLocation {
    @Override
    public CFANode getLocationNode() {
      return node;
    }
  }

  private static ARGState state() {
    return new ARGState(new State(CFANode.newDummyCFANode()), null);
  }

  private static void connect(ARGState pParent, ARGState pChild) {
    CFANode from = AbstractStates.extractLocation(pParent);
    CFANode to = AbstractStates.extractLocation(pChild);
    BlankEdge edge = new BlankEdge("", FileLocation.DUMMY, from, to, "test");
    from.addLeavingEdge(edge);
    to.addEnteringEdge(edge);
    pChild.addParent(pParent);
  }

  @Test
  public void exponentiallyManyPathsAreCountedWithoutEnumeratingThem() {
    ARGState root = state();
    ARGState end = root;
    for (int i = 0; i < 60; i++) {
      ARGState left = state();
      ARGState right = state();
      ARGState join = state();
      connect(end, left);
      connect(end, right);
      connect(left, join);
      connect(right, join);
      end = join;
    }
    DssBlockPathGraph graph = new DssBlockPathGraph(root, ImmutableList.of(end));
    assertThat(graph.pathCount(end)).isEqualTo(BigInteger.ONE.shiftLeft(60));
    var iterator = graph.pathsTo(end).iterator();
    var first = iterator.next();
    var second = iterator.next();
    assertThat(first.getFullPath()).hasSize(120);
    assertThat(second.getFullPath()).hasSize(120);
    assertThat(first.getFullPath()).isNotEqualTo(second.getFullPath());
  }

  @Test
  public void snapshotSurvivesArgRemoval() {
    ARGState root = state();
    ARGState middle = state();
    ARGState end = state();
    connect(root, middle);
    connect(middle, end);
    DssBlockPathGraph graph = new DssBlockPathGraph(root, ImmutableList.of(end));
    var expected = graph.pathsTo(end).iterator().next().getFullPath();
    middle.removeFromARG();
    assertThat(graph.pathsTo(end).iterator().next().getFullPath()).isEqualTo(expected);
  }

  @Test
  public void snapshotsOfTheSameTargetRetainLateArrivingPaths() {
    ARGState root = state();
    ARGState left = state();
    ARGState end = state();
    connect(root, left);
    connect(left, end);
    DssBlockPathGraph first = new DssBlockPathGraph(root, ImmutableList.of(end));
    ARGState right = state();
    connect(root, right);
    connect(right, end);
    DssBlockPathGraph second = new DssBlockPathGraph(root, ImmutableList.of(end));
    var firstResult = new ArgPathAndCondition(first, end, null);
    var secondResult = new ArgPathAndCondition(second, end, null);
    assertThat(firstResult).isEqualTo(new ArgPathAndCondition(first, end, null));
    assertThat(firstResult).isNotEqualTo(secondResult);
    assertThat(firstResult.pathCount()).isEqualTo(BigInteger.ONE);
    assertThat(secondResult.pathCount()).isEqualTo(BigInteger.TWO);
  }

  @Test
  public void cyclesAreRejectedInsteadOfDroppingLoopIterations() {
    ARGState root = state();
    ARGState middle = state();
    ARGState end = state();
    connect(root, middle);
    connect(middle, end);
    connect(end, middle);
    var targets = ImmutableList.of(end);
    assertThrows(IllegalArgumentException.class, () -> new DssBlockPathGraph(root, targets));
  }
}
