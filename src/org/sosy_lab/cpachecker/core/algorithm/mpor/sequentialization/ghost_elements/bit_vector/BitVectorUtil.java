// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BinaryBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.DecimalBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.HexadecimalBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;

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

  public static final int MIN_BINARY_LENGTH = BitVectorDataType.UINT8_T.size;

  public static final int MAX_BINARY_LENGTH = BitVectorDataType.UINT64_T.size;

  // Creation ======================================================================================

  public static BitVectorValueExpression buildBitVectorExpression(
      BitVectorEncoding pBitVectorEncoding,
      MemoryModel pMemoryModel,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations) {

    checkArgument(pBitVectorEncoding.isEnabled(), "no bit vector encoding specified");
    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    // retrieve all relevant memory location IDs
    ImmutableSet<Integer> setBits = getSetBits(pMemoryLocations, pMemoryModel);
    return buildBitVectorExpressionByEncoding(pBitVectorEncoding, pMemoryModel, setBits);
  }

  /**
   * Creates a bit vector expression based on {@code pSetBits} where the left most index is {@code
   * 0} and the right most index is one smaller than the length of the bit vector.
   */
  private static BitVectorValueExpression buildBitVectorExpressionByEncoding(
      BitVectorEncoding pEncoding, MemoryModel pMemoryModel, ImmutableSet<Integer> pSetBits) {

    int length = getBitVectorLengthByEncoding(pEncoding, pMemoryModel);
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
      MemoryModel pMemoryModel, ImmutableSet<SeqMemoryLocation> pMemoryLocations) {

    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    // for decimal, use the sum of variable ids (starting from 1)
    ImmutableSet<Integer> setBits = getSetBits(pMemoryLocations, pMemoryModel);
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        getTypeByBinaryLength(getBinaryLength(pMemoryModel)),
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

  private static ImmutableSet<Integer> getSetBits(
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations, MemoryModel pMemoryModel) {

    ImmutableSet.Builder<Integer> rSetBits = ImmutableSet.builder();
    final ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocations =
        pMemoryModel.getRelevantMemoryLocations();
    for (SeqMemoryLocation accessedMemoryLocation : pAccessedMemoryLocations) {
      if (relevantMemoryLocations.containsKey(accessedMemoryLocation)) {
        rSetBits.add(Objects.requireNonNull(relevantMemoryLocations.get(accessedMemoryLocation)));
      }
    }
    return rSetBits.build();
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
      BitVectorEncoding pEncoding, MemoryModel pMemoryModel) {

    checkArgument(
        pMemoryModel.getRelevantMemoryLocationAmount() <= MAX_BINARY_LENGTH,
        "cannot have more than %s global variables, please disable bit vectors for this program.",
        MAX_BINARY_LENGTH);

    return switch (pEncoding) {
      case NONE -> throw new IllegalArgumentException("no bit vector encoding specified");
      case BINARY -> getBinaryLength(pMemoryModel);
      // the length does not matter for these, but we use the number of global variables
      case DECIMAL, SPARSE -> pMemoryModel.getRelevantMemoryLocationAmount();
      case HEXADECIMAL -> convertBinaryLengthToHex(getBinaryLength(pMemoryModel));
    };
  }

  public static int getBinaryLength(MemoryModel pMemoryModel) {
    int rLength = MIN_BINARY_LENGTH;
    while (rLength < pMemoryModel.getRelevantMemoryLocationAmount()) {
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

  /**
   * Returns {@code true} if creating a bit vector with the given {@link MemoryAccessType} and
   * {@link ReachType} is required based on the specified options.
   */
  public static boolean isAccessReachPairNeeded(
      boolean pReduceIgnoreSleep,
      ReductionMode pReductionMode,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    if (pReachType.equals(ReachType.DIRECT) && !pReduceIgnoreSleep) {
      return false;
    }
    return switch (pReductionMode) {
      case NONE -> throw new IllegalArgumentException("cannot check for reductionMode NONE");
      case ACCESS_ONLY -> pAccessType.equals(MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          switch (pReachType) {
            case DIRECT -> pAccessType.in(MemoryAccessType.READ, MemoryAccessType.WRITE);
            case REACHABLE -> pAccessType.in(MemoryAccessType.ACCESS, MemoryAccessType.WRITE);
          };
    };
  }
}
