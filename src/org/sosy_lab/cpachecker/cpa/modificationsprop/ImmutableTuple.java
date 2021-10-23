// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

/**
 * An implementation for immutable tuples that can be used as return value.
 *
 * @param <A> the first tuple element type
 * @param <B> the second tuple element type
 */
class ImmutableTuple<A, B> {
  private final A first;
  private final B second;

  public ImmutableTuple(A pFirst, B pSecond) {
    first = pFirst;
    second = pSecond;
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }
}
