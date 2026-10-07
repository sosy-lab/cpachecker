// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;

public class CallstackStateCoverageOperatorTest {

  private CallstackStateCoverageOperator operator;

  /**
   * One dummy call node per function name. Two stacks that were built independently but that name
   * the same functions therefore have equal call nodes, just like two runs of a callstack analysis
   * on the same program.
   */
  private Map<String, CFANode> callNodes;

  @Before
  public void setUp() {
    operator = new CallstackStateCoverageOperator();
    callNodes = new HashMap<>();
  }

  /**
   * A DSS callstack state for the given stack.
   *
   * @param pCanBeTopState whether the bottom frame of the stack stands for an unknown callstack
   * @param pFunctions the functions on the stack, outermost function first
   */
  private DssCallstackState state(boolean pCanBeTopState, String... pFunctions) {
    CallstackState stack = null;
    for (String function : pFunctions) {
      CFANode callNode =
          callNodes.computeIfAbsent(function, name -> CFANode.newDummyCFANode("caller_of_" + name));
      stack = new CallstackState(stack, function, callNode);
    }
    return new DssCallstackState(stack, pCanBeTopState);
  }

  /**
   * If the covering state knows its callstack, coverage requires the two stacks to be structurally
   * equal. They need not be the same objects, though: {@link CallstackState} uses identity
   * equality, but two independent analyses of the same block create separate state objects.
   */
  @Test
  public void equalStacksAreSubsumedIfCoveringStackIsKnown() {
    DssCallstackState covered = state(false, "main", "f", "g");
    DssCallstackState covering = state(false, "main", "f", "g");

    assertThat(operator.isSubsumed(covered, covering)).isTrue();
  }

  /**
   * A state that may be a top state is not subsumed by a state with a known callstack, not even if
   * the two stacks are currently equal. The flag is inherited by all successors, so the covered
   * state may still return to a stack of depth one and then allow transfers that the covering state
   * prunes. Exploring the covering state therefore does not cover everything the covered state can
   * reach.
   */
  @Test
  public void topStateIsNotSubsumedByStateWithKnownStack() {
    assertThat(operator.isSubsumed(state(true, "main", "f"), state(false, "main", "f"))).isFalse();
    assertThat(operator.isSubsumed(state(true, "f"), state(false, "f"))).isFalse();
  }

  /**
   * The other way round it works: a covering state that may be a top state is at least as
   * permissive as the covered state, whether or not the covered state has that flag.
   */
  @Test
  public void topStateFlagOfCoveredStateIsIrrelevantForCoveringTopState() {
    assertThat(operator.isSubsumed(state(true, "main", "f"), state(true, "main", "f"))).isTrue();
    assertThat(operator.isSubsumed(state(false, "main", "f"), state(true, "main", "f"))).isTrue();
  }

  @Test
  public void differentFunctionIsNotSubsumedIfCoveringStackIsKnown() {
    assertThat(operator.isSubsumed(state(false, "main", "f"), state(false, "main", "g"))).isFalse();
  }

  /**
   * A known callstack does not permit any wildcard, so a deeper stack is not covered even if its
   * innermost frames match.
   */
  @Test
  public void deeperStackIsNotSubsumedIfCoveringStackIsKnown() {
    assertThat(operator.isSubsumed(state(false, "main", "f", "g"), state(false, "f", "g")))
        .isFalse();
  }

  /**
   * The bottom frame of a covering state that may be a top state stands for an arbitrary unknown
   * callstack, so a state whose stack consists of that frame only covers every state.
   */
  @Test
  public void topStateSubsumesEveryStack() {
    DssCallstackState topState = state(true, "f");

    assertThat(operator.isSubsumed(state(false, "main", "h", "f"), topState)).isTrue();
    assertThat(operator.isSubsumed(state(false, "unrelated"), topState)).isTrue();
    assertThat(operator.isSubsumed(topState, topState)).isTrue();
  }

  /**
   * The frames above the wildcard have to match, the frames that the wildcard stands for do not.
   */
  @Test
  public void deeperStackIsSubsumedIfKnownFramesMatch() {
    // the covering state started its block in "f" without knowing that "f" was called from "main"
    assertThat(operator.isSubsumed(state(false, "main", "h", "f", "g"), state(true, "f", "g")))
        .isTrue();
  }

  /** Even the wildcard does not help if a frame above it differs. */
  @Test
  public void differingKnownFrameIsNotSubsumed() {
    assertThat(operator.isSubsumed(state(false, "main", "f", "h"), state(true, "f", "g")))
        .isFalse();
  }

  /**
   * The wildcard stands for at least the bottom frame, so it does not cover a state whose stack ran
   * out of frames while the known frames were compared.
   */
  @Test
  public void shorterStackIsNotSubsumedByDeeperTopState() {
    assertThat(operator.isSubsumed(state(false, "g"), state(true, "main", "f", "g"))).isFalse();
  }

  /**
   * Only the functions and call nodes of a frame are compared, not the identity of the state
   * objects, so a stack that a different analysis created is covered.
   */
  @Test
  public void wildcardComparesFramesStructurally() {
    DssCallstackState covered = state(false, "main", "f", "g");
    DssCallstackState covering = state(true, "f", "g");

    assertThat(covered.getWrappedState().getPreviousState())
        .isNotSameInstanceAs(covering.getWrappedState().getPreviousState());
    assertThat(operator.isSubsumed(covered, covering)).isTrue();
  }
}
