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

  private final boolean stopIt;

  private int hashCache = 0;

  public LoopBoundState() {
    this(new LoopStack(UndeterminedLoopIterationState.newState()), false);
  }

  private LoopBoundState(LoopStack pLoopStack, boolean pStopIt) {
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
      return new LoopBoundState(loopStack.pop(), stopIt);
    }
    return this;
  }

  public LoopBoundState enter(Loop pLoop) {
    return new LoopBoundState(loopStack.push(DeterminedLoopIterationState.newState(pLoop)), stopIt);
  }

  public LoopBoundState visitLoopHead(Loop pLoop) {
    assert !loopStack.isEmpty()
        : "Visiting loop head without entering the loop. Explicitly use an"
            + " UndeterminedLoopIterationState if you cannot determine the loop entry.";
    if (isLoopCounterAbstracted()) {
      return this;
    }
    LoopIterationState loopIterationState = loopStack.peek();
    LoopIterationState newLoopIterationState = loopIterationState.visitLoopHead(pLoop);
    if (newLoopIterationState != loopIterationState) {
      return new LoopBoundState(loopStack.pop().push(newLoopIterationState), stopIt);
    }
    return this;
  }

  public LoopBoundState setStop(boolean pStop) {
    if (stopIt == pStop) {
      return this;
    }
    return new LoopBoundState(loopStack, pStop);
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
        + ", stack depth "
        + getDepth()
        + " ["
        + Integer.toHexString(System.identityHashCode(loopStack.pop()))
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LoopBoundState)) {
      return false;
    }

    LoopBoundState other = (LoopBoundState) obj;
    return stopIt == other.stopIt && loopStack.equals(other.loopStack);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(stopIt, loopStack);
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
    for (LoopIterationState loopIterationState : loopStack) {
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
    int deepestIteration = 0;
    for (LoopIterationState loopIterationState : loopStack) {
      deepestIteration = Math.max(deepestIteration, loopIterationState.getMaxIterationCount());
    }
    return deepestIteration;
  }

  @Override
  public Set<Loop> getDeepestIterationLoops() {
    if (loopStack.isEmpty()) {
      return ImmutableSet.of();
    }
    int deepestIteration = getDeepestIteration();
    return FluentIterable.from(loopStack)
        .filter(l -> l.getMaxIterationCount() == deepestIteration)
        .transformAndConcat(l -> l.getDeepestIterationLoops())
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
    return new LoopBoundState(loopStack.pop().push(newLoopIterationState), stopIt);
  }

  public int getMaxNumberOfIterationsInLoopstackFrame() {
    return loopStack.peek().getMaxIterationCount();
  }
}
