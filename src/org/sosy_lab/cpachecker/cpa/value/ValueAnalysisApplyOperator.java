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

import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisApplyOperator implements ApplyOperator {

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    // assert !(pState2 instanceof ValueAnalysisStateWithEdge) : "Normal state expected";
    if (pState2 instanceof ValueAnalysisStateWithEdge) {
      ValueAnalysisState state1 = (ValueAnalysisState) pState1;
      ValueAnalysisStateWithEdge state2 = (ValueAnalysisStateWithEdge) pState2;

      if (compatible(state1, state2)) {
        return new ValueAnalysisStateWithEdge(state1, state2.getAbstractEdge());
      } else {
        return null;
      }
    } else {
      // We call apply for all possible states in reached, just return null
      return null;
    }
  }

  private boolean compatible(ValueAnalysisState pState1, ValueAnalysisState pState2) {
    @SuppressWarnings("deprecation")
    Set<MemoryLocation> diff = pState1.getDifference(pState2);

    for (MemoryLocation mem : diff) {
      if (!mem.isOnFunctionStack() && pState1.contains(mem)) {
        Value thisValue = pState1.getValueFor(mem);
        Value otherValue = pState2.getValueFor(mem);
        if (!thisValue.equals(otherValue)) {
          return false;
        }
      }
    }

    return true;
  }

  private AbstractEdge
      prepareEdge(ValueAnalysisState pParent, ValueAnalysisState pChild, AbstractEdge pEdge) {

    if (pEdge instanceof WrapperCFAEdge) {
      CFAEdge edge = ((WrapperCFAEdge) pEdge).getCFAEdge();
      if (edge instanceof CDeclarationEdge
          || edge instanceof BlankEdge
          || edge instanceof CAssumeEdge) {
        return EmptyEdge.getInstance();
      }
    }

    ValueAnalysisState newSrc = ValueAnalysisState.copyOf(pParent);
    PersistentMap<MemoryLocation, ValueAndType> assignments = PathCopyingPersistentTreeMap.of();

    Set<MemoryLocation> allMems = newSrc.getTrackedMemoryLocations();

    for (MemoryLocation mem : allMems) {
      if (mem.isOnFunctionStack()) {
        newSrc.forget(mem);
      }
    }

    allMems = pChild.getTrackedMemoryLocations();

    for (MemoryLocation mem : allMems) {
      if (mem.isOnFunctionStack()) {
        // Do nothing
      } else if (newSrc.contains(mem)) {
        ValueAndType oldVal = newSrc.getValueAndTypeFor(mem);
        ValueAndType newVal = pChild.getValueAndTypeFor(mem);
        if (!oldVal.equals(newVal)) {
          assignments = assignments.putAndCopy(mem, newVal);
        }
      } else {
        ValueAndType newVal = pChild.getValueAndTypeFor(mem);
        assignments = assignments.putAndCopy(mem, newVal);
      }
    }

    for (MemoryLocation mem : newSrc.getTrackedMemoryLocations()) {
      if (!pChild.contains(mem)) {
        ValueAndType newVal =
            new ValueAndType(UnknownValue.getInstance(), newSrc.getTypeForMemoryLocation(mem));
        assignments = assignments.putAndCopy(mem, newVal);
      }
    }

    if (assignments.size() == 0) {
      return EmptyEdge.getInstance();
    } else {
      ValueAnalysisInformation info =
          new ValueAnalysisInformation(assignments);
      return new ValueAbstractEdge(info);
    }
  }

  @Override
  public AbstractState project(AbstractState pState1, AbstractState pState2) {
    return project(pState1, pState2, null);
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    AbstractEdge edge =
        prepareEdge((ValueAnalysisState) pParent, (ValueAnalysisState) pChild, pEdge);

    ValueAnalysisState guard = ValueAnalysisState.copyOf((ValueAnalysisState) pParent);
    Collection<MemoryLocation> mems = ((ValueAnalysisState) pParent).getTrackedMemoryLocations();
    for (MemoryLocation mem : mems) {
      if (mem.isOnFunctionStack()) {
        guard.forget(mem);
      }
    }

    return new ValueAnalysisStateWithEdge(guard, edge);
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    ValueAnalysisState state = (ValueAnalysisState) pState;
    return state.getSize() == 0;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    return true;
  }
}
