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
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBinaryBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;

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

  public static SeqBitVector createDefaultBitVector(int pLength) {
    return new SeqBinaryBitVector(pLength, ImmutableSet.of());
  }

  public static <T> SeqBitVector createBitVector(
      @NonNull ImmutableMap<T, Integer> pIndices, @NonNull ImmutableSet<T> pVariables) {

    // TODO test - concurrent programs without global variables?
    checkArgument(!pIndices.isEmpty(), "no global variable found");
    /*checkArgument(
    pIndices.entrySet().containsAll(pVariables),
    "pIndices must contain all pVariables as keys.");*/

    final ImmutableSet<Integer> setBits =
        pIndices.entrySet().stream()
            .filter(entry -> pVariables.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(ImmutableSet.toImmutableSet());
    return new SeqBinaryBitVector(getBitVectorLength(pIndices.size()), setBits);
  }

  public static int getBitVectorLength(int pMinLength) {
    checkArgument(
        pMinLength <= MAX_LENGTH,
        "cannot have more than "
            + MAX_LENGTH
            + " global variables, please disable bit vectors for this program.");
    int rLength = MIN_LENGTH;
    while (rLength < pMinLength) {
      rLength *= 2;
    }
    assert isValidLength(rLength);
    return rLength;
  }
}
