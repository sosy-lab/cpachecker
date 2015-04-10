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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
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


@Options(prefix="cpa.interval")
public class IntervalAnalysisTransferRelation extends ForwardingTransferRelation<Collection<IntervalAnalysisState>, IntervalAnalysisState, Precision> {

  @Option(secure=true, description="decides whether one (false) or two (true) successors should be created "
    + "when an inequality-check is encountered")
  private boolean splitIntervals = false;
  /**
   * base name of the variable that is introduced to pass results from returning function calls
   */
  private static final String RETURN_VARIABLE_BASE_NAME = "___cpa_temp_result_var_";

  private final Set<String> globalFieldVars = new HashSet<>();

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
    return soleSuccessor(state);
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
    newState.dropFrame(functionName);

    // expression is an assignment operation, e.g. a = g(b);
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)summaryExpr;

      CExpression operand1 = funcExp.getLeftHandSide();

      // left hand side of the expression has to be a variable
      if ((operand1 instanceof CIdExpression) || (operand1 instanceof CFieldReference)) {

        String returnedVariableName = functionName + "::" + RETURN_VARIABLE_BASE_NAME;

        // set the value of the assigned variable to the value of the returned variable
        Interval interval = state.contains(returnedVariableName) ? state.getInterval(returnedVariableName) : Interval.createUnboundInterval();
        newState.addInterval(constructVariableName(operand1, callerFunctionName), interval, this.threshold);
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current state
      else if (operand1 instanceof CPointerExpression) {
        return soleSuccessor(IntervalAnalysisState.copyOf(state));
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
    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    List<String> parameterNames = functionEntryNode.getFunctionParameterNames();

    assert (parameterNames.size() == arguments.size());

    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, functionName, edge);

    // set the interval of each formal parameter to the interval of its respective actual parameter
    for (int i = 0; i < arguments.size(); i++) {
      //Interval interval = evaluateInterval(previousState, arguments.get(i), callerFunctionName, callEdge);
      // get value of actual parameter in caller function context
      Interval interval = arguments.get(i).accept(visitor);

      String formalParameterName = constructLocalVariableName(parameterNames.get(i), calledFunctionName);

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
    CExpression exp = returnEdge.getExpression().or(CIntegerLiteralExpression.ZERO); // 0 is the default in C

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, returnEdge.getPredecessor().getFunctionName(), edge);

    // assign the value of the function return to a new variable
    return soleSuccessor(handleAssignmentToVariable(RETURN_VARIABLE_BASE_NAME, exp, visitor));
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
    // first, unpack the expression to deal with a raw assumption
    if (expression instanceof CUnaryExpression) {
      throw new UnrecognizedCCodeException("unexpected operator in assumption", cfaEdge, expression);
    }

    // -> *exp - don't know anything
    else if (expression instanceof CPointerExpression) {
      return soleSuccessor(IntervalAnalysisState.copyOf(state));
    }

    // a plain (boolean) identifier, e.g. if (a)
    else if (expression instanceof CIdExpression) {
      // this is simplified in the frontend
      throw new UnrecognizedCCodeException("unexpected expression in assumption", cfaEdge, expression);
    } else if (expression instanceof CBinaryExpression) {
      IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);

      BinaryOperator operator = ((CBinaryExpression)expression).getOperator();
      CExpression operand1 = ((CBinaryExpression)expression).getOperand1();
      CExpression operand2 = ((CBinaryExpression)expression).getOperand2();

      ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);

      Interval interval1 = operand1.accept(visitor);
      Interval interval2 = operand2.accept(visitor);

      switch (operator) {
        case MINUS:
        case PLUS:
          Interval result = null;

          if (operator == BinaryOperator.MINUS) {
            result = interval1.minus(interval2);
          } else if (operator == BinaryOperator.PLUS) {
            result = interval1.plus(interval2);
          }

          // in then-branch and interval maybe true, or in else-branch and interval maybe false, add a successor
          if ((truthValue && !result.isFalse()) || (!truthValue && !result.isTrue())) {
            return soleSuccessor(newState);
          } else {
            return noSuccessors();
          }

        case EQUALS:
        case NOT_EQUALS:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case LESS_THAN:
        case LESS_EQUAL:
          return processAssumption(operator, operand1, operand2, truthValue);

        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
          return soleSuccessor(newState);

        default:
          throw new UnrecognizedCCodeException("unexpected operator in assumption", cfaEdge, expression);
      }
    }

    return noSuccessors();
  }


  private Collection<IntervalAnalysisState> processAssumption(
      BinaryOperator operator, CExpression operand1, CExpression operand2, boolean truthValue)
          throws UnrecognizedCCodeException {
    if (!truthValue) {
      operator = negateOperator(operator);
    }

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, functionName, edge);
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
      if (tmpInterval1.isSingular() && tmpInterval1.equals(tmpInterval2)) {
        return noSuccessors();
      }

      // TODO: currently depends on the fact that operand1 is a identifier, while operand2 is a literal
      if (splitIntervals && isIdOp1 && !isIdOp2) {
        IntervalAnalysisState newState2 = null;

        Collection<IntervalAnalysisState> successors = new LinkedList<>();

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

      String varName;

      // if this is a global variable, add it to the list of global variables
      if (decl.isGlobal()) {
        varName = decl.getName();
        globalFieldVars.add(varName);
      } else {
        varName = constructLocalVariableName(decl.getName(), declarationEdge.getPredecessor().getFunctionName());
      }

      Interval interval;

      CInitializer init = decl.getInitializer();

      // variable may be initialized explicitly on the spot ...
      if (init instanceof CInitializerExpression) {
        CExpression exp = ((CInitializerExpression) init).getExpression();
        interval = evaluateInterval(state, exp, declarationEdge.getPredecessor().getFunctionName(), declarationEdge);
      } else {
        if (decl.isGlobal()) {
          // according to C standard non initialized global vars are set to 0
          interval = new Interval(0L);
        } else {
          interval = Interval.createUnboundInterval();
        }
      }

      newState.addInterval(varName, interval, this.threshold);

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
    IntervalAnalysisState successor;
    if (expression instanceof CAssignment) {
      successor = handleAssignment(state, (CAssignment)expression, cfaEdge);
    } else if (expression instanceof CFunctionCallStatement) {
      successor = IntervalAnalysisState.copyOf(state);
    } else if (expression instanceof CExpressionStatement) {
      successor = IntervalAnalysisState.copyOf(state);
    } else {
      throw new UnrecognizedCodeException("unknown statement", cfaEdge, expression);
    }
    return soleSuccessor(successor);
  }

   /**
   * This method handles assignments.
   *
   * @param state the analysis state
   * @param assignExpression the expression containing the binary expression
   * @param declarationEdge the CFA edge
   * @return the successor state
   * TODO pointer dereferencing via strengthening
   */
  private IntervalAnalysisState handleAssignment(IntervalAnalysisState state, CAssignment assignExpression, CFAEdge cfaEdge)
    throws UnrecognizedCCodeException {
    CExpression op1 = assignExpression.getLeftHandSide();
    CRightHandSide op2 = assignExpression.getRightHandSide();

    // a = ?
    if (op1 instanceof CIdExpression) {
      ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);

      return handleAssignmentToVariable(constructVariableName(op1,visitor.functionName), op2, visitor);
    }

    // TODO: assignment to pointer, *a = ?
    else if (op1 instanceof CPointerExpression) {
      return IntervalAnalysisState.copyOf(state);
    } else if (op1 instanceof CFieldReference) {
      return IntervalAnalysisState.copyOf(state);
    } else if (op1 instanceof CArraySubscriptExpression) {
      return IntervalAnalysisState.copyOf(state);
    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  /**
   * This method handles the assignment of a variable.
   *
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   * @param cfaEdge the respective CFA edge
   * @return the successor state
   */
  private IntervalAnalysisState handleAssignmentToVariable(String pFullVariableName, CRightHandSide expression, ExpressionValueVisitor v)
    throws UnrecognizedCCodeException {
    Interval value = expression.accept(v);

    IntervalAnalysisState newState = IntervalAnalysisState.copyOf(state);

    newState.addInterval(pFullVariableName, value, this.threshold);

    return newState;
  }

  /**
   * This method evaluates an expression and returns the respective interval.
   *
   * @param state the analysis state
   * @param expression the expression containing the expression to be evaluated
   * @param functionName the name of the function currently being analyzed
   * @param cfaEdge the respective CFA edge
   * @return the interval in respect to the evaluated expression of null, if the expression could not be evaluated properly
   */
  //getExpressionValue
  private Interval evaluateInterval(IntervalAnalysisState state, CRightHandSide expression, String functionName, CFAEdge cfaEdge)
    throws UnrecognizedCCodeException {
    if (expression instanceof CLiteralExpression) {
      Long value = parseLiteral((CLiteralExpression)expression, cfaEdge);

      return (value == null) ? Interval.createUnboundInterval() : new Interval(value, value);

    } else if (expression instanceof CIdExpression) {
      String varName = constructVariableName((CIdExpression)expression, functionName);

      return (state.contains(varName)) ? state.getInterval(varName) : Interval.createUnboundInterval();

    } else if (expression instanceof CCastExpression) {
      return evaluateInterval(state, ((CCastExpression)expression).getOperand(), functionName, cfaEdge);
    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression)expression;

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {
        case MINUS:
          Interval interval = evaluateInterval(state, unaryOperand, functionName, cfaEdge);

          return (interval == null) ? Interval.createUnboundInterval() : interval.negate();

        default:
          throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    }
    // clause for CPointerexpression does the same, as UnaryExpression clause would have done for the star operator
    else if (expression instanceof CPointerExpression) {
      throw new UnrecognizedCCodeException("PointerExpressions are not allowed at this place", cfaEdge, expression);
    }

    // added for expression "if (! (req_a___0 + 50 == rsp_d___0))" in "systemc/mem_slave_tlm.1.cil.c"
    else if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression)expression;

      Interval interval1 = evaluateInterval(state, binaryExpression.getOperand1(), functionName, cfaEdge);
      Interval interval2 = evaluateInterval(state, binaryExpression.getOperand2(), functionName, cfaEdge);

      switch (binaryExpression.getOperator()) {
        case PLUS:
          return interval1.plus(interval2);

        default:
          throw new UnrecognizedCCodeException("unknown binary operator", cfaEdge, binaryExpression);
      }
    } else {
      //throw new UnrecognizedCCodeException(cfaEdge, expression);
      return Interval.createUnboundInterval();
    }
  }

  /**
   * This method parses an expression to retrieve its literal value.
   *
   * @param expression the expression to parse
   * @return a number or null if the parsing failed
   * @throws UnrecognizedCCodeException
   */
  private static Long parseLiteral(CLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    if (expression instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)expression).asLong();

    } else if (expression instanceof CFloatLiteralExpression) {
      return null;

    } else if (expression instanceof CCharLiteralExpression) {
      return (long)((CCharLiteralExpression)expression).getCharacter();

    } else if (expression instanceof CStringLiteralExpression) {
      return null;

    } else {
      throw new UnrecognizedCCodeException("unknown literal", edge, expression);
    }
  }


  private String constructLocalVariableName(String pVariableName, String pCalledFunctionName) {
    return pCalledFunctionName + "::" + pVariableName;
  }

  private String constructVariableName(CExpression pVariableName, String pCalledFunctionName) {
    if (pVariableName instanceof CIdExpression) {
        CSimpleDeclaration decl = ((CIdExpression) pVariableName).getDeclaration();
        if (decl instanceof CDeclaration) {
          if  (((CDeclaration) decl).isGlobal()) {
            return pVariableName.toASTString();
          }
      }
    }
    if (pVariableName instanceof CFieldReference && globalFieldVars.contains(pVariableName.toASTString())) {
      return pVariableName.toASTString();
    }
    return pCalledFunctionName + "::" + pVariableName.toASTString();
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
   * Visitor that get's the value from an expression.
   * The result may be null, i.e., the value is unknown.
   */
  private class ExpressionValueVisitor extends DefaultCExpressionVisitor<Interval, UnrecognizedCCodeException>
                                       implements CRightHandSideVisitor<Interval, UnrecognizedCCodeException> {

    private final IntervalAnalysisState readableState;

    private final String functionName;

    private final CFAEdge cfaEdge;

    public ExpressionValueVisitor(IntervalAnalysisState pState, String pFunctionName, CFAEdge edge) {
      readableState = pState;
      functionName = pFunctionName;
      cfaEdge = edge;
    }

    // TODO fields, arrays

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

      switch (binaryExpression.getOperator()) {
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

        case EQUALS:
          return new Interval(interval1.intersects(interval2) ? 1L : 0L);

        case NOT_EQUALS:
          return new Interval(!interval1.intersects(interval2) ? 1L : 0L);

        case GREATER_THAN:
          return new Interval(interval1.mayBeGreaterThan(interval2) ? 1L : 0L);

        case GREATER_EQUAL:
          return new Interval(interval1.mayBeGreaterOrEqualThan(interval2) ? 1L : 0L);

        case LESS_THAN:
          return new Interval(interval1.mayBeLessThan(interval2) ? 1L : 0L);

        case LESS_EQUAL:
          return new Interval(interval1.mayBeLessOrEqualThan(interval2) ? 1L : 0L);

        case MODULO:
          return interval1.modulo(interval2);
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
          // can these be handled?
          return Interval.createUnboundInterval();

        default:
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge, binaryExpression);
      }
    }

    @Override
    public Interval visit(CCastExpression cast) throws UnrecognizedCCodeException {
      return cast.getOperand().accept(this);
    }

    @Override
    public Interval visit(CComplexCastExpression cast) throws UnrecognizedCCodeException {
      // evaluation of complex numbers is not supported by now
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(CFunctionCallExpression functionCall) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(CCharLiteralExpression charLiteral) throws UnrecognizedCCodeException {
      Long l = parseLiteral(charLiteral, cfaEdge);
      return l == null ? Interval.createUnboundInterval() : new Interval(l);
    }

    @Override
    public Interval visit(CFloatLiteralExpression floatLiteral) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(CImaginaryLiteralExpression exp) throws UnrecognizedCCodeException {
      return exp.getValue().accept(this);
    }

    @Override
    public Interval visit(CIntegerLiteralExpression integerLiteral) throws UnrecognizedCCodeException {
      return new Interval(parseLiteral(integerLiteral, cfaEdge));
    }

    @Override
    public Interval visit(CStringLiteralExpression stringLiteral) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(CIdExpression identifier) throws UnrecognizedCCodeException {
      if (identifier.getDeclaration() instanceof CEnumerator) {
        return new Interval(((CEnumerator)identifier.getDeclaration()).getValue());
      }

      String variableName = constructVariableName(identifier, functionName);
      if (readableState.contains(variableName)) {
        return readableState.getInterval(variableName);
      } else {
        return Interval.createUnboundInterval();
      }
    }

    @Override
    public Interval visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      Interval interval = unaryOperand.accept(this);

      switch (unaryOperator) {

      case MINUS:
        return (interval != null) ? interval.negate() : Interval.createUnboundInterval();

      case AMPER:
        return Interval.createUnboundInterval(); // valid expression, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    }

    @Override
    public Interval visit(CPointerExpression pointerExpression) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }
  }
}

