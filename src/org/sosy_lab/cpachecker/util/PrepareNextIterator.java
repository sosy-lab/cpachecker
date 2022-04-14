// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An iterator implementation that relies on the single method {@link
 * PrepareNextIterator#prepareNext()} which either returns the next element in the iteration or
 * {@code null} if there are no more elements left.
 */
public abstract class PrepareNextIterator<E> implements Iterator<E> {

  private @Nullable E nextElement = null;

  /**
   * Returns the next element in the iteration or {@code null} if there are no more elements left.
   *
   * @return the next element in the iteration or {@code null} if there are no more elements left.
   */
  protected abstract @Nullable E prepareNext();

  @Override
  public final boolean hasNext() {

    if (nextElement == null) {
      nextElement = prepareNext();
    }

    return nextElement != null;
  }

  @Override
  public final E next() {

    if (nextElement == null) {
      nextElement = prepareNext();
    }

    if (nextElement == null) {
      throw new NoSuchElementException();
    }

    E element = nextElement;
    nextElement = null;

    return element;
  }
}
