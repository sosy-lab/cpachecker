// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A generic Triple class based on Pair.java.
 *
 * <p>PLEASE DO NOT USE THIS CLASS! It is better design to use proper specific classes that have
 * semantically meaningful names instead of Pair and Triple. There might be cases where usage of
 * such generic classes is understandable, but their mere presence invites to misuse them and
 * introduce non-understandable code using things like {@code Triple<String, String, String>}. Thus
 * the general goal is to remove these classes completely, CPAchecker just relies too heavily on
 * them for now.
 *
 * <p>Please do not use these two classes in new code. Either write a custom class with meaningful
 * names, or we start using AutoValue in CPAchecker to generate such classes automatically.
 */
public class Triple<A, B, C> implements Serializable {

  private static final long serialVersionUID = 1272029955865151903L;

  private final @Nullable A first;
  private final @Nullable B second;
  private final @Nullable C third;

  private Triple(@Nullable A first, @Nullable B second, @Nullable C third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public static <A, B, C> Triple<A, B, C> of(
      @Nullable A first, @Nullable B second, @Nullable C third) {
    return new Triple<>(first, second, third);
  }

  public final @Nullable A getFirst() {
    return first;
  }

  public final @Nullable B getSecond() {
    return second;
  }

  public final @Nullable C getThird() {
    return third;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ", " + third + ")";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof Triple<?, ?, ?>)
        && Objects.equals(first, ((Triple<?, ?, ?>) other).first)
        && Objects.equals(second, ((Triple<?, ?, ?>) other).second)
        && Objects.equals(third, ((Triple<?, ?, ?>) other).third);
  }

  @Override
  public int hashCode() {
    if (first == null && second == null) {
      return (third == null) ? 0 : third.hashCode() + 1;
    } else if (first == null && third == null) {
      return second.hashCode() + 2;
    } else if (first == null) {
      return second.hashCode() * 7 + third.hashCode();
    } else if (second == null && third == null) {
      return first.hashCode() + 3;
    } else if (second == null) {
      return first.hashCode() * 11 + third.hashCode();
    } else if (third == null) {
      return first.hashCode() * 13 + second.hashCode();
    } else {
      return first.hashCode() * 17 + second.hashCode() * 5 + third.hashCode();
    }
  }
}
