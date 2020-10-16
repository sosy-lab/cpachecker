// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

public class InvalidAutomatonException extends Exception {

  private static final long serialVersionUID = 4881083051895812266L;

  public InvalidAutomatonException(String pMsg, Throwable pCause) {
    super(pMsg, pCause);
  }

  public InvalidAutomatonException(String pMsg) {
    super(pMsg);
  }
}
