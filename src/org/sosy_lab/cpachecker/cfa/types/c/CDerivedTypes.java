// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * Utility class providing constant instances for all derived C types including pointer, array,
 * union, and function types.
 *
 * <p>Note: This class explicitly deals only with derived types and does not include simple types
 * such as ints, floats, etc., which are available in {@link CNumericTypes}.
 */
public final class CDerivedTypes {

  public static final CArrayType UNSIGNED_INT_ARRAY =
      new CArrayType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT);
}
