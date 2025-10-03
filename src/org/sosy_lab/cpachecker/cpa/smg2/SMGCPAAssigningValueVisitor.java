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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult.Satisfiability;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstraintAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

public class SMGCPAAssigningValueVisitor extends SMGCPAValueVisitor {

  private final ConstraintsSolver solver;

  private final boolean truthValue;

  // Tracked boolean variables for non-equal assumptions where we assume these tracked variables as
  // true if the other compared variable is already known as false
  private final Collection<String> booleans;

  /** The function BEFORE the current edge */
  private final String callerFunctionName;

  public SMGCPAAssigningValueVisitor(
      SMGCPAExpressionEvaluator pEvaluator,
      ConstraintsSolver pSolver,
      SMGState currentState,
      CFAEdge edge,
      LogManagerWithoutDuplicates pLogger,
      boolean pTruthValue,
      SMGOptions pOptions,
      Collection<String> booleanVariables,
      String pCallerFunctionName) {
    super(pEvaluator, currentState, edge, pLogger, pOptions);
    booleans = booleanVariables;
    truthValue = pTruthValue;
    solver = pSolver;
    callerFunctionName = pCallerFunctionName;
  }

  // Assumes that it was already checked if the expression can be evaluated to a boolean expression,
  // but allows it if it's not.
  // Returns null for paths that are not feasible, else a list of states that are feasible.
  // The returned values can be ignored.
  @Override
  public List<ValueAndSMGState> visit(CBinaryExpression pE) throws CPATransferException {
    BinaryOperator binaryOperator = pE.getOperator();
    CExpression lVarInBinaryExp = pE.getOperand1();
    CExpression rVarInBinaryExp = pE.getOperand2();
    // Array and composite type comparisons are filtered out by the parser!

    SMGCPAExpressionEvaluator evaluator = super.getInitialVisitorEvaluator();
    LogManagerWithoutDuplicates logger = super.getInitialVisitorLogger();
    CFAEdge edge = super.getInitialVisitorCFAEdge();
    SMGState initialState = super.getInitialVisitorState();

    ImmutableList.Builder<ValueAndSMGState> finalValueAndStateBuilder = ImmutableList.builder();

    for (ValueAndSMGState leftValueAndState :
        lVarInBinaryExp.accept(
            new SMGCPAValueVisitor(
                evaluator, initialState, edge, logger, getInitialVisitorOptions()))) {
      Value leftValue = leftValueAndState.getValue();
      for (ValueAndSMGState rightValueAndState :
          rVarInBinaryExp.accept(
              new SMGCPAValueVisitor(
                  evaluator,
                  leftValueAndState.getState(),
                  edge,
                  logger,
                  getInitialVisitorOptions()))) {
        Value rightValue = rightValueAndState.getValue();
        SMGState currentState = rightValueAndState.getState();

        List<SMGState> handledStates = ImmutableList.of(currentState);

        // We handle non-equality assumptions a little differently, see below
        if (!isNonEqualityAssumption(binaryOperator)) {
          // TODO: might be inefficient due to multiple checks for nested equality/inequality
          // expressions
          // Check SAT and add constraints if eligible (the method returns the initial state if we
          // don't
          // use a solver in this analysis. But also might return null for UNSAT)
          Optional<SMGState> maybeStatesWithConstraints =
              addConstraintsAndCheckSat(currentState, pE);

          if (maybeStatesWithConstraints.isEmpty()) {
            // Don't add any states as we know its UNSAT
            continue;
          }

          // SAT, try to assign values
          SMGState stateWithConstraints = maybeStatesWithConstraints.orElseThrow();

          // (a == b) case
          if (isEqualityAssumption(binaryOperator)) {
            handledStates =
                handleEqualityAssumption(
                    lVarInBinaryExp,
                    leftValue,
                    rVarInBinaryExp,
                    rightValue,
                    stateWithConstraints,
                    edge);
          } else {
            handledStates = ImmutableList.of(stateWithConstraints);
          }
        }

        // !(a == b) case
        if (isNonEqualityAssumption(binaryOperator)) {
          // SAT check is performed inside
          handledStates =
              handleInEqualityAssumption(
                  pE, lVarInBinaryExp, leftValue, rVarInBinaryExp, rightValue, currentState, edge);
        }

        for (SMGState handledState : handledStates) {
          // The states are set such that now we get the values we want in the value visitor
          finalValueAndStateBuilder.addAll(
              pE.accept(
                  new SMGCPAValueVisitor(
                      evaluator, handledState, edge, logger, getInitialVisitorOptions())));
        }
      }
    }

    ImmutableList<ValueAndSMGState> returnList = finalValueAndStateBuilder.build();
    return returnList.isEmpty() ? null : returnList;
  }

  /**
   * Handles equality assumptions. Examples: a == b, possibly nested expressions, e.g. (a==b)==c,
   * possibly compared to a known value e.g. a==0 Will use a SAT check if we track predicates and
   * allow SAT checks. Will add the constraints of SAT constraints in non-trivial cases to the
   * states. Will also try to assign values for assumptions. Examples: for value analysis it might
   * just assume that the expression can be fulfilled by overapproximation and assigns a value for
   * assumptions with a constant. This might also happen after a SAT check, but then it is actually
   * precise.
   */
  @Nullable
  private List<SMGState> handleEqualityAssumption(
      CExpression lVarInBinaryExp,
      Value leftValue,
      CExpression rVarInBinaryExp,
      Value rightValue,
      SMGState initialState,
      CFAEdge edge)
      throws CPATransferException {
    SMGCPAExpressionEvaluator evaluator = super.getInitialVisitorEvaluator();
    ImmutableList.Builder<SMGState> retStatesBuilder = ImmutableList.builder();

    // Now we need to use all updated states from the assumptions (or the base case if none
    // was chosen)
    // Never assume values for addresses!! Address equality is only truly checkable
    // by the areNonEqualAddresses() method in the state, which is done by the value visitor.
    for (ValueAndSMGState uselessValueAndUpdatedState :
        updateNested(lVarInBinaryExp, leftValue, rVarInBinaryExp, rightValue, initialState)) {
      SMGState currentState = uselessValueAndUpdatedState.getState();

      if (isEligibleForAssignment(leftValue, currentState) && rightValue.isExplicitlyKnown()) {

        // e.g. x == 0 or x == 1
        List<SMGStateAndOptionalSMGObjectAndOffset> leftHandSideAssignments =
            getAssignable(lVarInBinaryExp, currentState);
        Preconditions.checkArgument(leftHandSideAssignments.size() == 1);
        SMGStateAndOptionalSMGObjectAndOffset leftHandSideAssignment =
            leftHandSideAssignments.getFirst();
        currentState = leftHandSideAssignment.getSMGState();

        if (isAssignable(leftHandSideAssignments)) {

          CType lType = SMGCPAExpressionEvaluator.getCanonicalType(lVarInBinaryExp);
          Value size = evaluator.getBitSizeof(currentState, lType, edge);
          if (!SMGCPAExpressionEvaluator.getCanonicalType(rVarInBinaryExp).equals(lType)) {
            // Cast first
            ValueAndSMGState newRightValueAndState = castCValue(rightValue, lType, currentState);
            rightValue = newRightValueAndState.getValue();
            currentState = newRightValueAndState.getState();
          }
          // Replace mapping of the SMGValue with the new value!
          currentState =
              currentState.writeValueWithChecks(
                  leftHandSideAssignment.getSMGObject(),
                  leftHandSideAssignment.getOffsetForObject(),
                  size,
                  rightValue,
                  lType,
                  edge);

        } else if (isEligibleForAssignment(rightValue, currentState)
            && leftValue.isExplicitlyKnown()) {

          // e.g. 0 == x or 1 == x
          List<SMGStateAndOptionalSMGObjectAndOffset> rightHandSideAssignments =
              getAssignable(rVarInBinaryExp, currentState);
          Preconditions.checkArgument(rightHandSideAssignments.size() == 1);
          SMGStateAndOptionalSMGObjectAndOffset rightHandSideAssignment =
              rightHandSideAssignments.getFirst();
          currentState = rightHandSideAssignment.getSMGState();

          if (isAssignable(rightHandSideAssignments)) {

            CType rType = SMGCPAExpressionEvaluator.getCanonicalType(lVarInBinaryExp);
            Value size = evaluator.getBitSizeof(currentState, rType, edge);

            if (!SMGCPAExpressionEvaluator.getCanonicalType(lVarInBinaryExp).equals(rType)) {
              // Cast first
              ValueAndSMGState newRightValueAndState = castCValue(leftValue, rType, currentState);
              leftValue = newRightValueAndState.getValue();
              currentState = newRightValueAndState.getState();
            }

            currentState =
                currentState.writeValueWithChecks(
                    rightHandSideAssignment.getSMGObject(),
                    rightHandSideAssignment.getOffsetForObject(),
                    size,
                    leftValue,
                    rType,
                    edge);
          }
        }
      }
      // We might not be able to assign anything, but the assumption is fulfilled
      retStatesBuilder.add(currentState);
    }

    return retStatesBuilder.build();
  }

  /**
   * Handles inequality assumptions. Examples: a != b or !(a==b), possibly nested expressions, e.g.
   * !((a==b)==c), possibly compared to a known value e.g. !(a==0) Will use a SAT check if we track
   * predicates and allow SAT checks. Will add the constraints of SAT constraints in non-trivial
   * cases to the states. Will also try to assign values for assumptions. Examples: for value
   * analysis it might just assume that the expression can be fulfilled by overapproximation and
   * assigns a value. Since this is an inequality, this usually only happens for boolean variables.
   * This might also happen after a SAT check, but then it is actually precise.
   */
  private List<SMGState> handleInEqualityAssumption(
      CBinaryExpression originalExpr,
      CExpression lVarInBinaryExp,
      Value leftValue,
      CExpression rVarInBinaryExp,
      Value rightValue,
      SMGState initialState,
      CFAEdge edge)
      throws CPATransferException {
    SMGState currentState = initialState;
    SMGCPAExpressionEvaluator evaluator = super.getInitialVisitorEvaluator();

    // We choose our constraints depending on possible simplifications.
    // Often we compare against 0 like this: !(x == 0), and x is only used as boolean variable.
    // Now we could assign 1 to a variable x for example,
    // we choose the constraint to be x == 1 instead of x != 0. This has to be SAT of course,
    // or we are in value analysis.
    if (isAssumptionComparedToZero(rightValue)) {
      // !(x == 0) or x != 0
      if (isEligibleForAssignment(leftValue, currentState)) {
        if (isNestingHandleable((CExpression) unwrap(lVarInBinaryExp))) {
          List<SMGStateAndOptionalSMGObjectAndOffset> leftHandSideAssignments =
              getAssignable(lVarInBinaryExp, currentState);
          Preconditions.checkArgument(leftHandSideAssignments.size() == 1);
          SMGStateAndOptionalSMGObjectAndOffset leftHandSideAssignment =
              leftHandSideAssignments.getFirst();
          currentState = leftHandSideAssignment.getSMGState();
          if (isAssignable(leftHandSideAssignments)
              && getInitialVisitorOptions().isOptimizeBooleanVariables()) {
            String leftMemLocName = getExtendedQualifiedName((CExpression) unwrap(lVarInBinaryExp));

            if (booleans.contains(leftMemLocName)
                || getInitialVisitorOptions().isInitAssumptionVars()) {

              // The constraint changed from the CExpression to x == 1
              Optional<SMGState> maybeStatesWithConstraints =
                  addConstraintsAndCheckSat(currentState, modifyInEqualityToEquality(originalExpr));

              if (maybeStatesWithConstraints.isEmpty()) {
                // Don't add any states as we know its UNSAT
                return ImmutableList.of();
              }

              // SAT, try to assign values
              SMGState stateToAssign = maybeStatesWithConstraints.orElseThrow();

              if (leftValue instanceof ConstantSymbolicExpression
                  && currentState.getNumberOfValueUsages(leftValue) == 1
                  && !currentState.valueContainedInConstraints(leftValue)) {
                // Its SAT, but we don't need to remember the constraint
                stateToAssign = currentState;
              }

              CType type = SMGCPAExpressionEvaluator.getCanonicalType(lVarInBinaryExp);
              Value size = evaluator.getBitSizeof(stateToAssign, type, edge);
              stateToAssign =
                  stateToAssign.writeValueWithChecks(
                      leftHandSideAssignment.getSMGObject(),
                      leftHandSideAssignment.getOffsetForObject(),
                      size,
                      new NumericValue(1L),
                      type,
                      edge);

              return ImmutableList.of(stateToAssign);
            }
          }
        }
      }
    } else if (isAssumptionComparedToZero(leftValue)) {
      // 0 != x or !(0 == x)
      if (isEligibleForAssignment(rightValue, currentState)
          && leftValue.isExplicitlyKnown()
          && !evaluator.isPointerValue(rightValue, currentState)
          && getInitialVisitorOptions().isOptimizeBooleanVariables()) {
        if (isNestingHandleable((CExpression) unwrap(rVarInBinaryExp))) {
          String rightMemLocName = getExtendedQualifiedName((CExpression) unwrap(rVarInBinaryExp));

          if (booleans.contains(rightMemLocName)
              || getInitialVisitorOptions().isInitAssumptionVars()) {
            List<SMGStateAndOptionalSMGObjectAndOffset> rightHandSideAssignments =
                getAssignable(rVarInBinaryExp, currentState);
            Preconditions.checkArgument(rightHandSideAssignments.size() == 1);
            SMGStateAndOptionalSMGObjectAndOffset rightHandSideAssignment =
                rightHandSideAssignments.getFirst();
            currentState = rightHandSideAssignment.getSMGState();

            if (isAssignable(rightHandSideAssignments)) {

              // Constraint changed
              Optional<SMGState> maybeStatesWithConstraints =
                  addConstraintsAndCheckSat(currentState, modifyInEqualityToEquality(originalExpr));

              if (maybeStatesWithConstraints.isEmpty()) {
                // Don't add any states as we know its UNSAT
                return ImmutableList.of();
              }

              // SAT, try to assign values
              SMGState stateToAssign = maybeStatesWithConstraints.orElseThrow();

              if (rightValue instanceof ConstantSymbolicExpression
                  && currentState.getNumberOfValueUsages(rightValue) == 1
                  && !currentState.valueContainedInConstraints(rightValue)) {
                // Its SAT, but we don't need to remember the constraint
                stateToAssign = currentState;
              }

              CType type = SMGCPAExpressionEvaluator.getCanonicalType(rVarInBinaryExp);
              Value size = evaluator.getBitSizeof(stateToAssign, type, edge);
              stateToAssign =
                  stateToAssign.writeValueWithChecks(
                      rightHandSideAssignment.getSMGObject(),
                      rightHandSideAssignment.getOffsetForObject(),
                      size,
                      new NumericValue(1L),
                      type,
                      edge);

              return ImmutableList.of(stateToAssign);
            }
          }
        }
      }
    }
    // TODO: add nesting update here as well, or pull it out and do it before assigning anything

    // TODO: might be inefficient due to multiple checks for nested equality/inequality
    // expressions
    // Check SAT and add constraints if eligible (the method returns the initial state if we
    // don't
    // use a solver in this analysis. But also might return null for UNSAT)
    // The expression/constraint we add might be changed, depending on the above
    Optional<SMGState> maybeStatesWithConstraints =
        addConstraintsAndCheckSat(currentState, originalExpr);

    if (maybeStatesWithConstraints.isEmpty()) {
      // Don't add any states as we know its UNSAT
      return ImmutableList.of();
    }

    // SAT but nothing to assign
    return ImmutableList.of(maybeStatesWithConstraints.orElseThrow());
  }

  private CBinaryExpression modifyInEqualityToEquality(CBinaryExpression inequalityExpr) {
    // We expect an expression of the form !(a == b) with a or b being 0, to be switched to 1
    BinaryOperator binaryOperator = inequalityExpr.getOperator();
    CExpression lVarInBinaryExp = inequalityExpr.getOperand1();
    CExpression rVarInBinaryExp = inequalityExpr.getOperand2();
    CIntegerLiteralExpression oneIntLiteral = CIntegerLiteralExpression.ONE;
    if (binaryOperator == BinaryOperator.EQUALS && !truthValue) {
      if (lVarInBinaryExp instanceof CIntegerLiteralExpression lIntLit
          && lIntLit.getValue().equals(BigInteger.ZERO)) {
        return new CBinaryExpression(
            inequalityExpr.getFileLocation(),
            inequalityExpr.getExpressionType(),
            inequalityExpr.getCalculationType(),
            oneIntLiteral,
            rVarInBinaryExp,
            BinaryOperator.NOT_EQUALS);
      } else {
        Preconditions.checkArgument(
            rVarInBinaryExp instanceof CIntegerLiteralExpression rIntLit
                && rIntLit.getValue().equals(BigInteger.ZERO));
        return new CBinaryExpression(
            inequalityExpr.getFileLocation(),
            inequalityExpr.getExpressionType(),
            inequalityExpr.getCalculationType(),
            lVarInBinaryExp,
            oneIntLiteral,
            BinaryOperator.NOT_EQUALS);
      }
    } else {
      Preconditions.checkArgument(binaryOperator == BinaryOperator.NOT_EQUALS && truthValue);
      if (lVarInBinaryExp instanceof CIntegerLiteralExpression lIntLit
          && lIntLit.getValue().equals(BigInteger.ZERO)) {
        return new CBinaryExpression(
            inequalityExpr.getFileLocation(),
            inequalityExpr.getExpressionType(),
            inequalityExpr.getCalculationType(),
            oneIntLiteral,
            rVarInBinaryExp,
            BinaryOperator.EQUALS);
      } else {
        Preconditions.checkArgument(
            rVarInBinaryExp instanceof CIntegerLiteralExpression rIntLit
                && rIntLit.getValue().equals(BigInteger.ZERO));
        return new CBinaryExpression(
            inequalityExpr.getFileLocation(),
            inequalityExpr.getExpressionType(),
            inequalityExpr.getCalculationType(),
            lVarInBinaryExp,
            oneIntLiteral,
            BinaryOperator.EQUALS);
      }
    }
  }

  /**
   * Handles nested assumptions. e.g. ((a != b) == c) by appyling this visitor (visit) on the nexted
   * expressions.
   */
  private List<ValueAndSMGState> updateNested(
      CExpression lVarInBinaryExp,
      Value leftValue,
      CExpression rVarInBinaryExp,
      Value rightValue,
      SMGState currentState)
      throws CPATransferException {
    SMGCPAExpressionEvaluator evaluator = super.getInitialVisitorEvaluator();
    LogManagerWithoutDuplicates logger = super.getInitialVisitorLogger();
    CFAEdge edge = super.getInitialVisitorCFAEdge();
    // Just use the current state as base case (with an unknown value that we won't use)
    List<ValueAndSMGState> updatedStates =
        ImmutableList.of(ValueAndSMGState.ofUnknownValue(currentState));
    // if (true == (unknown == concrete_value)) we set the value (for true left and right)
    if (leftValue.isExplicitlyKnown()) {
      Number lNum = leftValue.asNumericValue().getNumber();
      if (BigInteger.ONE.equals(lNum)) {
        updatedStates =
            rVarInBinaryExp.accept(
                new SMGCPAAssigningValueVisitor(
                    evaluator,
                    solver,
                    currentState,
                    edge,
                    logger,
                    truthValue,
                    getInitialVisitorOptions(),
                    booleans,
                    callerFunctionName));
      }
    } else if (rightValue.isExplicitlyKnown()) {
      Number rNum = rightValue.asNumericValue().bigIntegerValue();
      if (BigInteger.ONE.equals(rNum)) {
        updatedStates =
            lVarInBinaryExp.accept(
                new SMGCPAAssigningValueVisitor(
                    evaluator,
                    solver,
                    currentState,
                    edge,
                    logger,
                    truthValue,
                    getInitialVisitorOptions(),
                    booleans,
                    callerFunctionName));
      }
    }
    return updatedStates;
  }

  private static AExpression unwrap(AExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    while (expression instanceof CCastExpression) {
      expression = ((CCastExpression) expression).getOperand();
    }

    return expression;
  }

  private boolean isEqualityAssumption(BinaryOperator binaryOperator) {
    return truthValue
        ? binaryOperator == BinaryOperator.EQUALS
        : binaryOperator == BinaryOperator.NOT_EQUALS;
  }

  private boolean isNonEqualityAssumption(BinaryOperator binaryOperator) {
    return truthValue
        ? binaryOperator == BinaryOperator.NOT_EQUALS
        : binaryOperator == BinaryOperator.EQUALS;
  }

  /**
   * Returns the list of assignable objects with offsets of the position of the entered CExpression.
   * If the optional is empty (or the list is empty) then there is no assignable object in the
   * expression entered, i.e. a constant.
   *
   * @param expression {@link CExpression}
   * @param currentState {@link SMGState}
   * @return a list of non-empty Optionals in the case of an assignable expression.
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
              super.getInitialVisitorLogger(),
              getInitialVisitorOptions());
      // The exception is only thrown if an invalid statement is detected i.e. usage of undeclared
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

  private boolean isEligibleForAssignment(final Value pValue, SMGState currentState) {
    // Make sure that we only assign to expressions that are assignable, for example a single
    // symbolic
    // Make sure that we don't assign to symbolic values with constraints preventing assignments
    // We can't assign memory location carriers. They are indicators for pointers, which are treated
    // like unknown but concrete values!
    return !pValue.isExplicitlyKnown()
        && getInitialVisitorOptions().isAssignEqualityAssumptions()
        && (!getInitialVisitorOptions().trackPredicates()
            || currentState.getNumberOfValueUsages(pValue) == 1)
        && !(pValue instanceof AddressExpression)
        && !currentState.getMemoryModel().isPointer(pValue)
        && (!(pValue instanceof SymbolicIdentifier symIdent)
            || !symIdent.getRepresentedLocation().isPresent());
  }

  // TODO: option?
  private static boolean isAssumptionComparedToZero(Value value) {
    return value.equals(new NumericValue(BigInteger.ZERO));
  }

  /**
   * This should only be called on CExpressions representing a variable i.e. CIdExpression CArray...
   * etc. Simply call isAssignable beforehand.
   *
   * @param expr {@link CExpression} you want the qualified name for.
   * @return the qualified name of the variable expression.
   * @throws SMGException in case of a critical error.
   */
  private String getExtendedQualifiedName(CExpression expr) throws SMGException {
    if (expr instanceof CIdExpression cIdExpression) {
      return cIdExpression.getDeclaration().getQualifiedName();
    } else if (expr instanceof CArraySubscriptExpression) {
      return expr.toQualifiedASTString();
    } else if (expr instanceof CFieldReference cFieldReference) {
      return cFieldReference.getFieldOwner().toQualifiedASTString()
          + cFieldReference.getFieldName();
    } else if (expr instanceof CPointerExpression) {
      return expr.toQualifiedASTString();
    }
    throw new SMGException("Internal error when getting the qualified name of a CExpression.");
  }

  /**
   * Tests if getExtendedQualifiedName() will succeed.
   *
   * @param expr current CExpression
   * @return whether the expression is handleable by the assigning visitor
   */
  private boolean isNestingHandleable(CExpression expr) {
    if (expr instanceof CBinaryExpression cBinaryExpression) {
      return isNestingHandleable(cBinaryExpression.getOperand1())
          && isNestingHandleable(cBinaryExpression.getOperand2());
    } else {
      return expr instanceof CIdExpression
          || expr instanceof CArraySubscriptExpression
          || expr instanceof CFieldReference
          || expr instanceof CPointerExpression;
    }
  }

  /**
   * Checks satisfiablity for the given CExpression on the given SMGState (existing constraints).
   * Adds the constraints to the state(s) if SAT and returns the state(s). If there is no SAT state,
   * returns an empty Optional. Returns the initial, unchanged state if we don't track predicates.
   */
  private Optional<SMGState> addConstraintsAndCheckSat(
      SMGState initialState, CExpression cExpression) throws CPATransferException {
    ImmutableList.Builder<SMGState> resultStateBuilder = ImmutableList.builder();
    try {
      if (getInitialVisitorOptions().trackPredicates()) {
        // Symbolic Execution for assumption edges
        Collection<SMGState> statesWithConstraints =
            computeNewStateByCreatingConstraint(
                initialState, cExpression, truthValue, getInitialVisitorCFAEdge());
        Preconditions.checkArgument(statesWithConstraints.size() == 1);

        for (SMGState stateWithConstraint : statesWithConstraints) {
          if (getInitialVisitorOptions().isSatCheckStrategyAtAssume()) {
            SolverResult solverResult =
                solver.checkUnsatWithOptionDefinedSolverReuse(
                    stateWithConstraint.getConstraints(), callerFunctionName);
            if (solverResult.satisfiability().equals(Satisfiability.SAT)) {
              resultStateBuilder.add(
                  stateWithConstraint.replaceModelAndDefAssignmentAndCopy(
                      solverResult.definiteAssignments(), solverResult.model()));
            }
            // We might add/return nothing here if the check was UNSAT
          } else {
            // If either we don't check SAT or the path is SAT we return the state
            resultStateBuilder.add(stateWithConstraint);
          }
        }
      } else {
        // return ImmutableList.of(currentState);
        return Optional.of(initialState);
      }
      // TODO: replace this ugliness once we know its only ever 1 returned state
      ImmutableList<SMGState> returnList = resultStateBuilder.build();
      return returnList.isEmpty() ? Optional.empty() : Optional.of(returnList.getFirst());

    } catch (InterruptedException | SolverException e) {
      throw new SMGSolverException(e, initialState);
    }
  }

  // ######### Constraint creation for Symbolic Execution #########
  private Collection<SMGState> computeNewStateByCreatingConstraint(
      final SMGState pOldState,
      final AExpression pExpression,
      final boolean pTruthAssumption,
      CFAEdge pEdge)
      throws CPATransferException, SolverException, InterruptedException {

    if (pExpression instanceof CBinaryExpression binExpr
        && !ConstraintFactory.binaryExpressionIsConstraint(binExpr)) {
      // For example an expression of the kind array[i] % 2 == 1 is split and array[i] % 2 ends up
      // here and would fail below.
      return ImmutableList.of(pOldState);
    }

    final ConstraintFactory constraintFactory =
        ConstraintFactory.getInstance(
            pOldState,
            getInitialVisitorEvaluator().getMachineModel(),
            getInitialVisitorLogger(),
            getInitialVisitorOptions(),
            getInitialVisitorEvaluator(),
            pEdge);

    // final String functionName = pEdge.getPredecessor().getFunctionName();
    // The constraints are not yet in the state here!
    Collection<ConstraintAndSMGState> newConstraintsAndStates =
        createConstraint(pExpression, constraintFactory, pTruthAssumption);

    ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
    for (ConstraintAndSMGState newConstraintAndState : newConstraintsAndStates) {
      final Constraint newConstraint = newConstraintAndState.getConstraint();
      SMGState currentState = newConstraintAndState.getState();

      // If a constraint is trivial, its satisfiability is not influenced by other constraints.
      // So to evade more expensive SAT checks, we just check the constraint on its own.
      // TODO: is this still correct for more than one returned constraint? I.e. can a trivial
      //   constraint be non-trivial with a second constraint?
      if (newConstraint.isTrivial()) {
        if (solver
            .checkUnsatWithFreshSolver(newConstraint, callerFunctionName)
            .equals(Satisfiability.SAT)) {
          // Iff SAT -> we go that path with this state
          // We don't add the constraint as it is trivial
          stateBuilder.add(currentState);
        }
      } else {
        stateBuilder.add(currentState.addConstraint(newConstraint));
      }
    }
    ImmutableList<SMGState> newStates = stateBuilder.build();

    if (newStates.isEmpty()) {
      return null;
    }

    return newStates;
  }

  private Collection<ConstraintAndSMGState> createConstraint(
      AExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws CPATransferException {

    if (pExpression instanceof CBinaryExpression cBinaryExpression) {
      return createConstraint(cBinaryExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof CIdExpression cIdExpression) {
      // id expressions in assume edges are created by a call of __VERIFIER_assume(x), for example
      return createConstraint(cIdExpression, pFactory, pTruthAssumption);

    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }
  }

  private Collection<ConstraintAndSMGState> createConstraint(
      CBinaryExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws CPATransferException {

    if (pTruthAssumption) {
      return pFactory.createPositiveConstraint(pExpression);
    } else {
      return pFactory.createNegativeConstraint(pExpression);
    }
  }

  // Unneeded/Useless constraints have already been filtered out.
  // The Constraints only need to be combined with the states now
  private Collection<ConstraintAndSMGState> createConstraint(
      CIdExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws CPATransferException {
    Collection<ConstraintAndSMGState> constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }
    return constraint.stream()
        .filter(cas -> cas.getConstraint() != null)
        .collect(ImmutableList.toImmutableList());
  }
}
