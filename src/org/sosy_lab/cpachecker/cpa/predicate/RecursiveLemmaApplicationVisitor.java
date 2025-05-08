// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.Lists;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.AbstractionLemma;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

/**
 * A visitor that replaces calls for a lemma function in {@link Formula} with definition of the
 * Lemma.
 */
public class RecursiveLemmaApplicationVisitor extends DefaultFormulaVisitor<Formula> {
  private final LemmaPrecision lemmaPrecision;
  private final FormulaManager fmgr;

  public RecursiveLemmaApplicationVisitor(LemmaPrecision pLemmaMap, FormulaManager pFmgr) {
    lemmaPrecision = pLemmaMap;
    fmgr = pFmgr;
  }

  @Override
  public Formula visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    if (lemmaPrecision.getLemmas().containsKey(functionDeclaration.getName())) {
      AbstractionLemma lemma = lemmaPrecision.getLemmas().get(functionDeclaration.getName());
      BitvectorFormula signature = lemma.getSignature();
      LemmaVariableReplacementVisitor variableReplacer =
          new LemmaVariableReplacementVisitor(lemmaPrecision, functionDeclaration, args, fmgr);
      return fmgr.visit(signature, variableReplacer);
    }
    List<Formula> newArgs = Lists.transform(args, arg -> fmgr.visit(arg, this));
    return fmgr.makeApplication(functionDeclaration, newArgs);
  }

  @Override
  protected Formula visitDefault(Formula pFormula) {
    return pFormula;
  }
}
