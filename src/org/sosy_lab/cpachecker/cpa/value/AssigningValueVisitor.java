/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.Collection;

/**
 * Visitor that derives further information from an assume edge
 */
class AssigningValueVisitor extends ExpressionValueVisitor {

  private ValueAnalysisState assignableState;

  private Collection<String> booleans;

  protected boolean truthValue = false;

  private final ValueTransferOptions options;

  public AssigningValueVisitor(
      ValueAnalysisState assignableState,
      boolean truthValue,
      Collection<String> booleanVariables,
      String functionName,
      ValueAnalysisState state,
      MachineModel machineModel,
      LogManagerWithoutDuplicates logger,
      ValueTransferOptions options) {
    super(state, functionName, machineModel, logger);
    this.assignableState = assignableState;
    this.booleans = booleanVariables;
    this.truthValue = truthValue;
    this.options = options;
  }

  private static AExpression unwrap(AExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    while (expression instanceof CCastExpression) {
      expression = ((CCastExpression) expression).getOperand();
    }

    return expression;
  }

  @Override
  public Value visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
    BinaryOperator binaryOperator = pE.getOperator();
    CExpression lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());
    CExpression rVarInBinaryExp = pE.getOperand2();

    Value leftValue = lVarInBinaryExp.accept(this);
    Value rightValue = rVarInBinaryExp.accept(this);

    if (isEqualityAssumption(binaryOperator)) {
      if (isEligibleForAssignment(leftValue)
          && rightValue.isExplicitlyKnown()
          && isAssignable(lVarInBinaryExp)) {
        assignConcreteValue(
            lVarInBinaryExp, leftValue, rightValue, pE.getOperand2().getExpressionType());

      } else if (isEligibleForAssignment(rightValue)
          && leftValue.isExplicitlyKnown()
          && isAssignable(rVarInBinaryExp)) {
        assignConcreteValue(
            rVarInBinaryExp, rightValue, leftValue, pE.getOperand1().getExpressionType());
      }
    }

    if (isNonEqualityAssumption(binaryOperator)) {
      if (assumingUnknownToBeZero(leftValue, rightValue) && isAssignable(lVarInBinaryExp)) {
        MemoryLocation leftMemLoc = getMemoryLocation(lVarInBinaryExp);

        if (options.isOptimizeBooleanVariables()
            && (booleans.contains(leftMemLoc.getAsSimpleString())
                || options.isInitAssumptionVars())) {
          assignableState.assignConstant(
              leftMemLoc, new NumericValue(1L), pE.getOperand1().getExpressionType());
        }

      } else if (options.isOptimizeBooleanVariables()
          && (assumingUnknownToBeZero(rightValue, leftValue) && isAssignable(rVarInBinaryExp))) {
        MemoryLocation rightMemLoc = getMemoryLocation(rVarInBinaryExp);

        if (booleans.contains(rightMemLoc.getAsSimpleString()) || options.isInitAssumptionVars()) {
          assignableState.assignConstant(
              rightMemLoc, new NumericValue(1L), pE.getOperand2().getExpressionType());
        }
      }
    }

    return super.visit(pE);
  }

  private boolean isEligibleForAssignment(final Value pValue) {
    return pValue.isUnknown()
        || (!pValue.isExplicitlyKnown() && options.isAssignSymbolicAssumptionVars());
  }

  private void assignConcreteValue(
      final CExpression pVarInBinaryExp,
      final Value pOldValue,
      final Value pNewValue,
      final CType pValueType)
      throws UnrecognizedCCodeException {
    if (pOldValue instanceof SymbolicValue) {
      SymbolicIdentifier id = null;

      if (pOldValue instanceof SymbolicIdentifier) {
        id = (SymbolicIdentifier) pOldValue;
      } else if (pOldValue instanceof ConstantSymbolicExpression) {
        Value innerVal = ((ConstantSymbolicExpression) pOldValue).getValue();

        if (innerVal instanceof SymbolicValue) {
          assert innerVal instanceof SymbolicIdentifier;
          id = (SymbolicIdentifier) innerVal;
        }
      }

      if (id != null) {
        assignableState.assignConstant(id, pNewValue);
      }
    }

    assignableState.assignConstant(getMemoryLocation(pVarInBinaryExp), pNewValue, pValueType);
  }

  private static boolean assumingUnknownToBeZero(Value value1, Value value2) {
    return value1.isUnknown() && value2.equals(new NumericValue(BigInteger.ZERO));
  }

  private boolean isEqualityAssumption(BinaryOperator binaryOperator) {
    return (binaryOperator == BinaryOperator.EQUALS && truthValue)
        || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue);
  }

  private boolean isNonEqualityAssumption(BinaryOperator binaryOperator) {
    return (binaryOperator == BinaryOperator.EQUALS && !truthValue)
        || (binaryOperator == BinaryOperator.NOT_EQUALS && truthValue);
  }

  @Override
  public Value visit(JBinaryExpression pE) {
    JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();

    JExpression lVarInBinaryExp = pE.getOperand1();

    lVarInBinaryExp = (JExpression) unwrap(lVarInBinaryExp);

    JExpression rVarInBinaryExp = pE.getOperand2();

    Value leftValueV = lVarInBinaryExp.accept(this);
    Value rightValueV = rVarInBinaryExp.accept(this);

    if ((binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && truthValue)
        || (binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && !truthValue)) {

      if (leftValueV.isUnknown()
          && rightValueV.isExplicitlyKnown()
          && isAssignableVariable(lVarInBinaryExp)) {
        assignValueToState((AIdExpression) lVarInBinaryExp, rightValueV);

      } else if (rightValueV.isUnknown()
          && leftValueV.isExplicitlyKnown()
          && isAssignableVariable(rVarInBinaryExp)) {
        assignValueToState((AIdExpression) rVarInBinaryExp, leftValueV);
      }
    }

    if (options.isInitAssumptionVars()) {
      // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
      // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
      if ((binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && truthValue)
          || (binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && !truthValue)) {

        if (leftValueV.isUnknown()
            && rightValueV.isExplicitlyKnown()
            && isAssignableVariable(lVarInBinaryExp)) {

          // we only want BooleanValue objects for boolean values in the future
          assert rightValueV instanceof BooleanValue;
          BooleanValue booleanValueRight = BooleanValue.valueOf(rightValueV).get();

          if (!booleanValueRight.isTrue()) {
            assignValueToState((AIdExpression) lVarInBinaryExp, BooleanValue.valueOf(true));
          }

        } else if (rightValueV.isUnknown()
            && leftValueV.isExplicitlyKnown()
            && isAssignableVariable(rVarInBinaryExp)) {

          // we only want BooleanValue objects for boolean values in the future
          assert leftValueV instanceof BooleanValue;
          BooleanValue booleanValueLeft = BooleanValue.valueOf(leftValueV).get();

          if (!booleanValueLeft.isTrue()) {
            assignValueToState((AIdExpression) rVarInBinaryExp, BooleanValue.valueOf(true));
          }
        }
      }
    }
    return super.visit(pE);
  }

  // Assign the given value of the given IdExpression to the state of this TransferRelation
  private void assignValueToState(AIdExpression pIdExpression, Value pValue) {
    ASimpleDeclaration declaration = pIdExpression.getDeclaration();

    if (declaration != null) {
      assignableState.assignConstant(declaration.getQualifiedName(), pValue);
    } else {
      MemoryLocation memLoc = MemoryLocation.valueOf(getFunctionName(), pIdExpression.getName());
      assignableState.assignConstant(memLoc, pValue, pIdExpression.getExpressionType());
    }
  }

  private MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCCodeException {
    ExpressionValueVisitor v = getVisitor();
    assert pLValue instanceof CLeftHandSide;
    return checkNotNull(v.evaluateMemoryLocation(pLValue));
  }

  private static boolean isAssignableVariable(JExpression expression) {

    if (expression instanceof JIdExpression) {
      JSimpleDeclaration decl = ((JIdExpression) expression).getDeclaration();

      if (decl == null) {
        return false;
      } else if (decl instanceof JFieldDeclaration) {
        return ((JFieldDeclaration) decl).isStatic();
      } else {
        return true;
      }
    }

    return false;
  }

  private boolean isAssignable(CExpression expression) throws UnrecognizedCCodeException {

    if (expression instanceof CIdExpression) {
      return true;
    }

    if (expression instanceof CFieldReference || expression instanceof CArraySubscriptExpression) {
      ExpressionValueVisitor evv = getVisitor();
      return evv.canBeEvaluated(expression);
    }

    return false;
  }

  /** returns an initialized, empty visitor */
  ExpressionValueVisitor getVisitor() {
    return new ExpressionValueVisitor(
        getState(), getFunctionName(), getMachineModel(), getLogger());
  }
}
