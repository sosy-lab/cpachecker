/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import static com.google.common.base.Preconditions.*;

import java.util.AbstractList;
import java.util.RandomAccess;

import com.google.errorprone.annotations.ForOverride;

/**
 * Immutable list that is backed by a <code>long[]</code> and can have any
 * element type.
 * Sub-classes need to be defined and implement methods for creating
 * element instances from a long value.
 * These classes should not override any other methods to guarantee immutability.
 * @param <E> The element type.
 */
public abstract class LongArrayBackedList<E> extends AbstractList<E> implements RandomAccess {

  private final long[] array;

  /**
   * Create an instance backed by a given array.
   * For efficiency, the array is not copied and should thus not be changed afterwards.
   */
  protected LongArrayBackedList(long[] pArray) {
    array = checkNotNull(pArray);
  }

  @ForOverride
  protected abstract E convert(long e);

  @Override
  public final E get(int pIndex) {
    checkElementIndex(pIndex, array.length);
    return convert(array[pIndex]);
  }

  @Override
  public final int size() {
    return array.length;
  }

}
