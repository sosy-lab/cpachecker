// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * Local options for assignments that are to be performed. They change the assignment behavior
 * depending on the needs of the caller.
 *
 * <p>To create the assignment options, use {@link AssignmentOptions.Builder} for conciseness and
 * clarity.
 *
 * @param conversionType How conversion of right-hand-side type to target type should be handled.
 * @param useOldSSAIndicesIfAliased Whether old SSA indices should be reused if the left-hand side
 *     is stored in aliased location. Intended to be used for initial assignment to a left-hand
 *     side, but cannot be used for subsequent assignments.
 * @param forceEncodingQuantifiers If set, quantifiers are encoded within the assignment even if
 *     they are not globally set to be encoded. Intended to be used for enabling quantification of
 *     some assignments while leaving others to be unrolled.
 * @param forcePointerAssignment If set, the left-hand side is treated as a pointer; the assignment
 *     caller is responsible for ensuring that the left-hand side type can be treated as such.
 *     Intended to be used for function parameters which may need to be assigned as a pointer even
 *     though their actual type is an array type. Cannot be set together with {@link
 *     #forceArrayAttachment}.
 * @param forceArrayAttachment If set, left-hand-side array address is attached to right-hand-side
 *     aliased location or value. It is up to caller to ensure that the left-hand side is an array
 *     and the right-hand side formula will be a memory address. Cannot be set together with {@link
 *     #forcePointerAssignment}.
 * @param forceLeftSideAssignment If set, LHS relevancy is ignored.
 */
record AssignmentOptions(
    AssignmentOptions.ConversionType conversionType,
    boolean useOldSSAIndicesIfAliased,
    boolean forceEncodingQuantifiers,
    boolean forcePointerAssignment,
    boolean forceArrayAttachment,
    boolean forceLeftSideAssignment) {

  AssignmentOptions {
    checkArgument(!forcePointerAssignment || !forceArrayAttachment);
  }

  boolean forcePointerAssignmentOrArrayAttachment() {
    return forcePointerAssignment || forceArrayAttachment;
  }

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

  static final class Builder {
    /**
     * @see AssignmentOptions#conversionType
     */
    private AssignmentOptions.ConversionType conversionType;

    /**
     * @see AssignmentOptions#useOldSSAIndicesIfAliased
     */
    private boolean useOldSSAIndicesIfAliased = false;

    /**
     * @see AssignmentOptions#forceEncodingQuantifiers
     */
    private boolean forceEncodingQuantifiers = false;

    /**
     * @see AssignmentOptions#forcePointerAssignment
     */
    private boolean forcePointerAssignment = false;

    /**
     * @see AssignmentOptions#forceArrayAttachment
     */
    private boolean forceArrayAttachment = false;

    /**
     * @see AssignmentOptions#forceLeftSideAssignment
     */
    private boolean forceLeftSideAssignment = false;

    /**
     * Constructs an assignment options builder with the given conversion type, not setting any
     * flags.
     *
     * @see AssignmentOptions
     * @param conversionType Conversion type to be used in the assignment.
     */
    Builder(AssignmentOptions.ConversionType conversionType) {
      this.conversionType = conversionType;
    }

    /**
     * Sets whether old SSA indices should be used if the left-hand side is stored in aliased
     * location.
     *
     * <p>False by default when building.
     *
     * @see AssignmentOptions#useOldSSAIndicesIfAliased
     * @return This builder with the flag set to the given value.
     */
    Builder setUseOldSSAIndicesIfAliased(boolean value) {
      useOldSSAIndicesIfAliased = value;
      return this;
    }

    /**
     * Sets whether quantifiers should be encoded in this assignment even not globally set to be
     * encoded.
     *
     * <p>False by default when building.
     *
     * @see AssignmentOptions#forceEncodingQuantifiers
     * @return This builder with the flag set to the given value.
     */
    Builder setForceEncodingQuantifiers(boolean value) {
      forceEncodingQuantifiers = value;
      return this;
    }

    /**
     * Sets whether the left-hand side should be treated as a pointer even when not of pointer type.
     * If true, the assignment caller is responsible for ensuring that the left-hand side type can
     * be treated as such.
     *
     * <p>False by default when building.
     *
     * @see AssignmentOptions#forcePointerAssignment
     * @return This builder with the flag set to the given value.
     */
    Builder setForcePointerAssignment(boolean value) {
      forcePointerAssignment = value;
      return this;
    }

    /**
     * Sets whether array attachment will be forced. If set, left-hand-side array address is
     * attached to right-hand-side aliased location or value. It is up to caller to ensure that the
     * left-hand side is an array and the right-hand side formula will be a memory address.
     *
     * <p>False by default when building.
     *
     * @see AssignmentOptions#forceArrayAttachment
     * @return This builder with the flag set to the given value.
     */
    Builder setForceArrayAttachment(boolean value) {
      forceArrayAttachment = value;
      return this;
    }

    /**
     * Sets whether LHS relevancy is ignored.
     *
     * <p>False by default when building.
     *
     * @see AssignmentOptions#forceLeftSideAssignment
     * @return This builder with the flag set to the given value.
     */
    Builder setForceLeftSideAssignment(boolean value) {
      forceLeftSideAssignment = value;
      return this;
    }

    /**
     * Builds assignment options using the settings in this builder.
     *
     * @return The built assignment options.
     */
    AssignmentOptions build() {
      return new AssignmentOptions(this);
    }
  }

  /**
   * Constructs the assignment options using a builder.
   *
   * @param builder The builder to construct the assignment options from.
   */
  private AssignmentOptions(Builder builder) {
    this(
        builder.conversionType,
        builder.useOldSSAIndicesIfAliased,
        builder.forceEncodingQuantifiers,
        builder.forcePointerAssignment,
        builder.forceArrayAttachment,
        builder.forceLeftSideAssignment);
  }
}
