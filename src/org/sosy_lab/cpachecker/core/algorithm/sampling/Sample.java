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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
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
