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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpressionFactory;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;

/**
 * Factory for creating {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint} objects.
 */
public class ConstraintFactory {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final String functionName;
  private final Optional<ValueAnalysisState> valueState;

  private boolean missingInformation = false;
  private SymbolicExpressionFactory expressionFactory;


  private ConstraintFactory(String pFunctionName, Optional<ValueAnalysisState> pValueState, MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {

    machineModel = pMachineModel;
    logger = pLogger;
    functionName = pFunctionName;
    valueState = pValueState;
    expressionFactory = SymbolicExpressionFactory.getInstance();
  }

  public static ConstraintFactory getInstance(String pFunctionName, Optional<ValueAnalysisState> pValueState,
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    return new ConstraintFactory(pFunctionName, pValueState, pMachineModel, pLogger);
  }

  /**
   * Returns whether information was missing while creating the last constraint.
   *
   * <p>This method always resets after one call. So when calling this method after the creation of a constraint,
   * it will only return <code>true</code> at the first call, if at all.</p>
   *
   * @return <code>true</code> if information was missing, <code>false</code> otherwise
   */
  public boolean hasMissingInformation() {
    boolean hasMissingInformation = missingInformation;
    missingInformation = false;

    return hasMissingInformation;
  }

  public Constraint createNegativeConstraint(CUnaryExpression pExpression) throws UnrecognizedCodeException {
    Constraint positiveConstraint = createPositiveConstraint(pExpression);

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }

  public Constraint createNegativeConstraint(CBinaryExpression pExpression) throws UnrecognizedCodeException {
    Constraint positiveConstraint = createPositiveConstraint(pExpression);

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }

  public Constraint createNegativeConstraint(JUnaryExpression pExpression) throws UnrecognizedCodeException {
    Constraint positiveConstraint = createNot(createPositiveConstraint(pExpression));

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }

  public Constraint createNegativeConstraint(JBinaryExpression pExpression) throws UnrecognizedCodeException {
    Constraint positiveConstraint = createPositiveConstraint(pExpression);

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }

  public Constraint createNegativeConstraint(AIdExpression pExpression) throws UnrecognizedCodeException {
   Constraint positiveConstraint = createPositiveConstraint(pExpression);

    if (positiveConstraint == null) {
      return null;
    } else {
      return createNot(positiveConstraint);
    }
  }


  public Constraint createPositiveConstraint(CUnaryExpression pExpression) throws UnrecognizedCodeException {
    CUnaryExpression.UnaryOperator operator = pExpression.getOperator();

    switch (operator) {
      case SIZEOF:
      case ALIGNOF:
      case AMPER:
        return null;

      default:
        SymbolicExpression operandExpression = getCTransformer().transform(pExpression);

        if (operandExpression == null) {
          return null;
        } else {
          return transformValueToConstraint(operandExpression, pExpression.getExpressionType());
        }
    }
  }

  public Constraint createPositiveConstraint(CBinaryExpression pExpression) throws UnrecognizedCodeException {
    final CBinaryExpression.BinaryOperator operator = pExpression.getOperator();
    final Type expressionType = pExpression.getExpressionType();
    final Type calculationType = pExpression.getCalculationType();

    final CExpressionTransformer transformer = getCTransformer();

    SymbolicExpression leftOperand = transformer.transform(pExpression.getOperand1());

    checkForMissingInfo(transformer);
    if (leftOperand == null) {
      return null;
    }

    SymbolicExpression rightOperand = transformer.transform(pExpression.getOperand2());

    checkForMissingInfo(transformer);
    if (rightOperand == null) {
      return null;
    }

    switch (operator) {
      case EQUALS:
        return createEqual(leftOperand, rightOperand, expressionType, calculationType);
      case NOT_EQUALS:
        return createNotEqual(leftOperand, rightOperand, expressionType, calculationType);
      case GREATER_EQUAL: {
        SymbolicExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        return createLessOrEqual(leftOperand, rightOperand, expressionType, calculationType);

      case GREATER_THAN: {
        SymbolicExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        return createLess(leftOperand, rightOperand, expressionType, calculationType);

      default:
        throw new AssertionError("Operation " + operator + " not a constraint.");
    }
  }

  public Constraint createPositiveConstraint(JUnaryExpression pExpression) throws UnrecognizedCodeException {
    assert pExpression.getOperator() == JUnaryExpression.UnaryOperator.NOT;

    JExpression operand = pExpression.getOperand();
    SymbolicExpression operandExpression = getJavaTransformer().transform(operand);

    if (operandExpression == null) {
      return null;

    } else {
      return createNot(operandExpression);
    }
  }


  public Constraint createPositiveConstraint(JBinaryExpression pExpression) throws UnrecognizedCodeException {
    final JBinaryExpression.BinaryOperator operator = pExpression.getOperator();
    final Type expressionType = pExpression.getExpressionType();

    final JExpressionTransformer transformer = getJavaTransformer();

    SymbolicExpression leftOperand = transformer.transform(pExpression.getOperand1());

    checkForMissingInfo(transformer);
    if (leftOperand == null) {
      return null;
    }

    SymbolicExpression rightOperand = transformer.transform(pExpression.getOperand2());

    checkForMissingInfo(transformer);
    if (rightOperand == null) {
      return null;
    }

    switch (operator) {
      case EQUALS:
        return createEqual(leftOperand, rightOperand, expressionType, expressionType);
      case NOT_EQUALS:
        return createNotEqual(leftOperand, rightOperand, expressionType, expressionType);
      case GREATER_EQUAL: {
        SymbolicExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_EQUAL:
        return createLessOrEqual(leftOperand, rightOperand, expressionType, expressionType);

      case GREATER_THAN: {
        SymbolicExpression swap = leftOperand;
        leftOperand = rightOperand;
        rightOperand = swap;
      }
      // $FALL-THROUGH$
      case LESS_THAN:
        return createLess(leftOperand, rightOperand, expressionType, expressionType);

      default:
        throw new AssertionError("Operation " + operator + " not a constraint.");
    }
  }

  public Constraint createPositiveConstraint(AIdExpression pExpression) throws UnrecognizedCodeException {
    ExpressionTransformer transformer = new ExpressionTransformer(functionName, valueState);
    SymbolicExpression symbolicExpression = transformer.visit(pExpression);

    if (symbolicExpression == null) {
      return null;
    } else if (symbolicExpression instanceof Constraint) {
      return (Constraint) symbolicExpression;

    } else {
      return transformValueToConstraint(symbolicExpression, pExpression.getExpressionType());
    }
  }

  private Constraint transformValueToConstraint(SymbolicExpression pExpression, Type expressionType) {

    if (isNumeric(expressionType)) {
      return createLessOrEqual(getZeroConstant(expressionType), pExpression, expressionType,
          getCalculationType(expressionType));

    } else if (isBoolean(expressionType)) {
      assert expressionType instanceof JType : "Expression is boolean but not a constraint in C!";
      return expressionFactory.equal(pExpression, getTrueValueConstant(), expressionType, expressionType);

    } else {
      throw new AssertionError("Unexpected type " + expressionType);
    }

  }

  private JExpressionTransformer getJavaTransformer() {
    return new JExpressionTransformer(functionName, valueState);
  }

  private CExpressionTransformer getCTransformer() {
    return new CExpressionTransformer(functionName, valueState, machineModel, logger);
  }

  private boolean isNumeric(Type pType) {
    if (pType instanceof CType) {
      CType canonicalType = ((CType) pType).getCanonicalType();
      if (canonicalType instanceof CSimpleType) {
        switch (((CSimpleType) canonicalType).getType()) {
          case FLOAT:
          case INT:
            return true;
          default:
            // DO NOTHING, false is returned below
        }
      }

      return false;
    } else if (pType instanceof JSimpleType) {
      switch (((JSimpleType)pType).getType()) {
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
          return true;
        default:
          // DO NOTHING, false is returned below
      }

      return false;
    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
  }

  private boolean isBoolean(Type pType) {
    if (pType instanceof CType) {
      CType canonicalType = ((CType) pType).getCanonicalType();

      return canonicalType instanceof CSimpleType
          && ((CSimpleType) canonicalType).getType() == CBasicType.BOOL;
    }

    if(pType instanceof JSimpleType) {
      return ((JSimpleType)pType).getType() == JBasicType.BOOLEAN;
    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
  }

  private SymbolicExpression getZeroConstant(Type pType) {
    return expressionFactory.asConstant(new NumericValue(0L), pType);
  }

  private SymbolicExpression getTrueValueConstant() {
    return expressionFactory.asConstant(BooleanValue.valueOf(true), JSimpleType.getBoolean());
  }

  private Type getCalculationType(Type pType) {
    if (pType instanceof CType) {
      return getCCalculationType((CType) pType);
    } else if (pType instanceof JType) {
      return getJCalculationType((JType) pType);
    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
  }

  private Type getCCalculationType(CType pType) {
    CType canonicalType = pType.getCanonicalType();

    if (canonicalType instanceof CSimpleType) {
      switch (((CSimpleType)canonicalType).getType()) {
        case CHAR:
        case INT:
          return CNumericTypes.SIGNED_INT;
        default:
          return pType;
      }
    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
  }

  private Type getJCalculationType(JType pType) {
    return pType;
  }

  private void checkForMissingInfo(ExpressionTransformer pTransformer) {
    missingInformation |= pTransformer.hasMissingInformation();
  }

  private UnaryConstraint createNot(Constraint pConstraint) {
    // We use ConstraintExpression as Constraints, so this should be possible
    return createNot((SymbolicExpression) pConstraint);
  }

  private UnaryConstraint createNot(SymbolicExpression pSymbolicExpression) {
    return (UnaryConstraint)
        expressionFactory.logicalNot(pSymbolicExpression, pSymbolicExpression.getType());
  }

  private Constraint createLess(SymbolicExpression pLeftOperand, SymbolicExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint)expressionFactory.lessThan(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }

  private Constraint createLessOrEqual(SymbolicExpression pLeftOperand, SymbolicExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint)expressionFactory.lessThanOrEqual(pLeftOperand, pRightOperand, pExpressionType,
        pCalculationType);
  }

  private Constraint createNotEqual(SymbolicExpression pLeftOperand, SymbolicExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint)expressionFactory.notEqual(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }

  private Constraint createEqual(SymbolicExpression pLeftOperand, SymbolicExpression pRightOperand,
      Type pExpressionType, Type pCalculationType) {

    return (Constraint) expressionFactory.equal(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }
}
