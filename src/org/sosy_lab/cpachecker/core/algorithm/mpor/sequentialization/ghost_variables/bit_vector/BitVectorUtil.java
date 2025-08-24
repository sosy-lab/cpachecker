// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.value_expression.BinaryBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.value_expression.DecimalBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.value_expression.HexadecimalBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorUtil {

  /** The right-most index is 0, with the left-most index being the length of the bit vector - 1. */
  public static final int RIGHT_INDEX = 0;

  /**
   * Returns the left index (i.e. the first index from left to right) of the bit vector based on its
   * binary length.
   */
  public static int getLeftIndexByBinaryLength(int pBinaryLength) {
    return pBinaryLength + RIGHT_INDEX - 1;
  }

  public static final int MIN_BINARY_LENGTH = BitVectorDataType.__UINT8_T.size;

  public static final int MAX_BINARY_LENGTH = BitVectorDataType.__UINT64_T.size;

  // Creation ======================================================================================

  public static BitVectorValueExpression buildBitVectorExpression(
      MPOROptions pOptions,
      @NonNull ImmutableMap<MemoryLocation, Integer> pMemoryLocationIds,
      @NonNull ImmutableSet<MemoryLocation> pMemoryLocations) {

    checkArgument(pOptions.bitVectorEncoding.isEnabled(), "no bit vector encoding specified");
    checkArgument(
        pMemoryLocationIds.keySet().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    // retrieve all variable ids from pMemoryLocationIds that are in pMemoryLocations
    ImmutableSet<Integer> setBits =
        pMemoryLocations.stream()
            .map(pMemoryLocationIds::get)
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet());
    return buildBitVectorExpressionByEncoding(
        pOptions.bitVectorEncoding, pMemoryLocationIds.size(), setBits);
  }

  /**
   * Creates a bit vector expression based on {@code pSetBits} where the left most index is {@code
   * 0} and the right most index is one smaller than the length of the bit vector.
   */
  private static BitVectorValueExpression buildBitVectorExpressionByEncoding(
      BitVectorEncoding pEncoding, int pMemoryLocationAmount, ImmutableSet<Integer> pSetBits) {

    int length = getBitVectorLengthByEncoding(pEncoding, pMemoryLocationAmount);
    return switch (pEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY -> new BinaryBitVectorValueExpression(length, pSetBits);
      case DECIMAL -> new DecimalBitVectorValueExpression(pSetBits);
      case HEXADECIMAL -> new HexadecimalBitVectorValueExpression(length, pSetBits);
      // TODO this is not so nice ...
      case SPARSE ->
          throw new IllegalArgumentException("use constructor directly for sparse bit vectors");
    };
  }

  public static CIntegerLiteralExpression buildDirectBitVectorExpression(
      @NonNull ImmutableMap<MemoryLocation, Integer> pMemoryLocationIds,
      @NonNull ImmutableSet<MemoryLocation> pMemoryLocations) {

    checkArgument(
        pMemoryLocationIds.keySet().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    // for decimal, use the sum of variable ids (starting from 1)
    ImmutableSet<Integer> setBits =
        pMemoryLocations.stream()
            .map(pMemoryLocationIds::get)
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet());

    return new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        getTypeByBinaryLength(getBinaryLength(pMemoryLocationIds.size())),
        new BigInteger(String.valueOf(buildDecimalBitVector(setBits))));
  }

  public static long buildDecimalBitVector(ImmutableSet<Integer> pSetBits) {
    // use long to support up to 64 bits
    long rSum = 0;
    for (int bit : pSetBits) {
      // use shift expression, equivalent to 2^bit
      rSum += 1L << (bit - BitVectorUtil.RIGHT_INDEX);
    }
    return rSum;
  }

  private static CSimpleType getTypeByBinaryLength(int pBinaryLength) {
    for (BitVectorDataType dataType : BitVectorDataType.values()) {
      if (dataType.size == pBinaryLength) {
        return dataType.simpleType;
      }
    }
    throw new IllegalArgumentException("invalid pBinaryLength");
  }

  // Vector Length =================================================================================

  public static BitVectorDataType getDataTypeByLength(int pLength) {
    for (BitVectorDataType type : BitVectorDataType.values()) {
      if (type.size == pLength) {
        return type;
      }
    }
    throw new IllegalArgumentException("no bit vector type with given length found: " + pLength);
  }

  public static int getBitVectorLengthByEncoding(
      BitVectorEncoding pEncoding, int pMemoryLocationAmount) {

    checkArgument(
        pMemoryLocationAmount <= MAX_BINARY_LENGTH,
        "cannot have more than %s global variables, please disable bit vectors for this program.",
        MAX_BINARY_LENGTH);

    return switch (pEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY -> getBinaryLength(pMemoryLocationAmount);
      // the length does not matter for these, but we use the number of global variables
      case DECIMAL, SPARSE -> pMemoryLocationAmount;
      case HEXADECIMAL -> convertBinaryLengthToHex(getBinaryLength(pMemoryLocationAmount));
    };
  }

  public static int getBinaryLength(int pMinLength) {
    int rLength = MIN_BINARY_LENGTH;
    while (rLength < pMinLength) {
      rLength *= 2;
    }
    assert isValidBinaryLength(rLength) : "binary bit vector length is invalid: " + rLength;
    return rLength;
  }

  public static boolean isValidBinaryLength(int pBinaryLength) {
    for (BitVectorDataType type : BitVectorDataType.values()) {
      if (type.size == pBinaryLength) {
        return true;
      }
    }
    return false;
  }

  // Sparse ========================================================================================

  public static CIdExpression createSparseAccessVariable(
      MPOROptions pOptions,
      MPORThread pThread,
      MemoryLocation pMemoryLocation,
      MemoryAccessType pAccessType) {

    // we use the original variable name here, not the substitute -> less code
    String name =
        SeqNameUtil.buildSparseBitVectorNameByAccessType(
            pOptions, pThread.id, pMemoryLocation, pAccessType);
    // always initialize with 0, the actual bit vectors are set inside main()
    CSimpleDeclaration declaration =
        SeqDeclarationBuilder.buildVariableDeclaration(
            true, SeqSimpleType.UNSIGNED_CHAR, name, SeqInitializer.INT_0);
    return SeqExpressionBuilder.buildIdExpression(declaration);
  }

  // Helpers =======================================================================================

  /** Pads the resulting hex string to pLength, e.g. 0x0 -> 0x00 for length 2. */
  public static String padHexString(int pLength, BigInteger pBigInteger) {
    return SeqStringUtil.hexFormat(pLength, pBigInteger);
  }

  public static int convertBinaryLengthToHex(int pBinaryLength) {
    assert isValidBinaryLength(pBinaryLength) : "pBinaryLength is invalid";
    return pBinaryLength / 4;
  }

  public static int convertHexLengthToBinary(int pHexLength) {
    return pHexLength * 4;
  }
}
