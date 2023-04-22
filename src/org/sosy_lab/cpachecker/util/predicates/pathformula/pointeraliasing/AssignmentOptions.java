// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

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
   * if one of them is an integer and the other one is a floating-point type.
   */
  enum ConversionType {
    /**
     * Casting is used in normal assignment circumstances. For example, it converts integers to
     * different sizes and converts them to floats (and vice versa). If the types are incompatible,
     * an exception is thrown during assignment.
     */
    CAST,
    /**
     * Reinterpretation preserves the exact bit content; both types must have the same number of
     * bits. Otherwise, an exception is thrown during assignment.
     *
     * <p>This assignment type is intended chiefly for the {@code memmove} and {@code memcpy}
     * functions.
     *
     * @see MemoryManipulationFunctionHandler
     */
    REINTERPRET,
    /**
     * Byte repeat conversion requires byte-sized right-hand side. It fills each byte of left-hand
     * side with the given right-hand side value.
     *
     * <p>In case this conversion is used, the caller must ensure that right-hand-side slice
     * expression contains no modifiers and the base is a cast to unsigned char. Otherwise, an
     * exception is thrown during assignment.
     *
     * <p>An {@link UnsupportedCodeException} is thrown if the full left-hand side type is void,
     * which signifies incomplete type discovery.
     *
     * <p>An {@link UnsupportedCodeException} is thrown if there are bitfields within the left-hand
     * side and it cannot be determined that rhs is either all-ones or all-zeros, so bitfield value
     * would be heavily implementation-defined.
     *
     * <p>This assignment type is intended chiefly for the {@code memset} function.
     *
     * @see MemoryManipulationFunctionHandler
     */
    BYTE_REPEAT
  }

  AssignmentOptions {
    checkNotNull(conversionType);
  }
}
