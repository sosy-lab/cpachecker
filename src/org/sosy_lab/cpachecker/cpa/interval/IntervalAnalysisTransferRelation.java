/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


@Options(prefix="cpa.interval")
public class IntervalAnalysisTransferRelation extends ForwardingTransferRelation<Collection<IntervalAnalysisState>, IntervalAnalysisState, Precision> {

  @Option(secure=true, description="decides whether one (false) or two (true) successors should be created "
    + "when an inequality-check is encountered")
  private boolean splitIntervals = false;

  @Option(secure=true, description="at most that many intervals will be tracked per variable, -1 if number not restricted")
  private int threshold = -1;

  private final LogManager logger;

  public IntervalAnalysisTransferRelation(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  @Override
  protected Collection<IntervalAnalysisState> postProcessing(Collection<IntervalAnalysisState> successors, CFAEdge edge) {
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
      newState = IntervalAnalysisState.copyOf(state);
      newState.dropFrame(functionName);
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
  protected Collection<IntervalAnalysisState> handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
    throws UnrecognizedCodeException {

    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);
    Optional<CVariableDeclaration> retVar = fnkCall.getFunctionEntry().getReturnVariable();
    if (retVar.isPresent()) {
      newState.removeInterval(retVar.get().getQualifiedName());
    }

    // expression is an assignment operation, e.g. a = g(b);
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)summaryExpr;

      // left hand side of the expression has to be a variable
      if (state.contains(retVar.get().getQualifiedName())) {
        addInterval(newState, funcExp.getLeftHandSide(), state.getInterval(retVar.get().getQualifiedName()));
      }

    } else if (summaryExpr instanceof CFunctionCallStatement) {
      // nothing to do
    } else {
      throw new UnrecognizedCCodeException("on function return", cfaEdge, summaryExpr);
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
  protected Collection<IntervalAnalysisState> handleFunctionCallEdge(CFunctionCallEdge callEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCCodeException {

    if (callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert parameters.size() <= arguments.size();
      logger.log(Level.WARNING, "Ignoring parameters passed as varargs to function",
          callEdge.getSuccessor().getFunctionDefinition().toASTString());
    } else {
      assert parameters.size() == arguments.size();
    }

    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);

    // set the interval of each formal parameter to the interval of its respective actual parameter
    for (int i = 0; i < parameters.size(); i++) {
      // get value of actual parameter in caller function context
      Interval interval = evaluateInterval(state, arguments.get(i), callEdge);
      String formalParameterName = parameters.get(i).getQualifiedName();
      newState.addInterval(formalParameterName, interval, this.threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles the statement edge which leads the function to the last node of its CFA (not same as a return edge).
   *
   * @param returnEdge the CFA edge corresponding to this statement
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws UnrecognizedCCodeException {
    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);
    newState.dropFrame(functionName);

    // assign the value of the function return to a new variable
    if (returnEdge.asAssignment().isPresent()) {
      CAssignment ass = returnEdge.asAssignment().get();
      newState.addInterval(
          ((CIdExpression)ass.getLeftHandSide()).getDeclaration().getQualifiedName(),
          evaluateInterval(state, ass.getRightHandSide(), returnEdge), threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles assumptions.
   *
   * @param expression the expression containing the assumption
   * @param cfaEdge the CFA edge corresponding to this expression
   * @param truthValue flag to determine whether this is the then- or the else-branch of the assumption
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthValue)
          throws UnrecognizedCCodeException {

    if ((truthValue ? Interval.ZERO : Interval.ONE).equals(evaluateInterval(state, expression, cfaEdge))) {
      // the assumption is unsatisfiable
      return noSuccessors();
    }

    // otherwise the assumption is satisfiable or unknown
    // --> we try to get additional information from the assumption

    BinaryOperator operator = ((CBinaryExpression)expression).getOperator();
    CExpression operand1 = ((CBinaryExpression)expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression)expression).getOperand2();

    if (!truthValue) {
      operator = negateOperator(operator);
    }

    // the following lines assume that one of the operands is an identifier
    // and the other one represented with an interval (example "x<[3;5]").
    // If none of the operands is an identifier, nothing is done.

    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge);
    Interval interval1 = operand1.accept(visitor);
    Interval interval2 = operand2.accept(visitor);

    assert !interval1.isEmpty() : operand1;
    assert !interval2.isEmpty() : operand2;

    switch(operator) {
    // a < b, a < 1
    case LESS_THAN: {
      addInterval(newState, operand1, interval1.limitUpperBoundBy(interval2.minus(1L)));
      addInterval(newState, operand2, interval2.limitLowerBoundBy(interval1.plus(1L)));
      return soleSuccessor(newState);
    }

    // a <= b, a <= 1
    case LESS_EQUAL: {
      addInterval(newState, operand1, interval1.limitUpperBoundBy(interval2));
      addInterval(newState, operand2, interval2.limitLowerBoundBy(interval1));
      return soleSuccessor(newState);
    }

    // a > b, a > 1
    case GREATER_THAN: {
      addInterval(newState, operand1, interval1.limitLowerBoundBy(interval2.plus(1L)));
      addInterval(newState, operand2, interval2.limitUpperBoundBy(interval1.minus(1L)));
      return soleSuccessor(newState);
    }

    // a >= b, a >= 1
    case GREATER_EQUAL: {
      addInterval(newState, operand1, interval1.limitLowerBoundBy(interval2));
      addInterval(newState, operand2, interval2.limitUpperBoundBy(interval1));
      return soleSuccessor(newState);
    }

    // a == b, a == 1
    case EQUALS: {
      addInterval(newState, operand1, interval1.intersect(interval2));
      addInterval(newState, operand2, interval2.intersect(interval1));
      return soleSuccessor(newState);
    }

    // a != b, a != 1
    case NOT_EQUALS: {

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
      throw new UnrecognizedCCodeException("unexpected operator in assumption", cfaEdge, expression);
    }
  }

  /**
   * For an interval [2;5] and a splitPoint [3;3]
   * we build two states with assignments for [2;2] and [4;5].
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

      Interval part1 = interval.intersect(Interval.createUpperBoundedInterval(splitPoint.getLow() - 1L));
      Interval part2 = interval.intersect(Interval.createLowerBoundedInterval(splitPoint.getLow() + 1L));

      if (!part1.isEmpty()) {
        IntervalAnalysisState newState2 = IntervalAnalysisState.copyOf(newState);
        addInterval(newState2, lhs, part1);
        successors.add(newState2);
      }

      if (!part2.isEmpty()) {
        IntervalAnalysisState newState3 = IntervalAnalysisState.copyOf(newState);
        addInterval(newState3, lhs, part2);
        successors.add(newState3);
      }

      return successors;
    } else {
      return soleSuccessor(newState);
    }
  }

  private void addInterval(IntervalAnalysisState newState,
      CExpression lhs, Interval interval) {
    // we currently only handle IdExpressions and ignore more complex Expressions
    if (lhs instanceof CIdExpression) {
      newState.addInterval(
          ((CIdExpression) lhs).getDeclaration().getQualifiedName(),
          interval,
          threshold);
    }
  }

  /**
   * This method return the negated counter part for a given operator
   *
   * @param operator the operator to negate
   * @return the negated counter part of the given operator
   */
  private static BinaryOperator negateOperator(BinaryOperator operator) {
    switch (operator) {
      case EQUALS:
        return BinaryOperator.NOT_EQUALS;

      case NOT_EQUALS:
        return BinaryOperator.EQUALS;

      case LESS_THAN:
        return BinaryOperator.GREATER_EQUAL;

      case LESS_EQUAL:
        return BinaryOperator.GREATER_THAN;

      case GREATER_EQUAL:
        return BinaryOperator.LESS_THAN;

      case GREATER_THAN:
        return BinaryOperator.LESS_EQUAL;

      default:
        return operator;
    }
  }

  /**
   * This method handles variable declarations.
   *
   * So far, only primitive types are supported, pointers are not supported either.
   *
   * @param declarationEdge the CFA edge
   * @return the successor state
   */
  @Override
  protected Collection<IntervalAnalysisState> handleDeclarationEdge(CDeclarationEdge declarationEdge, CDeclaration declaration)
      throws UnrecognizedCCodeException {

    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);
    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration)declarationEdge.getDeclaration();

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
        interval = Interval.createUnboundInterval();
      }

      newState.addInterval(decl.getQualifiedName(), interval, this.threshold);
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
  protected Collection<IntervalAnalysisState> handleStatementEdge(CStatementEdge cfaEdge, CStatement expression)
    throws UnrecognizedCodeException {
    // expression is an assignment operation, e.g. a = b;
    if (expression instanceof CAssignment) {
      CAssignment assignExpression = (CAssignment)expression;
      CExpression op1 = assignExpression.getLeftHandSide();
      CRightHandSide op2 = assignExpression.getRightHandSide();

      // a = ?
      IntervalAnalysisState successor = IntervalAnalysisState.copyOf(state);
      addInterval(successor, op1, evaluateInterval(state, op2, cfaEdge));
      return soleSuccessor(successor);
    }
    return soleSuccessor(state);
  }

  private Interval evaluateInterval(IntervalAnalysisState readableState, CRightHandSide expression, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    return expression.accept(new ExpressionValueVisitor(readableState, cfaEdge));
  }

  private Collection<IntervalAnalysisState> soleSuccessor(IntervalAnalysisState successor) {
    return Collections.singleton(successor);
  }

  private Collection<IntervalAnalysisState> noSuccessors() {
    return Collections.emptySet();
  }
}

