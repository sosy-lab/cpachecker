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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
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

    FileLocation fileLocation = SampleUtils.getLocationForNode(location);
    Path filename = fileLocation.getFileName();
    String function = location.getFunction().getOrigName();
    int line = fileLocation.getStartingLineInOrigin();
    // TODO: Computing column requires access to file
    //       (e.g. by using offsets computed by InvariantStoreUtil::getLineOffsetsByFile)
    //       but offsets are still not reliable if --preprocess is used.
    int column = 0;

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
