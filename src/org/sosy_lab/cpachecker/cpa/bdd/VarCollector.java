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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
  private static final String FUNCTION_RETURN_VARIABLE = "__CPAchecker_return_var";

  private final CFA cfa;
  private Collection<String> allVars = new HashSet<String>();

  private Collection<String> nonBooleanVars = new HashSet<String>();
  private Map<String, String> boolDependencies = new HashMap<String, String>();

  private Collection<String> nonSimpleNumberVars = new HashSet<String>();
  private Map<String, String> simpleNumberDependencies = new HashMap<String, String>();

  private Collection<String> nonIncVars = new HashSet<String>();
  private Map<String, String> incDependencies = new HashMap<String, String>();

  private Collection<String> booleanVars = new HashSet<String>();
  private Collection<String> simpleNumberVars = new HashSet<String>();
  private Collection<String> incVars = new HashSet<String>();

  public VarCollector(CFA cfa) {
    this.cfa = cfa;
  }

  public void collectBooleanVars() throws UnrecognizedCCodeException {
    Collection<CFANode> nodes = cfa.getAllNodes();
    for (CFANode node : nodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        handleEdge(edge);
      }
    }

    // if a value is nonbool, all dependent vars are nonbool and viceversa
    addDependVars(nonBooleanVars, boolDependencies);

    // if a value is no simple number, all dependent vars are no simple numbers and viceversa
    addDependVars(nonSimpleNumberVars, simpleNumberDependencies);

    // if a value is not incremented, all dependent vars are not incremented and viceversa
    addDependVars(nonIncVars, incDependencies);


    for (String s : allVars) {
      if (!nonBooleanVars.contains(s)) {
        booleanVars.add(s);
      }
    }

    for (String s : allVars) {
      if (!nonSimpleNumberVars.contains(s) && nonBooleanVars.contains(s)) {
        simpleNumberVars.add(s);
      }
    }

    for (String s : allVars) {
      if (!nonIncVars.contains(s) && nonBooleanVars.contains(s)) {
        incVars.add(s);
      }
    }
  }

  private void handleEdge(CFAEdge edge)
      throws UnrecognizedCCodeException {
    switch (edge.getEdgeType()) {

    case AssumeEdge: {
      CExpression exp = ((CAssumeEdge) edge).getExpression();
      BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor());
      exp.accept(bcv);

      NumberCollectingVisitor ncv = new NumberCollectingVisitor(edge.getPredecessor());
      exp.accept(ncv);

      IncCollectingVisitor icv = new IncCollectingVisitor(edge.getPredecessor());
      exp.accept(icv);

      break;
    }

    case DeclarationEdge: {
      CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
      if (!(declaration instanceof CVariableDeclaration)) { return; }

      CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
      String varName = buildVarName(vdecl.getName(), vdecl.isGlobal(),
          edge.getPredecessor().getFunctionName());
      allVars.add(varName);

      CInitializer initializer = vdecl.getInitializer();
      if ((initializer == null) || !(initializer instanceof CInitializerExpression)) { return; }

      CExpression exp = ((CInitializerExpression) initializer).getExpression();
      if (exp == null) { return; }

      handleExpression(edge, exp, varName);

      break;
    }

    case StatementEdge: {
      CStatement statement = ((CStatementEdge) edge).getStatement();
      if (!(statement instanceof CAssignment)) { return; }

      CAssignment assignment = (CAssignment) statement;
      CRightHandSide rhs = assignment.getRightHandSide();
      if (!(rhs instanceof CExpression)) { return; }

      CExpression lhs = assignment.getLeftHandSide();
      String varName = buildVarName(lhs.toASTString(), isGlobal(lhs),
          edge.getPredecessor().getFunctionName());
      allVars.add(varName);

      handleExpression(edge, ((CExpression) rhs), varName);

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
        handleExpression(edge, args.get(i), varName);
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
        boolDependencies.put(varName, retVar);
        simpleNumberDependencies.put(varName, retVar);
        incDependencies.put(varName, retVar);
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
        handleExpression(edge, ((CExpression) rhs), varName);
      }
      break;
    }

    case BlankEdge:
    case CallToReturnEdge:
    default:
      // other cases are not interesting
    }
  }

  /** evaluates an expression and adds containing vars to the sets. */
  private void handleExpression(CFAEdge edge, CExpression exp, String varName)
      throws UnrecognizedCCodeException {
    BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor());
    Collection<String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, possibleBoolean, nonBooleanVars, boolDependencies);

    NumberCollectingVisitor ncv = new NumberCollectingVisitor(edge.getPredecessor());
    Collection<String> possibleNumbers = exp.accept(ncv);
    handleResult(varName, possibleNumbers, nonSimpleNumberVars, simpleNumberDependencies);

    IncCollectingVisitor icv = new IncCollectingVisitor(edge.getPredecessor());
    Collection<String> possibleIncs = exp.accept(icv);
    handleResult(varName, possibleIncs, nonIncVars, incDependencies);

  }

  private void addDependVars(Collection<String> vars, Map<String, String> dependencies) {
    for (Entry<String, String> entry : dependencies.entrySet()) {
      if (vars.contains(entry.getKey())) {
        vars.add(entry.getValue());
      }
      if (vars.contains(entry.getValue())) {
        vars.add(entry.getKey());
      }
    }
  }

  private void handleResult(String varName, Collection<String> result,
      Collection<String> vars, Map<String, String> dependencies) {
    if (result == null) {
      vars.add(varName);
    } else {
      for (String s : result) {
        dependencies.put(varName, s);
      }
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

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("\nALL  " + allVars.size() + "\n    "
        + Arrays.toString(allVars.toArray()).replace(", ", ",\n    "));

    //    str.append("\nBOOL DEPENDENCIES\n    ");
    //    for (Entry<String, String> entry : boolDependencies.entrySet()) {
    //      str.append(entry.getKey() + " --> " + entry.getValue() + "\n    ");
    //    }
    //    str.append("\nNONBOOL\n    " + Arrays.toString(nonBooleanVars.toArray()).replace(", ", ",\n    "));
    str.append("\nBOOL  " + booleanVars.size() + "\n    "
        + Arrays.toString(booleanVars.toArray()).replace(", ", ",\n    "));

    //    str.append("\nSIMPLE NUMBER DEPENDENCIES\n    ");
    //    for (Entry<String, String> entry : simpleNumberDependencies.entrySet()) {
    //      str.append(entry.getKey() + " --> " + entry.getValue() + "\n    ");
    //    }
    //    str.append("\nNO SIMPLE NUMBER\n    " + Arrays.toString(nonSimpleNumberVars.toArray()).replace(", ", ",\n    "));
    str.append("\nSIMPLE NUMBER  " + simpleNumberVars.size() + "\n    "
        + Arrays.toString(simpleNumberVars.toArray()).replace(", ", ",\n    "));

    //    str.append("\nINCREMENT DEPENDENCIES\n    ");
    //    for (Entry<String, String> entry : incDependencies.entrySet()) {
    //      str.append(entry.getKey() + " --> " + entry.getValue() + "\n    ");
    //    }
    //    str.append("\nNON INCREMENT\n    " + Arrays.toString(nonIncVars.toArray()).replace(", ", ",\n    "));
    str.append("\nINCREMENT  " + incVars.size() + "\n    "
        + Arrays.toString(incVars.toArray()).replace(", ", ",\n    "));

    return str.toString();
  }

  /** This Visitor evaluates an Expression. It collects all variables.
   * a visit of IdExpression or CFieldReference returns a collection containing the varName,
   * a visit of CastExpression return the containing visit,
   * other visits return null. */
  private class VarCollectingVisitor implements CExpressionVisitor<Collection<String>, UnrecognizedCCodeException> {

    private CFANode predecessor;

    public VarCollectingVisitor(CFANode pre) {
      this.predecessor = pre;
    }

    @Override
    public Collection<String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Collection<String> visit(CBinaryExpression exp) throws UnrecognizedCCodeException {
      exp.getOperand1().accept(this);
      exp.getOperand2().accept(this);
      return null;
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
      return null;
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
      return null;
    }
  }


  /** This Visitor evaluates an Expression. It also collects all variables.
   * Each visit-function returns
   * - null, if the expression is not boolean
   * - a collection, if the expression is boolean.
   * The collection contains all boolean vars. */
  private class BoolCollectingVisitor extends VarCollectingVisitor {

    public BoolCollectingVisitor(CFANode pre) {
      super(pre);
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
    public Collection<String> visit(CIntegerLiteralExpression exp) {
      BigInteger value = exp.getValue();
      if (BigInteger.ZERO.equals(value) || BigInteger.ONE.equals(value)) {
        return Collections.emptySet();
      } else {
        return null;
      }
    }

    @Override
    public Collection<String> visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> inner = exp.getOperand().accept(this);

      if (UnaryOperator.NOT == exp.getOperator()) {
        // boolean operation, return inner vars
        return inner;
      } else { // PLUS, MINUS, etc --> not boolean
        nonBooleanVars.addAll(inner); // -X --> X is not boolean
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression contains calculations
   * - a collection, if the expression is a number, unaryExp, == or != */
  private class NumberCollectingVisitor extends VarCollectingVisitor {

    public NumberCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Collection<String> visit(CBinaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> operand1 = exp.getOperand1().accept(this);
      Collection<String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
        if (operand1 != null) {
          nonSimpleNumberVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonSimpleNumberVars.addAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case EQUALS:
      case NOT_EQUALS: // ==, != work with numbers
        Collection<String> result = new HashSet<String>();
        result.addAll(operand1);
        result.addAll(operand2);
        return result;

      default: // +-*/ --> no boolean operators
        nonSimpleNumberVars.addAll(operand1); // a+b --> a and b are not boolean
        nonSimpleNumberVars.addAll(operand2);
        return null;
      }
    }

    @Override
    public Collection<String> visit(CIntegerLiteralExpression exp) {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> inner = exp.getOperand().accept(this);

      switch (exp.getOperator()) {
      case PLUS: // simple calculations, no usage of another param
      case MINUS:
        return inner;
      default: // *, ~, etc --> not numeral
        nonSimpleNumberVars.addAll(inner);
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression contains calculations
   * - a collection, if the expression is a var or 1 */
  private class IncCollectingVisitor extends VarCollectingVisitor {

    public IncCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Collection<String> visit(CBinaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> operand1 = exp.getOperand1().accept(this);
      Collection<String> operand2 = exp.getOperand2().accept(this);

      // operand1 contains exactly one var
      // operand2 is empty (numeral ONE)
      // operator is PLUS
      if (operand1 != null && operand2 != null
          && operand1.size() == 1 && operand2.isEmpty()
          && BinaryOperator.PLUS.equals(exp.getOperator())) {
        return operand1;

      } else {
        if (operand1 != null) {
          nonIncVars.addAll(operand1);
        }
        if (operand2 != null) {
          nonIncVars.addAll(operand2);
        }
        return null;
      }
    }

    @Override
    public Collection<String> visit(CIntegerLiteralExpression exp) {
      if (BigInteger.ONE.equals(exp.getValue())) {
        return Collections.emptySet();
      } else {
        return null;
      }
    }

    @Override
    public Collection<String> visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
      Collection<String> inner = exp.getOperand().accept(this);
      nonIncVars.addAll(inner); // increment is no unary operation
      return null;
    }
  }
}
