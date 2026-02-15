// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;

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

  public static CIntegerLiteralExpression buildBitVectorExpression(
      BitVectorEncoding pBitVectorEncoding,
      MemoryModel pMemoryModel,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations) {

    checkArgument(pBitVectorEncoding.isEnabled(), "no bit vector encoding specified");
    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    BitVectorDataType type =
        BitVectorDataType.getTypeByBinaryLength(pMemoryModel.getRelevantMemoryLocationAmount());
    BigInteger mask = getRelevantMemoryLocationMask(pMemoryLocations, pMemoryModel);
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type.simpleType, mask);
  }

  public static CIntegerLiteralExpression buildDirectBitVectorExpression(
      MemoryModel pMemoryModel, ImmutableSet<SeqMemoryLocation> pMemoryLocations) {

    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    // for decimal, use the sum of variable ids (starting from 1)
    BigInteger mask = getRelevantMemoryLocationMask(pMemoryLocations, pMemoryModel);
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, getTypeByBinaryLength(getBinaryLength(pMemoryModel)), mask);
  }

  private static CSimpleType getTypeByBinaryLength(int pBinaryLength) {
    for (BitVectorDataType dataType : BitVectorDataType.values()) {
      if (dataType.size == pBinaryLength) {
        return dataType.simpleType;
      }
    }
    throw new IllegalArgumentException("invalid pBinaryLength");
  }

  private static BigInteger getRelevantMemoryLocationMask(
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations, MemoryModel pMemoryModel) {

    BigInteger mask = BigInteger.ZERO;
    final ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocationIds =
        pMemoryModel.getRelevantMemoryLocations();
    for (SeqMemoryLocation accessedMemoryLocation : pAccessedMemoryLocations) {
      Integer bitIndex = checkNotNull(relevantMemoryLocationIds.get(accessedMemoryLocation));
      // setBit(i) returns a new BigInteger with the i-th bit set (2^i)
      mask = mask.setBit(bitIndex);
    }
    return mask;
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
