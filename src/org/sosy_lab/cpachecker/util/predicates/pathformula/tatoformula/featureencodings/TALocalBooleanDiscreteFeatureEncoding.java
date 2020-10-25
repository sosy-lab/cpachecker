// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TALocalBooleanDiscreteFeatureEncoding<T> implements TADiscreteFeatureEncoding<T> {
  private final Map<TaDeclaration, TAGlobalBooleanDiscreteFeatureEncoding<T>> encodings;

  public TALocalBooleanDiscreteFeatureEncoding(
      FormulaManagerView pFmgr, String pVariableName, Map<TaDeclaration, Collection<T>> pDomain) {
    encodings = new HashMap<>();

    for (var automaton : pDomain.keySet()) {
      var encoding =
          new TAGlobalBooleanDiscreteFeatureEncoding<>(
              pFmgr, pVariableName + "#" + automaton.getName(), pDomain.get(automaton));
      encodings.put(automaton, encoding);
    }
  }

  @Override
  public BooleanFormula makeEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, T pFeature) {
    return encodings.get(pAutomaton).makeEqualsFormula(pVariableIndex, pFeature);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    return encodings.get(pAutomaton).makeUnchangedFormula(pAutomaton, pIndexBefore);
  }
}
