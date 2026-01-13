// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import java.io.Serial;

/**
 * This exception is thrown when a language element is unsupported by design, e.g., when the NLA
 * replacer encounters a bitwise operation without approximations enabled.
 */
public class UnsupportedOperationByDesignException extends UnsupportedOperationException {
  @Serial private static final long serialVersionUID = 6173271465032705649L;

  public UnsupportedOperationByDesignException(String message) {
    super(message);
  }

  public UnsupportedOperationByDesignException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnsupportedOperationByDesignException(Throwable cause) {
    super(cause);
  }
}
