// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.List;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaExtractorVisitor
    extends DefaultFormulaVisitor<Pair<BitvectorFormula, BitvectorFormula>> {

  protected LemmaExtractorVisitor() {}

  @Override
  protected Pair<BitvectorFormula, BitvectorFormula> visitDefault(Formula pFormula) {
    return null;
  }

  @Override
  public Pair<BitvectorFormula, BitvectorFormula> visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    assert args.size() == 2
        && args.get(0) instanceof BitvectorFormula
        && args.get(1) instanceof BitvectorFormula;
    BitvectorFormula signature = (BitvectorFormula) args.get(1);
    BitvectorFormula body = (BitvectorFormula) args.get(0);
    return Pair.of(signature, body);
  }

  @Override
  public Pair<BitvectorFormula, BitvectorFormula> visitFreeVariable(Formula f, String name) {
    return null;
  }
}
