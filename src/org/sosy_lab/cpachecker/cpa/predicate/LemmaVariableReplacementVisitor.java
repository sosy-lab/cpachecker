// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.util.predicates.AbstractionLemma;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaVariableReplacementVisitor extends DefaultFormulaVisitor<Formula> {
  private final LemmaPrecision lemmaPrecision;
  private final FunctionDeclaration<?> declaration;
  private final List<Formula> arguments;
  private final FormulaManager fmgr;

  public LemmaVariableReplacementVisitor(
      LemmaPrecision pPrecision,
      FunctionDeclaration<?> pDeclaration,
      List<Formula> pArgs,
      FormulaManager pFmgr) {
    lemmaPrecision = pPrecision;
    declaration = pDeclaration;
    arguments = pArgs;
    fmgr = pFmgr;
  }

  @Override
  public Formula visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    if (declaration.equals(functionDeclaration)) {
      // Just create a map of both lists??
      AbstractionLemma lemma = lemmaPrecision.getLemmas().get(functionDeclaration.getName());
      assert args.size() == arguments.size();
      Map<Formula, Formula> variableMap =
          IntStream.range(0, arguments.size())
              .boxed()
              .collect(Collectors.toMap(args::get, arguments::get));

      LemmaInitializationVisitor initializationVisitor =
          new LemmaInitializationVisitor(variableMap, fmgr);
      return fmgr.visit(lemma.getBody(), initializationVisitor);
      /*
      List<Formula> newArgs = Lists.transform(args, arg -> fmgr.visit(arg, this));
      return fmgr.makeApplication(functionDeclaration, newArgs);
      */
    }
    return f;
  }

  @Override
  public Formula visitFreeVariable(Formula f, String name) {
    return this.visitDefault(f);
  }

  @Override
  protected Formula visitDefault(Formula pFormula) {
    return pFormula;
  }
}
