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

/**
 * Instances of this class are used to expose a specified variable in visited
 * formulae. By using an environment, the operation is applied recursively on
 * the formulae of other variables. After applying the visitor to a formula,
 * all occurrences of the variable in question in the formula are revealed and,
 * if necessary, other variables are resolved. Variables that need not be
 * resolved because their formulae do not contain the variable in question
 * (even after recursively applying the exposing visitor to them) will stay
 * unresolved.
 *
 * <pre>
 * Example:
 * Environment: {x = a + b, y = a + c}
 * Variable in question: b
 * Formula the visitor is applied to: (x + y) * z
 * Resulting formula: ((a + b) + y) * z
 * </pre>
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public class ExposeVarVisitor<T> implements ParameterizedInvariantsFormulaVisitor<T, String, InvariantsFormula<T>> {

  /**
   * The visitor used to determine whether or not a formula contains a
   * variable.
   */
  private final ContainsVarVisitor<T> containsVarVisitor = new ContainsVarVisitor<>();

  /**
   * The environment used to resolve variables.
   */
  private final Map<? extends String, ? extends InvariantsFormula<T>> environment;

  /**
   * Creates a new visitor for exposing variables in formulae using the given
   * environment to resolve variables.
   *
   * @param pEnvironment the environment used to resolve variables.
   */
  public ExposeVarVisitor(Map<? extends String, ? extends InvariantsFormula<T>> pEnvironment) {
    this.environment = pEnvironment;
  }

  @Override
  public InvariantsFormula<T> visit(Add<T> pAdd, String pVarName) {
    InvariantsFormula<T> summand1 = pAdd.getSummand1().accept(this, pVarName);
    InvariantsFormula<T> summand2 = pAdd.getSummand2().accept(this, pVarName);
    boolean s1ContainsVar = summand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = summand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(BinaryAnd<T> pAnd, String pVarName) {
    InvariantsFormula<T> operand1 = pAnd.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pAnd.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(BinaryNot<T> pNot, String pVarName) {
    InvariantsFormula<T> operand = pNot.getFlipped().accept(this, pVarName);
    if (!operand.accept(containsVarVisitor, pVarName)
        || operand.equals(pNot.getFlipped())) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.binaryNot(operand);
  }

  @Override
  public InvariantsFormula<T> visit(BinaryOr<T> pOr, String pVarName) {
    InvariantsFormula<T> operand1 = pOr.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pOr.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(BinaryXor<T> pXor, String pVarName) {
    InvariantsFormula<T> operand1 = pXor.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pXor.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(Constant<T> pConstant, String pVarName) {
    return pConstant;
  }

  @Override
  public InvariantsFormula<T> visit(Divide<T> pDivide, String pVarName) {
    InvariantsFormula<T> numerator = pDivide.getNumerator().accept(this, pVarName);
    InvariantsFormula<T> denominator = pDivide.getDenominator().accept(this, pVarName);
    boolean s1ContainsVar = numerator.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = denominator.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(Equal<T> pEqual, String pVarName) {
    InvariantsFormula<T> operand1 = pEqual.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pEqual.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(LessThan<T> pLessThan, String pVarName) {
    InvariantsFormula<T> operand1 = pLessThan.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pLessThan.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(LogicalAnd<T> pAnd, String pVarName) {
    InvariantsFormula<T> operand1 = pAnd.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pAnd.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(LogicalNot<T> pNot, String pVarName) {
    InvariantsFormula<T> operand = pNot.getNegated().accept(this, pVarName);
    if (!operand.accept(containsVarVisitor, pVarName)
        || operand.equals(pNot.getNegated())) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.logicalNot(operand);
  }

  @Override
  public InvariantsFormula<T> visit(Modulo<T> pModulo, String pVarName) {
    InvariantsFormula<T> numerator = pModulo.getNumerator().accept(this, pVarName);
    InvariantsFormula<T> denominator = pModulo.getDenominator().accept(this, pVarName);
    boolean s1ContainsVar = numerator.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = denominator.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(Multiply<T> pMultiply, String pVarName) {
    InvariantsFormula<T> factor1 = pMultiply.getFactor1().accept(this, pVarName);
    InvariantsFormula<T> factor2 = pMultiply.getFactor2().accept(this, pVarName);
    boolean s1ContainsVar = factor1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = factor2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(ShiftLeft<T> pShiftLeft, String pVarName) {
    InvariantsFormula<T> shifted = pShiftLeft.getShifted().accept(this, pVarName);
    InvariantsFormula<T> shiftDistance = pShiftLeft.getShiftDistance().accept(this, pVarName);
    boolean s1ContainsVar = shifted.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = shiftDistance.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(ShiftRight<T> pShiftRight, String pVarName) {
    InvariantsFormula<T> shifted = pShiftRight.getShifted().accept(this, pVarName);
    InvariantsFormula<T> shiftDistance = pShiftRight.getShiftDistance().accept(this, pVarName);
    boolean s1ContainsVar = shifted.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = shiftDistance.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(Union<T> pUnion, String pVarName) {
    InvariantsFormula<T> operand1 = pUnion.getOperand1().accept(this, pVarName);
    InvariantsFormula<T> operand2 = pUnion.getOperand2().accept(this, pVarName);
    boolean s1ContainsVar = operand1.accept(containsVarVisitor, pVarName);
    boolean s2ContainsVar = operand2.accept(containsVarVisitor, pVarName);
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
  public InvariantsFormula<T> visit(Variable<T> pVariable, String pVarName) {
    // If the visited formula is the variable in question, it remains unchanged
    if (pVariable.getName().equals(pVarName)) {
      return pVariable;
    }
    // If the visited formula is a different variable, resolve it
    InvariantsFormula<T> value = environment.get(pVariable.getName());
    // If the environment does not contain a value for the variable, return
    // the variable, because no further exposing is possible
    if (value == null) {
      return pVariable;
    }
    // Try to expose the variable in the formula of the resolved variable
    InvariantsFormula<T> resolved = value.accept(this, pVarName);
    // If the variable could not be exposed in the formula, return the visited
    // formula
    if (!resolved.accept(containsVarVisitor, pVarName)) {
      return pVariable;
    }
    return resolved;
  }

}
