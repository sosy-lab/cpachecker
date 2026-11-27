// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.PrecisionScopeVariables;
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
        .orderKeysBy(Comparator.naturalOrder())
        .orderValuesBy(Comparator.naturalOrder());
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
    AstCfaRelation astCfaRelation = pCfa.getAstCfaRelation();
    if (pCfa.getLanguage() != Language.C || astCfaRelation == null) {
      // Without the relation to the AST we cannot describe the scope of any location
      return ImmutableList.of();
    }
    PrecisionScopeVariables variables = new PrecisionScopeVariables(pCfa);

    // Several CFA nodes may describe the same scope, for example all nodes of one statement, so
    // collect the memory locations per scope to export every scope exactly once.
    SequencedMap<PrecisionScope, Set<String>> variablesPerScope = new LinkedHashMap<>();

    for (CFANode currentLocation : rawPrecision.keySet()) {
      String functionName = currentLocation.getFunctionName();
      Optional<PrecisionScope> localScope =
          PrecisionScope.localPrecisionScopeFor(currentLocation, astCfaRelation);
      // Nodes without a scope of their own are overapproximated by the scope of their function
      PrecisionScope precisionScope =
          localScope.orElseGet(() -> new FunctionPrecisionScope(functionName));
      Map<String, AbstractSimpleDeclaration> namedVariables =
          localScope.isPresent()
              ? variables.variablesAt(currentLocation).orElse(ImmutableMap.of())
              : variables.variablesIn(functionName);

      Set<String> expressions =
          asCExpressions(rawPrecision.get(currentLocation), namedVariables, pCfa.getMachineModel());
      if (!expressions.isEmpty()) {
        variablesPerScope
            .computeIfAbsent(precisionScope, _ -> new LinkedHashSet<>())
            .addAll(expressions);
      }
    }

    return transformedImmutableListCopy(
        variablesPerScope.entrySet(),
        entry ->
            new PrecisionExchangeEntry(
                YAMLWitnessExpressionType.C,
                entry.getKey(),
                PrecisionType.MEMORY_LOCATIONS,
                ImmutableList.copyOf(entry.getValue())));
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
