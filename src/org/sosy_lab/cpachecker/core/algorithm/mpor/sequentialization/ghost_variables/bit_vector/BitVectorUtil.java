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
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBinaryBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqHexadecimalBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;

public class BitVectorUtil {

  public static final int MIN_BINARY_LENGTH = SeqBitVectorType.__UINT8_T.size;
  public static final int MAX_BINARY_LENGTH = SeqBitVectorType.__UINT64_T.size;

  // Creation ======================================================================================

  public static SeqBitVector createZeroBitVector(MPOROptions pOptions, int pLength) {
    return createBitVectorByEncoding(pOptions.porBitVectorEncoding, pLength, ImmutableSet.of());
  }

  public static <T> SeqBitVector createBitVector(
      MPOROptions pOptions,
      @NonNull ImmutableMap<T, Integer> pIndices,
      @NonNull ImmutableSet<T> pVariables) {

    checkArgument(
        pIndices.keySet().containsAll(pVariables), "pIndices must contain all pVariables as keys.");

    // retrieve all variable ids from pIndices that are in the set pVariables
    final ImmutableSet<Integer> setBits =
        pIndices.entrySet().stream()
            .filter(entry -> pVariables.contains(entry.getKey()))
            .map(Entry::getValue)
            .collect(ImmutableSet.toImmutableSet());
    return createBitVectorByEncoding(pOptions.porBitVectorEncoding, pIndices.size(), setBits);
  }

  private static SeqBitVector createBitVectorByEncoding(
      SeqBitVectorEncoding pEncoding, int pNumGlobalVariables, ImmutableSet<Integer> pSetBits) {

    int length = getBitVectorLengthByEncoding(pEncoding, pNumGlobalVariables);
    return switch (pEncoding) {
      case BINARY -> new SeqBinaryBitVector(length, pSetBits);
      case HEXADECIMAL -> new SeqHexadecimalBitVector(length, pSetBits);
      case SCALAR -> /* TODO */ new SeqBinaryBitVector(length, pSetBits);
    };
  }

  // Vector Length =================================================================================

  public static SeqBitVectorType getTypeByLength(int pLength) {
    for (SeqBitVectorType type : SeqBitVectorType.values()) {
      if (type.size == pLength) {
        return type;
      }
    }
    throw new IllegalArgumentException("no bit vector type with given length found: " + pLength);
  }

  public static int getBitVectorLengthByEncoding(
      SeqBitVectorEncoding pEncoding, int pNumGlobalVariables) {
    checkArgument(
        pNumGlobalVariables <= MAX_BINARY_LENGTH,
        "cannot have more than %s global variables, please disable bit vectors for this program.",
        MAX_BINARY_LENGTH);

    return switch (pEncoding) {
      case BINARY -> getBinaryLength(pNumGlobalVariables);
      case HEXADECIMAL -> convertBinaryLengthToHex(getBinaryLength(pNumGlobalVariables));
      case SCALAR -> pNumGlobalVariables;
    };
  }

  public static int getBinaryLength(int pMinLength) {
    int rLength = MIN_BINARY_LENGTH;
    while (rLength < pMinLength) {
      rLength *= 2;
    }
    assert isValidLength(rLength) : "binary bit vector length is invalid: " + rLength;
    return rLength;
  }

  public static boolean isValidLength(int pLength) {
    for (SeqBitVectorType type : SeqBitVectorType.values()) {
      if (type.size == pLength) {
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
    return pBinaryLength / 4;
  }

  public static int convertHexLengthToBinary(int pHexLength) {
    return pHexLength * 4;
  }
}
