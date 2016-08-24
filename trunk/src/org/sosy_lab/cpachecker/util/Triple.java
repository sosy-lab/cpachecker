/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * A generic Triple class based on Pair.java.
 *
 * PLEASE DO NOT USE THIS CLASS!
 * It is better design to use proper specific classes
 * that have semantically meaningful names instead of Pair and Triple.
 * There might be cases where usage of such generic classes
 * is understandable, but their mere presence invites to misuse them
 * and introduce non-understandable code using things like
 * Triple<String, String, String>.
 * Thus the general goal is to remove these classes completely,
 * CPAchecker just relies too heavily on them for now.
 *
 * Please do not use these two classes in new code.
 * Either write a custom class with meaningful names,
 * or we start using AutoValue in CPAchecker to generate such classes
 * automatically.
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
