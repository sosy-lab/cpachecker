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


public class ExposeVarVisitor<T> implements InvariantsFormulaVisitor<T, InvariantsFormula<T>> {

  private final ContainsVarVisitor<T> containsVarVisitor;

  private final String varName;

  private final Map<? extends String, ? extends InvariantsFormula<T>> environment;

  public ExposeVarVisitor(String pVarName, Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    this.varName = pVarName;
    this.containsVarVisitor = new ContainsVarVisitor<>(pVarName);
    this.environment = pEnvironment;
  }

  @Override
  public InvariantsFormula<T> visit(Add<T> pAdd) {
    InvariantsFormula<T> summand1 = pAdd.getSummand1().accept(this);
    InvariantsFormula<T> summand2 = pAdd.getSummand2().accept(this);
    boolean s1ContainsVar = summand1.accept(containsVarVisitor);
    boolean s2ContainsVar = summand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pAdd;
    }
    if (!s1ContainsVar) {
      summand1 = pAdd.getSummand1();
    } else if (!s2ContainsVar) {
      summand2 = pAdd.getSummand2();
    }
    if (summand1.equals(pAdd.getSummand1()) && summand2.equals(pAdd.getSummand2())) {
      return pAdd;
    }
    return InvariantsFormulaManager.INSTANCE.add(summand1, summand2);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryAnd<T> pAnd) {
    InvariantsFormula<T> operand1 = pAnd.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pAnd.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pAnd;
    }
    if (!s1ContainsVar) {
      operand1 = pAnd.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pAnd.getOperand2();
    }
    if (operand1.equals(pAnd.getOperand1()) && operand2.equals(pAnd.getOperand2())) {
      return pAnd;
    }
    return InvariantsFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryNot<T> pNot) {
    InvariantsFormula<T> operand = pNot.getFlipped();
    if (!operand.accept(containsVarVisitor)
        || operand.equals(pNot.getFlipped())) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.binaryNot(operand);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryOr<T> pOr) {
    InvariantsFormula<T> operand1 = pOr.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pOr.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pOr;
    }
    if (!s1ContainsVar) {
      operand1 = pOr.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pOr.getOperand2();
    }
    if (operand1.equals(pOr.getOperand1()) && operand2.equals(pOr.getOperand2())) {
      return pOr;
    }
    return InvariantsFormulaManager.INSTANCE.binaryOr(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryXor<T> pXor) {
    InvariantsFormula<T> operand1 = pXor.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pXor.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pXor;
    }
    if (!s1ContainsVar) {
      operand1 = pXor.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pXor.getOperand2();
    }
    if (operand1.equals(pXor.getOperand1()) && operand2.equals(pXor.getOperand2())) {
      return pXor;
    }
    return InvariantsFormulaManager.INSTANCE.binaryXor(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(Constant<T> pConstant) {
    return pConstant;
  }

  @Override
  public InvariantsFormula<T> visit(Divide<T> pDivide) {
    InvariantsFormula<T> numerator = pDivide.getNumerator().accept(this);
    InvariantsFormula<T> denominator = pDivide.getDenominator().accept(this);
    boolean s1ContainsVar = numerator.accept(containsVarVisitor);
    boolean s2ContainsVar = denominator.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pDivide;
    }
    if (!s1ContainsVar) {
      numerator = pDivide.getNumerator();
    } else if (!s2ContainsVar) {
      denominator = pDivide.getDenominator();
    }
    if (numerator.equals(pDivide.getNumerator()) && denominator.equals(pDivide.getDenominator())) {
      return pDivide;
    }
    return InvariantsFormulaManager.INSTANCE.divide(numerator, denominator);
  }

  @Override
  public InvariantsFormula<T> visit(Equal<T> pEqual) {
    InvariantsFormula<T> operand1 = pEqual.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pEqual.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pEqual;
    }
    if (!s1ContainsVar) {
      operand1 = pEqual.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pEqual.getOperand2();
    }
    if (operand1.equals(pEqual.getOperand1()) && operand2.equals(pEqual.getOperand2())) {
      return pEqual;
    }
    return InvariantsFormulaManager.INSTANCE.equal(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(LessThan<T> pLessThan) {
    InvariantsFormula<T> operand1 = pLessThan.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pLessThan.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pLessThan;
    }
    if (!s1ContainsVar) {
      operand1 = pLessThan.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pLessThan.getOperand2();
    }
    if (operand1.equals(pLessThan.getOperand1()) && operand2.equals(pLessThan.getOperand2())) {
      return pLessThan;
    }
    return InvariantsFormulaManager.INSTANCE.lessThan(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(LogicalAnd<T> pAnd) {
    InvariantsFormula<T> operand1 = pAnd.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pAnd.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pAnd;
    }
    if (!s1ContainsVar) {
      operand1 = pAnd.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pAnd.getOperand2();
    }
    if (operand1.equals(pAnd.getOperand1()) && operand2.equals(pAnd.getOperand2())) {
      return pAnd;
    }
    return InvariantsFormulaManager.INSTANCE.logicalAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(LogicalNot<T> pNot) {
    InvariantsFormula<T> operand = pNot.getNegated();
    if (!operand.accept(containsVarVisitor)
        || operand.equals(pNot.getNegated())) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.logicalNot(operand);
  }

  @Override
  public InvariantsFormula<T> visit(Modulo<T> pModulo) {
    InvariantsFormula<T> numerator = pModulo.getNumerator().accept(this);
    InvariantsFormula<T> denominator = pModulo.getDenominator().accept(this);
    boolean s1ContainsVar = numerator.accept(containsVarVisitor);
    boolean s2ContainsVar = denominator.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pModulo;
    }
    if (!s1ContainsVar) {
      numerator = pModulo.getNumerator();
    } else if (!s2ContainsVar) {
      denominator = pModulo.getDenominator();
    }
    if (numerator.equals(pModulo.getNumerator()) && denominator.equals(pModulo.getDenominator())) {
      return pModulo;
    }
    return InvariantsFormulaManager.INSTANCE.modulo(numerator, denominator);
  }

  @Override
  public InvariantsFormula<T> visit(Multiply<T> pMultiply) {
    InvariantsFormula<T> factor1 = pMultiply.getFactor1().accept(this);
    InvariantsFormula<T> factor2 = pMultiply.getFactor2().accept(this);
    boolean s1ContainsVar = factor1.accept(containsVarVisitor);
    boolean s2ContainsVar = factor2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pMultiply;
    }
    if (!s1ContainsVar) {
      factor1 = pMultiply.getFactor1();
    } else if (!s2ContainsVar) {
      factor2 = pMultiply.getFactor2();
    }
    if (factor1.equals(pMultiply.getFactor1()) && factor2.equals(pMultiply.getFactor2())) {
      return pMultiply;
    }
    return InvariantsFormulaManager.INSTANCE.multiply(factor1, factor2);
  }

  @Override
  public InvariantsFormula<T> visit(Negate<T> pNegate) {
    InvariantsFormula<T> operand = pNegate.getNegated();
    if (!operand.accept(containsVarVisitor)
        || operand.equals(pNegate.getNegated())) {
      return pNegate;
    }
    return InvariantsFormulaManager.INSTANCE.negate(operand);
  }

  @Override
  public InvariantsFormula<T> visit(ShiftLeft<T> pShiftLeft) {
    InvariantsFormula<T> shifted = pShiftLeft.getShifted().accept(this);
    InvariantsFormula<T> shiftDistance = pShiftLeft.getShiftDistance().accept(this);
    boolean s1ContainsVar = shifted.accept(containsVarVisitor);
    boolean s2ContainsVar = shiftDistance.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pShiftLeft;
    }
    if (!s1ContainsVar) {
      shifted = pShiftLeft.getShifted();
    } else if (!s2ContainsVar) {
      shiftDistance = pShiftLeft.getShiftDistance();
    }
    if (shifted.equals(pShiftLeft.getShifted()) && shiftDistance.equals(pShiftLeft.getShiftDistance())) {
      return pShiftLeft;
    }
    return InvariantsFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<T> visit(ShiftRight<T> pShiftRight) {
    InvariantsFormula<T> shifted = pShiftRight.getShifted().accept(this);
    InvariantsFormula<T> shiftDistance = pShiftRight.getShiftDistance().accept(this);
    boolean s1ContainsVar = shifted.accept(containsVarVisitor);
    boolean s2ContainsVar = shiftDistance.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pShiftRight;
    }
    if (!s1ContainsVar) {
      shifted = pShiftRight.getShifted();
    } else if (!s2ContainsVar) {
      shiftDistance = pShiftRight.getShiftDistance();
    }
    if (shifted.equals(pShiftRight.getShifted()) && shiftDistance.equals(pShiftRight.getShiftDistance())) {
      return pShiftRight;
    }
    return InvariantsFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<T> visit(Union<T> pUnion) {
    InvariantsFormula<T> operand1 = pUnion.getOperand1().accept(this);
    InvariantsFormula<T> operand2 = pUnion.getOperand2().accept(this);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor);
    if (!s1ContainsVar && !s2ContainsVar) {
      return pUnion;
    }
    if (!s1ContainsVar) {
      operand1 = pUnion.getOperand1();
    } else if (!s2ContainsVar) {
      operand2 = pUnion.getOperand2();
    }
    if (operand1.equals(pUnion.getOperand1()) && operand2.equals(pUnion.getOperand2())) {
      return pUnion;
    }
    return InvariantsFormulaManager.INSTANCE.binaryXor(operand1, operand2);
  }

  @Override
  public InvariantsFormula<T> visit(Variable<T> pVariable) {
    if (pVariable.getName().equals(varName)) {
      return pVariable;
    }
    InvariantsFormula<T> value = environment.get(pVariable.getName());
    if (value == null) {
      return pVariable;
    }
    InvariantsFormula<T> resolved = value.accept(this);
    if (!resolved.accept(containsVarVisitor)) {
      return pVariable;
    }
    return resolved;
  }

}
