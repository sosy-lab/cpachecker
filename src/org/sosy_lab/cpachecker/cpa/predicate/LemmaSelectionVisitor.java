// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.AbstractionLemma;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaSelectionVisitor extends DefaultFormulaVisitor<ImmutableSet<BooleanFormula>> {

  private final ImmutableSet<AbstractionLemma> abstractionLemmas;
  private final FormulaManagerView fmgr;

  public LemmaSelectionVisitor(Collection<AbstractionLemma> pLemmas, FormulaManagerView pFmgr) {
    abstractionLemmas = ImmutableSet.copyOf(pLemmas);
    fmgr = pFmgr;
  }

  @Override
  public ImmutableSet<BooleanFormula> visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    ImmutableSet.Builder<BooleanFormula> result = ImmutableSet.builder();
    for (AbstractionLemma lemma : abstractionLemmas) {
      if (functionDeclaration.getName().equals(lemma.getIdentifier())) {
        result.addAll(lemma.getFormulas());
        // Map the variables to each other here and just append them?
        // eg A = a
        LemmaVariableVisitor variableVisitor =
            new LemmaVariableVisitor(functionDeclaration.getName(), args, fmgr);
        for (BooleanFormula formula : lemma.getFormulas()) {
          result.addAll(fmgr.visit(formula, variableVisitor));
        }
      }
      for (Formula arg : args) {
        result.addAll(fmgr.visit(arg, this));
      }
    }
    return result.build();
  }

  @Override
  protected ImmutableSet<BooleanFormula> visitDefault(Formula pFormula) {
    return ImmutableSet.of();
  }
}
