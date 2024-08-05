// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

/**
 * Super class for all exceptions thrown by CPA operators.
 *
 * <p>TODO This exception should probably be abstract, and specialized sub-classes should be used
 * for specific reasons.
 */
public class CPAException extends Exception {

  private static final long serialVersionUID = 6846683924964869559L;

  public CPAException(String msg) {
    super(msg);
  }

  public CPAException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
