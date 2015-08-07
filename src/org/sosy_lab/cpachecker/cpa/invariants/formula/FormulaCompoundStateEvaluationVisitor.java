/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.util.Objects;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorType;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManager;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundIntervalManagerFactory;

/**
 * Instances of this class are visitors for compound state invariants formulae
 * which are used to evaluate the visited formulae to compound states. This
 * visitor uses a stronger evaluation strategy than a
 * {@link FormulaAbstractionVisitor} in order to enable the CPA strategy to
 * obtain very exact values for the expressions in the analyzed code.
 */
public class FormulaCompoundStateEvaluationVisitor implements FormulaEvaluationVisitor<CompoundInterval> {

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory;

  public FormulaCompoundStateEvaluationVisitor(CompoundIntervalManagerFactory pCompoundIntervalManagerFactory) {
    this.compoundIntervalManagerFactory = pCompoundIntervalManagerFactory;
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorInfo pBitVectorInfo) {
    return compoundIntervalManagerFactory.createCompoundIntervalManager(pBitVectorInfo);
  }

  private CompoundIntervalManager getCompoundIntervalManager(BitVectorType pBitvectorType) {
    return getCompoundIntervalManager(pBitvectorType.getBitVectorInfo());
  }

  @Override
  public CompoundInterval visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pAdd).add(pAdd.getSummand1().accept(this, pEnvironment), pAdd.getSummand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryAnd<CompoundInterval> pAnd, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pAnd).binaryAnd(pAnd.getOperand1().accept(this, pEnvironment), pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryNot<CompoundInterval> pNot, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pNot).binaryNot(pNot.getFlipped().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryOr<CompoundInterval> pOr, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pOr).binaryOr(pOr.getOperand1().accept(this, pEnvironment), pOr.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(BinaryXor<CompoundInterval> pXor, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pXor).binaryXor(pXor.getOperand1().accept(this, pEnvironment), pXor.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Constant<CompoundInterval> pConstant, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return pConstant.getValue();
  }

  @Override
  public CompoundInterval visit(Divide<CompoundInterval> pDivide, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pDivide).divide(pDivide.getNumerator().accept(this, pEnvironment), pDivide.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(Equal<CompoundInterval> pEqual, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval operand1 = pEqual.getOperand1().accept(this, pEnvironment);
    CompoundInterval operand2 = pEqual.getOperand2().accept(this, pEnvironment);

    CompoundInterval result = getCompoundIntervalManager(pEqual.getOperand1()).logicalEquals(operand1, operand2);
    if (result.isDefinitelyTrue()) {
      return BooleanConstant.getTrue();
    }
    if (result.isDefinitelyFalse()) {
      return BooleanConstant.getFalse();
    }
    if (result.isTop()) {
      if (pEqual.getOperand1() instanceof Variable) {
        Variable<CompoundInterval> var = (Variable<CompoundInterval>) pEqual.getOperand1();
        NumeralFormula<CompoundInterval> value = pEnvironment.get(var.getName());
        while (value != null) {
          if (value instanceof Exclusion) {
            Exclusion<CompoundInterval> exclusion = (Exclusion<CompoundInterval>) value;
            if (exclusion.getExcluded().equals(pEqual.getOperand2())) {
              return BooleanConstant.getFalse();
            }
          }
          if (value instanceof Variable) {
            if (value.equals(var)) {
              return BooleanConstant.getTrue();
            }
            var = (Variable<CompoundInterval>) value;
            value = pEnvironment.get(var.getName());
          } else {
            value = null;
          }
        }
      }
      if (pEqual.getOperand2() instanceof Variable) {
        Variable<CompoundInterval> var = (Variable<CompoundInterval>) pEqual.getOperand2();
        NumeralFormula<CompoundInterval> value = pEnvironment.get(var.getName());
        while (value != null) {
          if (value.equals(pEqual.getOperand1())) {
            return BooleanConstant.getTrue();
          }
          if (value instanceof Exclusion) {
            Exclusion<CompoundInterval> exclusion = (Exclusion<CompoundInterval>) value;
            if (exclusion.getExcluded().equals(pEqual.getOperand1())) {
              return BooleanConstant.getFalse();
            }
          }
          if (value instanceof Variable) {
            var = (Variable<CompoundInterval>) value;
            value = pEnvironment.get(var.getName());
          } else {
            value = null;
          }
        }
      }
    }
    return null;
  }

  @Override
  public CompoundInterval visit(Exclusion<CompoundInterval> pExclusion,
      Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval excluded = pExclusion.getExcluded().accept(this, pEnvironment);
    if (excluded.isSingleton()) {
      return excluded.invert();
    }
    return getCompoundIntervalManager(pExclusion).allPossibleValues();
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(LessThan<CompoundInterval> pLessThan, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval value = getCompoundIntervalManager(pLessThan.getOperand1()).lessThan(
        pLessThan.getOperand1().accept(this, pEnvironment),
        pLessThan.getOperand2().accept(this, pEnvironment));
    if (value.isDefinitelyTrue()) {
      return BooleanConstant.getTrue();
    }
    if (value.isDefinitelyFalse()) {
      return BooleanConstant.getFalse();
    }
    return null;
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(LogicalAnd<CompoundInterval> pAnd, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BooleanConstant<CompoundInterval> leftEval = pAnd.getOperand1().accept(this, pEnvironment);
    BooleanConstant<CompoundInterval> rightEval = pAnd.getOperand2().accept(this, pEnvironment);
    // If one operand is false, return it
    if (leftEval != null && !leftEval.getValue()) {
      return leftEval;
    }
    if (rightEval != null && !rightEval.getValue()) {
      return rightEval;
    }
    // If both operands are true, return the first one
    if (leftEval != null && rightEval != null && leftEval.getValue() && rightEval.getValue()) {
      return leftEval;
    }
    // Otherwise, the evaluation is undecided
    return null;
  }

  @Override
  public BooleanConstant<CompoundInterval> visit(LogicalNot<CompoundInterval> pNot, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BooleanConstant<CompoundInterval> operandEval =  pNot.getNegated().accept(this, pEnvironment);
    if (operandEval == null) {
      return operandEval;
    }
    return operandEval.negate();
  }

  @Override
  public CompoundInterval visit(Modulo<CompoundInterval> pModulo, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pModulo).modulo(pModulo.getNumerator().accept(this, pEnvironment), pModulo.getDenominator().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Multiply<CompoundInterval> pMultiply, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pMultiply).multiply(pMultiply.getFactor1().accept(this, pEnvironment), pMultiply.getFactor2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pShiftLeft).shiftLeft(pShiftLeft.getShifted().accept(this, pEnvironment), pShiftLeft.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(ShiftRight<CompoundInterval> pShiftRight, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pShiftRight).shiftRight(pShiftRight.getShifted().accept(this, pEnvironment), pShiftRight.getShiftDistance().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Union<CompoundInterval> pUnion, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return getCompoundIntervalManager(pUnion).union(pUnion.getOperand1().accept(this, pEnvironment), pUnion.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    NumeralFormula<CompoundInterval> varState = pEnvironment.get(pVariable.getName());
    if (varState == null) {
      return getCompoundIntervalManager(pVariable).allPossibleValues();
    }
    return varState.accept(this, pEnvironment);
  }

  @Override
  public BooleanConstant<CompoundInterval> visitFalse(Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return BooleanConstant.getFalse();
  }

  @Override
  public BooleanConstant<CompoundInterval> visitTrue(Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return BooleanConstant.getTrue();
  }

  @Override
  public CompoundInterval visit(IfThenElse<CompoundInterval> pIfThenElse,
      Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BooleanConstant<CompoundInterval> condition = pIfThenElse.getCondition().accept(this, pEnvironment);
    if (BooleanConstant.isTrue(condition)) {
      return pIfThenElse.getPositiveCase().accept(this, pEnvironment);
    }
    if (BooleanConstant.isFalse(condition)) {
      return pIfThenElse.getNegativeCase().accept(this, pEnvironment);
    }
    return getCompoundIntervalManager(pIfThenElse).union(
        pIfThenElse.getPositiveCase().accept(this, pEnvironment),
        pIfThenElse.getNegativeCase().accept(this, pEnvironment));
  }

  @Override
  public CompoundInterval visit(Cast<CompoundInterval> pCast,
      Map<? extends String, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval casted = pCast.getCasted().accept(this, pEnvironment);
    return getCompoundIntervalManager(pCast).cast(pCast.getBitVectorInfo(), casted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.compoundIntervalManagerFactory);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof FormulaCompoundStateEvaluationVisitor) {
      return compoundIntervalManagerFactory.equals(((FormulaCompoundStateEvaluationVisitor) pOther).compoundIntervalManagerFactory);
    }
    return false;
  }

}
