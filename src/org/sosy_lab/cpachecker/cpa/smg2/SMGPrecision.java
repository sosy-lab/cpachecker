// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.defaults.precision.RefinablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGPrecision extends RefinablePrecision {

  /** the collection that determines which variables are tracked within a specific scope */
  private final ImmutableSortedSet<MemoryLocation> rawPrecision;

  private final ImmutableSet<Value> trackedHeapValues;

  SMGPrecision(VariableTrackingPrecision pBaseline) {
    super(pBaseline);
    rawPrecision = ImmutableSortedSet.of();
    trackedHeapValues = ImmutableSet.of();
  }

  private SMGPrecision(
      VariableTrackingPrecision pBaseline,
      Iterable<MemoryLocation> pRawPrecision,
      Iterable<Value> pTrackedHeapValues) {
    super(pBaseline);
    rawPrecision = ImmutableSortedSet.copyOf(pRawPrecision);
    trackedHeapValues = ImmutableSet.copyOf(pTrackedHeapValues);
  }

  @Override
  public SMGPrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
    if (rawPrecision.containsAll(increment.values())) {
      return this;
    } else {
      Iterable<MemoryLocation> refinedPrec = Iterables.concat(rawPrecision, increment.values());
      return new SMGPrecision(super.getBaseline(), refinedPrec, trackedHeapValues);
    }
  }

  public SMGPrecision withValueIncrement(Multimap<CFANode, Value> increment) {
    if (trackedHeapValues.containsAll(increment.values())) {
      return this;
    } else {
      Iterable<Value> refinedTrackedHeapValues =
          Iterables.concat(trackedHeapValues, increment.values());
      return new SMGPrecision(super.getBaseline(), rawPrecision, refinedTrackedHeapValues);
    }
  }

  @Override
  public void serialize(Writer writer) throws IOException {

    List<String> globals = new ArrayList<>();
    String previousScope = null;

    for (MemoryLocation variable : rawPrecision) {
      if (variable.isOnFunctionStack()) {
        String functionName = variable.getFunctionName();
        if (!functionName.equals(previousScope)) {
          writer.write("\n" + functionName + ":\n");
        }
        writer.write(variable.getExtendedQualifiedName() + "\n");

        previousScope = functionName;
      } else {
        globals.add(variable.getExtendedQualifiedName());
      }
    }

    if (previousScope != null) {
      writer.write("\n");
    }

    writer.write("*:\n" + Joiner.on("\n").join(globals));
  }

  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision pConsolidatedPrecision) {
    Preconditions.checkArgument(getClass().equals(pConsolidatedPrecision.getClass()));
    SMGPrecision consolidatedPrecision = (SMGPrecision) pConsolidatedPrecision;
    checkArgument(super.getBaseline().equals(consolidatedPrecision.getBaseline()));

    Iterable<MemoryLocation> joinedPrec =
        Iterables.concat(rawPrecision, consolidatedPrecision.rawPrecision);
    return new SMGPrecision(
        super.getBaseline(),
        joinedPrec,
        ImmutableSet.<Value>builder()
            .addAll(trackedHeapValues)
            .addAll(consolidatedPrecision.trackedHeapValues)
            .build());
  }

  public ImmutableSet<Value> getTrackedHeapValues() {
    return trackedHeapValues;
  }

  @Override
  public int getSize() {
    return rawPrecision.size() + trackedHeapValues.size();
  }

  @Override
  public String toString() {
    return rawPrecision + " " + trackedHeapValues;
  }

  @Override
  public boolean isEmpty() {
    return rawPrecision.isEmpty() && trackedHeapValues.isEmpty();
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
    return super.isTracking(pVariable, pType, pLocation) && rawPrecision.contains(pVariable);
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    if (pOtherPrecision.getClass().equals(getClass())
        && super.getBaseline().equals(((SMGPrecision) pOtherPrecision).getBaseline())
        && rawPrecision.equals(((SMGPrecision) pOtherPrecision).rawPrecision)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other)
        && other instanceof SMGPrecision
        && rawPrecision.equals(((SMGPrecision) other).rawPrecision);
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + rawPrecision.hashCode();
  }
}
