/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.holder;

import java.util.Objects;

/**
 * A Holder holds a value. This class is meant as a workaround for inner classes and lambdas which
 * require the variables they use from the outer block to be final or effectively final.
 *
 * @param <T> Type of the value to hold
 */
public final class Holder<T> {

  /**
   * Use this as you would use a local variable
   */
  public T value;

  private Holder(T pValue) {
    value = pValue;
  }

  /**
   * Creates a new Holder
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static <T> Holder<T> of(T value) {
    return new Holder<T>(value);
  }

  /**
   * Creates a new HolderLong
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderLong of(long value) {
    return HolderLong.of(value);
  }

  /**
   * Creates a new HolderShort
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderShort of(short value) {
    return HolderShort.of(value);
  }

  /**
   * Creates a new HolderByte
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderByte of(byte value) {
    return HolderByte.of(value);
  }

  /**
   * Creates a new HolderInt
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderInt of(int value) {
    return HolderInt.of(value);
  }

  /**
   * Creates a new HolderFloat
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderFloat of(float value) {
    return HolderFloat.of(value);
  }

  /**
   * Creates a new HolderDouble
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderDouble of(double value) {
    return HolderDouble.of(value);
  }

  /**
   * Creates a new HolderBoolean
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderBoolean of(boolean value) {
    return HolderBoolean.of(value);
  }

  /**
   * Creates a new HolderChar
   *
   * @param value the initial value to hold
   * @return the new Holder
   */
  public static HolderChar of(char value) {
    return HolderChar.of(value);
  }


  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    Holder<?> holder = (Holder<?>) pO;
    return Objects.equals(value, holder.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
