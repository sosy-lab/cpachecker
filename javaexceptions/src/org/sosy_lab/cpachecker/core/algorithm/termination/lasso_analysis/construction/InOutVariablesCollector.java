// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

class InOutVariablesCollector extends DefaultFormulaVisitor<TraversalProcess> {

  private final FormulaManagerView formulaManagerView;

  private final Set<Formula> inVariables = new LinkedHashSet<>();
  private final Set<Formula> outVariables = new LinkedHashSet<>();
  private final Map<Pair<String, List<Formula>>, NavigableMap<Integer, Formula>> ufs;

  private final SSAMap outVariablesSsa;
  private final SSAMap inVariablesSsa;
  private final Set<String> relevantVariables;
  private final Map<Formula, Formula> substitution;

  public InOutVariablesCollector(
      FormulaManagerView pFormulaManagerView,
      SSAMap pInVariablesSsa,
      SSAMap pOutVariablesSsa,
      ImmutableSet<String> pRelevantVariables,
      ImmutableMap<Formula, Formula> pSubstitution) {
    formulaManagerView = pFormulaManagerView;
    outVariablesSsa = pOutVariablesSsa;
    inVariablesSsa = pInVariablesSsa;
    relevantVariables = pRelevantVariables;
    substitution = pSubstitution;
    ufs = new LinkedHashMap<>();
  }

  @Override
  protected TraversalProcess visitDefault(Formula pF) {
    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitFreeVariable(Formula pF, String pName) {
    if (inVariables.contains(pF) && outVariables.contains(pF)) {
      return TraversalProcess.CONTINUE; // nothing to do
    }

    if (substitution.containsValue(pF)) {
      Formula originalFormula =
          substitution.entrySet().stream()
              .filter(e -> e.getValue().equals(pF))
              .map(Entry::getKey)
              .findAny()
              .orElseThrow();

      formulaManagerView.visit(originalFormula, new SubstitutionVisitor());

    } else {
      if (!formulaManagerView.isIntermediate(pName, outVariablesSsa)) {
        outVariables.add(pF);
      }
      if (!formulaManagerView.isIntermediate(pName, inVariablesSsa)) {
        inVariables.add(pF);
      }
    }

    return TraversalProcess.CONTINUE;
  }

  private class SubstitutionVisitor extends DefaultFormulaVisitor<TraversalProcess> {

    @Override
    protected TraversalProcess visitDefault(Formula pF) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFreeVariable(Formula pF, String pName) {
      Formula variable = substitution.get(pF);
      if (variable != null) {
        if (!formulaManagerView.isIntermediate(pName, outVariablesSsa)) {
          outVariables.add(variable);
        }
        if (!formulaManagerView.isIntermediate(pName, inVariablesSsa)) {
          inVariables.add(variable);
        }
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFunction(
        Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
      // ignore meta variables and variables that are not in the current scope
      if (!pArgs.stream()
          .map(formulaManagerView::uninstantiate)
          .flatMap(f -> formulaManagerView.extractVariableNames(f).stream())
          .allMatch(relevantVariables::contains)) {
        return TraversalProcess.CONTINUE;
      }

      int argIndexes =
          pArgs.stream()
              .flatMap(f -> formulaManagerView.extractFunctionNames(f).stream())
              .map(FormulaManagerView::parseName)
              .map(Pair::getSecondNotNull)
              .filter(OptionalInt::isPresent)
              .mapToInt(OptionalInt::getAsInt)
              .sum();

      String name = pFunctionDeclaration.getName();
      Pair<String, OptionalInt> tokens = FormulaManagerView.parseName(name);
      List<Formula> uninstatiatedArgs =
          transformedImmutableListCopy(pArgs, formulaManagerView::uninstantiate);
      Pair<String, List<Formula>> key = Pair.of(tokens.getFirstNotNull(), uninstatiatedArgs);
      ufs.putIfAbsent(key, new TreeMap<>());
      Map<Integer, Formula> ufApplications = ufs.get(key);
      int functionIndex = tokens.getSecondNotNull().orElse(0);
      ufApplications.put(functionIndex + argIndexes, substitution.get(pF));

      return TraversalProcess.CONTINUE;
    }
  }

  public Set<Formula> getInVariables() {
    ImmutableSet.Builder<Formula> allInVariables = ImmutableSet.builder();
    allInVariables.addAll(inVariables);
    ufs.values().stream().map(m -> m.firstEntry().getValue()).forEach(allInVariables::add);
    return allInVariables.build();
  }

  public Set<Formula> getOutVariables() {
    ImmutableSet.Builder<Formula> allOutVariables = ImmutableSet.builder();
    allOutVariables.addAll(outVariables);
    ufs.values().stream().map(m -> m.lastEntry().getValue()).forEach(allOutVariables::add);
    return allOutVariables.build();
  }
}
