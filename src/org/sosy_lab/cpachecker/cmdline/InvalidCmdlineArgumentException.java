// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import java.io.Serial;

/** Exception thrown when something invalid is specified on the command line. */
public class InvalidCmdlineArgumentException extends Exception {

  @Serial private static final long serialVersionUID = -6526968677815416436L;

  InvalidCmdlineArgumentException(final String msg) {
    super(msg);
  }

  public InvalidCmdlineArgumentException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
