/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util.ltl;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.ltl.formulas.BinaryFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.BooleanConstant;
import org.sosy_lab.cpachecker.util.ltl.formulas.Conjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Disjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Finally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Globally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Next;
import org.sosy_lab.cpachecker.util.ltl.formulas.Release;
import org.sosy_lab.cpachecker.util.ltl.formulas.StrongRelease;
import org.sosy_lab.cpachecker.util.ltl.formulas.UnaryFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Until;
import org.sosy_lab.cpachecker.util.ltl.formulas.WeakUntil;

public class LtlStringVisitor implements LtlFormulaVisitor {

  private final ImmutableList<Literal> literals;

  private LtlStringVisitor(ImmutableList<Literal> pLiterals) {
    literals = pLiterals;
  }

  public static String toString(LtlFormula pFormula, ImmutableList<Literal> pLiterals) {
    LtlStringVisitor visitor = new LtlStringVisitor(pLiterals);
    return pFormula.accept(visitor);
  }

  @Override
  public String visit(BooleanConstant pBooleanConstant) {
    return pBooleanConstant.toString();
  }

  @Override
  public String visit(Conjunction pConjunction) {
    return "("
        + pConjunction
            .children
            .stream()
            .map((Function<LtlFormula, CharSequence>) this::visitFormula)
            .collect(Collectors.joining(String.format(" %s ", pConjunction.getSymbol())))
        + ")";
  }

  @Override
  public String visit(Disjunction pDisjunction) {
    return "("
        + pDisjunction
            .children
            .stream()
            .map((Function<LtlFormula, CharSequence>) this::visitFormula)
            .collect(Collectors.joining(String.format(" %s ", pDisjunction.getSymbol())))
        + ")";
  }

  @Override
  public String visit(Finally pFinally) {
    return visit((UnaryFormula) pFinally);
  }

  @Override
  public String visit(Globally pGlobally) {
    return visit((UnaryFormula) pGlobally);
  }

  @Override
  public String visit(Literal pLiteral) {
    String atom = pLiteral.getAtom();
    int index = literals.indexOf(Literal.of(atom, false));
    if (index == -1) {
      return pLiteral.isNegated() ? "! " + atom : atom;
    } else {
      String alias = "val" + index;
      return pLiteral.isNegated() ? "! " + alias : alias;
    }
  }

  @Override
  public String visit(Next pNext) {
    return visit((UnaryFormula) pNext);
  }

  @Override
  public String visit(Release pRelease) {
    return visit((BinaryFormula) pRelease);
  }

  @Override
  public String visit(StrongRelease pStrongRelease) {
    return visit((BinaryFormula) pStrongRelease);
  }

  @Override
  public String visit(Until pUntil) {
    return visit((BinaryFormula) pUntil);
  }

  @Override
  public String visit(WeakUntil pWeakUntil) {
    return visit((BinaryFormula) pWeakUntil);
  }

  private String visit(UnaryFormula pFormula) {
    return pFormula.getSymbol() + " " + pFormula.operand.accept(this);
  }

  private String visit(BinaryFormula pFormula) {
    return "(("
        + pFormula.left.accept(this)
        + ") "
        + pFormula.getSymbol()
        + " ("
        + pFormula.right.accept(this)
        + "))";
  }

  private String visitFormula(LtlFormula pFormula) {
    return pFormula.accept(this);
  }
}
