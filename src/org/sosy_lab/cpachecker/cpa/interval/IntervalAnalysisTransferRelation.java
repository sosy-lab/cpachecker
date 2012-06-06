/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.NumericTypes;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer.PointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

@Options(prefix="cpa.interval")
public class IntervalAnalysisTransferRelation implements TransferRelation
{
  @Option(description="decides whether one (false) or two (true) successors should be created "
    + "when an inequality-check is encountered")
  private boolean splitIntervals = false;
  /**
   * base name of the variable that is introduced to pass results from returning function calls
   */
  private static final String RETURN_VARIABLE_BASE_NAME = "___cpa_temp_result_var_";

  private final Set<String> globalVars = new HashSet<String>();

  @Option(description="at most that many intervals will be tracked per variable")
  private int threshold = 0;

  public IntervalAnalysisTransferRelation(Configuration config) throws InvalidConfigurationException
  {
    config.inject(this);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors (AbstractState element, Precision precision, CFAEdge cfaEdge) throws CPATransferException
  {
    Collection<? extends AbstractState> successors  = null;

    AbstractState successor                         = null;

    IntervalAnalysisState intervalElement           = (IntervalAnalysisState)element;

    // check the type of the edge
    switch(cfaEdge.getEdgeType())
    {
      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge:
        StatementEdge statementEdge = (StatementEdge)cfaEdge;
        successor = handleStatement(intervalElement, statementEdge.getStatement(), cfaEdge);
        break;

        // this is the statement edge which leads the function to the last node of its CFA (not same as a return edge)
      case ReturnStatementEdge:
        ReturnStatementEdge returnEdge = (ReturnStatementEdge)cfaEdge;
        successor = handleExitFromFunction(intervalElement, returnEdge.getExpression(), returnEdge, cfaEdge);
        break;

      // edge is a declaration edge, e.g. int a;
      case DeclarationEdge:
        successor = handleDeclaration(intervalElement, (DeclarationEdge)cfaEdge);
        break;

      // this is an assumption, e.g. if(a == b)
      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge)cfaEdge;
        successors = handleAssumption(intervalElement, assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption());
        break;

      case BlankEdge:
        successor = intervalElement.clone();
        break;

      case FunctionCallEdge:
        FunctionCallEdge functionCallEdge = (FunctionCallEdge)cfaEdge;
        successor = handleFunctionCall(intervalElement, functionCallEdge, cfaEdge);
        break;

      // this is a return edge from function, this is different from return statement
      // of the function. See case in statement edge for details
      case FunctionReturnEdge:
        successor = handleFunctionReturn(intervalElement, (FunctionReturnEdge)cfaEdge);
        break;

      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    if(successors != null)
      return successors;

    else if(successor == null)
      return noSuccessors();

    else
      return soleSuccessor(successor);
  }

  /**
   * Handles return from one function to another function.
   *
   * @param element previous abstract element.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract element.
   */
  private IntervalAnalysisState handleFunctionReturn(IntervalAnalysisState element, FunctionReturnEdge functionReturnEdge)
    throws UnrecognizedCCodeException
  {
    CallToReturnEdge summaryEdge = functionReturnEdge.getSuccessor().getEnteringSummaryEdge();

    IASTFunctionCall expression = summaryEdge.getExpression();

    IntervalAnalysisState newElement = element.getPreviousState().clone();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);
    if(expression instanceof IASTFunctionCallAssignmentStatement)
    {
      IASTFunctionCallAssignmentStatement funcExp = (IASTFunctionCallAssignmentStatement)expression;

      IASTExpression operand1 = funcExp.getLeftHandSide();

      // left hand side of the expression has to be a variable
      if((operand1 instanceof IASTIdExpression) || (operand1 instanceof IASTFieldReference))
      {
        String assignedVariableName = operand1.toASTString();

        String returnedVariableName = calledFunctionName + "::" + RETURN_VARIABLE_BASE_NAME;

        for(String globalVar : globalVars)
        {
          // if the assigned variable represents global variable, set the global variable to the value of the returning variable or unknown
          if(globalVar.equals(assignedVariableName))
          {
            Interval interval = element.contains(returnedVariableName) ? element.getInterval(returnedVariableName) : Interval.createUnboundInterval();

            newElement.addInterval(globalVar, interval, this.threshold);
          }

          // import the global variables into the scope of the called function
          else
          {
            Interval interval = element.contains(globalVar) ? element.getInterval(globalVar) : Interval.createUnboundInterval();

            newElement.addInterval(globalVar, interval, this.threshold);
          }
        }

        // set the value of the assigned variable to the value of the returned variable
        if(!globalVars.contains(assignedVariableName))
        {
          Interval interval = element.contains(returnedVariableName) ? element.getInterval(returnedVariableName) : Interval.createUnboundInterval();

          newElement.addInterval(constructVariableName(assignedVariableName, callerFunctionName), interval, this.threshold);
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if(operand1 instanceof IASTUnaryExpression && ((IASTUnaryExpression)operand1).getOperator() == UnaryOperator.STAR)
          return element.clone();

      else
        throw new UnrecognizedCCodeException("on function return", summaryEdge, operand1);
    }

    // import the global variables back into the scope of the calling function
    else if(expression instanceof IASTFunctionCallStatement)
    {
      for(String globalVar : globalVars)
      {
          Interval interval = element.contains(globalVar) ? element.getInterval(globalVar) : Interval.createUnboundInterval();

          newElement.addInterval(globalVar, interval, this.threshold);
      }
    }

    else
      throw new UnrecognizedCCodeException("on function return", summaryEdge, expression.asStatement());

    return newElement;
  }

  /**
   * This method handles function calls.
   *
   * @param previousElement the previous element of the analysis, before the function call
   * @param callEdge the respective CFA edge
   * @return the successor element
   * @throws UnrecognizedCCodeException
   */
  private IntervalAnalysisState handleFunctionCall(IntervalAnalysisState previousElement, FunctionCallEdge callEdge, CFAEdge edge)
    throws UnrecognizedCCodeException
  {
    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();

    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> parameterNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments  = callEdge.getArguments();

    assert(parameterNames.size() == arguments.size());

    IntervalAnalysisState newElement = new IntervalAnalysisState(previousElement);

    // import global variables into the current scope first
    for(String globalVar : globalVars)
    {
      if(previousElement.contains(globalVar))
        newElement.addInterval(globalVar, previousElement.getInterval(globalVar), threshold);
    }

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(previousElement, callerFunctionName, edge);

    // set the interval of each formal parameter to the interval of its respective actual parameter
    for(int i = 0; i < arguments.size(); i++)
    {
      //Interval interval = evaluateInterval(previousElement, arguments.get(i), callerFunctionName, callEdge);
      // get value of actual parameter in caller function context
      Interval interval = arguments.get(i).accept(visitor);

      String formalParameterName = constructVariableName(parameterNames.get(i), calledFunctionName);

      newElement.addInterval(formalParameterName, interval, this.threshold);
    }

    return newElement;
  }

  /**
   * This method handles the statement edge which leads the function to the last node of its CFA (not same as a return edge).
   *
   * @param element the analysis element
   * @param expression the expression
   * @param ReturnStatementEdge the CFA edge corresponding to this statement
   * @return the successor elements
   */
  private IntervalAnalysisState handleExitFromFunction(IntervalAnalysisState element, IASTExpression expression, ReturnStatementEdge returnEdge, CFAEdge edge)
    throws UnrecognizedCCodeException
  {
    if (expression == null)
      expression = NumericTypes.ZERO; // this is the default in C

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(element, returnEdge.getPredecessor().getFunctionName(), edge);

    // assign the value of the function return to a new variable
    return handleAssignmentToVariable(RETURN_VARIABLE_BASE_NAME, expression, visitor);
  }

  /**
   * This method handles assumptions.
   *
   * @param element the analysis element
   * @param expression the expression containing the assumption
   * @param cfaEdge the CFA edge corresponding to this expression
   * @param truthValue flag to determine whether this is the then- or the else-branch of the assumption
   * @return the successor elements
   */
  private Collection<? extends AbstractState> handleAssumption(IntervalAnalysisState element, IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
    throws UnrecognizedCCodeException
  {
    // first, unpack the expression to deal with a raw assumption
    if(expression instanceof IASTUnaryExpression)
    {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);

      switch(unaryExp.getOperator())
      {
        // remove negation
        case NOT:
          return handleAssumption(element, unaryExp.getOperand(), cfaEdge, !truthValue);

        case STAR:
          // *exp - don't know anything
          return soleSuccessor(element.clone());

        default:
          throw new UnrecognizedCCodeException(cfaEdge, unaryExp);
      }
    }

    // a plain (boolean) identifier, e.g. if(a)
    else if(expression instanceof IASTIdExpression)
        return handleAssumption(element, convertToBinaryAssume((IASTIdExpression)expression), cfaEdge, truthValue);

    // a binary expression, e.g. if(exp1 op exp2)
    else if(expression instanceof IASTBinaryExpression)
    {
      IntervalAnalysisState newElement = element.clone();

      BinaryOperator operator = ((IASTBinaryExpression)expression).getOperator();
      IASTExpression operand1 = ((IASTBinaryExpression)expression).getOperand1();
      IASTExpression operand2 = ((IASTBinaryExpression)expression).getOperand2();

      ExpressionValueVisitor visitor = new ExpressionValueVisitor(newElement, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);

      Interval interval1 = operand1.accept(visitor);
      Interval interval2 = operand2.accept(visitor);

      switch(operator)
      {
        case MINUS:
        case PLUS:
          Interval result = null;

          if(operator == BinaryOperator.MINUS)
            result = interval1.minus(interval2);

          else if(operator == BinaryOperator.PLUS)
            result = interval1.plus(interval2);

          // in then-branch and interval maybe true, or in else-branch and interval maybe false, add a successor
          if((truthValue && !result.isFalse()) || (!truthValue && !result.isTrue()))
            return soleSuccessor(newElement);

          else
            return noSuccessors();

        case EQUALS:
        case NOT_EQUALS:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case LESS_THAN:
        case LESS_EQUAL:
          return processAssumption(newElement, operator, operand1, operand2, truthValue, cfaEdge);

        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
          return soleSuccessor(newElement);

        default:
          throw new UnrecognizedCCodeException(cfaEdge, expression);
      }
    }

    return noSuccessors();
  }

  private Collection<? extends AbstractState> processAssumption(IntervalAnalysisState element, BinaryOperator operator, IASTExpression operand1, IASTExpression operand2, boolean truthValue, CFAEdge cfaEdge) throws UnrecognizedCCodeException
  {
    if(!truthValue)
      return processAssumption(element, negateOperator(operator), operand1, operand2, !truthValue, cfaEdge);

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(element, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);

    Interval orgInterval1 = operand1.accept(visitor);
    Interval orgInterval2 = operand2.accept(visitor);

    //Interval orgInterval1 = evaluateInterval(element, operand1, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);
    Interval tmpInterval1 = orgInterval1.clone();

    //Interval orgInterval2 = evaluateInterval(element, operand2, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);
    Interval tmpInterval2 = orgInterval2.clone();

    String variableName1 = constructVariableName(operand1.toASTString(), cfaEdge.getPredecessor().getFunctionName());
    String variableName2 = constructVariableName(operand2.toASTString(), cfaEdge.getPredecessor().getFunctionName());

    // determine whether or not the respective operand is an identifier
    boolean isIdOp1 = operand1 instanceof IASTIdExpression;
    boolean isIdOp2 = operand2 instanceof IASTIdExpression;

    // a < b, a < 1
    if(operator == BinaryOperator.LESS_THAN)
    {
      // a may be less than b, so there can be a successor
      if(tmpInterval1.mayBeLessThan(tmpInterval2))
      {
        if(isIdOp1) element.addInterval(variableName1, orgInterval1.limitUpperBoundBy(tmpInterval2.minus(1L)), threshold);
        if(isIdOp2) element.addInterval(variableName2, orgInterval2.limitLowerBoundBy(tmpInterval1.plus(1L)), threshold);
      }

      // a is always greater than b, so a can't be less than b, so there can't be a successor
      else
        return noSuccessors();
    }

    // a <= b, a <= 1
    else if(operator == BinaryOperator.LESS_EQUAL)
    {
      // a may be less or equal than b, so there can be a successor
      if(tmpInterval1.mayBeLessOrEqualThan(tmpInterval2))
      {
        if(isIdOp1) element.addInterval(variableName1, orgInterval1.limitUpperBoundBy(tmpInterval2), threshold);
        if(isIdOp2) element.addInterval(variableName2, orgInterval2.limitLowerBoundBy(tmpInterval1), threshold);
      }

      // a is always greater than b, so a can't be less than b, so there can't be a successor
      else
        return noSuccessors();
    }

    // a > b, a > 1
    else if(operator == BinaryOperator.GREATER_THAN)
    {
      // a may be greater than b, so there can be a successor
      if(tmpInterval1.mayBeGreaterThan(tmpInterval2))
      {
        if(isIdOp1) element.addInterval(variableName1, orgInterval1.limitLowerBoundBy(tmpInterval2.plus(1L)), threshold);
        if(isIdOp2) element.addInterval(variableName2, orgInterval2.limitUpperBoundBy(tmpInterval1.minus(1L)), threshold);
      }

      // a is always less than b, so a can't be greater than b, so there can't be a successor
      else
        return noSuccessors();
    }

    // a >= b, a >= 1
    else if(operator == BinaryOperator.GREATER_EQUAL)
    {
      // a may be greater or equal than b, so there can be a successor
      if(tmpInterval1.mayBeGreaterOrEqualThan(tmpInterval2))
      {
        if(isIdOp1) element.addInterval(variableName1, orgInterval1.limitLowerBoundBy(tmpInterval2), threshold);
        if(isIdOp2) element.addInterval(variableName2, orgInterval2.limitUpperBoundBy(tmpInterval1), threshold);
      }

      // a is always less than b, so a can't be greater than b, so there can't be a successor
      else
        return noSuccessors();
    }

    // a == b, a == 1
    else if(operator == BinaryOperator.EQUALS)
    {
      // a and b intersect, so they may have the same value, so they may be equal
      if(tmpInterval1.intersects(tmpInterval2))
      {
        if(isIdOp1) element.addInterval(variableName1, orgInterval1.intersect(tmpInterval2), threshold);
        if(isIdOp2) element.addInterval(variableName2, orgInterval2.intersect(tmpInterval1), threshold);
      }

      // a and b do not intersect, so they can't be equal, so there can't be a successor
      else
        return noSuccessors();
    }

    // a != b, a != 1
    else if(operator == BinaryOperator.NOT_EQUALS)
    {
      // a = [x, x] = b => a and b are always equal, so there can't be a successor
      if(tmpInterval1.isSingular() && tmpInterval1.equals(tmpInterval2))
        return noSuccessors();

      // TODO: currently depends on the fact that operand1 is a identifier, while operand2 is a literal
      if(splitIntervals && isIdOp1 && !isIdOp2)
      {
        IntervalAnalysisState newElement = null;

        Collection<IntervalAnalysisState> successors = new LinkedList<IntervalAnalysisState>();

        Interval result = null;

        if(!(result = orgInterval1.intersect(Interval.createUpperBoundedInterval(orgInterval2.getLow() - 1L))).isEmpty())
        {
          newElement = element.clone();

          newElement.addInterval(variableName1, result, threshold);

          successors.add(newElement);
        }

        if(!(result = orgInterval1.intersect(Interval.createLowerBoundedInterval(orgInterval2.getLow() + 1L))).isEmpty())
        {
          newElement = element.clone();

          newElement.addInterval(variableName1, result, threshold);

          successors.add(newElement);
        }

        return successors;
      }
    }
    else
      throw new UnrecognizedCCodeException("unknown operator", cfaEdge);

    return soleSuccessor(element);
  }

  /**
   * This method return the negated counter part for a given operator
   *
   * @param operator
   * @return the negated counter part of the given operator
   */
  private BinaryOperator negateOperator(BinaryOperator operator)
  {
    switch(operator) {
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
   * @param element the analysis element
   * @param declarationEdge the CFA edge
   * @return the successor element
   */
  private IntervalAnalysisState handleDeclaration(IntervalAnalysisState element, DeclarationEdge declarationEdge)
  throws UnrecognizedCCodeException
  {
    IntervalAnalysisState newElement = element.clone();
    if (declarationEdge.getDeclaration() instanceof IASTVariableDeclaration) {
        IASTVariableDeclaration decl = (IASTVariableDeclaration)declarationEdge.getDeclaration();

        // ignore pointer variables
        if (decl.getDeclSpecifier() instanceof IASTPointerTypeSpecifier)
          return newElement;

        // if this is a global variable, add it to the list of global variables
        if(decl.isGlobal())
        {
          globalVars.add(decl.getName().toString());

          Interval interval;

          IASTInitializer init = decl.getInitializer();

          // global variables may be initialized explicitly on the spot ...
          if(init instanceof IASTInitializerExpression)
          {
            IASTExpression exp = ((IASTInitializerExpression)init).getExpression();

            interval = evaluateInterval(element, exp, "", declarationEdge);
          }

          // ... or implicitly initialized to 0
          else
            interval = new Interval(0L);

          String varName = constructVariableName(decl.getName().toString(), "");

          newElement.addInterval(varName, interval, this.threshold);
        }

        // non-global variables are initialized with an unbound interval
        else
        {
          String varName = constructVariableName(decl.getName(), declarationEdge.getPredecessor().getFunctionName());

          newElement.addInterval(varName, Interval.createUnboundInterval(), this.threshold);
          //newElement.addInterval(varName, new Interval(0L), this.threshold);
        }
    }

    return newElement;
  }

  /**
   * This method handles unary and binary statements.
   *
   * @param element the analysis element
   * @param expression the current expression
   * @param cfaEdge the CFA edge
   * @return the successor
   * @throws UnrecognizedCCodeException
   */
  private IntervalAnalysisState handleStatement(IntervalAnalysisState element, IASTStatement expression, CFAEdge cfaEdge)
    throws UnrecognizedCCodeException
  {
    // expression is an assignment operation, e.g. a = b;
    if(expression instanceof IASTAssignment)
      return handleAssignment(element, (IASTAssignment)expression, cfaEdge);

    // ext(); => do nothing
    else if(expression instanceof IASTFunctionCallStatement)
      return element.clone();

    // a; nothing to do
    else if(expression instanceof IASTExpressionStatement)
      return element.clone();

    else
      throw new UnrecognizedCCodeException(cfaEdge, expression);
  }

   /**
   * This method handles assignments.
   *
   * @param element the analysis element
   * @param assignExpression the expression containing the binary expression
   * @param declarationEdge the CFA edge
   * @return the successor element
   * TODO pointer dereferencing via strengthening
   */
  private IntervalAnalysisState handleAssignment(IntervalAnalysisState element, IASTAssignment assignExpression, CFAEdge cfaEdge)
    throws UnrecognizedCCodeException
  {
    IASTExpression op1 = assignExpression.getLeftHandSide();
    IASTRightHandSide op2 = assignExpression.getRightHandSide();

    // a = ?
    if(op1 instanceof IASTIdExpression)
    {
      ExpressionValueVisitor visitor = new ExpressionValueVisitor(element, cfaEdge.getPredecessor().getFunctionName(), cfaEdge);

      return handleAssignmentToVariable(((IASTIdExpression)op1).getName(), op2, visitor);
    }

    // TODO: assignment to pointer, *a = ?
    else if(op1 instanceof IASTUnaryExpression && ((IASTUnaryExpression)op1).getOperator() == UnaryOperator.STAR)
      return element.clone();

    // TODO assignment to field, a->b = ?
    else if (op1 instanceof IASTFieldReference)
      return element.clone();

    // TODO assignment to array cell, a[b] = ?
    else if (op1 instanceof IASTArraySubscriptExpression)
      return element.clone();

    else
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
  }

  /**
   * This method handles the assignment of a variable.
   *
   * @param element the analysis element
   * @param lParam the local name of the variable to assign to
   * @param rightExp the assigning expression
   * @param cfaEdge the respective CFA edge
   * @return the successor element
   */
  private IntervalAnalysisState handleAssignmentToVariable(String lParam, IASTRightHandSide expression, ExpressionValueVisitor v)
    throws UnrecognizedCCodeException
  {
    Interval value = expression.accept(v);

    IntervalAnalysisState newElement = v.element.clone();
    String variableName = constructVariableName(lParam, v.functionName);

    newElement.addInterval(variableName, value, this.threshold);

    return newElement;
  }

  /**
   * This method evaluates an expression and returns the respective interval.
   *
   * @param element the analysis element
   * @param expression the expression containing the expression to be evaluated
   * @param functionName the name of the function currently being analyzed
   * @param cfaEdge the respective CFA edge
   * @return the interval in respect to the evaluated expression of null, if the expression could not be evaluated properly
   */
  //getExpressionValue
  private Interval evaluateInterval(IntervalAnalysisState element, IASTRightHandSide expression, String functionName, CFAEdge cfaEdge)
    throws UnrecognizedCCodeException
  {
    if(expression instanceof IASTLiteralExpression)
    {
      Long value = parseLiteral((IASTLiteralExpression)expression, cfaEdge);

      return (value == null) ? Interval.createUnboundInterval() : new Interval(value, value);
    }

    else if(expression instanceof IASTIdExpression)
    {
      String varName = constructVariableName(((IASTIdExpression)expression).getName(), functionName);

      return (element.contains(varName)) ? element.getInterval(varName) : Interval.createUnboundInterval();
    }

    else if(expression instanceof IASTCastExpression)
      return evaluateInterval(element, ((IASTCastExpression)expression).getOperand(), functionName, cfaEdge);

    else if(expression instanceof IASTUnaryExpression)
    {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

      switch(unaryOperator)
      {
        case MINUS:
          Interval interval = evaluateInterval(element, unaryOperand, functionName, cfaEdge);

          return (interval == null) ? Interval.createUnboundInterval() : interval.negate();

        default:
          throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    }

    // added for expression "if (! (req_a___0 + 50 == rsp_d___0))" in "systemc/mem_slave_tlm.1.cil.c"
    else if(expression instanceof IASTBinaryExpression)
    {
      IASTBinaryExpression binaryExpression = (IASTBinaryExpression)expression;

      Interval interval1 = evaluateInterval(element, binaryExpression.getOperand1(), functionName, cfaEdge);
      Interval interval2 = evaluateInterval(element, binaryExpression.getOperand2(), functionName, cfaEdge);

      switch(binaryExpression.getOperator())
      {
        case PLUS:
          return interval1.plus(interval2);

        default:
          throw new UnrecognizedCCodeException("unknown binary operator", cfaEdge, binaryExpression);
      }
    }

    // TODO fields, arrays
    else
      //throw new UnrecognizedCCodeException(cfaEdge, expression);
      return Interval.createUnboundInterval();
  }

  /**
   * This method parses an expression to retrieve its literal value.
   *
   * @param expression the expression to parse
   * @return a number or null if the parsing failed
   * @throws UnrecognizedCCodeException
   */
  private static Long parseLiteral(IASTLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException
  {
    if (expression instanceof IASTIntegerLiteralExpression) {
      return ((IASTIntegerLiteralExpression)expression).asLong();

    } else if (expression instanceof IASTFloatLiteralExpression) {
      return null;

    } else if (expression instanceof IASTCharLiteralExpression) {
      return (long)((IASTCharLiteralExpression)expression).getCharacter();

    } else if (expression instanceof IASTStringLiteralExpression) {
      return null;

    } else {
      throw new UnrecognizedCCodeException("unknown literal", edge, expression);
    }
  }

  /**
   * This method created a scoped variable name.
   *
   * @param variableName
   * @param functionName
   * @return a scoped variable name
   */
  public String constructVariableName(String variableName, String functionName)
  {
    if(globalVars.contains(variableName))
      return variableName;

    return functionName + "::" + variableName;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements,
      CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException
  {
    assert element instanceof IntervalAnalysisState;
    IntervalAnalysisState intervalElement = (IntervalAnalysisState)element;

    for(AbstractState elem : elements)
    {
      if(elem instanceof PointerState)
        return strengthen(intervalElement, (PointerState)elem, cfaEdge, precision);
    }

    return null;
  }

  private Collection<? extends AbstractState> strengthen(IntervalAnalysisState intervalElement,
      PointerState pointerElement, CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException
  {
    return null;
  }

  private Collection<? extends AbstractState> soleSuccessor(AbstractState successor)
  {
    return Collections.singleton(successor);
  }

  private Collection<? extends AbstractState> noSuccessors()
  {
    return Collections.emptySet();
  }

  private IASTBinaryExpression convertToBinaryAssume(IASTIdExpression expression)
  {
    IASTIntegerLiteralExpression zero = new IASTIntegerLiteralExpression(expression.getFileLocation(),
                                                                         expression.getExpressionType(),
                                                                         BigInteger.ZERO);

    return new IASTBinaryExpression(expression.getFileLocation(),
                                    expression.getExpressionType(),
                                    expression,
                                    zero,
                                    BinaryOperator.NOT_EQUALS);
  }

  /**
   * Visitor that get's the value from an expression.
   * The result may be null, i.e., the value is unknown.
   */
  private class ExpressionValueVisitor extends DefaultExpressionVisitor<Interval, UnrecognizedCCodeException>
                                       implements RightHandSideVisitor<Interval, UnrecognizedCCodeException> {

    protected final IntervalAnalysisState element;

    protected final String functionName;

    protected final CFAEdge cfaEdge;

    public ExpressionValueVisitor(IntervalAnalysisState pElement, String pFunctionName, CFAEdge edge) {
      element = pElement;
      functionName = pFunctionName;
      cfaEdge = edge;
    }

    // TODO fields, arrays

    @Override
    protected Interval visitDefault(IASTExpression expression) {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(IASTBinaryExpression binaryExpression) throws UnrecognizedCCodeException
    {
      Interval interval1 = binaryExpression.getOperand1().accept(this);
      Interval interval2 = binaryExpression.getOperand2().accept(this);

      if(interval1 == null || interval2 == null)
        return Interval.createUnboundInterval();

      switch (binaryExpression.getOperator())
      {
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
    public Interval visit(IASTCastExpression cast) throws UnrecognizedCCodeException {
      return cast.getOperand().accept(this);
    }

    @Override
    public Interval visit(IASTFunctionCallExpression functionCall) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(IASTCharLiteralExpression charLiteral) throws UnrecognizedCCodeException {
      Long l = parseLiteral(charLiteral, cfaEdge);
      return l == null ? Interval.createUnboundInterval() : new Interval(l);
    }

    @Override
    public Interval visit(IASTFloatLiteralExpression floatLiteral) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(IASTIntegerLiteralExpression integerLiteral) throws UnrecognizedCCodeException {
      return new Interval(parseLiteral(integerLiteral, cfaEdge));
    }

    @Override
    public Interval visit(IASTStringLiteralExpression stringLiteral) throws UnrecognizedCCodeException {
      return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(IASTIdExpression identifier) throws UnrecognizedCCodeException
    {
      if(identifier.getDeclaration() instanceof IASTEnumerator)
        return new Interval(((IASTEnumerator)identifier.getDeclaration()).getValue());

      String variableName = constructVariableName(identifier.getName(), functionName);
      if (element.contains(variableName))
        return element.getInterval(variableName);

      else
        return Interval.createUnboundInterval();
    }

    @Override
    public Interval visit(IASTUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

      Interval interval = unaryOperand.accept(this);

      switch (unaryOperator) {

      case MINUS:
        return (interval != null) ? interval.negate() : Interval.createUnboundInterval();

      case NOT:
        if(interval.isFalse())
          return Interval.createTrueInterval();

        else if(interval.isTrue())
          return Interval.createFalseInterval();

        else
          return new Interval(0L, 1L);

      case AMPER:
        return Interval.createUnboundInterval(); // valid expression, but it's a pointer value

      case STAR:
        return Interval.createUnboundInterval();

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    }
  }
}

