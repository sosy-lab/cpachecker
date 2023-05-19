// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/** Exception if states in predicated analysis are created which violate property to be checked */
public class CPAEnabledAnalysisPropertyViolationException extends CPAException {

  private static final long serialVersionUID = 6723698516455641373L;

  private final AbstractState failureElem;
  private final boolean inMerge;

  public CPAEnabledAnalysisPropertyViolationException(
      String pMsg, AbstractState failureCause, boolean failedWhileMerge) {
    super(pMsg);
    failureElem = failureCause;
    inMerge = failedWhileMerge;
  }

  public CPAEnabledAnalysisPropertyViolationException(
      String msg, Throwable cause, AbstractState failureCause, boolean failedWhileMerge) {
    super(msg, cause);
    failureElem = failureCause;
    inMerge = failedWhileMerge;
  }

  public AbstractState getFailureCause() {
    return failureElem;
  }

  public boolean isMergeViolationCause() {
    return inMerge;
  }
}
