/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

/**
 * Assignment for {@link SymbolicIdentifier}s.
 */
public class IdentifierAssignment extends ForwardingMap<SymbolicIdentifier, Value> {
  private static final String ERROR_MSG_ASSIGNMENT_REMOVAL =
      "Definite assignments can't be removed!";
  private Map<SymbolicIdentifier, Value> assignment = new HashMap<>();

  public IdentifierAssignment() {
    super();
  }

  public IdentifierAssignment(IdentifierAssignment pAssignment) {
    assignment = Maps.newHashMap(pAssignment);
  }

  @Override
  public Value put(SymbolicIdentifier pIdentifier, Value pValue) {
    assert !pValue.isUnknown();

    return super.put(pIdentifier, pValue);
  }

  @Override
  public Value remove(Object pKey) {
    throw new UnsupportedOperationException(ERROR_MSG_ASSIGNMENT_REMOVAL);
  }

  @Override
  public Value standardRemove(Object pKey) {
    throw new UnsupportedOperationException(ERROR_MSG_ASSIGNMENT_REMOVAL);
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(ERROR_MSG_ASSIGNMENT_REMOVAL);
  }

  @Override
  protected Map<SymbolicIdentifier, Value> delegate() {
    return assignment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IdentifierAssignment that = (IdentifierAssignment) o;

    return assignment.equals(that.assignment);
  }

  @Override
  public int hashCode() {
    return assignment.hashCode();
  }

}
