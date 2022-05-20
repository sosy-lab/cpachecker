// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

/**
 * This exception will never be thrown. It can be used as type when visitors required an exception
 * type to be declared, but the respective visitor will never throw.
 */
public final class NoException extends RuntimeException {

  private static final long serialVersionUID = -249581932019736058L;

  /** Nope. */
  private NoException() {}
}
