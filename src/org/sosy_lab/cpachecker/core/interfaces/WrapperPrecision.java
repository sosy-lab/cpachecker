// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.base.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface WrapperPrecision extends Precision {

  /**
   * Retrieve one of the wrapped precision elements by type.
   *
   * @param <T> The type of the wrapped precision.
   * @param type The class object of the type of the wrapped precision.
   * @return An instance of a precision with type T or null if there is none.
   */
  @Nullable <T extends Precision> T retrieveWrappedPrecision(Class<T> type);

  /**
   * Create a new precision object where one of the wrapped precisions is replaced. It looks for a
   * precision with the same type as or a subtype of the given class and replaces this instance with
   * the argument precision. The references to all other precisions are kept. If no precision object
   * with the type of the argument is found, the argument is ignored and null is returned in order
   * to signal this.
   *
   * @param newPrecision A new precision object.
   * @param replaceType Type of precisions that should be replaced by newPrecision.
   * @return A new precision object containing the argument in some place or null.
   */
  @Nullable Precision replaceWrappedPrecision(
      Precision newPrecision, Predicate<? super Precision> replaceType);

  /**
   * Retrieve all wrapped precisions contained directly in this object.
   *
   * @return A non-empty list of precisions.
   */
  Iterable<Precision> getWrappedPrecisions();
}
