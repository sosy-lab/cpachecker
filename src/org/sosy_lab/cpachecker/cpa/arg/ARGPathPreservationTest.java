// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;

public class ARGPathPreservationTest {

  private record State(CFANode node, int value) implements AbstractStateWithLocation {
    @Override
    public CFANode getLocationNode() {
      return node;
    }
  }

  private ARGState root;
  private ARGState left;
  private ARGState right;
  private ARGState reached;
  private ARGState incoming;

  @Before
  public void setUp() {
    root = state(CFANode.newDummyCFANode(), null);
    left = state(CFANode.newDummyCFANode(), root);
    right = state(CFANode.newDummyCFANode(), root);
    CFANode join = CFANode.newDummyCFANode();
    reached = state(join, left);
    incoming = state(join, right);
  }

  private static ARGState state(CFANode pNode, ARGState pParent) {
    return new ARGState(new State(pNode, 0), pParent);
  }

  private static ARGStopSep stop(boolean pAccept, boolean pPreserve) {
    return new ARGStopSep(
        (s, r, p) -> pAccept, LogManager.createTestLogManager(), false, false, false, pPreserve);
  }

  @Test
  public void coveragePreservesBothPrefixesWithoutAMerge() throws Exception {
    assertThat(
            stop(true, true)
                .stop(incoming, ImmutableList.of(reached), SingletonPrecision.getInstance()))
        .isTrue();
    assertThat(reached.getParents()).containsExactly(left, right);
    assertThat(right.getChildren()).containsExactly(reached);
    assertThat(reached.getCoveredByThis()).isEmpty();
  }

  @Test
  public void rejectedCoverageDoesNotChangeProvenance() throws Exception {
    assertThat(
            stop(false, true)
                .stop(incoming, ImmutableList.of(reached), SingletonPrecision.getInstance()))
        .isFalse();
    assertThat(reached.getParents()).containsExactly(left);
    assertThat(incoming.getParents()).containsExactly(right);
  }

  @Test
  public void ordinaryCoverageKeepsItsExistingBehaviour() throws Exception {
    assertThat(
            stop(true, false)
                .stop(incoming, ImmutableList.of(reached), SingletonPrecision.getInstance()))
        .isTrue();
    assertThat(reached.getParents()).containsExactly(left);
    assertThat(incoming.isCovered()).isTrue();
    assertThat(incoming.getCoveringState()).isSameInstanceAs(reached);
  }

  @Test
  public void coveringAnAncestorCannotLoseLoopIterations() throws Exception {
    ARGState backEdge = state(((State) root.getWrappedState()).node(), incoming);
    assertThat(
            stop(true, true)
                .stop(backEdge, ImmutableList.of(root), SingletonPrecision.getInstance()))
        .isFalse();
    assertThat(root.getParents()).isEmpty();
  }

  @Test
  public void cannotSharePathsBetweenDifferentLocations() throws Exception {
    assertThat(
            stop(true, true)
                .stop(incoming, ImmutableList.of(left), SingletonPrecision.getInstance()))
        .isFalse();
  }

  private ARGMergeJoin merge(MergeOperator pWrapped) throws Exception {
    return new ARGMergeJoin(
        pWrapped,
        new FlatLatticeDomain(),
        LogManager.createTestLogManager(),
        new ARGMergeJoin.MergeOptions(
            Configuration.builder().setOption("cpa.arg.preservePaths", "true").build()));
  }

  @Test
  public void cycleCheckRunsBeforeWrappedMergeSideEffects() throws Exception {
    AtomicBoolean called = new AtomicBoolean();
    ARGState backEdge = state(((State) root.getWrappedState()).node(), incoming);
    ARGMergeJoin merge =
        merge(
            (a, b, p) -> {
              called.set(true);
              return b;
            });
    assertThat(merge.merge(backEdge, root, SingletonPrecision.getInstance()))
        .isSameInstanceAs(root);
    assertThat(called.get()).isFalse();
  }

  @Test
  public void successfulMergePreservesBothPrefixesAndExistingSuffix() throws Exception {
    ARGState child = state(CFANode.newDummyCFANode(), reached);
    ARGMergeJoin merge = merge((a, b, p) -> new State(((State) b).node(), 1));
    ARGState merged = (ARGState) merge.merge(incoming, reached, SingletonPrecision.getInstance());
    assertThat(merged.getParents()).containsExactly(left, right);
    assertThat(child.getParents()).containsExactly(merged);
    assertThat(incoming.getMergedWith()).isSameInstanceAs(merged);
    assertThat(
            stop(true, true)
                .stop(incoming, ImmutableList.of(merged), SingletonPrecision.getInstance()))
        .isTrue();
    assertThat(merged.getParents()).containsExactly(left, right);
  }

  @Test
  public void rejectedMergeLeavesBothOccurrencesAvailable() throws Exception {
    ARGMergeJoin merge = merge((a, b, p) -> b);
    assertThat(merge.merge(incoming, reached, SingletonPrecision.getInstance()))
        .isSameInstanceAs(reached);
    assertThat(reached.getParents()).containsExactly(left);
    assertThat(incoming.getParents()).containsExactly(right);
    assertThat(incoming.getMergedWith()).isNull();
  }

  @Test
  public void coveragePreservesDifferentEdgesFromTheSameParent() throws Exception {
    CFANode from = ((State) left.getWrappedState()).node();
    CFANode to = ((State) reached.getWrappedState()).node();
    var first = ImmutableList.of(new BlankEdge("", FileLocation.DUMMY, from, to, "first"));
    var second = ImmutableList.of(new BlankEdge("", FileLocation.DUMMY, from, to, "second"));
    reached.addParentWithPaths(left, ImmutableList.of(ImmutableList.copyOf(first)));
    ARGState another = state(to, left);
    another.addParentWithPaths(left, ImmutableList.of(ImmutableList.copyOf(second)));
    assertThat(
            stop(true, true)
                .stop(another, ImmutableList.of(reached), SingletonPrecision.getInstance()))
        .isTrue();
    assertThat(reached.getPathsFromParent(left)).containsExactly(first, second);

    ARGState replacement = state(to, null);
    reached.replaceInARGWith(replacement);
    assertThat(replacement.getPathsFromParent(left)).containsExactly(first, second);
  }
}
