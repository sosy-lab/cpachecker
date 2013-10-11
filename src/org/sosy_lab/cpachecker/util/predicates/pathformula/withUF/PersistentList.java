/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.ListIterator;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Joiner;

/**
* Persistent immutable single linked lists.
*/
@Immutable
public class PersistentList<T> extends AbstractSequentialList<T> {

  private PersistentList(final T head, final PersistentList<T> tail) {
    this.head = head;
    this.tail = tail;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static PersistentList makeEmpty() {
    PersistentList empty = new PersistentList(null, null);
    empty.tail = empty;
    return empty;
  }

  /** Returns the empty list.
   *  @return The empty list */
  @SuppressWarnings("unchecked")
  public static <T> PersistentList<T> empty() {
    return EMPTY;
  }

  /** Returns a list containing the specified value.
   *  @return A list containing the specified value */
  @SuppressWarnings("unchecked")
  public static <T> PersistentList<T> of(final T value) {
    return new PersistentList<>(value, EMPTY);
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentList<T> of(final T v1, final T v2) {
    return EMPTY.with(v2).with(v1);
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentList<T> of(final T v1, final T v2, final T v3) {
    return EMPTY.with(v3).with(v2).with(v1);
  }

  /** Returns a list containing the specified values.
   *  @return A list containing the specified values */
  @SuppressWarnings("unchecked")
  public static <T> PersistentList<T> of(final T ... values) {
    PersistentList<T> result = EMPTY;
    for (int i = values.length - 1; i >= 0; --i) {
      result = result.with(values[i]);
    }
    return result;
  }

  /** Returns A new list with the values from the Iterable.
   *  @return A new list with the values from the Iterable */
  public static <T> PersistentList<T> copyOf(final Iterable<T> values) {
    return new Builder<T>().addAll(values).build();
  }

  /** Returns the value at the start of the list.
   *  @return The value at the start of the list */
  public T head() {
    return head;
  }

  /** Returns the remainder of the list without the first element.
   *  @return The remainder of the list without the first element */
  public PersistentList<T> tail() {
    return tail;
  }

  /** Returns a new list with value as the head and the old list as the tail.
   *  @return A new list with value as the head and the old list as the tail */
  public PersistentList<T> with(final T value) {
    checkNotNull(value);
    return new PersistentList<>(value, this);
  }

  @Override
  public T get(int i) {
    if (i >= 0) {
      for (PersistentList<T> list = this; list != EMPTY; list = list.tail()) {
        if (i-- == 0) {
          return list.head();
        }
      }
    }
    throw new IndexOutOfBoundsException();
  }

  /** Returns a new list omitting the specified value.
   *  Note: O(N)
   *  @return A new list omitting the specified value
   */
  public PersistentList<T> without(final T value) {
    if (!contains(value)) {
      return this;
    }
    final Builder<T> head = new Builder<>();
    PersistentList<T> list = this;
    do {
      for (; list != EMPTY && !value.equals(list.head()); list = list.tail()) {
        head.add(list.head());
      }
      while (list != EMPTY && value.equals(list.head())) {
        list = list.tail();
      }
    } while (list.contains(value));
    // list is now the longest tail that doesn't contain x
    return head.buildOnto(list);
  }

  /** Note: O(N) */
  @Override
  public boolean contains(final Object value) {
    for (PersistentList<T> list = this; list != EMPTY; list = list.tail) {
      if (value.equals(list.head())) {
        return true;
      }
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PersistentList)) {
      return false;
    }
    PersistentList<? extends Object> x = this;
    PersistentList<Object> y = (PersistentList<Object>) other;
    for (; x != EMPTY && y != EMPTY; x = x.tail(), y = y.tail()) {
      if (!x.head().equals(y.head())) {
        return false;
      }
    }
    return x == EMPTY && y == EMPTY;
  }

  @Override
  public int hashCode() {
    final int prime = 3571;
    int result = 0;
    for (PersistentList<T> p = this; p != EMPTY; p = p.tail) {
      result = result * prime + p.head.hashCode();
    }
    return result;
  }

  @Override
  public String toString() {
    return "[" + Joiner.on(", ").join(this) + "]";
  }

  /**
   * Returns the number of elements in the list.
   * Note: O(N)
   * @return The number of elements in the list
   */
  @Override
  public int size() {
    int size = 0;
    for (PersistentList<T> list = this; list != EMPTY; list = list.tail) {
      ++size;
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    return this == EMPTY;
  }

  /** Returns a new list with the elements in the reverse order.
   *  @return A new list with the elements in the reverse order */
  public PersistentList<T> reversed() {
    PersistentList<T> result = empty();
    for (PersistentList<T> p = this; p != EMPTY; p = p.tail) {
      result = result.with(p.head);
    }
    return result;
  }

  /**
   * Returns concatenation of the list with the given list,
   * updating the tail of the last element of this list.
   * Note: updating operating, not persistent!
   * @return Concatenation of this list with the given list
   */
  public PersistentList<T> destructiveBuildOnto(PersistentList<T> newTail) {
    PersistentList<T> last = this;
    for (PersistentList<T> p = tail; p != empty(); p = p.tail) {
      last = p;
    }
    last.tail = newTail;
    return this;
  }

  public static class Builder<T> {

    public Builder<T> add(final T value) {
      list = list.with(value);
      return this;
    }

    public Builder<T> addAll(final Iterable<? extends T> values) {
      for (T value : values) {
        add(value);
      }
      return this;
    }

    /**
     * The Builder cannot be used after calling build()
     * @return The list
     */
    @SuppressWarnings("unchecked")
    public PersistentList<T> build() {
      return list;
    }

    public PersistentList<T> buildOnto(final PersistentList<T> tail) {
      if (list == empty()) {
        return tail;
      }
      PersistentList<T> last = list;
      for (PersistentList<T> p = list.tail; p != empty(); p = p.tail) {
        last = p;
      }
      last.tail = tail;
      return list;
    }

    /**
     * The Builder cannot be used after calling build()
     * @return The list
     */
    @SuppressWarnings("unchecked")
    public PersistentList<T> buildReversed() {
      return buildReversedOnto((PersistentList<T>) empty());
    }

    public PersistentList<T> buildReversedOnto(PersistentList<T> tail) {
      // reverse in place by changing pointers (no allocation)
      for (PersistentList<T> p = list; p != empty();) {
        final PersistentList<T> next = p.tail;
        p.tail = tail;
        tail = p;
        p = next;
      }
      list = null;
      return tail;
    }

    private PersistentList<T> list = empty();
  }

  @Override
  public Iterator<T> iterator() {
    return new Iter<>(this);
  }

  private static class Iter<T> implements Iterator<T> {

    private Iter(PersistentList<T> list) {
      this.list = new PersistentList<>(null, list);
    }

    @Override
    public boolean hasNext() {
      return list.tail() != EMPTY;
    }

    @Override
    public T next() {
      list = list.tail();
      return list.head();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private PersistentList<T> list;
  }

  @Override
  public ListIterator<T> listIterator(final int index) {
    throw new UnsupportedOperationException();
  }

  private final T head;
  // Builder uses mutable private copies
  private PersistentList<T> tail;

  @SuppressWarnings({ "rawtypes" })
  private static final PersistentList EMPTY = makeEmpty();
}