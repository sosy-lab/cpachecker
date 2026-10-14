// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.induction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.induction.InductionPrecision.LoopPrecision;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

/**
 * Counts the arrivals at the loops as {@link org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundCPA}
 * does, and decides at the (k+1)-th arrival since the last abstraction whether to abstract, to
 * split into the base case and an induction step, or to cut the path off. At a loop with several
 * heads, every arrival also starts an induction step.
 */
final class InductionTransferRelation extends SingleEdgeTransferRelation {

  private final LoopStructure loopStructure;
  private final boolean exploresNothing;

  final StatCounter abstractions = new StatCounter("Abstractions at loop heads");
  final StatCounter inductionStarts = new StatCounter("Induction starts");
  final StatCounter earlyInductionSteps =
      new StatCounter("Induction steps before the bound at loops with several heads");
  final StatCounter cutOffs = new StatCounter("Cut-offs");
  final StatCounter suppressedTargets = new StatCounter("Targets not reported in induction step");

  InductionTransferRelation(LoopStructure pLoopStructure, boolean pExploresNothing) {
    loopStructure = pLoopStructure;
    exploresNothing = pExploresNothing;
  }

  @Override
  public Collection<InductionState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pEdge) {
    InductionState state = (InductionState) pState;
    InductionPrecision precision = (InductionPrecision) pPrecision;
    if (exploresNothing || state.isCutOff()) {
      return ImmutableList.of();
    }

    boolean reportsTargets = state.successorsReportTargets(precision);
    CFANode successor = pEdge.getSuccessor();
    ImmutableSet<Loop> loops =
        pEdge instanceof FunctionCallEdge || pEdge instanceof FunctionReturnEdge
            ? ImmutableSet.of()
            : loopStructure.getLoopsForLoopHead(successor);
    if (loops.isEmpty()) {
      return ImmutableList.of(state.concrete(state.getArrivals(), false, reportsTargets));
    }

    boolean beforeFirstAbstraction = state.successorsInFirstBlock();
    PersistentSortedMap<Loop, Integer> arrivals = state.getArrivals();
    @Nullable Loop boundedLoop = null;
    for (Loop loop : loops) {
      int count = arrivals.getOrDefault(loop, 0) + 1;
      arrivals = arrivals.putAndCopy(loop, count);
      if (boundedLoop == null && count > precision.get(loop).bound(beforeFirstAbstraction)) {
        boundedLoop = loop;
      }
    }
    if (boundedLoop == null) {
      InductionState concrete = state.concrete(arrivals, true, reportsTargets);
      @Nullable InductionState start =
          earlyInductionStep(state, precision, loops, successor, reportsTargets);
      return start == null ? ImmutableList.of(concrete) : ImmutableList.of(concrete, start);
    }

    LoopPrecision loopPrecision = precision.get(boundedLoop);
    if (!loopPrecision.describes(state.getAbstractionIndex(successor))) {
      cutOffs.inc();
      return ImmutableList.of(state.cutOff(arrivals, successor, loopPrecision, reportsTargets));
    }
    if (state.continuesBaseCase()
        && arrivals.get(boundedLoop) > loopPrecision.bound(beforeFirstAbstraction) + 1) {
      // the continuation of the base case after an induction start ends here
      return ImmutableList.of();
    }
    abstractions.inc();
    if (state.isInInductionStep() || !loopPrecision.induction()) {
      return ImmutableList.of(
          state.abstraction(successor, loops, loopPrecision, precision, false, reportsTargets));
    }
    inductionStarts.inc();
    InductionState inductionStart =
        state.abstraction(successor, loops, loopPrecision, precision, true, reportsTargets);
    return ImmutableList.of(
        state.continuation(arrivals, reportsTargets, precision), inductionStart);
  }

  /**
   * The induction step that starts before the bound at a head of a loop with several heads, or
   * null. The abstractions at the bound may always reach the same head of such a loop, so that no
   * step would check the property after the other heads. Native k-induction starts its step case at
   * every loop head.
   */
  private @Nullable InductionState earlyInductionStep(
      InductionState pState,
      InductionPrecision pPrecision,
      Set<Loop> pLoops,
      CFANode pLoopHead,
      boolean pReportsTargets) {
    for (Loop loop : pLoops) {
      LoopPrecision loopPrecision = pPrecision.get(loop);
      if (loop.getLoopHeads().size() > 1
          && (pState.isInInductionStep() || loopPrecision.induction())
          && loopPrecision.describes(pState.getAbstractionIndex(pLoopHead))) {
        abstractions.inc();
        earlyInductionSteps.inc();
        boolean startsInductionStep = !pState.isInInductionStep();
        if (startsInductionStep) {
          inductionStarts.inc();
        }
        return pState.abstraction(
            pLoopHead, pLoops, loopPrecision, pPrecision, startsInductionStep, pReportsTargets);
      }
    }
    return null;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision) {
    InductionState state = (InductionState) pState;
    boolean target =
        Iterables.any(
            pOtherStates,
            other -> !(other instanceof InductionState) && AbstractStates.isTargetState(other));
    if (target && !state.reportsTargets()) {
      suppressedTargets.inc();
      return ImmutableList.of();
    }
    if (target && state.isInductionStart()) {
      return ImmutableList.of();
    }
    return ImmutableList.of(state);
  }
}
