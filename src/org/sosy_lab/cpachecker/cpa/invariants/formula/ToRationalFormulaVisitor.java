/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;


public class ToRationalFormulaVisitor implements InvariantsFormulaVisitor<CompoundState, RationalFormula> {

  private final BooleanFormulaManager bfmgr;

  private final RationalFormulaManager rfmgr;

  private final RationalFormula zero;

  private final RationalFormula one;

  private final ToBooleanFormulaVisitor toBooleanFormulaVisitor;

  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  public ToRationalFormulaVisitor(FormulaManager pFmgr,
      ToBooleanFormulaVisitor pToBooleanFormulaVisitor,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.rfmgr = pFmgr.getRationalFormulaManager();
    this.zero = this.rfmgr.makeNumber(0);
    this.one = this.rfmgr.makeNumber(1);
    this.toBooleanFormulaVisitor = pToBooleanFormulaVisitor;
    this.evaluationVisitor = pEvaluationVisitor;
  }

  private RationalFormula evaluate(InvariantsFormula<CompoundState> pFormula) {
    CompoundState value = pFormula.accept(this.evaluationVisitor);
    if (value.isSingleton()) {
      return this.rfmgr.makeNumber(value.getLowerBound().longValue());
    }
    return null;
  }

  private RationalFormula fromBooleanFormula(InvariantsFormula<CompoundState> pBooleanFormula) {
    return fromBooleanFormula(pBooleanFormula.accept(this.toBooleanFormulaVisitor));
  }

  private RationalFormula fromBooleanFormula(BooleanFormula pBooleanFormula) {
    if (pBooleanFormula == null) {
      return null;
    }
    return this.bfmgr.ifThenElse(pBooleanFormula, this.one, this.zero);
  }

  @Override
  public RationalFormula visit(Add<CompoundState> pAdd) {
    RationalFormula summand1 = pAdd.getSummand1().accept(this);
    RationalFormula summand2 = pAdd.getSummand2().accept(this);
    if (summand1 == null || summand2 == null) {
      return evaluate(pAdd);
    }
    return this.rfmgr.add(summand1, summand2);
  }

  @Override
  public RationalFormula visit(BinaryAnd<CompoundState> pAnd) {
    return evaluate(pAnd);
  }

  @Override
  public RationalFormula visit(BinaryNot<CompoundState> pNot) {
    return evaluate(pNot);
  }

  @Override
  public RationalFormula visit(BinaryOr<CompoundState> pOr) {
    return evaluate(pOr);
  }

  @Override
  public RationalFormula visit(BinaryXor<CompoundState> pXor) {
    return evaluate(pXor);
  }

  @Override
  public RationalFormula visit(Constant<CompoundState> pConstant) {
    return evaluate(pConstant);
  }

  @Override
  public RationalFormula visit(Divide<CompoundState> pDivide) {
    RationalFormula numerator = pDivide.getNumerator().accept(this);
    RationalFormula denominator = pDivide.getDenominator().accept(this);
    if (numerator == null || denominator == null) {
      return evaluate(pDivide);
    }
    return this.rfmgr.divide(numerator, denominator);
  }

  @Override
  public RationalFormula visit(Equal<CompoundState> pEqual) {
    return fromBooleanFormula(pEqual);
  }

  @Override
  public RationalFormula visit(LessThan<CompoundState> pLessThan) {
    return fromBooleanFormula(pLessThan);
  }

  @Override
  public RationalFormula visit(LogicalAnd<CompoundState> pAnd) {
    return fromBooleanFormula(pAnd);
  }

  @Override
  public RationalFormula visit(LogicalNot<CompoundState> pNot) {
    return fromBooleanFormula(pNot);
  }

  @Override
  public RationalFormula visit(Modulo<CompoundState> pModulo) {
    RationalFormula numerator = pModulo.getNumerator().accept(this);
    RationalFormula denominator = pModulo.getDenominator().accept(this);
    if (numerator == null || denominator == null) {
      return evaluate(pModulo);
    }
    return this.rfmgr.modulo(numerator, denominator);
  }

  @Override
  public RationalFormula visit(Multiply<CompoundState> pMultiply) {
    RationalFormula factor1 = pMultiply.getFactor1().accept(this);
    RationalFormula factor2 = pMultiply.getFactor2().accept(this);
    if (factor1 == null || factor2 == null) {
      return evaluate(pMultiply);
    }
    return this.rfmgr.modulo(factor1, factor2);
  }

  @Override
  public RationalFormula visit(Negate<CompoundState> pNegate) {
    RationalFormula negated = pNegate.getNegated().accept(this);
    if (negated == null) {
      return evaluate(pNegate);
    }
    return this.rfmgr.negate(negated);
  }

  @Override
  public RationalFormula visit(ShiftLeft<CompoundState> pShiftLeft) {
    return evaluate(pShiftLeft);
  }

  @Override
  public RationalFormula visit(ShiftRight<CompoundState> pShiftRight) {
    return evaluate(pShiftRight);
  }

  @Override
  public RationalFormula visit(Union<CompoundState> pUnion) {
    return evaluate(pUnion);
  }

  @Override
  public RationalFormula visit(Variable<CompoundState> pVariable) {
    return rfmgr.makeVariable(pVariable.getName());
  }

}
