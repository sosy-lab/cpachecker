// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AppliedCustomInstructionParsingFailedException extends CPAException {

  private static final long serialVersionUID = -1974068616247550237L;

  public AppliedCustomInstructionParsingFailedException(final String pMsg) {
    super(pMsg);
  }

  public AppliedCustomInstructionParsingFailedException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}
