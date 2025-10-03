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
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter.variableInOriginalProgram;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter.variableNameInFunction;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionType;

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
  public List<PrecisionExchangeEntry> asWitnessEntries(CFA pCfa) {
    ImmutableList.Builder<PrecisionExchangeEntry> entriesBuilder = ImmutableList.builder();
    AstCfaRelation astCfaRelation = pCfa.getAstCfaRelation();

    FluentIterable<@NonNull MemoryLocation> relevantVariables =
        FluentIterable.from(rawPrecision.values())
            .filter(memoryLocation -> notInternalVariable(memoryLocation.getQualifiedName()));

    for (CFANode currentLocation : rawPrecision.keySet()) {
      String functionName = currentLocation.getFunctionName();
      Optional<PrecisionScope> precisionScope =
          PrecisionScope.localPrecisionScopeFor(currentLocation, astCfaRelation);
      relevantVariables =
          relevantVariables.filter(
              memoryLocation ->
                  variableNameInFunction(memoryLocation.getQualifiedName(), functionName));
      if (precisionScope.isEmpty()) {
        // We overapproximate by making this function wide
        precisionScope = Optional.of(new FunctionPrecisionScope(functionName));
      } else {
        relevantVariables =
            relevantVariables.filter(
                memoryLocation ->
                    variableInOriginalProgram(
                        memoryLocation.getQualifiedName(), astCfaRelation, currentLocation));
      }

      entriesBuilder.add(
          new PrecisionExchangeEntry(
              YAMLWitnessExpressionType.C,
              precisionScope.orElseThrow(),
              PrecisionType.RELEVANT_MEMORY_LOCATIONS,
              relevantVariables.transform(MemoryLocation::asCExpression).toList()));
    }

    return entriesBuilder.build();
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
    return pOtherPrecision.getClass().equals(getClass())
        && super.getBaseline().equals(((LocalizedRefinablePrecision) pOtherPrecision).getBaseline())
        && rawPrecision.equals(((LocalizedRefinablePrecision) pOtherPrecision).rawPrecision);
  }

  @Override
  public boolean equals(Object pObj) {
    return super.equals(pObj)
        && pObj instanceof LocalizedRefinablePrecision other
        && rawPrecision.equals(other.rawPrecision);
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + rawPrecision.hashCode();
  }
}
