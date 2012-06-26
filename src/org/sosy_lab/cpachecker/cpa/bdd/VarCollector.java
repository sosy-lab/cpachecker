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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


public class VarCollector {

  /** name for return-variables, it is used for function-returns. */
  protected static final String FUNCTION_RETURN_VARIABLE = "__CPAchecker_return_var";
  protected static final String TMP_VARIABLE = "__CPAchecker_tmp_var";


  private final CFA cfa;
  private Collection<String> allVars = new HashSet<String>();
  private Collection<String> nonBooleanVars = new HashSet<String>();
  private Map<String, String> dependencies = new HashMap<String, String>();
  private Collection<String> booleanVars = new HashSet<String>();

  public VarCollector(CFA cfa) {
    this.cfa = cfa;
  }

  public void collectBooleanVars() throws UnrecognizedCCodeException {
    Collection<CFANode> nodes = cfa.getAllNodes();
    for (CFANode node : nodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        handleEdge(edge, allVars, nonBooleanVars, dependencies);
      }
    }

    // if a value is nonbool, all dependent vars are nonbool
    for (Entry<String, String> entry : dependencies.entrySet()) {
      if (nonBooleanVars.contains(entry.getKey())) {
        nonBooleanVars.add(entry.getValue());
      }
      if (nonBooleanVars.contains(entry.getValue())) {
        nonBooleanVars.add(entry.getKey());
      }
    }

    for (String s : allVars) {
      if (!nonBooleanVars.contains(s)) {
        booleanVars.add(s);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\nDEPENDENCIES");
    // if a value is nonbool, all dependent vars are nonbool
    for (Entry<String, String> entry : dependencies.entrySet()) {
      str.append(entry.getKey() + " --> " + entry.getValue());
    }

    str.append("\nALL\n    " + Arrays.toString(allVars.toArray()).replace(", ", ",\n    "));
    str.append("\nNONBOOL\n    " + Arrays.toString(nonBooleanVars.toArray()).replace(", ", ",\n    "));
    str.append("\nBOOL\n    " + Arrays.toString(booleanVars.toArray()).replace(", ", ",\n    "));
    return str.toString();
  }

  private void handleEdge(CFAEdge edge, Collection<String> allVars,
      Collection<String> nonBooleanVars, Map<String, String> dependencies)
      throws UnrecognizedCCodeException {
    switch (edge.getEdgeType()) {

    case AssumeEdge: {
      CExpression exp = ((CAssumeEdge) edge).getExpression();
      BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor(), allVars, nonBooleanVars);
      Collection<String> possibleBoolean = exp.accept(bcv); // can be null!
      break;
    }

    case DeclarationEdge: {
      CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
      if (!(declaration instanceof CVariableDeclaration)) { return; }

      CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
      String varName = buildVarName(vdecl.getName(), vdecl.isGlobal(), edge.getPredecessor().getFunctionName());
      allVars.add(varName);

      CInitializer initializer = vdecl.getInitializer();
      if ((initializer == null) || !(initializer instanceof CInitializerExpression)) { return; }

      CExpression exp = ((CInitializerExpression) initializer).getExpression();
      if (exp == null) { return; }


      BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor(), allVars, nonBooleanVars);
      Collection<String> possibleBoolean = exp.accept(bcv);

      if (possibleBoolean == null) { // non boolean
        nonBooleanVars.add(varName);
      } else {
        for (String s : possibleBoolean) {
          dependencies.put(varName, s);
        }
      }
      break;
    }

    case StatementEdge: {
      CStatement statement = ((CStatementEdge) edge).getStatement();
      if (!(statement instanceof CAssignment)) { return; }

      CAssignment assignment = (CAssignment) statement;
      CRightHandSide rhs = assignment.getRightHandSide();
      if (!(rhs instanceof CExpression)) { return; }

      CExpression lhs = assignment.getLeftHandSide();
      String varName = buildVarName(lhs.toASTString(), isGlobal(lhs), edge.getPredecessor().getFunctionName());
      allVars.add(varName);

      CExpression rhsExp = (CExpression) rhs;
      BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor(), allVars, nonBooleanVars);
      Collection<String> possibleBoolean = rhsExp.accept(bcv);

      if (possibleBoolean == null) { // non boolean
        nonBooleanVars.add(varName);
      } else {
        for (String s : possibleBoolean) {
          dependencies.put(varName, s);
        }
      }
      break;
    }

    case FunctionCallEdge:
      CFunctionCallEdge functionCall = (CFunctionCallEdge) edge;

      // overtake arguments from last functioncall into function,
      // get args from functioncall and make them equal with params from functionstart
      List<CExpression> args = functionCall.getArguments();
      List<CParameterDeclaration> params = functionCall.getSuccessor().getFunctionParameters();
      String innerFunctionName = functionCall.getSuccessor().getFunctionName();
      assert args.size() == params.size();

      for (int i = 0; i < args.size(); i++) {

        // build name for param, this variable is not global (->false)
        String varName = buildVarName(params.get(i).getName(), false, innerFunctionName);

        // make dependency for var and arg
        BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor(), allVars, nonBooleanVars);
        Collection<String> possibleBoolean = args.get(i).accept(bcv);

        if (possibleBoolean == null) { // non boolean
          nonBooleanVars.add(varName);
        } else {
          for (String s : possibleBoolean) {
            dependencies.put(varName, s);
          }
        }
      }
      break;

    case FunctionReturnEdge: {
      CFunctionReturnEdge functionReturn = (CFunctionReturnEdge) edge;
      // set result of function equal to variable on left side
      CFunctionSummaryEdge fnkCall = functionReturn.getSummaryEdge();
      CStatement call = fnkCall.getExpression().asStatement();

      // build name for RIGHT SIDE
      String retVar = buildVarName(FUNCTION_RETURN_VARIABLE,
          false, functionReturn.getPredecessor().getFunctionName());

      // handle assignments like "y = f(x);"
      if (call instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) call;
        CExpression lhs = cAssignment.getLeftHandSide();

        // build name for LEFT SIDE of assignment,
        String varName = buildVarName(lhs.toASTString(), isGlobal(lhs),
            functionReturn.getSuccessor().getFunctionName());
        dependencies.put(varName, retVar);
      }
      break;
    }

    case ReturnStatementEdge: {
      CReturnStatementEdge returnStatement = (CReturnStatementEdge) edge;
      // make variable for returnStatement,
      String varName = buildVarName(FUNCTION_RETURN_VARIABLE,
          false, edge.getPredecessor().getFunctionName());

      // make region for RIGHT SIDE, this is the 'x' from 'return (x);
      CRightHandSide rhs = returnStatement.getExpression();
      if (rhs instanceof CExpression) {

        // make dependency for var and arg
        BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor(), allVars, nonBooleanVars);
        Collection<String> possibleBoolean = ((CExpression) rhs).accept(bcv);

        if (possibleBoolean == null) { // non boolean
          nonBooleanVars.add(varName);
        } else {
          for (String s : possibleBoolean) {
            dependencies.put(varName, s);
          }
        }
      }
      break;
    }

    case BlankEdge:
    case CallToReturnEdge:
    default:
      // other cases are not interesting
    }
  }

  public static boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  public static String buildVarName(String variableName, boolean isGlobal, String function) {
    if (isGlobal) {
      return variableName;
    } else {
      return function + "::" + variableName;
    }
  }




  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression is not boolean
   * - a collection, if the expression is boolean.
   * The collection contains all boolean vars. */
  private class BoolCollectingVisitor implements CExpressionVisitor<Collection<String>, UnrecognizedCCodeException> {

    CFANode predecessor;
    Collection<String> nonBooleanVars;
    Collection<String> allVars;

    public BoolCollectingVisitor(CFANode pre, Collection<String> allVars, Collection<String> nonBoolean) {
      this.predecessor = pre;
      this.nonBooleanVars = nonBoolean;
      this.allVars = allVars;
    }

    @Override
    public Collection<String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Collection<String> visit(CBinaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> operand1 = exp.getOperand1().accept(this);
      Collection<String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
        if (operand1 != null) {
          nonBooleanVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonBooleanVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case LOGICAL_AND:
      case LOGICAL_OR:
      case EQUALS:
      case NOT_EQUALS: // &&, ||, ==, != work with boolean operands
        Collection<String> result = new HashSet<String>();
        result.addAll(operand1);
        result.addAll(operand2);
        return result;

      default: // +-*/ --> no boolean operators
        nonBooleanVars.addAll(operand1); // a+b --> a and b are not boolean
        nonBooleanVars.addAll(operand2);
        return null;
      }
    }

    @Override
    public Collection<String> visit(CCastExpression exp) throws UnrecognizedCCodeException {
      return exp.getOperand().accept(this);
    }

    @Override
    public Collection<String> visit(CFieldReference exp) {
      String varName = buildVarName(exp.getFieldName(), isGlobal(exp), predecessor.getFunctionName());
      allVars.add(varName);
      return Collections.singleton(varName);
    }

    @Override
    public Collection<String> visit(CIdExpression exp) {
      String varName = buildVarName(exp.getName(), isGlobal(exp), predecessor.getFunctionName());
      allVars.add(varName);
      return Collections.singleton(varName);
    }

    @Override
    public Collection<String> visit(CCharLiteralExpression exp) {
      return null;
    }

    @Override
    public Collection<String> visit(CFloatLiteralExpression exp) {
      return null;
    }

    @Override
    public Collection<String> visit(CIntegerLiteralExpression exp) {
      BigInteger value = exp.getValue();
      if (BigInteger.ZERO.equals(value) || BigInteger.ONE.equals(value)) {
        return Collections.emptySet();
      } else {
        return null;
      }
    }

    @Override
    public Collection<String> visit(CStringLiteralExpression exp) {
      return null;
    }

    @Override
    public Collection<String> visit(CTypeIdExpression exp) {
      return null;
    }

    @Override
    public Collection<String> visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> inner = exp.getOperand().accept(this);

      if (UnaryOperator.NOT == exp.getOperator()) {
        // boolean operation, return inner vars
        return inner;
      } else { // PLUS, MINUS, etc --> not boolean
        return null;
      }
    }
  }
}
