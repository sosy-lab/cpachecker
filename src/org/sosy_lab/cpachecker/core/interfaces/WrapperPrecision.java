/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;

public interface WrapperPrecision extends Precision {

  /**
   * Retrieve one of the wrapped precision elements by type.
   * @param <T> The type of the wrapped precision.
   * @param type The class object of the type of the wrapped precision.
   * @return An instance of a precision with type T or null if there is none.
   */
  public @Nullable <T extends Precision> T retrieveWrappedPrecision(Class<T> type);

  /**
   * Create a new precision object where one of the wrapped precisions is
   * replaced. It looks for a precision with the same type as or a subtype of
   * the given class and replaces this instance with the argument precision.
   * The references to all other precisions are kept. If
   * no precision object with the type of the argument is found, the argument is
   * ignored and null is returned in order to signal this.
   * @param newPrecision A new precision object.
   * @param replaceType Type of precisions that should be replaced by newPrecision.
   * @return A new precision object containing the argument in some place or null.
   */
  public @Nullable Precision replaceWrappedPrecision(
      Precision newPrecision, Predicate<? super Precision> replaceType);

  /**
   * Retrieve all wrapped precisions contained directly in this object.
   * @return A non-empty list of precisions.
   */
  public Iterable<Precision> getWrappedPrecisions();

}
