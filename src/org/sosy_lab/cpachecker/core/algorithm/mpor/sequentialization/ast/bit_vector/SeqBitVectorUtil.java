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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;

public class SeqBitVectorUtil {

  public static CIntegerLiteralExpression buildBitVectorExpression(
      SeqBitVectorEncoding pEncoding,
      MachineModel pMachineModel,
      MemoryModel pMemoryModel,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations) {

    checkArgument(pEncoding.isEnabled(), "no bit vector encoding specified");
    checkArgument(
        pMemoryModel.getAllMemoryLocations().containsAll(pAccessedMemoryLocations),
        "pMemoryLocationIds must contain all pAccessedMemoryLocations as keys.");

    CSimpleType type = SeqBitVectorUtil.getBitVectorTypeByMemoryModel(pMachineModel, pMemoryModel);
    BigInteger mask = getRelevantMemoryLocationMask(pAccessedMemoryLocations, pMemoryModel);
    CIntegerLiteralBase base = getIntegerLiteralBaseByEncoding(pEncoding);
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, mask, base);
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

  static CSimpleType getBitVectorTypeByMemoryModel(
      MachineModel pMachineModel, MemoryModel pMemoryModel) {

    final int minimumLength = getMinimumBitVectorLengthInBytes(pMachineModel, pMemoryModel);
    if (minimumLength == pMachineModel.getSizeofChar()) {
      return CNumericTypes.UNSIGNED_CHAR;
    }
    if (minimumLength == pMachineModel.getSizeofShortInt()) {
      return CNumericTypes.UNSIGNED_SHORT_INT;
    }
    if (minimumLength == pMachineModel.getSizeofInt()) {
      return CNumericTypes.UNSIGNED_INT;
    }
    if (minimumLength == pMachineModel.getSizeofLongInt()) {
      return CNumericTypes.UNSIGNED_LONG_INT;
    }
    if (minimumLength == pMachineModel.getSizeofLongLongInt()) {
      return CNumericTypes.UNSIGNED_LONG_LONG_INT;
    }
    throw new IllegalArgumentException(
        String.format(
            "Could not find an appropriate bit vector CType based on MachineModel %s for"
                + " minimumLength %s. The input program probably contains too many global memory"
                + " locations. Try setting bitVectorEncoding=SPARSE because it supports any amount"
                + " of memory locations.",
            pMachineModel, minimumLength));
  }

  private static int getMinimumBitVectorLengthInBytes(
      MachineModel pMachineModel, MemoryModel pMemoryModel) {

    int lengthInBit = pMachineModel.getSizeofCharInBits();
    while (lengthInBit < pMemoryModel.getRelevantMemoryLocationAmount()) {
      lengthInBit *= 2;
    }
    return lengthInBit / 4;
  }

  // Helpers =======================================================================================

  private static CIntegerLiteralBase getIntegerLiteralBaseByEncoding(
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
