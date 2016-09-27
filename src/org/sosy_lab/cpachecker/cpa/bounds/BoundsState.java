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
package org.sosy_lab.cpachecker.cpa.bounds;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class BoundsState implements AbstractState, Partitionable, AvoidanceReportingState {

  private final int deepestIteration;

  private final int deepestRecursion;

  private final boolean stopIt;

  private final boolean stopRec;

  private final PersistentSortedMap<ComparableLoop, Integer> iterations;

  private final String currentFunction;

  private final int returnFromCounter;

  private int hashCache = 0;

  public BoundsState() {
    this(false, false);
  }

  public BoundsState(boolean pStopIt, boolean pStopRec) {
    this(pStopIt, pStopRec, PathCopyingPersistentTreeMap.<ComparableLoop, Integer>of(), 0, 1, null, 0);
  }

  private BoundsState(boolean pStopIt, boolean pStopRec, PersistentSortedMap<ComparableLoop, Integer> pIterations, int pDeepestIteration, int pDeepestRecursion, String pCurrentFunction, int pReturnFromCounter) {
    Preconditions.checkArgument(pDeepestIteration >= 0);
    Preconditions.checkArgument(
        (pDeepestIteration == 0 && pIterations.isEmpty())
            || (pDeepestIteration > 0 && !pIterations.isEmpty()));
    Preconditions.checkArgument(pDeepestRecursion >= 1);
    this.stopIt = pStopIt;
    this.stopRec = pStopRec;
    this.iterations = pIterations;
    this.deepestIteration = pDeepestIteration;
    this.deepestRecursion = pDeepestRecursion;
    this.currentFunction = pCurrentFunction;
    this.returnFromCounter = pReturnFromCounter;
  }

  public BoundsState enter(Loop pLoop) {
    return enter(pLoop, Integer.MAX_VALUE);
  }

  public BoundsState enter(Loop pLoop, int pLoopIterationsBeforeAbstraction) {
    int iteration = getIteration(pLoop);
    if (pLoopIterationsBeforeAbstraction != 0
        && iteration >= pLoopIterationsBeforeAbstraction) {
      iteration = pLoopIterationsBeforeAbstraction;
    } else {
      ++iteration;
    }
    return new BoundsState(
        stopIt,
        stopRec,
        iterations.putAndCopy(new ComparableLoop(pLoop), iteration),
        iteration > deepestIteration ? iteration : deepestIteration,
        deepestRecursion,
        currentFunction,
        returnFromCounter);
  }



  public BoundsState stopIt() {
    return new BoundsState(true, stopRec, iterations, deepestIteration, deepestRecursion, currentFunction, returnFromCounter);
  }

  public BoundsState stopRec() {
    return new BoundsState(stopIt, true, iterations, deepestIteration, deepestRecursion, currentFunction, returnFromCounter);
  }

  public BoundsState setDeepestRecursion(int pDeepestRecursion) {
    Preconditions.checkArgument(pDeepestRecursion >= this.deepestRecursion);
    return new BoundsState(stopIt, stopRec, iterations, deepestIteration, pDeepestRecursion, currentFunction, returnFromCounter);
  }

  public BoundsState setCurrentFunction(String pCurrentFunction) {
    if (!Objects.equals(currentFunction, pCurrentFunction)) {
      return new BoundsState(stopIt, stopRec, iterations, deepestIteration, deepestRecursion, pCurrentFunction, 0);
    }
    return this;
  }

  public BoundsState returnFromFunction() {
    return new BoundsState(stopIt, stopRec, iterations, deepestIteration, deepestRecursion, currentFunction, returnFromCounter + 1);
  }

  public int getIteration(Loop pLoop) {
    Integer iteration = iterations.get(new ComparableLoop(pLoop));
    return iteration == null ? 0 : iteration;
  }

  public int getDeepestIteration() {
    return deepestIteration;
  }

  public int getDeepestRecursion() {
    return deepestRecursion;
  }

  public int getReturnFromCounter() {
    return returnFromCounter;
  }

  public Set<Loop> getDeepestIterationLoops() {
    return FluentIterable.from(iterations.entrySet()).filter(new Predicate<Entry<ComparableLoop, Integer>>() {

      @Override
      public boolean apply(Entry<ComparableLoop, Integer> pArg0) {
        return pArg0.getValue() == getDeepestIteration();
      }

    }).transform(new Function<Entry<ComparableLoop, Integer>, Loop>() {

      @Override
      public Loop apply(Entry<ComparableLoop, Integer> pArg0) {
        return pArg0.getKey().loop;
      }

    }).toSet();
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  public boolean isStopState() {
    return stopIt || stopRec;
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return isStopState();
  }

  @Override
  public String toString() {
    return " Deepest loop iteration " + deepestIteration
         + ", deepest recursion " + deepestRecursion;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof BoundsState)) {
      return false;
    }

    BoundsState other = (BoundsState)obj;
    return this.stopIt == other.stopIt
        && this.stopRec == other.stopRec
        && this.deepestRecursion == other.deepestRecursion
        && this.returnFromCounter == other.returnFromCounter
        && Objects.equals(this.currentFunction, other.currentFunction)
        && this.iterations.equals(other.iterations);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(deepestRecursion, returnFromCounter, stopIt, stopRec, currentFunction, iterations);
    }
    return hashCache;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView manager) {
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
    BooleanFormula reasonFormula = bfmgr.makeTrue();
    if (stopIt) {
      reasonFormula = bfmgr.and(reasonFormula, PreventingHeuristic.LOOPITERATIONS.getFormula(manager, getDeepestIteration()));
    }
    if (stopRec) {
      reasonFormula = bfmgr.and(reasonFormula, PreventingHeuristic.RECURSIONDEPTH.getFormula(manager, getDeepestRecursion()));
    }
    return reasonFormula;
  }

  private static class ComparableLoop implements Comparable<ComparableLoop> {

    private final Loop loop;

    public ComparableLoop(Loop pLoop) {
      Preconditions.checkNotNull(pLoop);
      this.loop = pLoop;
    }

    @Override
    public int hashCode() {
      return loop.hashCode();
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO instanceof ComparableLoop) {
        ComparableLoop other = (ComparableLoop) pO;
        return loop.equals(other.loop);
      }
      return false;
    }

    @Override
    public String toString() {
      return loop.toString();
    }

    @Override
    public int compareTo(ComparableLoop pOther) {
      // Compare by size
      int sizeComp = Integer.compare(loop.getLoopNodes().size(), pOther.loop.getLoopNodes().size());
      if (sizeComp != 0) {
        return sizeComp;
      }

      // If sizes are equal, compare lexicographically
      return Ordering.<CFANode>natural()
          .lexicographical()
          .compare(loop.getLoopNodes(), pOther.loop.getLoopNodes());
    }

  }
}
