// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Local options for assignments that are to be performed. Changes the assignment behavior depending
 * on the needs of the caller.
 */
record AssignmentOptions(
    boolean useOldSSAIndicesIfAliased,
    AssignmentOptions.ConversionType conversionType,
    boolean forceQuantifiers,
    boolean forcePointerAssignment) {

  /**
   * Determines how conversion of right-hand-side type to target type should be handled, especially
   * if one of them is an integer and the other one is a floating-point type. Casting is used in
   * normal assignment circumstances. Reinterpretation preserves the exact bit content; both types
   * must have the same number of bits.
   */
  enum ConversionType {
    CAST,
    REINTERPRET
  }

  AssignmentOptions {
    checkNotNull(conversionType);
  }
}
