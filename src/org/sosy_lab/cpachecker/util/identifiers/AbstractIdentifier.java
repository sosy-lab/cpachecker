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
package org.sosy_lab.cpachecker.util.identifiers;

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;


public interface AbstractIdentifier extends Comparable<AbstractIdentifier> {
  @Override
  public boolean equals(Object other);

  @Override
  public int hashCode();

  @Override
  public String toString();

  public boolean isGlobal();

  public AbstractIdentifier clone();

  public int getDereference();

  public void setDereference(int d);

  public boolean isPointer();

  /**
   * This method recursively checks owners of identifier, if any is contained in given collection.
   * It is useful for structures or binary identifiers, when we should check dependents of this identifier.
   * @param set - some collection of identifiers
   * @return first abstract identifier, which is found or null if no owners are found in collection
   */
  public AbstractIdentifier containsIn(Collection<? extends AbstractIdentifier> set);

  public DataType getType(Map<? extends AbstractIdentifier, DataType> localInfo);
}
