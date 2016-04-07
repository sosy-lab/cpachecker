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

import javax.annotation.Nullable;

/**
 * Interface for classes that allow their objects to be partitioned.
 */
public interface Partitionable {

  /**
   * Returns the key of the current object that indicates in which part of the
   * partition the object belongs.
   *
   * The result of this method:
   * - may be null
   * - must provide meaningful and consistent equals() and hashCode() implementations
   * - must stay constant regarding the relation defined by equals() throughout
   *   the whole lifetime of this object
   *
   * - needs not to stay constant regarding the object identity relation
   *   defined by the == operator
   * - should probably never be an array (they miss a meaningful equals() implementation),
   *   use an list instead
   *
   * There is no restriction on the number of different (regarding equals())
   * keys the objects of one class may have: It is legal for all objects to
   * have the same key as well as for all objects to have different keys.
   * In the former case this method might always return null.
   * In the latter case this method might for example return the current object
   * or the value of {@link Object#hashCode()} for the object (which may be
   * obtained by {@link System#identityHashCode(Object)} even if a superclass
   * overwrites hashCode()).
   *
   * @return a key indicating the part of the partition this object belongs to
   */
  @Nullable
  Object getPartitionKey();

}
