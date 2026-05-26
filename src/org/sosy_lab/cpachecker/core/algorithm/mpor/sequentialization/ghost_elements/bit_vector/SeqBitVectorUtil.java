// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SeqBitVectorUtil {

  public static CIntegerLiteralExpression buildBitVectorExpression(
      SeqBitVectorEncoding pEncoding,
      MachineModel pMachineModel,
      SeqPointerAliasingMap pPointerAliasingMap,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations)
      throws UnsupportedCodeException {

    checkArgument(pEncoding.isEnabled(), "no bit vector encoding specified");
    checkArgument(
        pPointerAliasingMap.getAllMemoryLocations().containsAll(pAccessedMemoryLocations),
        "pMemoryLocationIds must contain all pAccessedMemoryLocations as keys.");

    CSimpleType type = SeqBitVectorUtil.getBitVectorType(pMachineModel, pPointerAliasingMap);
    BigInteger mask = getRelevantMemoryLocationMask(pAccessedMemoryLocations, pPointerAliasingMap);
    CIntegerLiteralBase base = getIntegerLiteralBaseByEncoding(pEncoding);
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, mask, base);
  }

  private static BigInteger getRelevantMemoryLocationMask(
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      SeqPointerAliasingMap pPointerAliasingMap) {

    BigInteger mask = BigInteger.ZERO;
    final ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocationIds =
        pPointerAliasingMap.getRelevantMemoryLocations();
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

  static CSimpleType getBitVectorType(
      MachineModel pMachineModel, SeqPointerAliasingMap pPointerAliasingMap)
      throws UnsupportedCodeException {

    final int minimumLength = getMinimumBitVectorLengthInBytes(pMachineModel, pPointerAliasingMap);
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
    throw new UnsupportedCodeException(
        String.format(
            "Could not find an appropriate bit vector CType based on MachineModel %s for"
                + " minimumLength %s. The input program probably contains too many global memory"
                + " locations. Try setting bitVectorEncoding=SPARSE because it supports any amount"
                + " of memory locations.",
            pMachineModel, minimumLength),
        null);
  }

  private static int getMinimumBitVectorLengthInBytes(
      MachineModel pMachineModel, SeqPointerAliasingMap pPointerAliasingMap) {

    final int memoryLocationAmount = pPointerAliasingMap.getRelevantMemoryLocationAmount();
    // a char is always a byte, but a byte doesn't have to be 8 bits
    final int byteSize = pMachineModel.getSizeofCharInBits();
    int lengthInBit = byteSize;
    while (lengthInBit < memoryLocationAmount) {
      lengthInBit *= 2;
    }
    return lengthInBit / byteSize;
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
   * Returns {@code true} if creating a bit vector with the given {@link SeqMemoryAccessType} and
   * {@link SeqMemoryReachType} is required based on the specified options.
   */
  public static boolean isAccessReachPairNeeded(
      MPOROptions pOptions, SeqMemoryAccessType pAccessType, SeqMemoryReachType pReachType) {

    if (pReachType.equals(SeqMemoryReachType.DIRECT)
        && !pOptions.executeCommutingThreadsFirst()
        && !pOptions.abortCommutingContextSwitches()) {
      return false;
    }
    return switch (pOptions.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException("cannot check for partialOrderReductionMode NONE");
      case ACCESS_ONLY -> pAccessType.equals(SeqMemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          switch (pReachType) {
            case DIRECT -> pAccessType.in(SeqMemoryAccessType.READ, SeqMemoryAccessType.WRITE);
            case REACHABLE -> pAccessType.in(SeqMemoryAccessType.ACCESS, SeqMemoryAccessType.WRITE);
          };
    };
  }
}
