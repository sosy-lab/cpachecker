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

import com.google.common.base.Strings;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;

import javax.annotation.Nullable;

/**
 * Exception raised when the refinement procedure fails, or was
 * abandoned.
 */
public class RefinementFailedException extends CPAException {

  public static enum Reason {
    InterpolationFailed("Interpolation failed"),
    InvariantRefinementFailed("Could not find invariant"),
    RepeatedCounterexample("Counterexample could not be ruled out and was found again"),
    RepeatedPathPrefix("Error path prefix could not be ruled out and was used again"),
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

  private @Nullable ARGPath path;

  private final Reason reason;

  public RefinementFailedException(Reason r, @Nullable ARGPath p) {
    super(getMessage(r, null));
    reason = r;
    path = p;
  }

  public RefinementFailedException(Reason r, @Nullable ARGPath p, Throwable t) {
    super(getMessage(r, t), checkNotNull(t));
    reason = r;
    path = p;
  }

  private static String getMessage(Reason r, @Nullable Throwable t) {
    StringBuilder sb = new StringBuilder();
    sb.append("Refinement failed: ");
    sb.append(r.toString());
    if (t != null) {
      String msg = Strings.nullToEmpty(t.getMessage());
      if (!msg.isEmpty()) {
        sb.append(" (");
        sb.append(msg);
        sb.append(")");
      }
    }
    return sb.toString();
  }

  /** Return the path that caused the failure */
  public @Nullable ARGPath getErrorPath() {
    return path;
  }

  public void setErrorPath(ARGPath pPath) {
    path = checkNotNull(pPath);
  }

  public Reason getReason() {
    return reason;
  }
}
