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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueMergeForTransitions implements MergeOperator {

  private final boolean joinStates;

  ValueMergeForTransitions(boolean pFlag) {
    joinStates = pFlag;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    assert pState1.getClass() == pState2.getClass();

    if (pState1.getClass() == ValueAnalysisState.class) {
      if (!joinStates) {
        return pState2;
      } else {
        ValueAnalysisState state1 = (ValueAnalysisState) pState1;
        ValueAnalysisState state2 = (ValueAnalysisState) pState2;
        return state1.join(state2);
      }
    } else if (pState1.getClass() == ValueAnalysisStateWithEdge.class) {
      ValueAnalysisStateWithEdge state1 = (ValueAnalysisStateWithEdge) pState1;
      ValueAnalysisStateWithEdge state2 = (ValueAnalysisStateWithEdge) pState2;
      AbstractEdge edge1 = state1.getAbstractEdge();
      AbstractEdge edge2 = state2.getAbstractEdge();

      if (edge1 == EmptyEdge.getInstance() || edge2 == EmptyEdge.getInstance()) {
        return joinStatesWithEdge(state1, state2, edge2);
      } else if (edge1 instanceof WrapperCFAEdge || edge2 instanceof WrapperCFAEdge) {
        if (!edge1.equals(edge2)) {
          return pState2;
        } else {
          return joinStatesWithEdge(state1, state2, edge2);
        }
      } else {
        ValueAbstractEdge vEdge1 = (ValueAbstractEdge) edge1;
        ValueAbstractEdge vEdge2 = (ValueAbstractEdge) edge2;

        ValueAnalysisInformation diff1 = vEdge1.getDifference();
        ValueAnalysisInformation diff2 = vEdge2.getDifference();

        Map<MemoryLocation, ValueAndType> values1 = diff1.getAssignments();
        Map<MemoryLocation, ValueAndType> values2 = diff2.getAssignments();
        PersistentMap<MemoryLocation, ValueAndType> newValues = PathCopyingPersistentTreeMap.of();

        Set<MemoryLocation> jointMems = Sets.union(values1.keySet(), values2.keySet());

        for (MemoryLocation mem : jointMems) {
          if (values1.containsKey(mem) && !values2.containsKey(mem)) {
            Type newType = values1.get(mem).getType();
            newValues =
                  newValues.putAndCopy(mem, new ValueAndType(UnknownValue.getInstance(), newType));
          } else if (values2.containsKey(mem) && !values1.containsKey(mem)) {
            Type newType = values2.get(mem).getType();
            newValues =
                  newValues.putAndCopy(mem, new ValueAndType(UnknownValue.getInstance(), newType));
          } else if (values1.containsKey(mem) && values2.containsKey(mem)) {
            ValueAndType newType = join(values1.get(mem), values2.get(mem));
            newValues =
                newValues.putAndCopy(mem, newType);
          } else {
            throw new UnsupportedOperationException(
                "Inconsistency detected: " + mem + " is missed");
          }
        }

        return joinStatesWithEdge(
            state1,
            state2,
            new ValueAbstractEdge(new ValueAnalysisInformation(newValues)));
      }

    } else {
      throw new UnsupportedOperationException("Unknown state type: " + pState1.getClass());
    }
  }

  private ValueAndType join(ValueAndType val1, ValueAndType val2) {
    Value value1 = val1.getValue();
    Value value2 = val2.getValue();
    Value newValue;

    if (value1.equals(value2)) {
      newValue = value2;
    } else {
      newValue = UnknownValue.getInstance();
    }

    Type type1 = val1.getType();
    Type type2 = val1.getType();
    Type newType;

    if (type1 == null && type2 != null) {
      // Undef value
      newType = type2;
    } else if (type1 != null && type2 == null) {
      newType = type1;
    } else if (type1 != null && type2 != null && type1.equals(type2)) {
      newType = type1;
    } else {
      throw new UnsupportedOperationException(
          "Types for the same memory locations are not equal: " + type1 + ", " + type2);
    }

    return new ValueAndType(newValue, newType);
  }

  private ValueAnalysisStateWithEdge joinStatesWithEdge(
      ValueAnalysisStateWithEdge state1,
      ValueAnalysisStateWithEdge state2,
      AbstractEdge edge) {
    ValueAnalysisState base;

    if (!joinStates) {
      base = state2;
    } else {
      base = state1.join(state2);
    }
    ValueAnalysisStateWithEdge newState = new ValueAnalysisStateWithEdge(base, edge);

    if (newState.equals(state2)) {
      return state2;
    } else {
      return newState;
    }
  }
}
