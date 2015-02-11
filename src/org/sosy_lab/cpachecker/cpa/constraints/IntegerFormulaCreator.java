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
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
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
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.PointerExpression;
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

  private static long auxiliaryVariableAmount = 0;
  private static long floatVariableAmount = 0;

  private static final boolean SIGNED = true;
  private static final String B_AND_FUNC_NAME = "b_and";
  private static final String B_OR_FUNC_NAME = "b_or";
  private static final String B_XOR_FUNC_NAME = "b_xor";
  private static final String B_NOT_FUNC_NAME = "b_not";
  private static final String SHIFT_RIGHT_FUNC_NAME = "sh_r";
  private static final String SHIFT_LEFT_FUNC_NAME = "sh_l";
  private static final String FLOAT_VAR_NAME = "float";
  private static final String POINTER_EXP_FUNC_NAME = "pointer";
  private static final String AUXILIARY_NAME = "auxVar";


  private final FormulaManagerView formulaManager;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> numeralFormulaManager;
  private final BooleanFormulaManagerView booleanFormulaManager;
  private final FunctionFormulaManagerView functionFormulaManager;

  private final ValueAnalysisState valueState;

  private final FunctionSet declaredFunctions = new FunctionSet();

  private final IntegerFormula oneFormula;
  private final IntegerFormula zeroFormula;

  private List<BooleanFormula> conditions = new ArrayList<>();

  public IntegerFormulaCreator(FormulaManagerView pFormulaManager, ValueAnalysisState pValueState) {
    formulaManager = pFormulaManager;
    numeralFormulaManager = formulaManager.getIntegerFormulaManager();
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    functionFormulaManager = formulaManager.getFunctionFormulaManager();

    oneFormula = numeralFormulaManager.makeNumber(1);
    zeroFormula = numeralFormulaManager.makeNumber(0);

    valueState = pValueState;
  }

  @Override
  public BooleanFormula createFormula(Constraint pConstraint) {
    BooleanFormula originalFormula = (BooleanFormula) pConstraint.accept(this);

    if (conditions.isEmpty()) {
      return originalFormula;

    } else {
      BooleanFormula conditionsFormula = booleanFormulaManager.and(conditions);
      return formulaManager.makeAnd(originalFormula, conditionsFormula);
    }
  }

  @Override
  public IntegerFormula visit(AdditionExpression pAdd) {
    final Formula op1 = pAdd.getOperand1().accept(this);
    final Formula op2 = pAdd.getOperand2().accept(this);

    final BinaryCreator<IntegerFormula> creator = new BinaryCreator<IntegerFormula>() {

      @Override
      public IntegerFormula create(Formula pOp1, Formula pOp2) {
        return numeralFormulaManager.add((IntegerFormula) pOp1, (IntegerFormula) pOp2);
      }
    };

    return createFormulaWhileTransformingBooleanToInteger(op1, op2, creator);
  }

  @Override
  public Formula visit(BinaryAndExpression pAnd) {
    return handleUnsupportedExpression(pAnd);
  }

  private Formula handleUnsupportedExpression(BinarySymbolicExpression pExp) {
    Formula leftOperand = pExp.getOperand1().accept(this);
    Formula rightOperand = pExp.getOperand2().accept(this);

    final FormulaType<?> expressionType = getFormulaType(pExp.getType());
    final FormulaType<?> calculationType = getFormulaType(pExp.getCalculationType());
    final FunctionTypes functionTypes = new FunctionTypes(expressionType)
                                                         .parameter(calculationType)
                                                         .parameter(calculationType);

    final String functionName = getFunctionNameByExpression(pExp);

    UninterpretedFunctionDeclaration<?> functionDeclaration;

    if (!declaredFunctions.contains(functionName, functionTypes)) {

      // we pass calculation type two times because we have two arguments
      functionDeclaration = functionFormulaManager.declareUninterpretedFunction(
          functionName, expressionType, calculationType, calculationType);

      declaredFunctions.put(functionName, functionTypes, functionDeclaration);

    } else {
      functionDeclaration = declaredFunctions.get(functionName, functionTypes);
    }

    leftOperand = cast(leftOperand, calculationType);
    rightOperand = cast(rightOperand, calculationType);

    return functionFormulaManager.callUninterpretedFunction(functionDeclaration, leftOperand, rightOperand);
  }

  private Formula cast(Formula pFormula, FormulaType<?> pToType) {

    if (pFormula instanceof IntegerFormula && pToType.isBooleanType()) {
      return castToBoolean((IntegerFormula) pFormula);

    } else if (pFormula instanceof BooleanFormula && pToType.isIntegerType()) {
      return castToInteger((BooleanFormula) pFormula);

    } else {
      return pFormula;
    }
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

    if (pExpression instanceof PointerExpression) {
      return POINTER_EXP_FUNC_NAME;
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
    final FunctionTypes functionTypes = new FunctionTypes(expressionType)
                                                         .parameter(expressionType);
    final String functionName = getFunctionNameByExpression(pExp);

    UninterpretedFunctionDeclaration<?> functionDeclaration;

    if (!declaredFunctions.contains(functionName, functionTypes)) {

      // we pass calculation type two times because we have two arguments
      functionDeclaration = functionFormulaManager.declareUninterpretedFunction(
          functionName, expressionType, expressionType);

      declaredFunctions.put(functionName, functionTypes, functionDeclaration);

    } else {
      functionDeclaration = declaredFunctions.get(functionName, functionTypes);
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

    if (value instanceof SymbolicValue) {
      return ((SymbolicValue) value).accept(this);
    } else {
      return createFormulaFromConcreteValue(value);
    }

  }

  private Formula createFormulaFromConcreteValue(Value pValue) {

    if (pValue.isNumericValue()) {
      NumericValue valueAsNumeric = (NumericValue)pValue;
      long longValue = valueAsNumeric.longValue();
      double doubleValue = valueAsNumeric.doubleValue();

      if (doubleValue % 1 == 0 && longValue == doubleValue) {
        return numeralFormulaManager.makeNumber(valueAsNumeric.longValue());
      } else {
        return createFloatVariable(doubleValue);
      }

    } else if (pValue instanceof BooleanValue) {
      return booleanFormulaManager.makeBoolean(((BooleanValue)pValue).isTrue());
    }

    return null; // if we can't handle it, 'abort'
  }

  private Formula createFloatVariable(double pFloatValue) {
    Formula variable = formulaManager.makeVariable(FormulaType.IntegerType, FLOAT_VAR_NAME + floatVariableAmount++);

    assert (long) Math.floor(pFloatValue) == Math.floor(pFloatValue);
    Formula lowerBound = formulaManager.makeNumber(FormulaType.IntegerType, (long) Math.floor(pFloatValue));
    BooleanFormula lowerBoundConstraint = formulaManager.makeGreaterOrEqual(variable, lowerBound, SIGNED);

    assert (long) Math.ceil(pFloatValue) == Math.ceil(pFloatValue);
    Formula upperBound = formulaManager.makeNumber(FormulaType.IntegerType, (long) Math.ceil(pFloatValue));
    BooleanFormula upperBoundConstraint = formulaManager.makeLessOrEqual(variable, upperBound, SIGNED);

    BooleanFormula fullBoundConstraint = booleanFormulaManager.and(lowerBoundConstraint, upperBoundConstraint);
    conditions.add(fullBoundConstraint);

    return variable;
  }

  @Override
  public Formula visit(SymbolicIdentifier pValue) {

    if (valueState.hasKnownValue(pValue)) {
      return createFormulaFromConcreteValue(valueState.getValueFor(pValue));

    } else {
      return formulaManager.makeVariable(FormulaType.IntegerType, getVariableName(pValue));
    }
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
    final Formula op1 = pDivide.getOperand1().accept(this);
    final Formula op2 = pDivide.getOperand2().accept(this);

    final BinaryCreator<IntegerFormula> creator = new BinaryCreator<IntegerFormula>() {

      @Override
      public IntegerFormula create(Formula pOp1, Formula pOp2) {
        return numeralFormulaManager.divide((IntegerFormula)pOp1, (IntegerFormula)pOp2);
      }
    };

    return createFormulaWhileTransformingBooleanToInteger(op1, op2, creator);
  }

  @Override
  public BooleanFormula visit(EqualsExpression pEqual) {
    Formula op1 = pEqual.getOperand1().accept(this);
    Formula op2 = pEqual.getOperand2().accept(this);

    BinaryCreator<BooleanFormula> creator = new BinaryCreator<BooleanFormula>() {

      @Override
      public BooleanFormula create(Formula pOp1, Formula pOp2) {
        return formulaManager.makeEqual(pOp1, pOp2);
      }
    };

    return createFormulaWhileTransformingToIntegerIfNecessary(op1, op2, creator);
  }

  private <T extends Formula> T createFormulaWhileTransformingToIntegerIfNecessary(
      Formula pOp1, Formula pOp2, BinaryCreator<T> pCreator) {

    if (pOp1 instanceof BooleanFormula == pOp2 instanceof BooleanFormula) {
      return pCreator.create(pOp1, pOp2);

    } else {
      return createFormulaWhileTransformingBooleanToInteger(pOp1, pOp2, pCreator);
    }
  }

  private <T extends Formula> T createFormulaWhileTransformingIntegerToBoolean(
      Formula pOp1, Formula pOp2, BinaryCreator<T> pCreator) {

    BooleanFormula op1;
    BooleanFormula op2;

    if (pOp1 instanceof IntegerFormula) {
      IntegerFormula integerFormula = (IntegerFormula) pOp1;

      op1 = castToBoolean(integerFormula);

    } else {
      assert pOp1 instanceof BooleanFormula;
      op1 = (BooleanFormula) pOp1;
    }

    if (pOp2 instanceof IntegerFormula) {
      IntegerFormula integerFormula = (IntegerFormula) pOp2;

      op2 = castToBoolean(integerFormula);

    } else {
      assert pOp2 instanceof BooleanFormula;
      op2 = (BooleanFormula) pOp2;
    }

    return pCreator.create(op1, op2);
  }

  private BooleanFormula castToBoolean(IntegerFormula pFormula) {
    return numeralFormulaManager.greaterOrEquals(pFormula, oneFormula);
  }

  private <T extends Formula> T createFormulaWhileTransformingBooleanToInteger(
      Formula pOp1, Formula pOp2, BinaryCreator<T> pCreator) {

    IntegerFormula op1;
    IntegerFormula op2;

    if (pOp1 instanceof BooleanFormula) {
      BooleanFormula booleanFormula = (BooleanFormula) pOp1;

      op1 = castToInteger(booleanFormula);

    } else {
      assert pOp1 instanceof IntegerFormula;
      op1 = (IntegerFormula) pOp1;
    }

    if (pOp2 instanceof BooleanFormula) {
      BooleanFormula booleanFormula = (BooleanFormula) pOp2;

      op2 = castToInteger(booleanFormula);

    } else {
      assert pOp2 instanceof IntegerFormula;
      op2 = (IntegerFormula) pOp2;
    }

    return pCreator.create(op1, op2);
  }

  private IntegerFormula castToInteger(BooleanFormula pFormula) {
    IntegerFormula variable = getAuxiliaryVariable(FormulaType.IntegerType);
    final BooleanFormula trueAssignment = formulaManager.assignment(variable, oneFormula);
    final BooleanFormula falseAssignment = formulaManager.assignment(variable, zeroFormula);

    conditions.add(booleanFormulaManager.ifThenElse(pFormula, trueAssignment, falseAssignment));

    return variable;
  }

  private <T extends Formula> T getAuxiliaryVariable(FormulaType<T> pType) {
    return formulaManager.makeVariable(pType, getAuxiliaryVariableName());
  }

  @Override
  public Formula visit(LogicalOrExpression pExpression) {
    final Formula op1 = pExpression.getOperand1().accept(this);
    final Formula op2 = pExpression.getOperand2().accept(this);

    final BinaryCreator<BooleanFormula> orCreator = new BinaryCreator<BooleanFormula>() {

      @Override
      public BooleanFormula create(Formula pOp1, Formula pOp2) {
        return booleanFormulaManager.or((BooleanFormula)pOp1, (BooleanFormula)pOp2);
      }
    };

    return createFormulaWhileTransformingIntegerToBoolean(op1, op2, orCreator);
  }

  @Override
  public BooleanFormula visit(LessThanExpression pLessThan) {
    final Formula op1 = pLessThan.getOperand1().accept(this);
    final Formula op2 = pLessThan.getOperand2().accept(this);

    BinaryCreator<BooleanFormula> creator = new BinaryCreator<BooleanFormula>() {

      @Override
      public BooleanFormula create(Formula pOp1, Formula pOp2) {
        return formulaManager.makeLessThan(pOp1, pOp2, SIGNED);
      }
    };

    return createFormulaWhileTransformingBooleanToInteger(op1, op2, creator);
  }

  @Override
  public BooleanFormula visit(LogicalAndExpression pAnd) {
    final Formula op1 = pAnd.getOperand1().accept(this);
    final Formula op2 = pAnd.getOperand2().accept(this);

    final BinaryCreator<BooleanFormula> creator = new BinaryCreator<BooleanFormula>() {

      @Override
      public BooleanFormula create(Formula pOp1, Formula pOp2) {
        return booleanFormulaManager.and((BooleanFormula)pOp1, (BooleanFormula)pOp2);
      }
    };

    return createFormulaWhileTransformingIntegerToBoolean(op1, op2, creator);
  }

  @Override
  public Formula visit(CastExpression pExpression) {
    // ignore the cast for integer formulas
    return pExpression.getOperand().accept(this);
  }

  @Override
  public Formula visit(PointerExpression pExpression) {
    return handleUnsupportedExpression(pExpression);
  }

  @Override
  public BooleanFormula visit(LogicalNotExpression pNot) {
    Formula operandFormula = pNot.getOperand().accept(this);
    BooleanFormula op;

    if (operandFormula instanceof IntegerFormula) {
      op = castToBoolean((IntegerFormula) operandFormula);

    } else {
      assert operandFormula instanceof BooleanFormula;
      op = (BooleanFormula) operandFormula;
    }

    return booleanFormulaManager.not(op);
  }

  @Override
  public BooleanFormula visit(LessThanOrEqualExpression pExpression) {
    final Formula op1 = pExpression.getOperand1().accept(this);
    final Formula op2 = pExpression.getOperand2().accept(this);

    BinaryCreator<BooleanFormula> creator = new BinaryCreator<BooleanFormula>() {

      @Override
      public BooleanFormula create(Formula pOp1, Formula pOp2) {
        return formulaManager.makeLessOrEqual(pOp1, pOp2, SIGNED);
      }
    };

    return createFormulaWhileTransformingBooleanToInteger(op1, op2, creator);
  }

  @Override
  public IntegerFormula visit(ModuloExpression pModulo) {
    final Formula op1 = pModulo.getOperand1().accept(this);
    final Formula op2 = pModulo.getOperand2().accept(this);

    final BinaryCreator<IntegerFormula> creator = new BinaryCreator<IntegerFormula>() {

      @Override
      public IntegerFormula create(Formula pOp1, Formula pOp2) {
        return numeralFormulaManager.modulo((IntegerFormula)pOp1, (IntegerFormula)pOp2);
      }
    };

    return createFormulaWhileTransformingBooleanToInteger(op1, op2, creator);
  }

  @Override
  public IntegerFormula visit(MultiplicationExpression pMultiply) {
    final Formula op1 = pMultiply.getOperand1().accept(this);
    final Formula op2 = pMultiply.getOperand2().accept(this);

    final BinaryCreator<IntegerFormula> creator = new BinaryCreator<IntegerFormula>() {

      @Override
      public IntegerFormula create(Formula pOp1, Formula pOp2) {
        return numeralFormulaManager.multiply((IntegerFormula)pOp1, (IntegerFormula)pOp2);
      }
    };

    return createFormulaWhileTransformingBooleanToInteger(op1, op2, creator);
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

    } else if (termAssignment instanceof Boolean) {
      rightFormula = booleanFormulaManager.makeBoolean((Boolean) termAssignment);
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

  public String getAuxiliaryVariableName() {
    return AUXILIARY_NAME + auxiliaryVariableAmount++;
  }

  private static class FunctionSet
      extends ForwardingTable<String, FunctionTypes, UninterpretedFunctionDeclaration<?>> {

    private Table<String, FunctionTypes, UninterpretedFunctionDeclaration<?>> functionTable
        = HashBasedTable.create();

    @Override
    protected Table<String, FunctionTypes, UninterpretedFunctionDeclaration<?>> delegate() {
      return functionTable;
    }
  }

  private static class FunctionTypes {
    private final FormulaType<?> returnType;
    private final List<FormulaType<?>> parameterTypes = new ArrayList<>();

    public FunctionTypes(FormulaType<?> pReturnType) {
      returnType = pReturnType;
    }

    public FunctionTypes parameter(FormulaType<?> pParameterType) {
      parameterTypes.add(pParameterType);
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      FunctionTypes that = (FunctionTypes)o;

      if (!parameterTypes.equals(that.parameterTypes)) {
        return false;
      }
      if (!returnType.equals(that.returnType)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = returnType.hashCode();
      result = 31 * result + parameterTypes.hashCode();
      return result;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("FunctionTypes[ ReturnType: ");
      sb.append(returnType.toString());

      if (!parameterTypes.isEmpty()) {
        sb.append(", ParameterType(s):");

        for (FormulaType<?> t : parameterTypes) {
          sb.append(t.toString());
          sb.append(" ");
        }
      }

      sb.append("]");

      return sb.toString();
    }
  }

  private interface BinaryCreator<T extends Formula> {
    T create(Formula pOp1, Formula pOp2);
  }

}
