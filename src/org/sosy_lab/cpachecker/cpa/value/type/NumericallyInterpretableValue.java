// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

/**
 * For {@link Value}s that can return a numeric interpretation of themselves. I.e. {@link
 * NumericValue} returns itself, but {@link JBooleanValue} can be returned as 1 for true, and 0 for
 * false.
 */
public sealed interface NumericallyInterpretableValue extends Value
    permits NumericValue, JBooleanValue {

  /**
   * Return a numeric interpretation of the {@link Value}, i.e. {@link NumericValue} returns itself,
   * but {@link JBooleanValue} returns either 1 for {@link JBooleanValue#TRUE_VALUE}, and 0 for
   * {@link JBooleanValue#FALSE_VALUE}.
   */
  NumericValue interpretNumerically();
}
