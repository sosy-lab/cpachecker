// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopIterationState.DeterminedLoopIterationState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopIterationState.UndeterminedLoopIterationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class LoopBoundState
    implements AbstractState, Partitionable, AvoidanceReportingState, LoopIterationReportingState {

  private final LoopStack loopStack;

  /**
   * The largest iteration counts of the represented paths if this state was merged from states with
   * different iteration counts (see {@link LoopBoundRangeDomain}); {@link #loopStack} then holds
   * the smallest ones. Null if all represented paths have the iteration counts of {@link
   * #loopStack}.
   */
  private final @Nullable LoopStack upperBounds;

  private final boolean stopIt;

  private int hashCache = 0;

  public LoopBoundState() {
    this(new LoopStack(UndeterminedLoopIterationState.newState()), false);
  }

  private LoopBoundState(LoopStack pLoopStack, boolean pStopIt) {
    this(pLoopStack, null, pStopIt);
  }

  private LoopBoundState(LoopStack pLoopStack, @Nullable LoopStack pUpperBounds, boolean pStopIt) {
    Preconditions.checkArgument(
        pUpperBounds == null || (pLoopStack.getSize() == 1 && pUpperBounds.getSize() == 1),
        "Iteration ranges are only supported without tracking the loop stack");
    upperBounds = pUpperBounds;
    loopStack = Objects.requireNonNull(pLoopStack);
    Preconditions.checkArgument(
        !pLoopStack.isEmpty(),
        "Always initialize the stack with an UndeterminedLoopIterationState");
    Preconditions.checkArgument(
        (pLoopStack.getSize() == 1 && !pLoopStack.peek().isEntryKnown())
            || (pLoopStack.getSize() > 1 && pLoopStack.peek().isEntryKnown()),
        "The deepest element in the stack must be an UndeterminedLoopIterationState, and all other"
            + " elements must be determined.");
    stopIt = pStopIt;
  }

  public LoopBoundState exit(Loop pOldLoop) throws CPATransferException {
    assert !loopStack.isEmpty()
        : "Exiting loop without entering the loop. Explicitly use an UndeterminedLoopIterationState"
            + " if you cannot determine the loop entry.";
    LoopIterationState loopIterationState = loopStack.peek();
    if (loopIterationState.isEntryKnown()) {
      if (!pOldLoop.equals(loopIterationState.getLoop())) {
        throw new CPATransferException(
            "Unexpected exit from loop " + pOldLoop + " when loop stack is " + this);
      }
      Preconditions.checkState(upperBounds == null);
      return new LoopBoundState(loopStack.pop(), stopIt);
    }
    return this;
  }

  public LoopBoundState enter(Loop pLoop) {
    Preconditions.checkState(upperBounds == null);
    return new LoopBoundState(loopStack.push(DeterminedLoopIterationState.newState(pLoop)), stopIt);
  }

  public LoopBoundState visitLoopHead(Loop pLoop) {
    assert !loopStack.isEmpty()
        : "Visiting loop head without entering the loop. Explicitly use an"
            + " UndeterminedLoopIterationState if you cannot determine the loop entry.";
    if (isLoopCounterAbstracted()) {
      return this;
    }
    LoopStack newLoopStack = visitLoopHead(loopStack, pLoop);
    LoopStack newUpperBounds = upperBounds == null ? null : visitLoopHead(upperBounds, pLoop);
    if (newLoopStack != loopStack || newUpperBounds != upperBounds) {
      return new LoopBoundState(newLoopStack, newUpperBounds, stopIt);
    }
    return this;
  }

  private static LoopStack visitLoopHead(LoopStack pLoopStack, Loop pLoop) {
    LoopIterationState loopIterationState = pLoopStack.peek();
    LoopIterationState newLoopIterationState = loopIterationState.visitLoopHead(pLoop);
    if (newLoopIterationState != loopIterationState) {
      return pLoopStack.pop().push(newLoopIterationState);
    }
    return pLoopStack;
  }

  public LoopBoundState setStop(boolean pStop) {
    if (stopIt == pStop) {
      return this;
    }
    return new LoopBoundState(loopStack, upperBounds, pStop);
  }

  public boolean isLoopCounterAbstracted() {
    return loopStack.peek().isLoopCounterAbstracted();
  }

  @Override
  public Object getPartitionKey() {
    return setStop(false);
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return stopIt;
  }

  @Override
  public String toString() {
    return loopStack.peek()
        + (upperBounds == null ? "" : ", at most: " + upperBounds.peek())
        + ", stack depth "
        + getDepth()
        + " ["
        + Integer.toHexString(System.identityHashCode(loopStack.pop()))
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof LoopBoundState other
        && stopIt == other.stopIt
        && loopStack.equals(other.loopStack)
        && Objects.equals(upperBounds, other.upperBounds);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(stopIt, loopStack, upperBounds);
    }
    return hashCache;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    BooleanFormula reasonFormula = bfmgr.makeTrue();
    if (stopIt) {
      reasonFormula =
          bfmgr.and(
              reasonFormula,
              PreventingHeuristic.LOOPITERATIONS.getFormula(manager, getDeepestIteration()));
    }
    return reasonFormula;
  }

  @Override
  public int getIteration(Loop pLoop) {
    return getIteration(loopStack, pLoop);
  }

  @Override
  public int getIterationUpperBound(Loop pLoop) {
    return upperBounds == null ? getIteration(pLoop) : getIteration(upperBounds, pLoop);
  }

  private static int getIteration(LoopStack pLoopStack, Loop pLoop) {
    for (LoopIterationState loopIterationState : pLoopStack) {
      if (!loopIterationState.isEntryKnown()) {
        return loopIterationState.getLoopIterationCount(pLoop);
      }
      if (loopIterationState.getLoop().equals(pLoop)) {
        return loopIterationState.getLoopIterationCount(pLoop);
      }
    }
    return 0;
  }

  @Override
  public int getDeepestIteration() {
    return getDeepestIteration(loopStack);
  }

  @Override
  public int getDeepestIterationUpperBound() {
    return upperBounds == null ? getDeepestIteration() : getDeepestIteration(upperBounds);
  }

  private static int getDeepestIteration(LoopStack pLoopStack) {
    int deepestIteration = 0;
    for (LoopIterationState loopIterationState : pLoopStack) {
      deepestIteration = Math.max(deepestIteration, loopIterationState.getMaxIterationCount());
    }
    return deepestIteration;
  }

  @Override
  public boolean hasIterationRange() {
    return upperBounds != null;
  }

  @Override
  public Set<Loop> getDeepestIterationLoops() {
    if (loopStack.isEmpty()) {
      return ImmutableSet.of();
    }
    int deepestIteration = getDeepestIteration();
    return FluentIterable.from(loopStack)
        .filter(l -> l.getMaxIterationCount() == deepestIteration)
        .transformAndConcat(LoopIterationState::getDeepestIterationLoops)
        .toSet();
  }

  public int getDepth() {
    // Subtract 1 to account for the "undetermined" element at the bottom of the stack
    return loopStack.getSize() - 1;
  }

  public LoopBoundState enforceAbstraction(int pLoopIterationsBeforeAbstraction) {
    if (loopStack.isEmpty()) {
      return this;
    }
    LoopIterationState currentLoopIterationState = loopStack.peek();
    LoopIterationState newLoopIterationState =
        currentLoopIterationState.enforceAbstraction(pLoopIterationsBeforeAbstraction);
    if (currentLoopIterationState == newLoopIterationState) {
      return this;
    }
    Preconditions.checkState(upperBounds == null);
    return new LoopBoundState(loopStack.pop().push(newLoopIterationState), stopIt);
  }

  /**
   * Returns a state that represents the paths of both states: its lower bounds are the minimum and
   * its upper bounds the maximum of the iteration counts of both states.
   */
  LoopBoundState joinIterationRanges(LoopBoundState pOther) {
    if (coversIterationRanges(pOther)) {
      return this;
    }
    if (pOther.coversIterationRanges(this)) {
      return pOther;
    }
    return new LoopBoundState(
        new LoopStack(getOnlyFrame(loopStack).minimum(getOnlyFrame(pOther.loopStack))),
        new LoopStack(
            getOnlyFrame(upperOrLowerBounds()).maximum(getOnlyFrame(pOther.upperOrLowerBounds()))),
        stopIt && pOther.stopIt);
  }

  /** Returns whether the iteration ranges of this state enclose those of the other state. */
  boolean coversIterationRanges(LoopBoundState pOther) {
    return stopIt == pOther.stopIt
        && getOnlyFrame(loopStack).isAtMost(getOnlyFrame(pOther.loopStack))
        && getOnlyFrame(pOther.upperOrLowerBounds()).isAtMost(getOnlyFrame(upperOrLowerBounds()));
  }

  private LoopStack upperOrLowerBounds() {
    return upperBounds == null ? loopStack : upperBounds;
  }

  private static UndeterminedLoopIterationState getOnlyFrame(LoopStack pLoopStack) {
    Preconditions.checkState(
        pLoopStack.getSize() == 1,
        "Iteration ranges are only supported without tracking the loop stack");
    return (UndeterminedLoopIterationState) pLoopStack.peek();
  }

  public int getMaxNumberOfIterationsInLoopstackFrame() {
    return loopStack.peek().getMaxIterationCount();
  }
}
