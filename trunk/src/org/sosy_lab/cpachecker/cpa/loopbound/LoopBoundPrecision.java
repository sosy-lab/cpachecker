// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class LoopBoundPrecision implements Precision {

  private final boolean trackStack;

  private final int maxLoopIterations;

  private final int loopIterationsBeforeAbstraction;

  LoopBoundPrecision(boolean pStack, int pMaxLoopIterations, int pLoopIterationsBeforeAbstraction) {
    Preconditions.checkArgument(pMaxLoopIterations >= 0);
    Preconditions.checkArgument(pLoopIterationsBeforeAbstraction >= 0);
    trackStack = pStack;
    maxLoopIterations = pMaxLoopIterations;
    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
  }

  boolean shouldTrackStack() {
    return trackStack;
  }

  int getMaxLoopIterations() {
    return maxLoopIterations;
  }

  int getLoopIterationsBeforeAbstraction() {
    return loopIterationsBeforeAbstraction;
  }

  LoopBoundPrecision withMaxLoopIterations(int pMaxLoopIterations) {
    if (pMaxLoopIterations == maxLoopIterations) {
      return this;
    }
    return new LoopBoundPrecision(trackStack, pMaxLoopIterations, loopIterationsBeforeAbstraction);
  }

  LoopBoundPrecision withLoopIterationsBeforeAbstraction(int pLoopIterationsBeforeAbstraction) {
    if (pLoopIterationsBeforeAbstraction == loopIterationsBeforeAbstraction) {
      return this;
    }
    return new LoopBoundPrecision(trackStack, maxLoopIterations, pLoopIterationsBeforeAbstraction);
  }

  @Override
  public String toString() {
    return "k = "
        + maxLoopIterations
        + ", "
        + (shouldTrackStack() ? "track loop stack" : "do not track loop stack");
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof LoopBoundPrecision) {
      LoopBoundPrecision other = (LoopBoundPrecision) pOther;
      return trackStack == other.trackStack && maxLoopIterations == other.maxLoopIterations;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackStack, maxLoopIterations);
  }
}
