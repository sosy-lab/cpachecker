// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BitVectorUtil {

  public static final int MIN_BIT_VECTOR_LENGTH = 8;
  public static final int MAX_BIT_VECTOR_LENGTH = 64;

  public static boolean isValidLength(int pLength) {
    return pLength == MIN_BIT_VECTOR_LENGTH
        || pLength == 16
        || pLength == 32
        || pLength == MAX_BIT_VECTOR_LENGTH;
  }

  public static <T> SeqBitVector createBitVector(
      @NonNull ImmutableMap<T, Integer> pIndices, @NonNull ImmutableSet<T> pVariables) {

    // TODO test - concurrent programs without global variables?
    checkArgument(!pIndices.isEmpty(), "no global variable found");

    final int length = getBitVectorLength(pIndices);
    final ImmutableSet<Integer> setBits =
        pIndices.entrySet().stream()
            .filter(entry -> pVariables.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(ImmutableSet.toImmutableSet());
    return new SeqBinaryBitVector(length, setBits);
  }

  private static <T> int getBitVectorLength(ImmutableMap<T, Integer> pIndices) {
    checkArgument(
        pIndices.size() <= MAX_BIT_VECTOR_LENGTH,
        "cannot have more than "
            + MAX_BIT_VECTOR_LENGTH
            + " global variables, please disable bit vectors for this program.");
    int rLength = MIN_BIT_VECTOR_LENGTH;
    while (rLength < pIndices.size()) {
      rLength *= 2;
    }
    assert isValidLength(rLength);
    return rLength;
  }
}
