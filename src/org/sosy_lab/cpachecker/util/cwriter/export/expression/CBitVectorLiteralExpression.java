// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.expression;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public abstract sealed class CBitVectorLiteralExpression implements CExportExpression
    permits CBitVectorBinaryLiteralExpression,
        CBitVectorDecimalLiteralExpression,
        CBitVectorHexadecimalLiteralExpression {

  static final String BINARY_PREFIX = "0b";

  /**
   * The right most index in the bit vector, e.g., the {@code 1} in {@code 0b00000001} is at the
   * right-most index.
   */
  static final int RIGHT_MOST_INDEX = 0;

  static final String ZERO_BIT = "0";

  static final String ONE_BIT = "1";

  final ImmutableSet<Integer> oneBits;

  final CSimpleType type;

  /** The binary length of this bit vector, e.g., {@code 8} for {@code 0b00000001}. */
  final int binaryLength;

  /**
   * Returns a bit vector literal expression in C. Example: For a bit vector with a type that has
   * length {@code 8} and {@code pOneBits = { 0 }}, this creates the bit vector {@code 0b00000001}
   * when using a binary encoding.
   *
   * @param pOneBits The indices of {@code 1}s in the bit vector ranging from {@code 0} (right-most)
   *     to {@code bin_length(pType) - 1} (left-most)
   * @param pType The type of the bitvector, e.g. {@code signed int}
   */
  CBitVectorLiteralExpression(ImmutableSet<Integer> pOneBits, CSimpleType pType) {
    Optional<Integer> max = pOneBits.stream().max(Integer::compare);
    binaryLength = getBinaryLength(pType);
    if (max.isPresent()) {
      // fails if e.g. binLength is 32 for a normal int, but max is 32 too
      checkArgument(max.orElseThrow() < binaryLength);
    }
    oneBits = pOneBits;
    type = pType;
  }

  private int getBinaryLength(CSimpleType pType) {
    // TODO what about isComplex, isImaginary?
    return switch (pType.getType()) {
      case UNSPECIFIED ->
          throw new IllegalArgumentException(
              String.format("Cannot get binary length for type %s", CBasicType.UNSPECIFIED));
      case BOOL -> 1;
      case CHAR -> 8;
      case INT -> {
        if (pType.hasShortSpecifier()) {
          yield 16;
        }
        if (pType.hasLongSpecifier() || pType.hasLongLongSpecifier()) {
          yield 64;
        }
        yield 32;
      }
      case INT128, FLOAT128 -> 128;
      case FLOAT -> 32;
      case DOUBLE -> {
        if (pType.hasLongSpecifier()) {
          yield 80;
        }
        yield 64;
      }
    };
  }
}
