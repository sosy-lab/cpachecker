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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.CIntegerLiteralBase;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;

public class SeqBitVectorUtil {

  private static final int MIN_BINARY_LENGTH = 8;

  public static final int MAX_BINARY_LENGTH = 64;

  // Creation ======================================================================================

  public static CIntegerLiteralExpression buildBitVectorExpression(
      SeqBitVectorEncoding pEncoding,
      MemoryModel pMemoryModel,
      ImmutableSet<SeqMemoryLocation> pMemoryLocations) {

    checkArgument(pEncoding.isEnabled(), "no bit vector encoding specified");
    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    SeqBitVectorDataType type =
        SeqBitVectorDataType.getTypeByBinaryLength(pMemoryModel.getRelevantMemoryLocationAmount());
    BigInteger mask = getRelevantMemoryLocationMask(pMemoryLocations, pMemoryModel);
    CIntegerLiteralBase base = getIntegerLiteralBaseByEncoding(pEncoding);
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type.simpleType, mask, base);
  }

  public static CIntegerLiteralExpression buildDirectBitVectorExpression(
      MemoryModel pMemoryModel, ImmutableSet<SeqMemoryLocation> pMemoryLocations) {

    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pMemoryLocations),
        "pMemoryLocationIds must contain all pMemoryLocations as keys.");

    // for decimal, use the sum of variable ids (starting from 1)
    BigInteger mask = getRelevantMemoryLocationMask(pMemoryLocations, pMemoryModel);
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, getBitVectorTypeByMemoryModel(pMemoryModel), mask);
  }

  private static BigInteger getRelevantMemoryLocationMask(
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations, MemoryModel pMemoryModel) {

    BigInteger mask = BigInteger.ZERO;
    final ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocationIds =
        pMemoryModel.getRelevantMemoryLocations();
    for (SeqMemoryLocation accessedMemoryLocation : pAccessedMemoryLocations) {
      if (relevantMemoryLocationIds.containsKey(accessedMemoryLocation)) {
        Integer bitIndex = checkNotNull(relevantMemoryLocationIds.get(accessedMemoryLocation));
        // setBit(i) returns a new BigInteger with the i-th bit set (2^i)
        mask = mask.setBit(bitIndex);
      }
    }
    return mask;
  }

  // Vector Length =================================================================================

  static CSimpleType getBitVectorTypeByMemoryModel(MemoryModel pMemoryModel) {
    int binaryLength = MIN_BINARY_LENGTH;
    while (binaryLength < pMemoryModel.getRelevantMemoryLocationAmount()) {
      binaryLength *= 2;
    }
    for (SeqBitVectorDataType dataType : SeqBitVectorDataType.values()) {
      if (dataType.size == binaryLength) {
        return dataType.simpleType;
      }
    }
    throw new IllegalArgumentException(String.format("Invalid pBinaryLength %s", binaryLength));
  }

  // Helpers =======================================================================================

  public static CIntegerLiteralBase getIntegerLiteralBaseByEncoding(
      SeqBitVectorEncoding pEncoding) {

    return switch (pEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              "SeqBitVectorEncoding NONE does not have a corresponding CIntegerLiteralBase.");
      case BINARY -> CIntegerLiteralBase.BINARY;
      case OCTAL -> CIntegerLiteralBase.OCTAL;
      case DECIMAL, SPARSE -> CIntegerLiteralBase.DECIMAL;
      case HEXADECIMAL -> CIntegerLiteralBase.HEXADECIMAL;
    };
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
