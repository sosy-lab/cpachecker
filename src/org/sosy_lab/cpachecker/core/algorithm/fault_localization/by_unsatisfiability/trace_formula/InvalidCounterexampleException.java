// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

public class InvalidCounterexampleException extends Exception {

  private static final long serialVersionUID = 3730137043150121319L;

  public InvalidCounterexampleException(String pReason) {
    super(pReason);
  }
}
