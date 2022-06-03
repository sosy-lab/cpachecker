// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.Writer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class LocalizedRefinablePrecision extends RefinablePrecision {
  /**
   * the immutable collection that determines which variables are tracked at a specific location -
   * if it is null, all variables are tracked
   */
  private final ImmutableSetMultimap<CFANode, MemoryLocation> rawPrecision;

  LocalizedRefinablePrecision(VariableTrackingPrecision pBaseline) {
    super(pBaseline);
    rawPrecision = ImmutableSetMultimap.of();
  }

  private LocalizedRefinablePrecision(
      VariableTrackingPrecision pBaseline,
      ImmutableSetMultimap<CFANode, MemoryLocation> pRawPrecision) {
    super(pBaseline);
    rawPrecision = pRawPrecision;
  }

  @Override
  public LocalizedRefinablePrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
    if (rawPrecision.entries().containsAll(increment.entries())) {
      return this;
    } else {
      ImmutableSetMultimap<CFANode, MemoryLocation> refinedPrec =
          createBuilder().putAll(rawPrecision).putAll(increment).build();
      return new LocalizedRefinablePrecision(super.getBaseline(), refinedPrec);
    }
  }

  private static ImmutableSetMultimap.Builder<CFANode, MemoryLocation> createBuilder() {
    // sorted multimap so that we have deterministic output
    return ImmutableSetMultimap.<CFANode, MemoryLocation>builder()
        .orderKeysBy(Ordering.natural())
        .orderValuesBy(Ordering.natural());
  }

  @Override
  public void serialize(Writer writer) throws IOException {
    for (CFANode currentLocation : rawPrecision.keySet()) {
      writer.write("\n" + currentLocation + ":\n");

      for (MemoryLocation variable : rawPrecision.get(currentLocation)) {
        writer.write(variable.getExtendedQualifiedName() + "\n");
      }
    }
  }

  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision pConsolidatedPrecision) {
    checkArgument(getClass().equals(pConsolidatedPrecision.getClass()));
    LocalizedRefinablePrecision consolidatedPrecision =
        (LocalizedRefinablePrecision) pConsolidatedPrecision;
    checkArgument(super.getBaseline().equals(consolidatedPrecision.getBaseline()));

    ImmutableSetMultimap<CFANode, MemoryLocation> joinedPrec =
        createBuilder().putAll(rawPrecision).putAll(consolidatedPrecision.rawPrecision).build();
    return new LocalizedRefinablePrecision(
        super.getBaseline(), ImmutableSetMultimap.copyOf(joinedPrec));
  }

  @Override
  public int getSize() {
    return rawPrecision.size();
  }

  @Override
  public String toString() {
    return rawPrecision.toString();
  }

  @Override
  public boolean isEmpty() {
    return rawPrecision.isEmpty();
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
    return super.isTracking(pVariable, pType, pLocation)
        && rawPrecision.containsEntry(pLocation, pVariable);
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    if (pOtherPrecision.getClass().equals(getClass())
        && super.getBaseline().equals(((LocalizedRefinablePrecision) pOtherPrecision).getBaseline())
        && rawPrecision.equals(((LocalizedRefinablePrecision) pOtherPrecision).rawPrecision)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other)
        && other instanceof LocalizedRefinablePrecision
        && rawPrecision.equals(((LocalizedRefinablePrecision) other).rawPrecision);
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + rawPrecision.hashCode();
  }
}
