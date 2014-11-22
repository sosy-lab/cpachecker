/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Map;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import com.google.common.collect.ImmutableMap;

/**
 * State for Symbolic Execution Analysis.
 *
 * <p>This state contains a mapping of tracked variables and their corresponding symbolic value.
 * </p>
 */
public class ConstraintsState extends ValueAnalysisState {

  /**
   * Stores identifiers and their corresponding values
   */
  private PersistentMap<MemoryLocation, Value> values;

  /**
   * Create a new <code>ConstraintsState</code> object with the given values and their
   * corresponding types.
   *
   * @param pConstantsMap a map containing (identifier, value) pairs
   * @param pLocToTypeMap a map containing (identifier, type) pairs
   */
  public ConstraintsState(Map<MemoryLocation, Value> pConstantsMap, Map<MemoryLocation, Type> pLocToTypeMap) {
    super(PathCopyingPersistentTreeMap.copyOf(pConstantsMap), PathCopyingPersistentTreeMap.copyOf(pLocToTypeMap));
  }

  /**
   * Creates a new, initial  <code>ConstraintsState</code> object.
   */
  public ConstraintsState() {
    values = PathCopyingPersistentTreeMap.of();
  }

  public Map<MemoryLocation, Value> getValueMap() {
    return ImmutableMap.copyOf(values);
  }

  @Override
  public ConstraintsState join(ValueAnalysisState pReached) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns whether this state is less or equal than another given state.
   *
   * @param other the other state to check against
   * @return <code>true</code> if this state is less or equal than the given state, <code>false</code> otherwise
   */
  public boolean isLessOrEqual(ConstraintsState other) {
    boolean lessOrEqual = true;

    if (values.size() > other.values.size()) {
      return false;
    }

    for (Map.Entry<MemoryLocation, Value> otherEntry : other.values.entrySet()) {
      Value otherValue = otherEntry.getValue();
      Value currValue = values.get(otherEntry.getKey());

      if (!otherValue.equals(currValue)) {
        if (otherValue instanceof Constraint && currValue != null) {
          lessOrEqual = ((Constraint) otherValue).includes(currValue);
        } else {
          lessOrEqual = false;
        }
      }

      if (!lessOrEqual) {
        break;
      }
    }

    return lessOrEqual;
  }

  @Override
  public String getCPAName() {
    return "SymbolicExecution";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    return false;
  }

  @Override
  public Object evaluateProperty(String property) throws InvalidQueryException {
    return null;
  }

  @Override
  public void modifyProperty(String modification) throws InvalidQueryException {

  }
}
