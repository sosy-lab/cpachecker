// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaVariableVisitor extends DefaultFormulaVisitor<ImmutableSet<BooleanFormula>> {

  String predName;
  List<Formula> predArgs;
  FormulaManagerView fmgr;

  public LemmaVariableVisitor(String pName, List<Formula> pArgs, FormulaManagerView pFmgr) {
    predName = pName;
    predArgs = pArgs;
    fmgr = pFmgr;
  }

  public ImmutableSet<BooleanFormula> visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    ImmutableSet.Builder<BooleanFormula> result = ImmutableSet.builder();
    if (functionDeclaration.getName().equals(predName)) {
      assert predArgs.size() == args.size();
      for (int i = 0; i < args.size(); i++) {
        LemmaVariableEqualityVisitor equalityVisitor =
            new LemmaVariableEqualityVisitor(predArgs.get(i), fmgr);
        result.add(fmgr.visit(args.get(i), equalityVisitor));
      }
    }

    for (Formula arg : args) {
      result.addAll(fmgr.visit(arg, this));
    }
    return result.build();
  }

  public ImmutableSet<BooleanFormula> visitFreeVariable(Formula f, String name) {
    return ImmutableSet.of();
  }

  @Override
  protected ImmutableSet<BooleanFormula> visitDefault(Formula pFormula) {
    return ImmutableSet.of();
  }
}
