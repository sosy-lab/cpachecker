// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation;

/**
 * A class that makes implementing {@link CfaValidator} easier by providing static factory methods
 * for {@link CfaValidationResult} creation.
 */
public abstract class AbstractCfaValidator implements CfaValidator {

  /**
   * Returns a {@link CfaValidationResult} that indicates that the current check passed (i.e., the
   * current check didn't fail).
   *
   * @return a {@link CfaValidationResult} that indicates that the current check passed
   */
  protected final CfaValidationResult pass() {
    return CfaValidationResult.VALID;
  }

  /**
   * Creates a new {@link CfaValidationResult} that indicates that the check failed (i.e., the CFA
   * is not valid).
   *
   * @param pMessage a string that describes the cause of the failure, can be a format string that
   *     {@link String#format(String, Object...)} accepts
   * @param pArgs arguments referenced by the format specifiers in {@code pMessage}, if it's a
   *     format string
   * @return a new {@link CfaValidationResult} that indicates that the check failed
   * @throws NullPointerException if {@code pMessage == null}
   */
  protected final CfaValidationResult fail(String pMessage, Object... pArgs) {
    String message = String.format(pMessage, pArgs);
    return CfaValidationResult.error(String.format("[%s] %s", getClass().getSimpleName(), message));
  }
}
