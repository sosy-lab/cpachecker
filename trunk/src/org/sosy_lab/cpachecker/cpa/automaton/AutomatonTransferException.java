// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public final class AutomatonTransferException extends CPATransferException {

  private static final long serialVersionUID = 3470772694494993317L;

  AutomatonTransferException(String pMsg) {
    super(pMsg);
  }

  AutomatonTransferException(String pMsg, ResultValue<?> pResult) {
    super(pMsg + ": " + pResult.getFailureMessage());
  }

  AutomatonTransferException(String pMsg, Throwable pCause) {
    super(pMsg, pCause);
  }
}
