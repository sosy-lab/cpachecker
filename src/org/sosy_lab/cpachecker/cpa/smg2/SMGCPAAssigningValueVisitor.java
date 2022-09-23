// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
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

          // Now we need to use all updated states from the assumptions (or the base case if none
          // was chosen)
          // Never assume values for addresses!! Address equality is only truly checkable
          // by the areNonEqualAddresses() method in the state, which is done by the value visitor.
          for (ValueAndSMGState uselessValueAndupdatedState : updatedStates) {
            currentState = uselessValueAndupdatedState.getState();

            if (isEligibleForAssignment(leftValue)
                && rightValue.isExplicitlyKnown()
                && !evaluator.isPointerValue(leftValue, currentState)) {

              List<SMGStateAndOptionalSMGObjectAndOffset> leftHandSideAssignments =
                  getAssignable(lVarInBinaryExp, currentState);
              Preconditions.checkArgument(leftHandSideAssignments.size() == 1);
              SMGStateAndOptionalSMGObjectAndOffset leftHandSideAssignment =
                  leftHandSideAssignments.get(0);
              currentState = leftHandSideAssignment.getSMGState();

              if (isAssignable(leftHandSideAssignments)) {

                CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(rVarInBinaryExp);
                BigInteger size = evaluator.getBitSizeof(currentState, type);
                currentState =
                    currentState.writeValueTo(
                        leftHandSideAssignment.getSMGObject(),
                        leftHandSideAssignment.getOffsetForObject(),
                        size,
                        rightValue,
                        type);

              } else if (isEligibleForAssignment(rightValue)
                  && leftValue.isExplicitlyKnown()
                  && !evaluator.isPointerValue(rightValue, currentState)) {

                List<SMGStateAndOptionalSMGObjectAndOffset> rightHandSideAssignments =
                    getAssignable(rVarInBinaryExp, initialState);
                Preconditions.checkArgument(rightHandSideAssignments.size() == 1);
                SMGStateAndOptionalSMGObjectAndOffset rightHandSideAssignment =
                    rightHandSideAssignments.get(0);
                currentState = rightHandSideAssignment.getSMGState();

                if (isAssignable(rightHandSideAssignments)) {

                  CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(lVarInBinaryExp);
                  BigInteger size = evaluator.getBitSizeof(currentState, type);
                  currentState =
                      currentState.writeValueTo(
                          rightHandSideAssignment.getSMGObject(),
                          rightHandSideAssignment.getOffsetForObject(),
                          size,
                          leftValue,
                          type);
                }
              }
            }
          }
        }

        // !(a == b) case
        if (isNonEqualityAssumption(binaryOperator)) {
          if (assumingUnknownToBeZero(leftValue, rightValue)) {
            if (!isNestingHandleable((CExpression) unwrap(lVarInBinaryExp))) {
              continue;
            }
            String leftMemLocName = getExtendedQualifiedName((CExpression) unwrap(lVarInBinaryExp));

            if (options.isOptimizeBooleanVariables()
                && (booleans.contains(leftMemLocName) || options.isInitAssumptionVars())) {
              List<SMGStateAndOptionalSMGObjectAndOffset> leftHandSideAssignments =
                  getAssignable(lVarInBinaryExp, currentState);
              Preconditions.checkArgument(leftHandSideAssignments.size() == 1);
              SMGStateAndOptionalSMGObjectAndOffset leftHandSideAssignment =
                  leftHandSideAssignments.get(0);
              currentState = leftHandSideAssignment.getSMGState();
              if (isAssignable(leftHandSideAssignments)) {

                CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(rVarInBinaryExp);
                BigInteger size = evaluator.getBitSizeof(currentState, type);
                currentState =
                    currentState.writeValueTo(
                        leftHandSideAssignment.getSMGObject(),
                        leftHandSideAssignment.getOffsetForObject(),
                        size,
                        new NumericValue(1L),
                        type);
              }
            }
          }

          if (options.isOptimizeBooleanVariables()
              && assumingUnknownToBeZero(rightValue, leftValue)) {
            if (!isNestingHandleable((CExpression) unwrap(rVarInBinaryExp))) {
              continue;
            }
            String rightMemLocName =
                getExtendedQualifiedName((CExpression) unwrap(rVarInBinaryExp));

            if (booleans.contains(rightMemLocName) || options.isInitAssumptionVars()) {
              List<SMGStateAndOptionalSMGObjectAndOffset> rightHandSideAssignments =
                  getAssignable(rVarInBinaryExp, initialState);
              Preconditions.checkArgument(rightHandSideAssignments.size() == 1);
              SMGStateAndOptionalSMGObjectAndOffset rightHandSideAssignment =
                  rightHandSideAssignments.get(0);
              currentState = rightHandSideAssignment.getSMGState();

              if (isAssignable(rightHandSideAssignments)) {

                CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(lVarInBinaryExp);
                BigInteger size = evaluator.getBitSizeof(currentState, type);
                currentState =
                    currentState.writeValueTo(
                        rightHandSideAssignment.getSMGObject(),
                        rightHandSideAssignment.getOffsetForObject(),
                        size,
                        new NumericValue(1L),
                        type);
              }
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
  private List<SMGStateAndOptionalSMGObjectAndOffset> getAssignable(
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

    return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
  }

  private boolean isAssignable(List<SMGStateAndOptionalSMGObjectAndOffset> possibleAssignables) {
    for (SMGStateAndOptionalSMGObjectAndOffset possibleAssignable : possibleAssignables) {
      if (!possibleAssignable.hasSMGObjectAndOffset()) {
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
    // TODO: assume symbolic as 0 as well if we are not tracking them?
    return value1.isUnknown() && value2.equals(new NumericValue(BigInteger.ZERO));
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

  /**
   * Tests if getExtendedQualifiedName() will succeed.
   *
   * @param expr current CExpression
   * @return true if the expression is handleable by the assigning visitor
   */
  private boolean isNestingHandleable(CExpression expr) {
    if (expr instanceof CBinaryExpression) {
      return isNestingHandleable(((CBinaryExpression) expr).getOperand1())
          && isNestingHandleable(((CBinaryExpression) expr).getOperand2());
    } else {
      return expr instanceof CIdExpression
          || expr instanceof CArraySubscriptExpression
          || expr instanceof CFieldReference
          || expr instanceof CPointerExpression;
    }
  }
}
