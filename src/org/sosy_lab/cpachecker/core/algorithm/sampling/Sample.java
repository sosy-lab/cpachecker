// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class Sample {

  // Mapping of variables to their respective values and types
  private final ImmutableMap<MemoryLocation, ValueAndType> variableValues;
  // Location where this sample occurs
  private final CFANode location;

  public Sample(Map<MemoryLocation, ValueAndType> pVariableValues, CFANode pLocation) {
    variableValues = ImmutableMap.copyOf(pVariableValues);
    location = pLocation;
  }

  public static Sample fromAbstractState(
      AbstractState pState, Set<MemoryLocation> relevantVariables) {
    CFANode location = AbstractStates.extractLocation(pState);

    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(pState, ValueAnalysisState.class);
    if (valueState == null) {
      return new Sample(ImmutableMap.of(), location);
    }

    ImmutableMap.Builder<MemoryLocation, ValueAndType> builder = ImmutableMap.builder();
    for (MemoryLocation memoryLocation : valueState.createInterpolant().getMemoryLocations()) {
      if (relevantVariables.contains(memoryLocation)) {
        builder.put(memoryLocation, valueState.getValueAndTypeFor(memoryLocation));
      }
    }
    return new Sample(builder.build(), location);
  }

  public Map<MemoryLocation, ValueAndType> getVariableValues() {
    return variableValues;
  }

  public CFANode getLocation() {
    return location;
  }

  public String writePrecisionFile() {
    // TODO
    return null;
  }

  @Override
  public String toString() {
    return variableValues.toString();
  }
}
