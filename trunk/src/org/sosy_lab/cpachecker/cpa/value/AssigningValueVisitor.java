// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Visitor that derives further information from an assume edge */
class AssigningValueVisitor extends ExpressionValueVisitor {

  private ExpressionValueVisitor nonAssigningValueVisitor;

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
    nonAssigningValueVisitor =
        new ExpressionValueVisitor(state, functionName, machineModel, logger);
    this.assignableState = assignableState;
    booleans = booleanVariables;
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
  public Value visit(CBinaryExpression pE) throws UnrecognizedCodeException {
    BinaryOperator binaryOperator = pE.getOperator();
    CExpression lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());
    CExpression rVarInBinaryExp = pE.getOperand2();

    Value leftValue = lVarInBinaryExp.accept(nonAssigningValueVisitor);
    Value rightValue = rVarInBinaryExp.accept(nonAssigningValueVisitor);

    if (isEqualityAssumption(binaryOperator)) {
      if (leftValue.isExplicitlyKnown()) {
        Number lNum = leftValue.asNumericValue().getNumber();
        if (BigInteger.ONE.equals(lNum)) {
          rVarInBinaryExp.accept(this);
        }
      } else if (rightValue.isExplicitlyKnown()) {
        Number rNum = rightValue.asNumericValue().getNumber();
        if (BigInteger.ONE.equals(rNum)) {
          lVarInBinaryExp.accept(this);
        }
      }

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
            && (booleans.contains(leftMemLoc.getExtendedQualifiedName())
                || options.isInitAssumptionVars())) {
          assignableState.assignConstant(
              leftMemLoc, new NumericValue(1L), pE.getOperand1().getExpressionType());
        }

      } else if (options.isOptimizeBooleanVariables()
          && (assumingUnknownToBeZero(rightValue, leftValue) && isAssignable(rVarInBinaryExp))) {
        MemoryLocation rightMemLoc = getMemoryLocation(rVarInBinaryExp);

        if (booleans.contains(rightMemLoc.getExtendedQualifiedName())
            || options.isInitAssumptionVars()) {
          assignableState.assignConstant(
              rightMemLoc, new NumericValue(1L), pE.getOperand2().getExpressionType());
        }
      }
    }

    return nonAssigningValueVisitor.visit(pE);
  }

  private boolean isEligibleForAssignment(final Value pValue) {
    return pValue.isUnknown() && options.isAssignEqualityAssumptions();
  }

  private void assignConcreteValue(
      final CExpression pVarInBinaryExp,
      final Value pOldValue,
      final Value pNewValue,
      final CType pValueType)
      throws UnrecognizedCodeException {
    checkState(
        !(pOldValue instanceof SymbolicValue),
        "Symbolic values should never be replaced by a concrete value");

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

    Value leftValueV = lVarInBinaryExp.accept(nonAssigningValueVisitor);
    Value rightValueV = rVarInBinaryExp.accept(nonAssigningValueVisitor);

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
          BooleanValue booleanValueRight = BooleanValue.valueOf(rightValueV).orElseThrow();

          if (!booleanValueRight.isTrue()) {
            assignValueToState((AIdExpression) lVarInBinaryExp, BooleanValue.valueOf(true));
          }

        } else if (rightValueV.isUnknown()
            && leftValueV.isExplicitlyKnown()
            && isAssignableVariable(rVarInBinaryExp)) {

          // we only want BooleanValue objects for boolean values in the future
          assert leftValueV instanceof BooleanValue;
          BooleanValue booleanValueLeft = BooleanValue.valueOf(leftValueV).orElseThrow();

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
      MemoryLocation memLoc =
          MemoryLocation.forLocalVariable(getFunctionName(), pIdExpression.getName());
      assignableState.assignConstant(memLoc, pValue, pIdExpression.getExpressionType());
    }
  }

  private MemoryLocation getMemoryLocation(CExpression pLValue) throws UnrecognizedCodeException {
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

  private boolean isAssignable(CExpression expression) throws UnrecognizedCodeException {

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
    if (options.isIgnoreFunctionValue()) {
      return new ExpressionValueVisitor(
          getState(), getFunctionName(), getMachineModel(), getLogger());
    } else {
      return new FunctionPointerExpressionValueVisitor(
          getState(), getFunctionName(), getMachineModel(), getLogger());
    }
  }
}
