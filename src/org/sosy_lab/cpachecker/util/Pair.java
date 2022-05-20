// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A generic Pair class. Code borrowed from here:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6229146
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
public class Pair<A, B> implements Serializable {

  private static final long serialVersionUID = -8410959888808077296L;

  private final @Nullable A first;
  private final @Nullable B second;

  private Pair(@Nullable A first, @Nullable B second) {
    this.first = first;
    this.second = second;
  }

  public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    return new Pair<>(first, second);
  }

  public @Nullable A getFirst() {
    return first;
  }

  public @Nullable B getSecond() {
    return second;
  }

  /** Get the first parameter, crash if it is null. */
  public A getFirstNotNull() {
    checkNotNull(first);
    return first;
  }

  /** Get the second parameter, crash if it is null. */
  public B getSecondNotNull() {
    checkNotNull(second);
    return second;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ")";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof Pair<?, ?>)
        && Objects.equals(first, ((Pair<?, ?>) other).first)
        && Objects.equals(second, ((Pair<?, ?>) other).second);
  }

  @Override
  public int hashCode() {
    if (first == null) {
      return (second == null) ? 0 : second.hashCode() + 1;
    } else if (second == null) {
      return first.hashCode() + 2;
    } else {
      return first.hashCode() * 17 + second.hashCode();
    }
  }

  public static <A, B> List<Pair<A, B>> zipList(
      Collection<? extends A> a, Collection<? extends B> b) {
    List<Pair<A, B>> result = new ArrayList<>(a.size());

    Iterator<? extends A> iteratorA = a.iterator();
    Iterator<? extends B> iteratorB = b.iterator();
    while (iteratorA.hasNext()) {
      checkArgument(iteratorB.hasNext(), "Second list is shorter");

      result.add(Pair.of(iteratorA.next(), iteratorB.next()));
    }
    checkArgument(!iteratorB.hasNext(), "Second list is longer");

    return result;
  }
}
