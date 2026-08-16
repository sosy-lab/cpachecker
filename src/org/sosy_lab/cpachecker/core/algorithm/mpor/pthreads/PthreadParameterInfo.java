// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import java.util.Arrays;

// immutability is required for usage in enums
@Immutable
class PthreadParameterInfo {

  private final PthreadObjectType objectType;

  private final boolean isPointer;

  private final ImmutableSet<Integer> indices;

  /**
   * Note that {@code pIndices} start at {@code 0}. Use this constructor only for {@link
   * PthreadObjectType#PTHREAD_T} since it is the only parameter that is sometimes not a pointer.
   */
  PthreadParameterInfo(PthreadObjectType pObjectType, boolean pIsPointer, int... pIndices) {
    checkArgument(
        pObjectType.equals(PthreadObjectType.PTHREAD_T),
        "pObjectType must be PTHREAD_T, got %s instead",
        pObjectType);
    objectType = pObjectType;
    isPointer = pIsPointer;
    indices =
        Arrays.stream(pIndices)
            .boxed() // conversion from int to Integer is necessary
            .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Note that {@code pIndices} start at {@code 0}. Note that {@code pIndices} start at {@code 0}.
   * Use this constructor for all objects that are not {@link PthreadObjectType#PTHREAD_T} since it
   * is the only parameter that is sometimes not a pointer.
   */
  PthreadParameterInfo(PthreadObjectType pObjectType, int... pIndices) {
    checkArgument(
        !pObjectType.equals(PthreadObjectType.PTHREAD_T), "pObjectType cannot be PTHREAD_T");
    objectType = pObjectType;
    isPointer = true;
    indices =
        Arrays.stream(pIndices)
            .boxed() // convert from int to Integer
            .collect(ImmutableSet.toImmutableSet());
  }

  public PthreadObjectType getObjectType() {
    return objectType;
  }

  public boolean isPointer() {
    return isPointer;
  }

  public ImmutableSet<Integer> getIndices() {
    return indices;
  }
}
