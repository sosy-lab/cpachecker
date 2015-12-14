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
package org.sosy_lab.cpachecker.cpa.loopstats;

import static com.google.common.base.Preconditions.*;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class LoopstatsState implements AbstractState, Partitionable {

  private final transient LoopStatisticsReceiver statReceiver;

  private final ImmutableLoopStack activeLoops;
  private final ImmutableIntegerStack activeIterations;

  private int hashCache = 0;

  public LoopstatsState(final LoopStatisticsReceiver pStatReceiver,
      final ImmutableLoopStack pActiveLoops,
      final ImmutableIntegerStack pActiveIterations) {

    statReceiver = checkNotNull(pStatReceiver);
    activeLoops = checkNotNull(pActiveLoops);

    checkArgument(pActiveIterations.size() == pActiveLoops.size());
    activeIterations = pActiveIterations;
  }

  public LoopstatsState(final LoopStatisticsReceiver pStatReceiver) {
    this(pStatReceiver, ImmutableLoopStack.empty(), ImmutableIntegerStack.empty());
  }

  private Loop peekLoop() {
    return activeLoops.peekHead();
  }

  public int getDepth() {
    return activeLoops.size();
  }

  public int getIteration() {
    return activeIterations.peekHead();
  }

  @Override
  public Object getPartitionKey() {
    return activeLoops;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    if (peekLoop() != null) {
      result.append("Loop: ");
      result.append(peekLoop().getLoopHeads());
      result.append("; iteration: ");
      result.append(getIteration());
      result.append("; nesting: ");
      result.append(getDepth());

    } else {
      result.append("No loop active.");
    }
    return result.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LoopstatsState)) {
      return false;
    }

    LoopstatsState other = (LoopstatsState)obj;
    return (this.activeLoops.equals(other.activeLoops))
        && (this.activeIterations.equals(other.activeIterations));
  }

  /**
   * Create a successor state that encode the information that either
   *  (A) a different loop has been entered, or
   *  (B) another iteration of the loop is performed.
   *
   * @param pEnteringLoopBody The loop that gets entered.
   * @return  A new successor state in the abstract domain of the analysis.
   */
  static @Nonnull LoopstatsState createSuccessorForEnteringLoopBody (final LoopstatsState pPreviousState,
      final Loop pEnteringLoopBody) {

    Preconditions.checkNotNull(pPreviousState);
    Preconditions.checkNotNull(pEnteringLoopBody);

    Loop lastLoop = pPreviousState.peekLoop();
    if (lastLoop == null || !pEnteringLoopBody.equals(lastLoop)) {

      // Beginning a new 'instance' of a loop
      return new LoopstatsState(pPreviousState.statReceiver,
          pPreviousState.activeLoops.push(pEnteringLoopBody),
          pPreviousState.activeIterations.push(Integer.valueOf(1)));

    } else {

      // Another iteration of a loop
      Integer prevIterations = pPreviousState.activeIterations.peekHead();

      return new LoopstatsState(pPreviousState.statReceiver,
          pPreviousState.activeLoops,
          pPreviousState.activeIterations.getTail().push(prevIterations + 1));
    }
  }

  /**
   * Create a successor state that encodes the information that the loop
   * was left, i.e., we are no more in this loop. The loop body might not have been
   * entered because the condition for entering the loop might not have been
   * satisfied (in case of a while loop)!
   *
   * @param pLeavingLoop The loop that gets left.
   * @return A new successor state in the abstract domain of the analysis.
   */
  static @Nonnull LoopstatsState createSuccessorForLeavingLoop (final LoopstatsState pPreviousState,
      final Loop pLeavingLoop) {

    Preconditions.checkNotNull(pPreviousState);
    Preconditions.checkNotNull(pLeavingLoop);

    Loop activeLoop = pPreviousState.peekLoop();

    if (activeLoop == null || !pLeavingLoop.equals(activeLoop)) {
      // Was not active, just track the information that the loop was reached
      //  but the loop body was not entered.

      pPreviousState.statReceiver.signalLoopLeftAfter(pLeavingLoop,
          pPreviousState.getDepth(), pPreviousState.activeLoops, pPreviousState.getIteration());

      return pPreviousState;

    } else {
      return new LoopstatsState(pPreviousState.statReceiver,
          pPreviousState.activeLoops.getTail(),
          pPreviousState.activeIterations.getTail());
    }
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hashCode(activeLoops, activeIterations);
    }
    return hashCache;
  }

}
