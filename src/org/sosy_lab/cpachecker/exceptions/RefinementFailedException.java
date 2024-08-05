// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

/** Exception raised when the refinement procedure fails, or was abandoned. */
public class RefinementFailedException extends CPAException {

  private static final String MSG_PREFIX = "Refinement failed: ";

  public enum Reason {
    InterpolationFailed("Interpolation failed"),
    InvariantRefinementFailed("Could not find invariant"),
    StaticRefinementFailed("Static refinement failed"),
    NewtonRefinementFailed("Newton refinement failed"),
    RepeatedCounterexample("Counterexample could not be ruled out and was found again"),
    RepeatedPathPrefix("Error path prefix could not be ruled out and was used again"),
    TooMuchUnrolling("Too much unrolling"),
    InfeasibleCounterexample("External tool verified counterexample as infeasible"),
    SequenceOfAssertionsToWeak("Sequence of assertions is too weak to cover error trace"),
    TIMEOUT("SMT-solver timed out");

    private final String humanReableReason;

    Reason(String pHumanReableReason) {
      humanReableReason = pHumanReableReason;
    }

    @Override
    public String toString() {
      return humanReableReason;
    }
  }

  private static final long serialVersionUID = 2353178323706458175L;

  @SuppressWarnings("checkstyle:MutableException")
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

  private RefinementFailedException(
      Reason r, @Nullable ARGPath p, String message, @Nullable Throwable cause) {
    super(checkNotNull(message), cause);
    reason = checkNotNull(r);
    path = p;
  }

  public static RefinementFailedException forInterpolationFailureInSolver(
      SolverException e, Solver solver) {
    StringBuilder msg = new StringBuilder();
    msg.append(MSG_PREFIX);
    msg.append(Reason.InterpolationFailed);
    msg.append(" in solver ");
    msg.append(solver.getInterpolatingSolver());
    if (e != null) {
      String solverMessage = Strings.nullToEmpty(e.getMessage());
      if (solverMessage.isEmpty()) {
        msg.append(" without explanation.");
      } else {
        msg.append(" with message '");
        msg.append(solverMessage);
        msg.append("'.");
      }
    }
    return new RefinementFailedException(Reason.InterpolationFailed, null, msg.toString(), e);
  }

  private static String getMessage(Reason r, @Nullable Throwable t) {
    StringBuilder sb = new StringBuilder();
    sb.append(MSG_PREFIX);
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
