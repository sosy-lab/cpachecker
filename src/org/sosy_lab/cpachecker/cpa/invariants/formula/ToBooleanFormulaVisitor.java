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


public class ToBooleanFormulaVisitor implements InvariantsFormulaVisitor<CompoundState, BooleanFormula> {

  private final BooleanFormulaManager bfmgr;

  private final RationalFormulaManager rfmgr;

  private final RationalFormula zero;

  private final ToRationalFormulaVisitor toRationalFormulaVisitor;

  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  public ToBooleanFormulaVisitor(FormulaManager pFmgr,
      FormulaEvaluationVisitor<CompoundState> evaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.rfmgr = pFmgr.getRationalFormulaManager();
    this.zero = this.rfmgr.makeNumber(0);
    this.evaluationVisitor = evaluationVisitor;
    this.toRationalFormulaVisitor = new ToRationalFormulaVisitor(pFmgr, this, evaluationVisitor);
  }

  public ToRationalFormulaVisitor getToRationalFormulaVisitor() {
    return this.toRationalFormulaVisitor;
  }

  private BooleanFormula fromRationalFormula(InvariantsFormula<CompoundState> pRationalFormula) {
    RationalFormula rationalFormula = pRationalFormula.accept(this.toRationalFormulaVisitor);
    if (rationalFormula == null) {
      return evaluateAsBoolean(pRationalFormula);
    }
    return fromRationalFormula(rationalFormula);
  }

  private BooleanFormula fromRationalFormula(RationalFormula pRationalFormula) {
    if (pRationalFormula == null) {
      return null;
    }
    return this.bfmgr.not(this.rfmgr.equal(pRationalFormula, this.zero));
  }

  private BooleanFormula evaluateAsBoolean(InvariantsFormula<CompoundState> pRationalFormula) {
    CompoundState value = pRationalFormula.accept(this.evaluationVisitor);
    if (value.isDefinitelyFalse()) {
      return this.bfmgr.makeBoolean(false);
    }
    if (value.isDefinitelyTrue()) {
      return this.bfmgr.makeBoolean(true);
    }
    return null;
  }

  @Override
  public BooleanFormula visit(Add<CompoundState> pAdd) {
    return fromRationalFormula(pAdd);
  }

  @Override
  public BooleanFormula visit(BinaryAnd<CompoundState> pAnd) {
    return fromRationalFormula(pAnd);
  }

  @Override
  public BooleanFormula visit(BinaryNot<CompoundState> pNot) {
    return fromRationalFormula(pNot);
  }

  @Override
  public BooleanFormula visit(BinaryOr<CompoundState> pOr) {
    return fromRationalFormula(pOr);
  }

  @Override
  public BooleanFormula visit(BinaryXor<CompoundState> pXor) {
    return fromRationalFormula(pXor);
  }

  @Override
  public BooleanFormula visit(Constant<CompoundState> pConstant) {
    return fromRationalFormula(pConstant);
  }

  @Override
  public BooleanFormula visit(Divide<CompoundState> pDivide) {
    return fromRationalFormula(pDivide);
  }

  @Override
  public BooleanFormula visit(Equal<CompoundState> pEqual) {
    RationalFormula operand1 = pEqual.getOperand1().accept(this.toRationalFormulaVisitor);
    RationalFormula operand2 = pEqual.getOperand1().accept(this.toRationalFormulaVisitor);
    if (operand1 == null || operand2 == null) {
      return evaluateAsBoolean(pEqual);
    }
    return this.rfmgr.equal(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LessThan<CompoundState> pLessThan) {
    RationalFormula operand1 = pLessThan.getOperand1().accept(this.toRationalFormulaVisitor);
    RationalFormula operand2 = pLessThan.getOperand1().accept(this.toRationalFormulaVisitor);
    if (operand1 == null || operand2 == null) {
      return evaluateAsBoolean(pLessThan);
    }
    return this.rfmgr.lessThan(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LogicalAnd<CompoundState> pAnd) {
    BooleanFormula operand1 = pAnd.getOperand1().accept(this);
    BooleanFormula operand2 = pAnd.getOperand1().accept(this);
    if (operand1 == null || operand2 == null) {
      return evaluateAsBoolean(pAnd);
    }
    return this.bfmgr.and(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LogicalNot<CompoundState> pNot) {
    BooleanFormula operand = pNot.getNegated().accept(this);
    if (operand == null) {
      return evaluateAsBoolean(pNot);
    }
    return this.bfmgr.not(operand);
  }

  @Override
  public BooleanFormula visit(Modulo<CompoundState> pModulo) {
    return fromRationalFormula(pModulo);
  }

  @Override
  public BooleanFormula visit(Multiply<CompoundState> pMultiply) {
    return fromRationalFormula(pMultiply);
  }

  @Override
  public BooleanFormula visit(Negate<CompoundState> pNegate) {
    return fromRationalFormula(pNegate);
  }

  @Override
  public BooleanFormula visit(ShiftLeft<CompoundState> pShiftLeft) {
    return fromRationalFormula(pShiftLeft);
  }

  @Override
  public BooleanFormula visit(ShiftRight<CompoundState> pShiftRight) {
    return fromRationalFormula(pShiftRight);
  }

  @Override
  public BooleanFormula visit(Union<CompoundState> pUnion) {
    return fromRationalFormula(pUnion);
  }

  @Override
  public BooleanFormula visit(Variable<CompoundState> pVariable) {
    return this.bfmgr.makeVariable(pVariable.getName());
  }

}
