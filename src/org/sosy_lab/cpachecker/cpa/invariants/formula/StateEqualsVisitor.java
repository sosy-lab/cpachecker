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

import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;


public class StateEqualsVisitor extends DefaultParameterizedFormulaVisitor<CompoundState, InvariantsFormula<CompoundState>, Boolean> {

  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  private final Map<? extends String, ? extends InvariantsFormula<CompoundState>> environment;

  public StateEqualsVisitor(FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    this.evaluationVisitor = pEvaluationVisitor;
    this.environment = pEnvironment;
  }

  @Override
  protected Boolean visitDefault(InvariantsFormula<CompoundState> pFormula, InvariantsFormula<CompoundState> pOther) {
    CompoundState leftValue = pFormula.accept(evaluationVisitor, environment);
    CompoundState rightValue = pOther.accept(evaluationVisitor, environment);
    return leftValue.logicalEquals(rightValue).isDefinitelyTrue();
  }

  private Boolean visitCommutative(InvariantsFormula<CompoundState> pS1,
      InvariantsFormula<CompoundState> pS2,
      InvariantsFormula<CompoundState> pOtherS1,
      InvariantsFormula<CompoundState> pOtherS2) {
    return pS1.accept(this, pOtherS1)
        && pS2.accept(this, pOtherS2)
        || pS1.accept(this, pOtherS2)
        && pS2.accept(this, pOtherS1);
  }

  private Boolean visitNonCommutative(InvariantsFormula<CompoundState> pS1,
      InvariantsFormula<CompoundState> pS2,
      InvariantsFormula<CompoundState> pOtherS1,
      InvariantsFormula<CompoundState> pOtherS2) {
    return pS1.accept(this, pOtherS1)
        && pS2.accept(this, pOtherS2);
  }

  @Override
  public Boolean visit(Add<CompoundState> pAdd, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Add<?>) {
      Add<CompoundState> other = (Add<CompoundState>) pOther;
      return visitCommutative(pAdd.getSummand1(), pAdd.getSummand2(),
          other.getSummand1(), other.getSummand2());
    }
    return visitDefault(pAdd, pOther);
  }

  @Override
  public Boolean visit(BinaryAnd<CompoundState> pAnd, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof BinaryAnd<?>) {
      BinaryAnd<CompoundState> other = (BinaryAnd<CompoundState>) pOther;
      return visitCommutative(pAnd.getOperand1(), pAnd.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pAnd, pOther);
  }

  @Override
  public Boolean visit(BinaryNot<CompoundState> pNot, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof BinaryNot<?>) {
      BinaryNot<CompoundState> other = (BinaryNot<CompoundState>) pOther;
      return pNot.getFlipped().accept(this, other.getFlipped());
    }
    return visitDefault(pNot, pOther);
  }

  @Override
  public Boolean visit(BinaryOr<CompoundState> pOr, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof BinaryOr<?>) {
      BinaryOr<CompoundState> other = (BinaryOr<CompoundState>) pOther;
      return visitCommutative(pOr.getOperand1(), pOr.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pOr, pOther);
  }

  @Override
  public Boolean visit(BinaryXor<CompoundState> pXor, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof BinaryXor<?>) {
      BinaryXor<CompoundState> other = (BinaryXor<CompoundState>) pOther;
      return visitCommutative(pXor.getOperand1(), pXor.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pXor, pOther);
  }

  @Override
  public Boolean visit(Divide<CompoundState> pDivide, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Divide<?>) {
      Divide<CompoundState> other = (Divide<CompoundState>) pOther;
      return visitNonCommutative(pDivide.getNumerator(), pDivide.getDenominator(),
          other.getNumerator(), other.getDenominator());
    }
    return visitDefault(pDivide, pOther);
  }

  @Override
  public Boolean visit(Equal<CompoundState> pEqual, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Equal<?>) {
      Equal<CompoundState> other = (Equal<CompoundState>) pOther;
      return visitCommutative(pEqual.getOperand1(), pEqual.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pEqual, pOther);
  }

  @Override
  public Boolean visit(LessThan<CompoundState> pLessThan, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof LessThan<?>) {
      LessThan<CompoundState> other = (LessThan<CompoundState>) pOther;
      return visitNonCommutative(pLessThan.getOperand1(), pLessThan.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pLessThan, pOther);
  }

  @Override
  public Boolean visit(LogicalAnd<CompoundState> pAnd, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof LogicalAnd<?>) {
      LogicalAnd<CompoundState> other = (LogicalAnd<CompoundState>) pOther;
      return visitCommutative(pAnd.getOperand1(), pAnd.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pAnd, pOther);
  }

  @Override
  public Boolean visit(LogicalNot<CompoundState> pNot, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof LogicalNot<?>) {
      LogicalNot<CompoundState> other = (LogicalNot<CompoundState>) pOther;
      return pNot.getNegated().accept(this, other.getNegated());
    }
    return visitDefault(pNot, pOther);
  }

  @Override
  public Boolean visit(Modulo<CompoundState> pModulo, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Modulo<?>) {
      Modulo<CompoundState> other = (Modulo<CompoundState>) pOther;
      return visitNonCommutative(pModulo.getNumerator(), pModulo.getDenominator(),
          other.getNumerator(), other.getDenominator());
    }
    return visitDefault(pModulo, pOther);
  }

  @Override
  public Boolean visit(Multiply<CompoundState> pMultiply, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Multiply<?>) {
      Multiply<CompoundState> other = (Multiply<CompoundState>) pOther;
      return visitCommutative(pMultiply.getFactor1(), pMultiply.getFactor2(),
          other.getFactor1(), other.getFactor2());
    }
    return visitDefault(pMultiply, pOther);
  }

  @Override
  public Boolean visit(Negate<CompoundState> pNegate, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Negate<?>) {
      Negate<CompoundState> other = (Negate<CompoundState>) pOther;
      return pNegate.getNegated().accept(this, other.getNegated());
    }
    return visitDefault(pNegate, pOther);
  }

  @Override
  public Boolean visit(ShiftLeft<CompoundState> pShiftLeft, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof ShiftLeft<?>) {
      ShiftLeft<CompoundState> other = (ShiftLeft<CompoundState>) pOther;
      return visitNonCommutative(pShiftLeft.getShifted(), pShiftLeft.getShiftDistance(),
          other.getShifted(), other.getShiftDistance());
    }
    return visitDefault(pShiftLeft, pOther);
  }

  @Override
  public Boolean visit(ShiftRight<CompoundState> pShiftRight, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof ShiftRight<?>) {
      ShiftRight<CompoundState> other = (ShiftRight<CompoundState>) pOther;
      return visitNonCommutative(pShiftRight.getShifted(), pShiftRight.getShiftDistance(),
          other.getShifted(), other.getShiftDistance());
    }
    return visitDefault(pShiftRight, pOther);
  }

  @Override
  public Boolean visit(Union<CompoundState> pUnion, InvariantsFormula<CompoundState> pOther) {
    if (pOther instanceof Union<?>) {
      Union<CompoundState> other = (Union<CompoundState>) pOther;
      return visitCommutative(pUnion.getOperand1(), pUnion.getOperand2(),
          other.getOperand1(), other.getOperand2());
    }
    return visitDefault(pUnion, pOther);
  }

  @Override
  public Boolean visit(Variable<CompoundState> pVariable, InvariantsFormula<CompoundState> pOther) {
    String leftVarName = pVariable.getName();
    InvariantsFormula<CompoundState> resolvedLeft = this.environment.get(leftVarName);
    if (pOther instanceof Variable<?>) {
      String rightVarName = ((Variable<?>) pOther).getName();
      if (leftVarName.equals(rightVarName)) {
        return true;
      }
      InvariantsFormula<CompoundState> resolvedRight = this.environment.get(rightVarName);
      if (resolvedLeft.accept(this, resolvedRight)) {
        return true;
      }
      if (resolvedRight.accept(this, pVariable)) {
        return true;
      }
    }
    if (resolvedLeft.accept(this, pOther)) {
      return true;
    }
    return visitDefault(pVariable, pOther);
  }

}
