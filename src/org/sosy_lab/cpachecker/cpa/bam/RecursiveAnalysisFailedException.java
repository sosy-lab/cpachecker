// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class RecursiveAnalysisFailedException extends CPATransferException {

  private static final long serialVersionUID = 3822584071233172171L;

  private final int depth;

  public RecursiveAnalysisFailedException(CPAException e) {
    super(createMessage(e));

    if (e instanceof RecursiveAnalysisFailedException) {
      RecursiveAnalysisFailedException recursiveException = (RecursiveAnalysisFailedException) e;
      initCause(recursiveException.getCause());
      depth = recursiveException.depth + 1;
    } else {
      initCause(e);
      depth = 1;
    }
  }

  private static String createMessage(CPAException e) {
    if (e instanceof RecursiveAnalysisFailedException) {
      RecursiveAnalysisFailedException r = (RecursiveAnalysisFailedException) e;
      return "Error in recursive analysis at depth " + r.depth + ": " + r.getCause().getMessage();
    } else {
      return "Error in recursive analysis at depth 1: " + e.getMessage();
    }
  }

  @Override
  public synchronized CPAException getCause() {
    return (CPAException) super.getCause();
  }
}
