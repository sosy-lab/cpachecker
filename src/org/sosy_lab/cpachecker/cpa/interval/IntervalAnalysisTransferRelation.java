// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class IntervalAnalysisTransferRelation
    extends ForwardingTransferRelation<
        Collection<IntervalAnalysisState>, IntervalAnalysisState, Precision> {

  private final boolean splitIntervals;
  private final int threshold;
  private final LogManager logger;

  public IntervalAnalysisTransferRelation(
      boolean pSplitIntervals, int pThreshold, LogManager pLogger) {
    splitIntervals = pSplitIntervals;
    threshold = pThreshold;
    logger = pLogger;
  }

  @Override
  protected Collection<IntervalAnalysisState> postProcessing(
      Collection<IntervalAnalysisState> successors, CFAEdge edge) {
    return new HashSet<>(successors);
  }

  @Override
  protected Collection<IntervalAnalysisState> handleBlankEdge(BlankEdge cfaEdge) {
    IntervalAnalysisState newState = state;
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      assert "default return".equals(cfaEdge.getDescription())
          || "skipped unnecessary edges".equals(cfaEdge.getDescription());

      // delete variables from returning function,
      // we do not need them after this location, because the next edge is the functionReturnEdge.
      newState = newState.dropFrame(functionName);
    }

    return soleSuccessor(newState);
  }

  /**
   * Handles return from one function to another function.
   *
   * @param cfaEdge return edge from a function to its call site.
   * @return new abstract state.
   */
  @Override
  protected Collection<IntervalAnalysisState> handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws UnrecognizedCodeException {

    IntervalAnalysisState newState = state;
    Optional<CVariableDeclaration> retVar = fnkCall.getFunctionEntry().getReturnVariable();
    if (retVar.isPresent()) {
      newState = newState.removeInterval(retVar.orElseThrow().getQualifiedName());
    }

    // expression is an assignment operation, e.g. a = g(b);
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement) summaryExpr;

      // left hand side of the expression has to be a variable
      if (state.contains(retVar.orElseThrow().getQualifiedName())) {
        newState =
            addInterval(
                newState,
                funcExp.getLeftHandSide(),
                state.getInterval(retVar.orElseThrow().getQualifiedName()));
      }

    } else if (summaryExpr instanceof CFunctionCallStatement) {
      // nothing to do
    } else {
      throw new UnrecognizedCodeException("on function return", cfaEdge, summaryExpr);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles function calls.
   *
   * @param callEdge the respective CFA edge
   * @return the successor state
   */
  @Override
  protected Collection<IntervalAnalysisState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {

    if (callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert parameters.size() <= arguments.size();
      logger.log(
          Level.WARNING,
          "Ignoring parameters passed as varargs to function",
          callEdge.getSuccessor().getFunctionDefinition().toASTString());
    } else {
      assert parameters.size() == arguments.size();
    }

    IntervalAnalysisState newState = state;

    // set the interval of each formal parameter to the interval of its respective actual parameter
    for (int i = 0; i < parameters.size(); i++) {
      // get value of actual parameter in caller function context
      Interval interval = evaluateInterval(state, arguments.get(i), callEdge);
      String formalParameterName = parameters.get(i).getQualifiedName();
      newState = newState.addInterval(formalParameterName, interval, threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles the statement edge which leads the function to the last node of its CFA
   * (not same as a return edge).
   *
   * @param returnEdge the CFA edge corresponding to this statement
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleReturnStatementEdge(
      CReturnStatementEdge returnEdge) throws UnrecognizedCodeException {
    IntervalAnalysisState newState = state.dropFrame(functionName);

    // assign the value of the function return to a new variable
    if (returnEdge.asAssignment().isPresent()) {
      CAssignment ass = returnEdge.asAssignment().orElseThrow();
      newState =
          newState.addInterval(
              ((CIdExpression) ass.getLeftHandSide()).getDeclaration().getQualifiedName(),
              evaluateInterval(state, ass.getRightHandSide(), returnEdge),
              threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles assumptions.
   *
   * @param expression the expression containing the assumption
   * @param cfaEdge the CFA edge corresponding to this expression
   * @param truthValue flag to determine whether this is the then- or the else-branch of the
   *     assumption
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {

    if ((truthValue ? Interval.ZERO : Interval.ONE)
        .equals(evaluateInterval(state, expression, cfaEdge))) {
      // the assumption is unsatisfiable
      return noSuccessors();
    }

    // otherwise the assumption is satisfiable or unknown
    // --> we try to get additional information from the assumption

    BinaryOperator operator = ((CBinaryExpression) expression).getOperator();
    CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression) expression).getOperand2();

    if (!truthValue) {
      operator = operator.getOppositLogicalOperator();
    }

    // the following lines assume that one of the operands is an identifier
    // and the other one represented with an interval (example "x<[3;5]").
    // If none of the operands is an identifier, nothing is done.

    IntervalAnalysisState newState = state;
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge);
    Interval interval1 = operand1.accept(visitor);
    Interval interval2 = operand2.accept(visitor);

    assert !interval1.isEmpty() : operand1;
    assert !interval2.isEmpty() : operand2;

    switch (operator) {
        // a < b, a < 1
      case LESS_THAN:
        {
          newState =
              addInterval(newState, operand1, interval1.limitUpperBoundBy(interval2.minus(1L)));
          newState =
              addInterval(newState, operand2, interval2.limitLowerBoundBy(interval1.plus(1L)));
          return soleSuccessor(newState);
        }

        // a <= b, a <= 1
      case LESS_EQUAL:
        {
          newState = addInterval(newState, operand1, interval1.limitUpperBoundBy(interval2));
          newState = addInterval(newState, operand2, interval2.limitLowerBoundBy(interval1));
          return soleSuccessor(newState);
        }

        // a > b, a > 1
      case GREATER_THAN:
        {
          newState =
              addInterval(newState, operand1, interval1.limitLowerBoundBy(interval2.plus(1L)));
          newState =
              addInterval(newState, operand2, interval2.limitUpperBoundBy(interval1.minus(1L)));
          return soleSuccessor(newState);
        }

        // a >= b, a >= 1
      case GREATER_EQUAL:
        {
          newState = addInterval(newState, operand1, interval1.limitLowerBoundBy(interval2));
          newState = addInterval(newState, operand2, interval2.limitUpperBoundBy(interval1));
          return soleSuccessor(newState);
        }

        // a == b, a == 1
      case EQUALS:
        {
          newState = addInterval(newState, operand1, interval1.intersect(interval2));
          newState = addInterval(newState, operand2, interval2.intersect(interval1));
          return soleSuccessor(newState);
        }

        // a != b, a != 1
      case NOT_EQUALS:
        {

          // Splitting depends on the fact that one operand is a literal.
          // Then we try to split into two intervals.
          if (interval2.getLow().equals(interval2.getHigh())) {
            return splitInterval(newState, operand1, interval1, interval2);

          } else if (interval1.getLow().equals(interval1.getHigh())) {
            return splitInterval(newState, operand2, interval2, interval1);

          } else {
            // we know nothing more than before
            return soleSuccessor(newState);
          }
        }

      default:
        throw new UnrecognizedCodeException(
            "unexpected operator in assumption", cfaEdge, expression);
    }
  }

  /**
   * For an interval [2;5] and a splitPoint [3;3] we build two states with assignments for [2;2] and
   * [4;5].
   *
   * @param newState where to store the new intervals
   * @param lhs the left-hand-side of the assignment
   * @param interval to be split
   * @param splitPoint singular interval where to split
   * @return two states
   */
  private Collection<IntervalAnalysisState> splitInterval(
      IntervalAnalysisState newState, CExpression lhs, Interval interval, Interval splitPoint) {

    assert splitPoint.getLow().equals(splitPoint.getHigh()) : "invalid splitpoint for interval";

    // we split in following cases:
    // - either always because of the option 'splitIntervals'
    // - or if the splitPoint is the bound of the interval and thus we can shrink the interval.
    if (splitIntervals
        || interval.getLow().equals(splitPoint.getHigh())
        || interval.getHigh().equals(splitPoint.getHigh())) {

      Collection<IntervalAnalysisState> successors = new ArrayList<>();

      Interval part1 =
          interval.intersect(Interval.createUpperBoundedInterval(splitPoint.getLow() - 1L));
      Interval part2 =
          interval.intersect(Interval.createLowerBoundedInterval(splitPoint.getLow() + 1L));

      if (!part1.isEmpty()) {
        successors.add(addInterval(newState, lhs, part1));
      }

      if (!part2.isEmpty()) {
        successors.add(addInterval(newState, lhs, part2));
      }

      return successors;
    } else {
      return soleSuccessor(newState);
    }
  }

  private IntervalAnalysisState addInterval(
      IntervalAnalysisState newState, CExpression lhs, Interval interval) {
    // we currently only handle IdExpressions and ignore more complex Expressions
    if (lhs instanceof CIdExpression) {
      newState =
          newState.addInterval(
              ((CIdExpression) lhs).getDeclaration().getQualifiedName(), interval, threshold);
    }
    return newState;
  }

  /**
   * This method handles variable declarations.
   *
   * <p>So far, only primitive types are supported, pointers are not supported either.
   *
   * @param declarationEdge the CFA edge
   * @return the successor state
   */
  @Override
  protected Collection<IntervalAnalysisState> handleDeclarationEdge(
      CDeclarationEdge declarationEdge, CDeclaration declaration) throws UnrecognizedCodeException {

    IntervalAnalysisState newState = state;
    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration) declarationEdge.getDeclaration();

      // ignore pointer variables
      if (decl.getType() instanceof CPointerType) {
        return soleSuccessor(newState);
      }

      Interval interval;
      CInitializer init = decl.getInitializer();

      // variable may be initialized explicitly on the spot ...
      if (init instanceof CInitializerExpression) {
        CExpression exp = ((CInitializerExpression) init).getExpression();
        interval = evaluateInterval(state, exp, declarationEdge);
      } else {
        interval = Interval.UNBOUND;
      }

      newState = newState.addInterval(decl.getQualifiedName(), interval, threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles unary and binary statements.
   *
   * @param expression the current expression
   * @param cfaEdge the CFA edge
   * @return the successor
   */
  @Override
  protected Collection<IntervalAnalysisState> handleStatementEdge(
      CStatementEdge cfaEdge, CStatement expression) throws UnrecognizedCodeException {
    IntervalAnalysisState successor = state;
    // expression is an assignment operation, e.g. a = b;
    if (expression instanceof CAssignment) {
      CAssignment assignExpression = (CAssignment) expression;
      CExpression op1 = assignExpression.getLeftHandSide();
      CRightHandSide op2 = assignExpression.getRightHandSide();

      // a = ?
      successor = addInterval(successor, op1, evaluateInterval(state, op2, cfaEdge));
    }
    return soleSuccessor(successor);
  }

  private Interval evaluateInterval(
      IntervalAnalysisState readableState, CRightHandSide expression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    return expression.accept(new ExpressionValueVisitor(readableState, cfaEdge));
  }

  private Collection<IntervalAnalysisState> soleSuccessor(IntervalAnalysisState successor) {
    return Collections.singleton(successor);
  }

  private Collection<IntervalAnalysisState> noSuccessors() {
    return ImmutableSet.of();
  }
}
