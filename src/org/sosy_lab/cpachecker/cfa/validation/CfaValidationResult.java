// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;

/**
 * Indicates whether a CFA is valid (i.e., all checks passed) or invalid (i.e., at least one check
 * failed).
 *
 * <p>Use {@link #passOrThrow(Function)} to throw an exception if any check failed and the {@link
 * CfaValidationResult} contains errors.
 */
public final class CfaValidationResult {

  /** Indicates that the CFA is valid. */
  static final CfaValidationResult VALID = new CfaValidationResult(ImmutableList.of());

  private final ImmutableList<String> errors;

  private CfaValidationResult(ImmutableList<String> pErrors) {
    errors = pErrors;
  }

  /**
   * Creates a new {@link CfaValidationResult} that contains the specified error.
   *
   * @param pMessage a string that describes the error
   * @return a new {@link CfaValidationResult} that contains the specified error
   * @throws NullPointerException if {@code pError == null}
   */
  public static CfaValidationResult error(String pMessage) {
    return new CfaValidationResult(ImmutableList.of(pMessage));
  }

  /**
   * Returns a new {@link CfaValidationResult} that contains the errors of this {@link
   * CfaValidationResult} and {@code pOther}.
   *
   * @param pOther the {@link CfaValidationResult} to combine with this {@link CfaValidationResult}
   * @return a new {@link CfaValidationResult} that contains the errors of this {@link
   *     CfaValidationResult} and {@code pOther}
   * @throws NullPointerException if {@code pOther == null}
   */
  public CfaValidationResult combine(CfaValidationResult pOther) {
    // performance optimization to avoid unnecessary object creation
    if (pOther == VALID) {
      return this;
    }
    if (this == VALID) {
      return checkNotNull(pOther);
    }

    return new CfaValidationResult(
        ImmutableList.<String>builderWithExpectedSize(errors.size() + pOther.errors.size())
            .addAll(errors)
            .addAll(pOther.errors)
            .build());
  }

  /**
   * If this {@link CfaValidationResult} contains any errors, an exception produced by the specified
   * function is thrown.
   *
   * @param <X> type of exception to be thrown
   * @param pExceptionProducer the function (list of errors -> exception) that produces the
   *     exception to be thrown
   * @throws X if this {@link CfaValidationResult} contains any errors
   * @throws NullPointerException if {@code pExceptionProducer == null}
   */
  public <X extends Throwable> void passOrThrow(
      Function<? super ImmutableList<String>, ? extends X> pExceptionProducer) throws X {
    if (!errors.isEmpty()) {
      throw pExceptionProducer.apply(errors);
    }
  }
}
