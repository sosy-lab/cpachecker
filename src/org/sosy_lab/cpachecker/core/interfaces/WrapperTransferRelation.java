/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This interface is intended for TransferRelations that wrap another TransferRelation and use it to
 * compute abstract successor states.
 */
public interface WrapperTransferRelation extends TransferRelation {

  /**
   * Retrieve one of the wrapped TransferRelations by type. If the hierarchy of (wrapped)
   * TransferRelations has several levels, this method searches through them recursively.
   *
   * <p>The type does not need to match exactly, the returned element has just to be a sub-type of
   * the type passed as argument.
   *
   * @param <T> The type of the wrapped element.
   * @param type The class object of the type of the wrapped element.
   * @return An instance of an element with type T or null if there is none.
   */
  @Nullable
  <T extends TransferRelation> T retrieveWrappedTransferRelation(Class<T> type);

  /**
   * Retrieve all wrapped transfers contained directly in this object (not recursively).
   *
   * @return A non-empty unmodifiable list of TransferRelations.
   */
  Iterable<TransferRelation> getWrappedTransferRelations();
}
