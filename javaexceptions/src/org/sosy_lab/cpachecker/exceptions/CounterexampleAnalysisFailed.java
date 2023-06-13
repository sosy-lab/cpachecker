// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

/** Exception for cases when a counterexample could not be analyzed for its feasibility. */
public class CounterexampleAnalysisFailed extends CPAException {

  private static final long serialVersionUID = 1739510661568141393L;

  public CounterexampleAnalysisFailed(String msg) {
    super("Counterexample could not be analyzed: " + msg);
  }

  public CounterexampleAnalysisFailed(String msg, Throwable cause) {
    super("Counterexample could not be analyzed: " + msg, cause);
  }
}
