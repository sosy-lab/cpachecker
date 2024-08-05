// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

/** Super class for all exceptions thrown from transfer relation. */
public class CPATransferException extends CPAException {

  private static final long serialVersionUID = -7851950254941139295L;

  public CPATransferException(String msg) {
    super(msg);
  }

  public CPATransferException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
