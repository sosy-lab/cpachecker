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
package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;

/**
 * Exception raised when the refinement procedure fails, or was
 * abandoned.
 */
public class RefinementFailedException extends CPAException {

  public static enum Reason {
    InterpolationFailed("Interpolation failed"),
    InvariantRefinementFailed("Could not find invariant"),
    RepeatedCounterexample("Counterexample could not be ruled out and was found again"),
    TooMuchUnrolling("Too much unrolling"),
    InfeasibleCounterexample("External tool verified counterexample as infeasible"),
    TIMEOUT("SMT-solver timed out");

    private final String humanReableReason;

    private Reason(String pHumanReableReason) {
      humanReableReason = pHumanReableReason;
    }

    @Override
    public String toString() {
      return humanReableReason;
    }
  }

  private static final long serialVersionUID = 2353178323706458175L;

  private final Reason reason;
  private ARGPath path;
  private final int failurePoint;

  public RefinementFailedException(Reason r, ARGPath p, int pFailurePoint) {
    super("Refinement failed: " + checkNotNull(r));
    reason = r;
    path = p;
    failurePoint = pFailurePoint;
  }

  public RefinementFailedException(Reason r, ARGPath p) {
    this(r, p, -1);
  }

  /** Return the reason for the failure */
  public Reason getReason() {
    return reason;
  }

  /** Return the path that caused the failure */
  public ARGPath getErrorPath() {
    return path;
  }

  public void setErrorPath(ARGPath pPath) {
    path = pPath;
  }

  /**
   * Returns the position of the node in the past where
   * the failure occurred (or -1 if the failure cannot
   * be caused by a given node)
   */
  public int getFailurePoint() {
    return failurePoint;
  }
}
