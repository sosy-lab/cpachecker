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
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBinaryBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqHexadecimalBitVector;

public class BitVectorUtil {

  public static final int MIN_LENGTH = SeqBitVectorType.__UINT8_T.size;
  public static final int MAX_LENGTH = SeqBitVectorType.__UINT64_T.size;

  public static boolean isValidLength(int pLength) {
    for (SeqBitVectorType type : SeqBitVectorType.values()) {
      if (type.size == pLength) {
        return true;
      }
    }
    return false;
  }

  public static SeqBitVectorType getTypeByLength(int pLength) {
    for (SeqBitVectorType type : SeqBitVectorType.values()) {
      if (type.size == pLength) {
        return type;
      }
    }
    throw new IllegalArgumentException("no bit vector type with given length found: " + pLength);
  }

  public static SeqBitVector allZeroBitVector(MPOROptions pOptions, int pLength) {
    return createBitVectorByEncoding(pOptions.porBitVectorEncoding, pLength, ImmutableSet.of());
  }

  public static <T> SeqBitVector createBitVector(
      MPOROptions pOptions,
      @NonNull ImmutableMap<T, Integer> pIndices,
      @NonNull ImmutableSet<T> pVariables) {

    // TODO test - concurrent programs without global variables?
    checkArgument(!pIndices.isEmpty(), "no global variable found");
    /*checkArgument(
    pIndices.entrySet().containsAll(pVariables),
    "pIndices must contain all pVariables as keys.");*/

    // retrieve all variable ids from pIndices that are in the set pVariables
    final int length = getBitVectorLength(pIndices.size());
    final ImmutableSet<Integer> setBits =
        pIndices.entrySet().stream()
            .filter(entry -> pVariables.contains(entry.getKey()))
            .map(Entry::getValue)
            .collect(ImmutableSet.toImmutableSet());
    return createBitVectorByEncoding(pOptions.porBitVectorEncoding, length, setBits);
  }

  private static SeqBitVector createBitVectorByEncoding(
      SeqBitVectorEncoding pEncoding, int pLength, ImmutableSet<Integer> pSetBits) {
    return switch (pEncoding) {
      case BINARY -> new SeqBinaryBitVector(pLength, pSetBits);
      case HEXADECIMAL -> new SeqHexadecimalBitVector(pLength, pSetBits);
      case SCALAR -> /* TODO */ new SeqBinaryBitVector(pLength, pSetBits);
    };
  }

  public static int getBitVectorLength(int pMinLength) {
    checkArgument(
        pMinLength <= MAX_LENGTH,
        "cannot have more than %s global variables, please disable bit vectors for this program.",
        MAX_LENGTH);
    int rLength = MIN_LENGTH;
    while (rLength < pMinLength) {
      rLength *= 2;
    }
    assert isValidLength(rLength);
    return rLength;
  }

  /** Pads the resulting hex string to the next power of 2, e.g. 0x0 -> 0x00. */
  public static String padHexString(long pLong) {
    String hex = Long.toHexString(pLong);
    int initialLength = hex.length();
    if (initialLength <= 2) {
      return String.format("%0" + 2 + "x", pLong);
    } else if (initialLength <= 4) {
      return String.format("%0" + 4 + "x", pLong);
    } else if (initialLength <= 8) {
      return String.format("%0" + 8 + "x", pLong);
    } else { // long to hex is at most 16 long, so no extra check here
      return String.format("%0" + 16 + "x", pLong);
    }
  }
}
