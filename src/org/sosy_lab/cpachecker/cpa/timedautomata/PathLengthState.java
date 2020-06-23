// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.timedautomata;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class PathLengthState implements AbstractState {
  private final int pathLength;
  private boolean reachedBound;

  public PathLengthState(int pPathLength, boolean pReachedBound) {
    pathLength = pPathLength;
    reachedBound = pReachedBound;
  }

  public int getPathLength() {
    return pathLength;
  }

  public boolean didReachBound() {
    return reachedBound;
  }

  public void setDidReachedBoundFalse() {
    reachedBound = false;
  }
}
