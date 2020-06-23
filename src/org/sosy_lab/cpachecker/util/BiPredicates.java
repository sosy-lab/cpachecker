// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Utilities for {@link BiPredicate}, similar to {@link com.google.common.base.Predicates} */
public class BiPredicates {

  public static <T, U> BiPredicate<T, U> alwaysFalse() {
    return (a, b) -> false;
  }

  public static <T, U> BiPredicate<T, U> alwaysTrue() {
    return (a, b) -> true;
  }

  public static <T> BiPredicate<T, T> bothSatisfy(Predicate<? super T> p) {
    return (a, b) -> p.test(a) && p.test(b);
  }

  public static <T> BiPredicate<T, T> bothSatisfy(com.google.common.base.Predicate<? super T> p) {
    return (a, b) -> p.apply(a) && p.apply(b);
  }

  public static <T> BiPredicate<T, T> anySatisfies(Predicate<? super T> p) {
    return (a, b) -> p.test(a) || p.test(b);
  }

  public static <T> BiPredicate<T, T> anySatisfies(com.google.common.base.Predicate<? super T> p) {
    return (a, b) -> p.test(a) || p.test(b);
  }

  public static <T, U> BiPredicate<T, U> pairIn(Collection<Pair<T, U>> collection) {
    return (a, b) -> collection.contains(Pair.of(a, b));
  }

  public static <T, U, R> BiPredicate<T, U> compose(
      BiFunction<? super T, ? super U, R> func, Predicate<? super R> pred) {
    return (a, b) -> pred.test(func.apply(a, b));
  }
}
