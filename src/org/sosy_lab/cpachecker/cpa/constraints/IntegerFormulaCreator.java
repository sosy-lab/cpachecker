/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Creator for {@link Formula}s using only integer values.
 */
public class IntegerFormulaCreator implements FormulaCreator<Formula> {

  private static final boolean SIGNED = true;
  private static final String B_AND_FUNC_NAME = "b_and";
  private static final String B_OR_FUNC_NAME = "b_or";
  private static final String B_XOR_FUNC_NAME = "b_xor";
  private static final String B_NOT_FUNC_NAME = "b_not";
  private static final String SHIFT_RIGHT_FUNC_NAME = "sh_r";
  private static final String SHIFT_LEFT_FUNC_NAME = "sh_l";
  private static final String FLOAT_VAR_NAME = "float";


  private final FormulaManagerView formulaManager;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> numeralFormulaManager;
  private final BooleanFormulaManagerView booleanFormulaManager;
  private final FunctionFormulaManagerView functionFormulaManager;

  private final FunctionSet declaredFunctions = new FunctionSet();

  private int counter = 0;

  public IntegerFormulaCreator(FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    numeralFormulaManager = formulaManager.getIntegerFormulaManager();
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    functionFormulaManager = formulaManager.getFunctionFormulaManager();
  }


  @Override
  public IntegerFormula visit(AdditionExpression pAdd) {
    final IntegerFormula op1 = (IntegerFormula) pAdd.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pAdd.getOperand2().accept(this);

    return numeralFormulaManager.add(op1, op2);
  }

  @Override
  public Formula visit(BinaryAndExpression pAnd) {
    return handleUnsupportedExpression(pAnd);
  }

  private Formula handleUnsupportedExpression(BinarySymbolicExpression pExp) {
    final Formula leftOperand = pExp.getOperand1().accept(this);
    final Formula rightOperand = pExp.getOperand2().accept(this);

    final FormulaType<?> expressionType = getFormulaType(pExp.getType());
    final FormulaType<?> calculationType = getFormulaType(pExp.getCalculationType());
    final String functionName = getFunctionNameByExpression(pExp);

    UninterpretedFunctionDeclaration<?> functionDeclaration;

    if (!declaredFunctions.contains(functionName, expressionType)) {

      // we pass calculation type two times because we have two arguments
      functionDeclaration = functionFormulaManager.declareUninterpretedFunction(
          functionName, expressionType, calculationType, calculationType);

      declaredFunctions.put(functionName, expressionType, functionDeclaration);

    } else {
      functionDeclaration = declaredFunctions.get(functionName, expressionType);
    }

    return functionFormulaManager.callUninterpretedFunction(functionDeclaration, leftOperand, rightOperand);
  }

  private String getFunctionNameByExpression(SymbolicExpression pExpression) {
    if (pExpression instanceof BinaryAndExpression) {
      return B_AND_FUNC_NAME;
    }

    if (pExpression instanceof BinaryOrExpression) {
      return B_OR_FUNC_NAME;
    }

    if (pExpression instanceof BinaryXorExpression) {
      return B_XOR_FUNC_NAME;
    }

    if (pExpression instanceof BinaryNotExpression) {
      return B_NOT_FUNC_NAME;
    }

    if (pExpression instanceof ShiftLeftExpression) {
      return SHIFT_LEFT_FUNC_NAME;
    }

    if (pExpression instanceof ShiftRightExpression) {
      return SHIFT_RIGHT_FUNC_NAME;
    }

    throw new AssertionError("Unexpected expression " + pExpression);
  }

  @Override
  public Formula visit(BinaryNotExpression pNot) {
    return handleUnsupportedExpression(pNot);
  }

  private Formula handleUnsupportedExpression(UnarySymbolicExpression pExp) {
    final Formula operand = pExp.getOperand().accept(this);

    final FormulaType<?> expressionType = getFormulaType(pExp.getType());
    final String functionName = getFunctionNameByExpression(pExp);

    UninterpretedFunctionDeclaration<?> functionDeclaration;

    if (!declaredFunctions.contains(functionName, expressionType)) {

      // we pass calculation type two times because we have two arguments
      functionDeclaration = functionFormulaManager.declareUninterpretedFunction(
          functionName, expressionType, expressionType);

      declaredFunctions.put(functionName, expressionType, functionDeclaration);

    } else {
      functionDeclaration = declaredFunctions.get(functionName, expressionType);
    }

    return functionFormulaManager.callUninterpretedFunction(functionDeclaration, operand);
  }

  @Override
  public Formula visit(BinaryOrExpression pOr) {
    return handleUnsupportedExpression(pOr);
  }

  @Override
  public Formula visit(BinaryXorExpression pXor) {
    return handleUnsupportedExpression(pXor);
  }

  @Override
  public Formula visit(ConstantSymbolicExpression pConstant) {
    Value value = pConstant.getValue();

    if (value.isNumericValue()) {
      NumericValue valueAsNumeric = (NumericValue) value;
      long longValue = valueAsNumeric.longValue();
      double doubleValue = valueAsNumeric.doubleValue();

      if (doubleValue % 1 == 0 && longValue == doubleValue) {
        return numeralFormulaManager.makeNumber(valueAsNumeric.longValue());
      } else {
        return handleFloatValue(pConstant);
      }

    } else if (value instanceof BooleanValue) {
      return booleanFormulaManager.makeBoolean(((BooleanValue)value).isTrue());

    } else if (value instanceof SymbolicValue) {
      return ((SymbolicValue) value).accept(this);
    }

    return null; // if we can't handle it, 'abort'
  }

  private Formula handleFloatValue(ConstantSymbolicExpression pExpression) {
    return formulaManager.makeVariable(getFormulaType(pExpression.getType()),
                                       FLOAT_VAR_NAME + counter++);
  }

  @Override
  public Formula visit(SymbolicIdentifier pValue) {
    return formulaManager.makeVariable(FormulaType.IntegerType, getVariableName(pValue));
  }

  private String getVariableName(SymbolicIdentifier pValue) {
    return IdentifierConverter.getInstance().convert(pValue);
  }

  private FormulaType<?> getFormulaType(Type pType) {
    if (pType instanceof JSimpleType) {
      switch (((JSimpleType) pType).getType()) {
        case BOOLEAN:
          return FormulaType.BooleanType;
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case UNSPECIFIED:
          return FormulaType.IntegerType;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CType) {
      return FormulaType.IntegerType;
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public IntegerFormula visit(DivisionExpression pDivide) {
    final IntegerFormula op1 = (IntegerFormula) pDivide.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pDivide.getOperand2().accept(this);

    return numeralFormulaManager.divide(op1, op2);
  }

  @Override
  public BooleanFormula visit(EqualsExpression pEqual) {
    final IntegerFormula op1 = (IntegerFormula) pEqual.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pEqual.getOperand2().accept(this);

    return numeralFormulaManager.equal(op1, op2);
  }

  @Override
  public Formula visit(LogicalOrExpression pExpression) {
    final BooleanFormula op1 = (BooleanFormula) pExpression.getOperand1().accept(this);
    final BooleanFormula op2 = (BooleanFormula) pExpression.getOperand2().accept(this);

    return booleanFormulaManager.and(op1, op2);
  }

  @Override
  public BooleanFormula visit(LessThanExpression pLessThan) {
    final IntegerFormula op1 = (IntegerFormula) pLessThan.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pLessThan.getOperand2().accept(this);

    return numeralFormulaManager.lessThan(op1, op2);
  }

  @Override
  public BooleanFormula visit(LogicalAndExpression pAnd) {
    final BooleanFormula op1 = (BooleanFormula) pAnd.getOperand1().accept(this);
    final BooleanFormula op2 = (BooleanFormula) pAnd.getOperand2().accept(this);

    return booleanFormulaManager.and(op1, op2);
  }

  @Override
  public Formula visit(CastExpression pExpression) {
    // ignore the cast for integer formulas
    return pExpression.getOperand().accept(this);
  }

  @Override
  public BooleanFormula visit(LogicalNotExpression pNot) {
    final BooleanFormula op = (BooleanFormula) pNot.getOperand().accept(this);

    return booleanFormulaManager.not(op);
  }

  @Override
  public Formula visit(LessThanOrEqualExpression pExpression) {
    final Formula op1 = pExpression.getOperand1().accept(this);
    final Formula op2 = pExpression.getOperand2().accept(this);

    return formulaManager.makeLessOrEqual(op1, op2, SIGNED);
  }

  @Override
  public IntegerFormula visit(ModuloExpression pModulo) {
    final IntegerFormula op1 = (IntegerFormula) pModulo.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pModulo.getOperand2().accept(this);

    return numeralFormulaManager.modulo(op1, op2);
  }

  @Override
  public IntegerFormula visit(MultiplicationExpression pMultiply) {
    final IntegerFormula op1 = (IntegerFormula) pMultiply.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pMultiply.getOperand2().accept(this);

    return numeralFormulaManager.multiply(op1, op2);
  }

  @Override
  public Formula visit(ShiftLeftExpression pShiftLeft) {
    return handleUnsupportedExpression(pShiftLeft);
  }

  @Override
  public Formula visit(ShiftRightExpression pShiftRight) {
    return handleUnsupportedExpression(pShiftRight);
  }

  @Override
  public BooleanFormula transformAssignment(Model.AssignableTerm pTerm, Object termAssignment) {
    Formula variable = createVariable(pTerm);

    final FormulaType<?> type = getFormulaType(pTerm.getType());
    Formula rightFormula;

    if (termAssignment instanceof Number) {
      assert type.isIntegerType();

      if (termAssignment instanceof Long) {
        rightFormula = numeralFormulaManager.makeNumber((Long) termAssignment);
      } else if (termAssignment instanceof Double) {
        rightFormula = numeralFormulaManager.makeNumber((Double) termAssignment);
      } else if (termAssignment instanceof BigInteger) {
        rightFormula = numeralFormulaManager.makeNumber((BigInteger) termAssignment);
      } else if (termAssignment instanceof BigDecimal) {
        rightFormula = numeralFormulaManager.makeNumber((BigDecimal) termAssignment);
      } else {
        throw new AssertionError("Unhandled assignment number " + termAssignment);
      }

    } else {
      throw new AssertionError("Unhandled assignment object " + termAssignment);
    }

    return formulaManager.makeEqual(variable, rightFormula);
  }

  private Formula createVariable(Model.AssignableTerm pTerm) {
    final String name = pTerm.getName();
    final FormulaType<?> type = getFormulaType(pTerm.getType());

    return formulaManager.makeVariable(type, name);
  }

  private FormulaType<?> getFormulaType(Model.TermType pType) {
    if (pType.equals(Model.TermType.Boolean)) {
      return FormulaType.BooleanType;

    } else if (pType.equals(Model.TermType.Integer)) {
      return FormulaType.IntegerType;

    } else {
      throw new AssertionError("Unexpected term type " + pType);
    }
  }

  private static class FunctionSet
      extends ForwardingTable<String, FormulaType<?>, UninterpretedFunctionDeclaration<?>> {

    private Table<String, FormulaType<?>, UninterpretedFunctionDeclaration<?>> functionTable
        = HashBasedTable.create();

    @Override
    protected Table<String, FormulaType<?>, UninterpretedFunctionDeclaration<?>> delegate() {
      return functionTable;
    }
  }
}
