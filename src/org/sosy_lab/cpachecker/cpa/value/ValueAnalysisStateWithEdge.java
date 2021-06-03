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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisStateWithEdge extends ValueAnalysisState implements AbstractStateWithEdge {

  private static final long serialVersionUID = -2542917754288332149L;
  private final AbstractEdge edge;

  protected ValueAnalysisStateWithEdge(ValueAnalysisState pState, AbstractEdge pEdge) {
    super(pState);
    edge = pEdge;
  }

  @Override
  public AbstractEdge getAbstractEdge() {
    return edge;
  }

  @Override
  public boolean isLessOrEqual(ValueAnalysisState other) {

    if (getClass() != other.getClass()) {
      return false;
    }

    boolean result = super.isLessOrEqual(other);

    if (!result) {
      return false;
    }

    AbstractEdge otherEdge = ((ValueAnalysisStateWithEdge) other).getAbstractEdge();
    if (otherEdge.equals(edge)) {
      return true;
    } else if (edge instanceof ValueAbstractEdge && otherEdge instanceof ValueAbstractEdge) {
      Map<MemoryLocation, ValueAndType> assignments =
          ((ValueAbstractEdge) edge).getDifference().getAssignments();
      Map<MemoryLocation, ValueAndType> otherAssignments =
          ((ValueAbstractEdge) otherEdge).getDifference().getAssignments();

      for (Entry<MemoryLocation, ValueAndType> entry : otherAssignments.entrySet()) {
        MemoryLocation mem = entry.getKey();
        ValueAndType val = entry.getValue();
        if (assignments.containsKey(mem)) {
          ValueAndType otherVal = assignments.get(mem);
          return (val.equals(otherVal) || val.getValue() == UnknownValue.getInstance());
        } else {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public ValueAnalysisState join(ValueAnalysisState reachedState) {
    // Needs to computation base state, so it's normal, that it returns basic element
    return super.join(reachedState);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(edge);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    ValueAnalysisStateWithEdge other = (ValueAnalysisStateWithEdge) obj;
    return Objects.equals(edge, other.edge);
  }

  public boolean hasEqualStates(Object obj) {
    return super.equals(obj);
  }

  @Override
  public boolean hasEmptyEffect() {
    return edge == EmptyEdge.getInstance()
        || ((edge instanceof ValueAbstractEdge)
            && ((ValueAbstractEdge) edge).getDifference().getAssignments().isEmpty());
  }

  @Override
  public boolean isProjection() {
    return false;
  }

}
