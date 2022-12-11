// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class Sample {

  public enum SampleClass {
    POSITIVE,
    NEGATIVE,
    UNKNOWN
  }

  // Mapping of variables to their respective values and types
  private final ImmutableMap<MemoryLocation, ValueAndType> variableValues;
  // Location where this sample occurs
  private final CFANode location;

  private final SampleClass sampleClass;

  public Sample(Map<MemoryLocation, ValueAndType> pVariableValues, CFANode pLocation) {
    this(pVariableValues, pLocation, SampleClass.UNKNOWN);
  }

  public Sample(
      Map<MemoryLocation, ValueAndType> pVariableValues,
      CFANode pLocation,
      SampleClass pSampleClass) {
    variableValues = ImmutableMap.copyOf(pVariableValues);
    location = pLocation;
    sampleClass = pSampleClass;
  }

  public static Sample fromAbstractState(
      AbstractState pState, Set<MemoryLocation> relevantVariables, SampleClass pSampleClass) {
    CFANode location = AbstractStates.extractLocation(pState);

    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(pState, ValueAnalysisState.class);
    if (valueState == null) {
      return new Sample(ImmutableMap.of(), location, SampleClass.UNKNOWN);
    }

    ImmutableMap.Builder<MemoryLocation, ValueAndType> builder = ImmutableMap.builder();
    for (MemoryLocation memoryLocation : valueState.createInterpolant().getMemoryLocations()) {
      if (relevantVariables.contains(memoryLocation)) {
        builder.put(memoryLocation, valueState.getValueAndTypeFor(memoryLocation));
      }
    }
    return new Sample(builder.build(), location, pSampleClass);
  }

  public Map<MemoryLocation, ValueAndType> getVariableValues() {
    return variableValues;
  }

  public CFANode getLocation() {
    return location;
  }

  public SampleClass getSampleClass() {
    return sampleClass;
  }

  public String export() {
    StringBuilder sb =
        new StringBuilder().append(String.format("{\"sampleClass\": \"%s\", ", sampleClass));

    Path filename = location.getFunction().getFileLocation().getFileName();
    String function = location.getFunction().getOrigName();
    int line = -1;
    // TODO: Determining correct column likely requires access to file
    int column = 0;
    Set<FileLocation> fileLocations = new HashSet<>();
    if (location.getNumLeavingEdges() > 0) {
      fileLocations =
          CFAUtils.leavingEdges(location)
              .transform(CFAEdge::getFileLocation)
              .filter(fl -> fl != null && !FileLocation.DUMMY.equals(fl))
              .toSet();
      assert fileLocations.size() < 2 : "Ambiguous location for sample " + this;
      if (!fileLocations.isEmpty()) {
        line = fileLocations.iterator().next().getStartingLineNumber();
      }
    }
    if (fileLocations.isEmpty()) {
      // No leaving edges or all leaving edges are missing location information
      fileLocations =
          CFAUtils.enteringEdges(location)
              .transform(CFAEdge::getFileLocation)
              .filter(fl -> fl != null && !FileLocation.DUMMY.equals(fl))
              .toSet();
      assert fileLocations.size() == 1 : "Ambiguous location for sample " + this;
      line = fileLocations.iterator().next().getEndingLineNumber();
    }
    assert line > 0;
    sb.append(
        String.format(
            "\"location\": {\"filename\": \"%s\", \"function\": \"%s\", \"line\": %d, \"column\": %d}, \"assignments\": ",
            filename, function, line, column));

    StringJoiner stringJoiner = new StringJoiner(",", "[", "]}");
    for (Entry<MemoryLocation, ValueAndType> entry : variableValues.entrySet()) {
      // TODO: Currently only numeric values are supported
      assert entry.getValue().getValue().isNumericValue();
      stringJoiner.add(
          String.format(
              "{\"variable\": \"%s\", \"value\": \"%s\", \"type\": \"%s\"}",
              entry.getKey().getIdentifier(),
              ((NumericValue) entry.getValue().getValue()).getNumber(),
              entry.getValue().getType()));
    }
    return sb.append(stringJoiner).toString();
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
