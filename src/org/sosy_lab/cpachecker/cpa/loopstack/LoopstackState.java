/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.loopstack;

import static com.google.common.base.Preconditions.*;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import com.google.common.base.Objects;

public class LoopstackState implements AbstractState, Partitionable, AvoidanceReportingState {

  /**
   * Parent loop.
   */
  private final @Nullable LoopstackState previousState;

  /**
   * The loop we are currently in.
   */
  private final @Nullable Loop loop;

  /**
   * The depth of the stack of LoopstackStates.
   */
  private final int depth;

  /**
   * Number of iterations within the current loop.
   * Starts at one for any state inside the loop returned by
   * LoopstackTransferRelation.
   */
  private final int iteration;
  private final boolean stop;
  private final boolean loopCounterAbstracted;

  private int hashCache = 0;

  public LoopstackState(
      LoopstackState previousElement, Loop loop, int iteration, boolean stop,
      boolean pLoopCounterAbstracted) {
    this.previousState = checkNotNull(previousElement);
    this.loop = checkNotNull(loop);
    this.depth = previousElement.getDepth() + 1;
    checkArgument(iteration >= 0);
    this.iteration = iteration;
    this.stop = stop;
    loopCounterAbstracted = pLoopCounterAbstracted;
  }

  public LoopstackState() {
    previousState = null;
    loop = null;
    depth = 0;
    iteration = 0;
    stop = false;
    loopCounterAbstracted = false;
  }

  public boolean isLoopCounterAbstracted() {
    return loopCounterAbstracted;
  }

  public LoopstackState getPreviousState() {
    return previousState;
  }

  public Loop getLoop() {
    return loop;
  }

  public int getDepth() {
    return depth;
  }

  public int getIteration() {
    return iteration;
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return stop;
  }

  @Override
  public String toString() {
    if (loop == null) {
      return "Loop stack empty";
    } else {
      return " Loop starting at node " + loop.getLoopHeads() + " in iteration " + iteration
           + ", stack depth " + depth
           + " [" + Integer.toHexString(super.hashCode()) + "]";
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LoopstackState)) {
      return false;
    }

    LoopstackState other = (LoopstackState)obj;
    return (this.previousState == other.previousState)
        && (this.iteration == other.iteration)
        && (this.loop == other.loop);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hashCode(iteration, loop, previousState);
    }
    return hashCache;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    if (stop) {
      return PreventingHeuristic.LOOPITERATIONS.getFormula(manager, iteration);
    } else {
      return bfmgr.makeTrue();
    }
  }
}
