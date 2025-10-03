// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter.notInternalVariable;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter.variableNameInFunction;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.GlobalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionType;

public class ScopedRefinablePrecision extends RefinablePrecision {

  /** the collection that determines which variables are tracked within a specific scope */
  private final ImmutableSortedSet<MemoryLocation> rawPrecision;

  ScopedRefinablePrecision(VariableTrackingPrecision pBaseline) {
    super(pBaseline);
    rawPrecision = ImmutableSortedSet.of();
  }

  private ScopedRefinablePrecision(
      VariableTrackingPrecision pBaseline, Iterable<MemoryLocation> pRawPrecision) {
    super(pBaseline);
    rawPrecision = ImmutableSortedSet.copyOf(pRawPrecision);
  }

  @Override
  public ScopedRefinablePrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
    if (rawPrecision.containsAll(increment.values())) {
      return this;
    } else {
      Iterable<MemoryLocation> refinedPrec = Iterables.concat(rawPrecision, increment.values());
      return new ScopedRefinablePrecision(super.getBaseline(), refinedPrec);
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
  public List<PrecisionExchangeEntry> asWitnessEntries(CFA pCfa) {
    ImmutableList.Builder<String> globalVariables = ImmutableList.builder();
    ImmutableListMultimap.Builder<String, String> functionWideVariables =
        ImmutableListMultimap.builder();

    for (MemoryLocation variable : rawPrecision) {
      if (variable.isOnFunctionStack()) {
        String functionName = variable.getFunctionName();
        if (notInternalVariable(variable.getQualifiedName())
            && variableNameInFunction(variable.getIdentifier(), functionName)) {
          functionWideVariables.put(variable.getFunctionName(), variable.asCExpression());
        }
      } else {
        if (notInternalVariable(variable.getQualifiedName())) {
          globalVariables.add(variable.asCExpression());
        }
      }
    }

    ImmutableList.Builder<PrecisionExchangeEntry> entries = ImmutableList.builder();
    if (!globalVariables.build().isEmpty()) {
      entries.add(
          new PrecisionExchangeEntry(
              YAMLWitnessExpressionType.C,
              new GlobalPrecisionScope(),
              PrecisionType.RELEVANT_MEMORY_LOCATIONS,
              globalVariables.build()));
    }

    for (Entry<String, Collection<String>> functionEntry :
        functionWideVariables.build().asMap().entrySet()) {
      entries.add(
          new PrecisionExchangeEntry(
              YAMLWitnessExpressionType.C,
              new FunctionPrecisionScope(functionEntry.getKey()),
              PrecisionType.RELEVANT_MEMORY_LOCATIONS,
              ImmutableList.copyOf(functionEntry.getValue())));
    }

    return entries.build();
  }

  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision pConsolidatedPrecision) {
    Preconditions.checkArgument(getClass().equals(pConsolidatedPrecision.getClass()));
    ScopedRefinablePrecision consolidatedPrecision =
        (ScopedRefinablePrecision) pConsolidatedPrecision;
    checkArgument(super.getBaseline().equals(consolidatedPrecision.getBaseline()));

    Iterable<MemoryLocation> joinedPrec =
        Iterables.concat(rawPrecision, consolidatedPrecision.rawPrecision);
    return new ScopedRefinablePrecision(super.getBaseline(), joinedPrec);
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
    return super.isTracking(pVariable, pType, pLocation) && rawPrecision.contains(pVariable);
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    return pOtherPrecision.getClass().equals(getClass())
        && super.getBaseline().equals(((ScopedRefinablePrecision) pOtherPrecision).getBaseline())
        && rawPrecision.equals(((ScopedRefinablePrecision) pOtherPrecision).rawPrecision);
  }

  @Override
  public boolean equals(Object pObj) {
    return super.equals(pObj)
        && pObj instanceof ScopedRefinablePrecision other
        && rawPrecision.equals(other.rawPrecision);
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + rawPrecision.hashCode();
  }
}
