// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

/**
 * Signals that the check method of a AbstractState has been passed an Argument that could not be
 * evaluated.
 */
public class InvalidQueryException extends CPATransferException {
  private static final long serialVersionUID = 3410773868391514648L;

  /**
   * Constructs an {@code InvalidQueryException} with the specified detail message.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   */
  public InvalidQueryException(String message) {
    super(message);
  }

  public InvalidQueryException(String message, Throwable cause) {
    super(message, cause);
  }
}
