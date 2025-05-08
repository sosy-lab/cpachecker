// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.AbstractionLemma;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaVariableReplacementVisitor extends DefaultFormulaVisitor<Formula> {
  private final LemmaPrecision lemmaPrecision;
  private final FunctionDeclaration<?> declaration;
  private final List<Formula> programVariabls;
  private final FormulaManager fmgr;

  public LemmaVariableReplacementVisitor(
      LemmaPrecision pPrecision,
      FunctionDeclaration<?> pDeclaration,
      List<Formula> pArgs,
      FormulaManager pFmgr) {
    lemmaPrecision = pPrecision;
    declaration = pDeclaration;
    programVariabls = pArgs;
    fmgr = pFmgr;
  }

  @Override
  public Formula visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {

    if (declaration.equals(functionDeclaration)) {
      AbstractionLemma lemma = lemmaPrecision.getLemmas().get(functionDeclaration.getName());

      assert args.size() == programVariabls.size();
      ImmutableMap.Builder<Formula, Formula> variableMap = ImmutableMap.builder();
      for (int i = 0; i < args.size(); i++) {
        variableMap.put(args.get(i), programVariabls.get(i));
      }

      LemmaInitializationVisitor initializationVisitor =
          new LemmaInitializationVisitor(variableMap.buildOrThrow(), fmgr);
      return fmgr.visit(lemma.getBody(), initializationVisitor);
    }
    return f;
  }

  @Override
  protected Formula visitDefault(Formula pFormula) {
    return pFormula;
  }
}
