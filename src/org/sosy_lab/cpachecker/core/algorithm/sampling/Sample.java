// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
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

  /** Variable types supported by the sample format. */
  private enum VariableType {
    INT("Int"),
    BOOL("Bool"),
    STRING("String");

    private final String name;

    VariableType(String pName) {
      name = pName;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  // Mapping of variables to their respective values and types
  private final ImmutableSortedMap<MemoryLocation, ValueAndType> variableValues;
  // Location where this sample occurs
  private final CFANode location;
  private final Sample previous;
  private final SampleClass sampleClass;
  private final UUID uuid = UUID.randomUUID();

  public Sample(
      Map<MemoryLocation, ValueAndType> pVariableValues,
      CFANode pLocation,
      Sample pPrevious,
      SampleClass pSampleClass) {
    variableValues = ImmutableSortedMap.copyOf(pVariableValues);
    location = pLocation;
    previous = pPrevious;
    sampleClass = pSampleClass;
  }

  public static Map<MemoryLocation, ValueAndType> getValuesAndTypesFromAbstractState(
      AbstractState pState, Set<MemoryLocation> relevantVariables) {
    ValueAnalysisState valueState =
        AbstractStates.extractStateByType(pState, ValueAnalysisState.class);
    if (valueState == null) {
      return ImmutableSortedMap.of();
    }

    ImmutableSortedMap.Builder<MemoryLocation, ValueAndType> builder =
        ImmutableSortedMap.naturalOrder();
    for (MemoryLocation memoryLocation : valueState.createInterpolant().getMemoryLocations()) {
      if (relevantVariables.contains(memoryLocation)) {
        builder.put(memoryLocation, valueState.getValueAndTypeFor(memoryLocation));
      }
    }
    return builder.buildOrThrow();
  }

  public Sample withPrevious(Sample pPrevious) {
    Preconditions.checkArgument(pPrevious.getSampleClass().equals(sampleClass));
    return new Sample(variableValues, location, pPrevious, sampleClass);
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

  public UUID getId() {
    return uuid;
  }

  public String export() {
    StringBuilder sb = new StringBuilder().append(String.format("{\"id\": \"%s\", ", uuid));
    if (previous != null) {
      sb.append(String.format("\"previousSample\": \"%s\", ", previous.getId()));
    }
    sb.append(String.format("\"sampleClass\": \"%s\", ", sampleClass));

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
            "\"location\": {\"filename\": \"%s\", \"function\": \"%s\", \"line\": %d, \"column\":"
                + " %d}, \"assignments\": ",
            filename, function, line, column));

    StringJoiner stringJoiner = new StringJoiner(",", "[", "]}");
    for (Entry<MemoryLocation, ValueAndType> entry : variableValues.entrySet()) {
      // TODO: Support more types
      assert entry.getValue().getValue().isNumericValue();
      Type type = entry.getValue().getType();
      assert type instanceof CSimpleType;
      CSimpleType simpleType = (CSimpleType) type;

      VariableType variableType = null;
      if (simpleType.getType() == CBasicType.BOOL) {
        variableType = VariableType.BOOL;
      } else if (simpleType.getType().isIntegerType()) {
        variableType = VariableType.INT;
      }
      assert variableType != null;

      stringJoiner.add(
          String.format(
              "{\"variable\": \"%s\", \"value\": \"%s\", \"type\": \"%s\"}",
              entry.getKey().getIdentifier(),
              ((NumericValue) entry.getValue().getValue()).getNumber(),
              variableType));
    }
    return sb.append(stringJoiner).toString();
  }

  @Override
  public String toString() {
    return variableValues.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof Sample)) {
      return false;
    }
    Sample sample = (Sample) pO;
    return variableValues.equals(sample.variableValues) && location.equals(sample.location);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableValues, location);
  }
}
