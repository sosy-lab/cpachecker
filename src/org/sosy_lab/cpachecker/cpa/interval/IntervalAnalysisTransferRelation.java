/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;


@Options(prefix="cpa.interval")
public class IntervalAnalysisTransferRelation extends ForwardingTransferRelation<Collection<IntervalAnalysisState>, IntervalAnalysisState, Precision> {

  @Option(secure=true, description="decides whether one (false) or two (true) successors should be created "
    + "when an inequality-check is encountered")
  private boolean splitIntervals = false;

  @Option(secure=true, description="at most that many intervals will be tracked per variable, -1 if number not restricted")
  private int threshold = -1;

  public IntervalAnalysisTransferRelation(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  protected Collection<IntervalAnalysisState> postProcessing(Collection<IntervalAnalysisState> successors) {
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

  @Override
  protected Collection<IntervalAnalysisState> handleMultiEdge(MultiEdge cfaEdge)
      throws CPATransferException {
    return super.handleMultiEdgeReturningCollection(cfaEdge);
  }

  /**
   * Handles return from one function to another function.
   *
   * @param state previous abstract state.
   * @param functionReturnEdge return edge from a function to its call site.
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

      CExpression operand1 = funcExp.getLeftHandSide();

      // left hand side of the expression has to be a variable
      if ((operand1 instanceof CIdExpression) || (operand1 instanceof CFieldReference)) {
        // set the value of the assigned variable to the value of the returned variable
        Interval interval = state.contains(retVar.get().getQualifiedName())
            ? state.getInterval(retVar.get().getQualifiedName())
            : Interval.createUnboundInterval();
        newState.addInterval(constructVariableName(operand1, callerFunctionName), interval, this.threshold);
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current state
      else if (operand1 instanceof CPointerExpression) {
        // nothing to do
      } else {
        throw new UnrecognizedCCodeException("on function return", edge, operand1);
      }
    } else if (summaryExpr instanceof CFunctionCallStatement) {
      // nothing to do
    } else {
      throw new UnrecognizedCCodeException("on function return", edge, summaryExpr);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles function calls.
   *
   * @param previousState the previous state of the analysis, before the function call
   * @param callEdge the respective CFA edge
   * @return the successor state
   * @throws UnrecognizedCCodeException
   */
  @Override
  protected Collection<IntervalAnalysisState> handleFunctionCallEdge(CFunctionCallEdge callEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCCodeException {
    assert (parameters.size() == arguments.size());
    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);

    // set the interval of each formal parameter to the interval of its respective actual parameter
    for (int i = 0; i < arguments.size(); i++) {
      // get value of actual parameter in caller function context
      Interval interval = evaluateInterval(state, arguments.get(i));
      String formalParameterName = parameters.get(i).getQualifiedName();
      newState.addInterval(formalParameterName, interval, this.threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles the statement edge which leads the function to the last node of its CFA (not same as a return edge).
   *
   * @param state the analysis state
   * @param expression the expression
   * @param CReturnStatementEdge the CFA edge corresponding to this statement
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
          evaluateInterval(state, ass.getRightHandSide()), threshold);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method handles assumptions.
   *
   * @param state the analysis state
   * @param expression the expression containing the assumption
   * @param cfaEdge the CFA edge corresponding to this expression
   * @param truthValue flag to determine whether this is the then- or the else-branch of the assumption
   * @return the successor states
   */
  @Override
  protected Collection<IntervalAnalysisState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthValue)
          throws UnrecognizedCCodeException {

    BinaryOperator operator = ((CBinaryExpression)expression).getOperator();
    CExpression operand1 = ((CBinaryExpression)expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression)expression).getOperand2();

    if (!truthValue) {
      operator = negateOperator(operator);
    }

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, edge);
    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);

    Interval orgInterval1 = operand1.accept(visitor);
    Interval orgInterval2 = operand2.accept(visitor);

    //Interval orgInterval1 = evaluateInterval(state, operand1, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);
    Interval tmpInterval1 = orgInterval1;

    //Interval orgInterval2 = evaluateInterval(state, operand2, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);
    Interval tmpInterval2 = orgInterval2;

    String variableName1 = constructVariableName(operand1, functionName);
    String variableName2 = constructVariableName(operand2, functionName);

    // determine whether or not the respective operand is an identifier
    boolean isIdOp1 = operand1 instanceof CIdExpression;
    boolean isIdOp2 = operand2 instanceof CIdExpression;

    // a < b, a < 1
    if (operator == BinaryOperator.LESS_THAN) {
      // a may be less than b, so there can be a successor
      if (tmpInterval1.mayBeLessThan(tmpInterval2)) {
        if (isIdOp1) {
          newState.addInterval(variableName1, orgInterval1.limitUpperBoundBy(tmpInterval2.minus(1L)), threshold);
        }
        if (isIdOp2) {
          newState.addInterval(variableName2, orgInterval2.limitLowerBoundBy(tmpInterval1.plus(1L)), threshold);
        }
      } else {
        return noSuccessors();
      }
    }

    // a <= b, a <= 1
    else if (operator == BinaryOperator.LESS_EQUAL) {
      // a may be less or equal than b, so there can be a successor
      if (tmpInterval1.mayBeLessOrEqualThan(tmpInterval2)) {
        if (isIdOp1) {
          newState.addInterval(variableName1, orgInterval1.limitUpperBoundBy(tmpInterval2), threshold);
        }
        if (isIdOp2) {
          newState.addInterval(variableName2, orgInterval2.limitLowerBoundBy(tmpInterval1), threshold);
        }
      } else {
        return noSuccessors();
      }
    }

    // a > b, a > 1
    else if (operator == BinaryOperator.GREATER_THAN) {
      // a may be greater than b, so there can be a successor
      if (tmpInterval1.mayBeGreaterThan(tmpInterval2)) {
        if (isIdOp1) {
          newState.addInterval(variableName1, orgInterval1.limitLowerBoundBy(tmpInterval2.plus(1L)), threshold);
        }
        if (isIdOp2) {
          newState.addInterval(variableName2, orgInterval2.limitUpperBoundBy(tmpInterval1.minus(1L)), threshold);
        }
      } else {
        return noSuccessors();
      }
    }

    // a >= b, a >= 1
    else if (operator == BinaryOperator.GREATER_EQUAL) {
      // a may be greater or equal than b, so there can be a successor
      if (tmpInterval1.mayBeGreaterOrEqualThan(tmpInterval2)) {
        if (isIdOp1) {
          newState.addInterval(variableName1, orgInterval1.limitLowerBoundBy(tmpInterval2), threshold);
        }
        if (isIdOp2) {
          newState.addInterval(variableName2, orgInterval2.limitUpperBoundBy(tmpInterval1), threshold);
        }
      } else {
        return noSuccessors();
      }
    }

    // a == b, a == 1
    else if (operator == BinaryOperator.EQUALS) {
      // a and b intersect, so they may have the same value, so they may be equal
      if (tmpInterval1.intersects(tmpInterval2)) {
        if (isIdOp1) {
          newState.addInterval(variableName1, orgInterval1.intersect(tmpInterval2), threshold);
        }
        if (isIdOp2) {
          newState.addInterval(variableName2, orgInterval2.intersect(tmpInterval1), threshold);
        }
      } else {
        return noSuccessors();
      }
    }

    // a != b, a != 1
    else if (operator == BinaryOperator.NOT_EQUALS) {
      // a = [x, x] = b => a and b are always equal, so there can't be a successor
      if (tmpInterval1.getLow().equals(tmpInterval1.getHigh()) && tmpInterval1.equals(tmpInterval2)) {
        return noSuccessors();
      }

      // TODO: currently depends on the fact that operand1 is a identifier, while operand2 is a literal
      if (splitIntervals && isIdOp1 && !isIdOp2) {
        IntervalAnalysisState newState2 = null;

        Collection<IntervalAnalysisState> successors = new ArrayList<>();

        Interval result = null;

        if (!(result = orgInterval1.intersect(Interval.createUpperBoundedInterval(orgInterval2.getLow() - 1L))).isEmpty()) {
          newState2 = IntervalAnalysisState.copyOf(newState);

          newState2.addInterval(variableName1, result, threshold);

          successors.add(newState2);
        }

        if (!(result = orgInterval1.intersect(Interval.createLowerBoundedInterval(orgInterval2.getLow() + 1L))).isEmpty()) {
          newState2 = IntervalAnalysisState.copyOf(newState);

          newState2.addInterval(variableName1, result, threshold);

          successors.add(newState2);
        }

        return successors;
      }
    } else {
      throw new UnrecognizedCCodeException("unknown operator", edge);
    }

    return soleSuccessor(newState);
  }

  /**
   * This method return the negated counter part for a given operator
   *
   * @param operator
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
   * @param state the analysis state
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
        interval = exp.accept(new ExpressionValueVisitor(state, edge));
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
   * @param state the analysis state
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
      if (op1 instanceof CIdExpression) {
        IntervalAnalysisState successor = IntervalAnalysisState.copyOf(state);
        successor.addInterval(((CIdExpression) op1).getDeclaration().getQualifiedName(),
            evaluateInterval(state, op2), this.threshold);
        return soleSuccessor(successor);
      }
    }
    return soleSuccessor(state);
  }

  private Interval evaluateInterval(IntervalAnalysisState readableState, CRightHandSide expression) throws UnrecognizedCCodeException {
    return expression.accept(new ExpressionValueVisitor(readableState, edge));
  }

  private String constructVariableName(CExpression pVariableName, String pCalledFunctionName) {
    if (pVariableName instanceof CIdExpression) {
      return ((CIdExpression)pVariableName).getDeclaration().getQualifiedName();
    }
    return (isGlobal(pVariableName) ? "" : (pCalledFunctionName + "::"))
        + pVariableName.toASTString();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState state, List<AbstractState> states,
      CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException {
    return null;
  }

  private Collection<IntervalAnalysisState> soleSuccessor(IntervalAnalysisState successor) {
    return Collections.singleton(successor);
  }

  private Collection<IntervalAnalysisState> noSuccessors() {
    return Collections.emptySet();
  }

  /**
   * Visitor that get's the interval from an expression,
   */
  private static class ExpressionValueVisitor
      extends DefaultCExpressionVisitor<Interval, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Interval, UnrecognizedCCodeException> {

    private final IntervalAnalysisState readableState;

    private final CFAEdge cfaEdge;

    public ExpressionValueVisitor(IntervalAnalysisState pState, CFAEdge edge) {
      readableState = pState;
      cfaEdge = edge;
    }

    @Override
    protected Interval visitDefault(CExpression expression) {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(CBinaryExpression binaryExpression) throws UnrecognizedCCodeException {
      Interval interval1 = binaryExpression.getOperand1().accept(this);
      Interval interval2 = binaryExpression.getOperand2().accept(this);

      if (interval1 == null || interval2 == null) {
        return Interval.createUnboundInterval();
      }

      BinaryOperator operator = binaryExpression.getOperator();
      if (operator.isLogicalOperator()) {
        return getLogicInterval(operator, interval1, interval2);
      } else {
        return getArithmeticInterval(operator, interval1, interval2);
      }
    }

    private static Interval getLogicInterval(BinaryOperator operator,
        Interval interval1, Interval interval2) {
      switch (operator) {
      case EQUALS:
        if (!interval1.intersects(interval2)) {
          return Interval.ZERO;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]==[5;5]
          return Interval.ONE;
        } else {
          return Interval.createBooleanInterval();
        }

      case NOT_EQUALS:
        if (!interval1.intersects(interval2)) {
          return Interval.ONE;
        } else if (interval1.getLow().equals(interval1.getHigh()) && interval1.equals(interval2)) {
          // singular interval, [5;5]!=[5;5]
          return Interval.ZERO;
        } else {
          return Interval.createBooleanInterval();
        }

      case GREATER_THAN:
        if (interval1.isGreaterThan(interval2)) {
          return Interval.ONE;
        } else if (interval1.isLessThan(interval2)) {
          return Interval.ZERO;
        } else {
          return Interval.createBooleanInterval();
        }

      case GREATER_EQUAL: // a>=b == a+1>b, works only for integers
        return getLogicInterval(BinaryOperator.GREATER_THAN,
            interval1.plus(new Interval(1L)), interval2);

      case LESS_THAN: // a<b == b>a
        return getLogicInterval(BinaryOperator.GREATER_THAN,
            interval2, interval1);

      case LESS_EQUAL: // a<=b == b+1>a, works only for integers
        return getLogicInterval(BinaryOperator.GREATER_THAN,
            interval2.plus(new Interval(1L)), interval1);

      default:
        throw new AssertionError("unknown binary operator: " + operator);
      }
    }

    private static Interval getArithmeticInterval(BinaryOperator operator,
        Interval interval1, Interval interval2) {
      switch (operator) {
      case PLUS:
        return interval1.plus(interval2);
      case MINUS:
        return interval1.minus(interval2);
      case MULTIPLY:
        return interval1.times(interval2);
      case DIVIDE:
        return interval1.divide(interval2);
      case SHIFT_LEFT:
        return interval1.shiftLeft(interval2);
      case SHIFT_RIGHT:
        return interval1.shiftRight(interval2);
      case MODULO:
        return interval1.modulo(interval2);
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        return Interval.createUnboundInterval();
      default:
        throw new AssertionError("unknown binary operator: " + operator);
      }
    }

    @Override
    public Interval visit(CCastExpression cast) throws UnrecognizedCCodeException {
      return cast.getOperand().accept(this);
    }

    @Override
    public Interval visit(CFunctionCallExpression functionCall) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }
    @Override
    public Interval visit(CCharLiteralExpression charLiteral) throws UnrecognizedCCodeException {
      return new Interval((long)charLiteral.getCharacter());
    }

    @Override
    public Interval visit(CImaginaryLiteralExpression exp) throws UnrecognizedCCodeException {
      return exp.getValue().accept(this);
    }

    @Override
    public Interval visit(CIntegerLiteralExpression integerLiteral) throws UnrecognizedCCodeException {
      return new Interval(integerLiteral.asLong());
    }

    @Override
    public Interval visit(CIdExpression identifier) throws UnrecognizedCCodeException {
      if (identifier.getDeclaration() instanceof CEnumerator) {
        return new Interval(((CEnumerator)identifier.getDeclaration()).getValue());
      }

      final String variableName = identifier.getDeclaration().getQualifiedName();
      if (readableState.contains(variableName)) {
        return readableState.getInterval(variableName);
      } else {
        return Interval.createUnboundInterval();
      }
    }

    @Override
    public Interval visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      Interval interval = unaryExpression.getOperand().accept(this);
      switch (unaryExpression.getOperator()) {

      case MINUS:
        return (interval != null) ? interval.negate() : Interval.createUnboundInterval();

      case AMPER:
        return Interval.createUnboundInterval(); // valid expression, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    }
  }
}

