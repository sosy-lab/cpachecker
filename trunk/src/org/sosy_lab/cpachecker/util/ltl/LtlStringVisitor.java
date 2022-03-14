// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl;

import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.util.ltl.formulas.PropositionalFormula;
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

  private String visitPropositionalFormula(PropositionalFormula pProp) {
    return pProp.getChildren().stream()
        .map(this::visitFormula)
        .collect(Collectors.joining(String.format(" %s ", pProp.getSymbol()), "(", ")"));
  }

  @Override
  public String visit(Conjunction pConjunction) {
    return visitPropositionalFormula(pConjunction);
  }

  @Override
  public String visit(Disjunction pDisjunction) {
    return visitPropositionalFormula(pDisjunction);
  }

  @Override
  public String visit(Finally pFinally) {
    return visitUnaryFormula(pFinally);
  }

  @Override
  public String visit(Globally pGlobally) {
    return visitUnaryFormula(pGlobally);
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
    return visitUnaryFormula(pNext);
  }

  @Override
  public String visit(Release pRelease) {
    return visitBinaryFormula(pRelease);
  }

  @Override
  public String visit(StrongRelease pStrongRelease) {
    return visitBinaryFormula(pStrongRelease);
  }

  @Override
  public String visit(Until pUntil) {
    return visitBinaryFormula(pUntil);
  }

  @Override
  public String visit(WeakUntil pWeakUntil) {
    return visitBinaryFormula(pWeakUntil);
  }

  private String visitUnaryFormula(UnaryFormula pFormula) {
    return pFormula.getSymbol() + " " + pFormula.getOperand().accept(this);
  }

  private String visitBinaryFormula(BinaryFormula pFormula) {
    return String.format(
        "((%s) %s (%s))",
        pFormula.getLeft().accept(this), pFormula.getSymbol(), pFormula.getRight().accept(this));
  }

  private String visitFormula(LtlFormula pFormula) {
    return pFormula.accept(this);
  }
}
