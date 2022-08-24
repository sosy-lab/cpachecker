// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffsetOrSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SMGCPAAssigningValueVisitor extends SMGCPAValueVisitor {

  SMGOptions options;

  boolean truthValue;

  // Tracked boolean variables for non-equal assumptions where we assume these tracked variables as
  // true if the other compared variable is already known as false
  Collection<String> booleans;

  public SMGCPAAssigningValueVisitor(
      SMGCPAValueExpressionEvaluator pEvaluator,
      SMGState currentState,
      CFAEdge edge,
      LogManagerWithoutDuplicates pLogger,
      boolean pTruthValue,
      SMGOptions pOptions,
      Collection<String> booleanVariables) {
    super(pEvaluator, currentState, edge, pLogger);
    booleans = booleanVariables;
    truthValue = pTruthValue;
    options = pOptions;
  }

  @Override
  public List<ValueAndSMGState> visit(CBinaryExpression pE) throws CPATransferException {
    BinaryOperator binaryOperator = pE.getOperator();
    CExpression lVarInBinaryExp = pE.getOperand1();
    CExpression rVarInBinaryExp = pE.getOperand2();
    // Array and composite type comparisons are filtered out by the parser!

    SMGCPAValueExpressionEvaluator evaluator = super.getInitialVisitorEvaluator();
    LogManagerWithoutDuplicates logger = super.getInitialVisitorLogger();
    CFAEdge edge = super.getInitialVisitorCFAEdge();
    SMGState initialState = super.getInitialVisitorState();

    // First get possible assignables from the left and right hand expressions
    // Those will not change, but there could be side effects (thats when they return a state).
    // We only want to know if the right/left hand side values are assignable later on, so we ignore
    // the side effects and assume that the value visitor would find the same!
    List<SMGObjectAndOffsetOrSMGState> leftHandSideAssignments =
        getAssignable(lVarInBinaryExp, initialState);
    List<SMGObjectAndOffsetOrSMGState> rightHandSideAssignments =
        getAssignable(rVarInBinaryExp, initialState);

    ImmutableList.Builder<ValueAndSMGState> finalValueAndStateBuilder = ImmutableList.builder();

    for (ValueAndSMGState leftValueAndState :
        lVarInBinaryExp.accept(new SMGCPAValueVisitor(evaluator, initialState, edge, logger))) {
      Value leftValue = leftValueAndState.getValue();
      for (ValueAndSMGState rightValueAndState :
          rVarInBinaryExp.accept(
              new SMGCPAValueVisitor(evaluator, leftValueAndState.getState(), edge, logger))) {
        Value rightValue = rightValueAndState.getValue();
        SMGState currentState = rightValueAndState.getState();

        // (a == b) case
        if (isEqualityAssumption(binaryOperator)) {
          // Just use the current state as base case (with a unknown value that we wont use)
          List<ValueAndSMGState> updatedStates =
              ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
          // if (true == (unknown == concrete_value)) we set the value (for true left and right)
          if (leftValue.isExplicitlyKnown()) {
            Number lNum = leftValue.asNumericValue().getNumber();
            if (BigInteger.ONE.equals(lNum)) {
              updatedStates =
                  rVarInBinaryExp.accept(
                      new SMGCPAAssigningValueVisitor(
                          evaluator, currentState, edge, logger, truthValue, options, booleans));
            }
          } else if (rightValue.isExplicitlyKnown()) {
            Number rNum = rightValue.asNumericValue().bigInteger();
            if (BigInteger.ONE.equals(rNum)) {
              updatedStates =
                  lVarInBinaryExp.accept(
                      new SMGCPAAssigningValueVisitor(
                          evaluator, currentState, edge, logger, truthValue, options, booleans));
            }
            // TODO: if both left and right are nested we can go 1 step deeper and update first one
            // branch and if that does not update the other branch we can do that branch as well.
            // This is difficult because of different possible equivalence classes.
          }

          SMGState updatedState = currentState;
          // Now we need to use all updated states from the assumptions (or the base case if none
          // was chosen)
          // Never assume values for addresses!! Address equality is only truly checkable
          // by the areNonEqualAddresses() method in the state, which is done by the value visitor.
          for (ValueAndSMGState uselessValueAndupdatedState : updatedStates) {
            updatedState = uselessValueAndupdatedState.getState();
            if (isEligibleForAssignment(leftValue)
                && rightValue.isExplicitlyKnown()
                && !evaluator.isPointerValue(leftValue, updatedState)
                && isAssignable(leftHandSideAssignments, lVarInBinaryExp)) {
              updatedState = replaceValue(leftValue, rightValue, updatedState);

            } else if (isEligibleForAssignment(rightValue)
                && leftValue.isExplicitlyKnown()
                && !evaluator.isPointerValue(rightValue, updatedState)
                && isAssignable(rightHandSideAssignments, rVarInBinaryExp)) {
              updatedState = replaceValue(rightValue, leftValue, updatedState);
            }

            finalValueAndStateBuilder.addAll(
                pE.accept(new SMGCPAValueVisitor(evaluator, updatedState, edge, logger)));
          }

          // We know that if we are in this (equality) case, the inequality case can't be chosen
          // later on
          // The states are set such that now we get the values we want in the value visitor
          continue;
        }

        // !(a == b) case
        if (isNonEqualityAssumption(binaryOperator)) {
          if (assumingUnknownToBeZero(leftValue, rightValue)
              && isAssignable(leftHandSideAssignments, lVarInBinaryExp)) {
            String leftMemLocName = getExtendedQualifiedName((CExpression) unwrap(lVarInBinaryExp));

            if (options.isOptimizeBooleanVariables()
                && (booleans.contains(leftMemLocName) || options.isInitAssumptionVars())) {
              currentState =
                  currentState.copyAndReplaceValueMapping(leftValue, new NumericValue(1L));
            }

          } else if (options.isOptimizeBooleanVariables()
              && (assumingUnknownToBeZero(rightValue, leftValue)
                  && isAssignable(rightHandSideAssignments, rVarInBinaryExp))) {
            String rightMemLocName =
                getExtendedQualifiedName((CExpression) unwrap(rVarInBinaryExp));

            if (booleans.contains(rightMemLocName) || options.isInitAssumptionVars()) {
              currentState =
                  currentState.copyAndReplaceValueMapping(rightValue, new NumericValue(1L));
            }
          }
        }

        // TODO: Negated AND OR ?

        // The states are set such that now we get the values we want in the value visitor
        finalValueAndStateBuilder.addAll(
            pE.accept(new SMGCPAValueVisitor(evaluator, currentState, edge, logger)));
      }
    }

    return finalValueAndStateBuilder.build();
  }

  private static AExpression unwrap(AExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    while (expression instanceof CCastExpression) {
      expression = ((CCastExpression) expression).getOperand();
    }

    return expression;
  }

  private boolean isEqualityAssumption(BinaryOperator binaryOperator) {
    return (binaryOperator == BinaryOperator.EQUALS && truthValue)
        || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue);
  }

  private boolean isNonEqualityAssumption(BinaryOperator binaryOperator) {
    return (binaryOperator == BinaryOperator.EQUALS && !truthValue)
        || (binaryOperator == BinaryOperator.NOT_EQUALS && truthValue);
  }

  /**
   * Returns the list of assignable objects with offsets of the position of the entered CExpression.
   * If the optional is empty (or the list is empty) then there is no assignable object in the
   * expression entered, i.e. a constant.
   *
   * @param expression {@link CExpression}
   * @param currentState {@link SMGState}
   * @return a list of non-empty Optionals in the case of a assignable expression.
   * @throws CPATransferException in case of a critical error i.e. uninitialized variable used.
   */
  private List<SMGObjectAndOffsetOrSMGState> getAssignable(
      CExpression expression, SMGState currentState) throws CPATransferException {
    if (expression instanceof CFieldReference
        || expression instanceof CArraySubscriptExpression
        || expression instanceof CIdExpression
        || expression instanceof CPointerExpression) {
      SMGCPAAddressVisitor av =
          new SMGCPAAddressVisitor(
              super.getInitialVisitorEvaluator(),
              currentState,
              super.getInitialVisitorCFAEdge(),
              super.getInitialVisitorLogger());
      // The exception is only thrown if a invalid statement is detected i.e. usage of undeclared
      // variable. Invalid input like constants are returned as empty optional
      return expression.accept(av);
    }

    return ImmutableList.of(SMGObjectAndOffsetOrSMGState.of(currentState));
  }

  private boolean isAssignable(
      List<SMGObjectAndOffsetOrSMGState> possibleAssignables, CExpression expression) {
    for (SMGObjectAndOffsetOrSMGState possibleAssignable : possibleAssignables) {
      if (possibleAssignable.hasSMGState()) {
        // If there is a state, its either a error or a non existing memory. However, due to CEGAR
        // its possible to not have a memory location while not being blacklisted, so check that.
        if (expression instanceof CIdExpression) {
          CIdExpression cidExpr = (CIdExpression) expression;
          if (cidExpr.getDeclaration() == null) {
            return false;
          }
          String qualName = cidExpr.getDeclaration().getQualifiedName();
          // Initial state is fine! Blacklists don't change in visitor calls
          if (super.getInitialVisitorState().getVariableBlackList().contains(qualName)) {
            throw new RuntimeException(
                "Not handled case of variable declaration in the assigning value visitor.");
          }
        }
        return false;
      }
    }
    return true;
  }

  private boolean isEligibleForAssignment(final Value pValue) {
    // We can't assign memory location carriers. They are indicators for pointers, which are treated
    // like concrete values!
    return !pValue.isExplicitlyKnown()
        && options.isAssignEqualityAssumptions()
        && !(pValue instanceof AddressExpression)
        && !(pValue instanceof SymbolicIdentifier
            && ((SymbolicIdentifier) pValue).getRepresentedLocation().isPresent());
  }

  // TODO: option?
  private static boolean assumingUnknownToBeZero(Value value1, Value value2) {
    return value1.isUnknown() && value2.equals(new NumericValue(BigInteger.ZERO));
  }

  private SMGState replaceValue(Value oldValue, Value newValue, SMGState state) {
    if (options.isAssignEqualityAssumptions()) {
      return state.copyAndReplaceValueMapping(oldValue, newValue);
    } else if (!(oldValue instanceof SymbolicValue)) {
      // Symbolic values should never be replaced by a concrete values
      return state.copyAndReplaceValueMapping(oldValue, newValue);
    }
    return state;
  }

  /**
   * This should only be called on CExpressions representing a variable i.e. CIdExpression CArray...
   * etc. Simply call isAssignable beforehand.
   *
   * @param expr {@link CExpression} you want the qualified name for.
   * @return the qualified name of the variable expression.
   * @throws SMG2Exception in case of a critical error.
   */
  private String getExtendedQualifiedName(CExpression expr) throws SMG2Exception {
    if (expr instanceof CIdExpression) {
      return ((CIdExpression) expr).getName();
    } else if (expr instanceof CArraySubscriptExpression) {
      return ((CArraySubscriptExpression) expr).toQualifiedASTString();
    } else if (expr instanceof CFieldReference) {
      return ((CFieldReference) expr).getFieldOwner().toQualifiedASTString()
          + ((CFieldReference) expr).getFieldName();
    } else if (expr instanceof CPointerExpression) {
      return ((CPointerExpression) expr).toQualifiedASTString();
    }
    throw new SMG2Exception("Internal error when getting the qualified name of a CExpression.");
  }
}
