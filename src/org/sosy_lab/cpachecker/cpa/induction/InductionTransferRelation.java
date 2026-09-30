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
 * split into the base case and an induction step, or to cut the path off.
 */
final class InductionTransferRelation extends SingleEdgeTransferRelation {

  private final LoopStructure loopStructure;

  final StatCounter abstractions = new StatCounter("Abstractions at loop heads");
  final StatCounter inductionStarts = new StatCounter("Induction starts");
  final StatCounter cutOffs = new StatCounter("Cut-offs");
  final StatCounter suppressedTargets = new StatCounter("Targets not reported in induction step");

  InductionTransferRelation(LoopStructure pLoopStructure) {
    loopStructure = pLoopStructure;
  }

  @Override
  public Collection<InductionState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pEdge) {
    InductionState state = (InductionState) pState;
    InductionPrecision precision = (InductionPrecision) pPrecision;
    if (state.isCutOff()) {
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

    PersistentSortedMap<Loop, Integer> arrivals = state.getArrivals();
    @Nullable Loop boundedLoop = null;
    for (Loop loop : loops) {
      int count = arrivals.getOrDefault(loop, 0) + 1;
      arrivals = arrivals.putAndCopy(loop, count);
      if (boundedLoop == null && count > precision.get(loop).k()) {
        boundedLoop = loop;
      }
    }
    if (boundedLoop == null) {
      return ImmutableList.of(state.concrete(arrivals, true, reportsTargets));
    }

    LoopPrecision loopPrecision = precision.get(boundedLoop);
    if (!loopPrecision.describes(state.getAbstractionIndex(successor))) {
      cutOffs.inc();
      return ImmutableList.of(state.cutOff(arrivals, successor, loopPrecision, reportsTargets));
    }
    if (arrivals.get(boundedLoop) > loopPrecision.k() + 1) {
      // the continuation of the base case after an induction start ends here
      return ImmutableList.of();
    }
    abstractions.inc();
    if (state.isInInductionStep() || !loopPrecision.induction()) {
      return ImmutableList.of(state.abstraction(successor, loopPrecision, false, reportsTargets));
    }
    inductionStarts.inc();
    return ImmutableList.of(
        state.concrete(arrivals, true, reportsTargets),
        state.abstraction(successor, loopPrecision, true, reportsTargets));
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
