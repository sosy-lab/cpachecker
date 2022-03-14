// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

/**
 * This {@link Exception} class is meant to mark exceptional events in the communication between the
 * C part and the Java part of the {@link CFloatNativeAPI}.
 *
 * <p>Such exceptional events should normally indicate some false usage of the library like trying
 * to use an unimplemented type-flag or a type that cannot be used for the desired operation.
 */
public class NativeComputationException extends Exception {

  private static final long serialVersionUID = 1L;

  public NativeComputationException(String message) {
    super(message);
  }
}
