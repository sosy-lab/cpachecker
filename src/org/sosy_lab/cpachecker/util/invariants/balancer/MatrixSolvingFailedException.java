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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exception raised when the matrix solving procedure fails.
 */
public class MatrixSolvingFailedException extends Exception {

  private static final long serialVersionUID = -7312893758413993907L;

  //-----------------------------------------------------------------

  public static enum Reason {
    BadNonzeroAssumptions("Nonzero assumptions made matrix unsolvable (or else template does not work)."),
    BadTemplate("The template definitely does not work."),
    TIMEOUT("Redlog quantifier eliminator timed out");

    private final String humanReadableReason;

    private Reason(String pHumanReadableReason) {
      humanReadableReason = pHumanReadableReason;
    }

    @Override
    public String toString() {
      return humanReadableReason;
    }
  }

  //-----------------------------------------------------------------

  private final Reason reason;

  public MatrixSolvingFailedException(Reason r) {
    super("Matrix solving failed: " + checkNotNull(r));
    reason = r;
  }

  /** Return the reason for the failure */
  public Reason getReason() {
    return reason;
  }

}
