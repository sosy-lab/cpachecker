/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class LoopBoundPrecision implements Precision {

  private final boolean trackStack;

  private final int maxLoopIterations;

  private final int loopIterationsBeforeAbstraction;

  public LoopBoundPrecision(boolean pStack, int pMaxLoopIterations, int pLoopIterationsBeforeAbstraction) {
    Preconditions.checkArgument(pMaxLoopIterations >= 0);
    Preconditions.checkArgument(pLoopIterationsBeforeAbstraction >= 0);
    trackStack = pStack;
    maxLoopIterations = pMaxLoopIterations;
    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
  }

  public boolean shouldTrackStack() {
    return trackStack;
  }

  public int getMaxLoopIterations() {
    return maxLoopIterations;
  }

  public int getLoopIterationsBeforeAbstraction() {
    return loopIterationsBeforeAbstraction;
  }

  public LoopBoundPrecision withMaxLoopIterations(int pMaxLoopIterations) {
    if (pMaxLoopIterations == maxLoopIterations) {
      return this;
    }
    return new LoopBoundPrecision(trackStack, pMaxLoopIterations, loopIterationsBeforeAbstraction);
  }

  public LoopBoundPrecision withLoopIterationsBeforeAbstraction(int pLoopIterationsBeforeAbstraction) {
    if (pLoopIterationsBeforeAbstraction == loopIterationsBeforeAbstraction) {
      return this;
    }
    return new LoopBoundPrecision(trackStack, maxLoopIterations, pLoopIterationsBeforeAbstraction);
  }

  @Override
  public String toString() {
    return "k = " + maxLoopIterations + ", "
        + (shouldTrackStack() ? "track loop stack" : "do not track loop stack");
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof LoopBoundPrecision) {
      LoopBoundPrecision other = (LoopBoundPrecision) pOther;
      return trackStack == other.trackStack
          && maxLoopIterations == other.maxLoopIterations;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackStack, maxLoopIterations);
  }

}
