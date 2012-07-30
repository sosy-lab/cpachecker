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
package org.sosy_lab.cpachecker.util;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
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
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class VariableClassification {

  /** name for return-variables, it is used for function-returns. */
  public static final String FUNCTION_RETURN_VARIABLE = "__CPAchecker_return_var";

  /** normally a boolean value would be 0 or 1,
   * however there are cases, where the values are only 0 and 1,
   * but the variable is not boolean at all: "int x; if(x!=0 && x!= 1){}".
   * so we allow only 0 as boolean value, and not 1. */
  private boolean allowOneAsBooleanValue = false;

  private Multimap<String, String> allVars = null;

  private Multimap<String, String> nonBooleanVars;
  private Multimap<String, String> nonSimpleNumberVars;
  private Multimap<String, String> nonIncVars;

  private Dependencies dependencies;

  private Multimap<String, String> booleanVars;
  private Multimap<String, String> simpleNumberVars;
  private Multimap<String, String> incVars;

  private CFA cfa;

  public VariableClassification(CFA cfa) {
    this.cfa = cfa;
  }

  /** This function does the whole work:
   * creating all maps, collecting vars, solving dependencies. */
  private void build() {
    if (allVars == null) {

      // init maps
      allVars = LinkedHashMultimap.create();
      nonBooleanVars = LinkedHashMultimap.create();
      nonSimpleNumberVars = LinkedHashMultimap.create();
      nonIncVars = LinkedHashMultimap.create();

      dependencies = new Dependencies();

      booleanVars = LinkedHashMultimap.create();
      simpleNumberVars = LinkedHashMultimap.create();
      incVars = LinkedHashMultimap.create();

      // fill maps
      collectVars();

      // we have collected the nonBooleanVars, lets build the needed booleanVars.
      buildOpposites();

      // add last vars to dependencies,
      // this allows to get partitions for all vars,
      // otherwies only dependent vars are in the partitions
      for (Entry<String, String> var : allVars.entries()) {
        dependencies.addVar(var.getKey(), var.getValue());
      }

      // TODO is there a need to change the Maps later? make Maps immutable?
    }
  }

  /** This function returns a collection of (functionName, varNames).
   * This collection contains all vars. */
  public Multimap<String, String> getAllVars() {
    build();
    return allVars;
  }

  /** This function returns a collection of (functionName, varNames).
   * This collection contains all vars, that are boolean,
   * i.e. the value is 0 or 1. */
  public Multimap<String, String> getBooleanVars() {
    build();
    return booleanVars;
  }

  /** This function returns a collection of (functionName, varNames).
   * This collection contains all vars, that have only the values of simple numbers.
   * The collection also includes boolean vars, because they are simple numbers, too.
   * There are NO mathematical calculations (add, sub, mult) with these vars. */
  public Multimap<String, String> getSimpleNumberVars() {
    build();
    return simpleNumberVars;
  }

  /** This function returns a collection of (functionName, varNames).
   * This collection contains all vars, that can be incremented.
   * The collection includes all boolean vars and simple numbers, too.
   * The only allowed mathematical calculation with these vars is increment (add 1). */
  public Multimap<String, String> getIncNumberVars() {
    build();
    return incVars;
  }

  /** This function returns a collection of partitions.
   * A partition contains all vars, that are dependent from each other. */
  public List<Multimap<String, String>> getPartitions() {
    build();
    return dependencies.getPartitions();
  }

  /** This function returns a collection of (functionName, varName).
   * This collection contains all vars, that are dependent with the given variable. */
  public Multimap<String, String> getPartitionForVar(String function, String var) {
    build();
    return dependencies.getPartitionForVar(function, var);
  }

  /** This function iterates over all edges of the cfa, collects all variables
   * and orders them into different sets, i.e. nonBoolean and nonSimpleNumber. */
  private void collectVars() {
    Collection<CFANode> nodes = cfa.getAllNodes();
    for (CFANode node : nodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        handleEdge(edge);
      }
    }

    // if a value is nonbool, all dependent vars are nonbool and viceversa
    dependencies.solve(nonBooleanVars);

    // if a value is no simple number, all dependent vars are no simple numbers and viceversa
    dependencies.solve(nonSimpleNumberVars);

    // if a value is not incremented, all dependent vars are not incremented and viceversa
    dependencies.solve(nonIncVars);
  }

  /** This function builds the opposites of each non-x-vars-collection. */
  private void buildOpposites() {
    for (final String function : allVars.keySet()) {
      for (final String s : allVars.get(function)) {

        if (!nonBooleanVars.containsEntry(function, s)) {
          booleanVars.put(function, s);
        }

        if (!nonSimpleNumberVars.containsEntry(function, s)) {
          simpleNumberVars.put(function, s);
        }

        if (!nonIncVars.containsEntry(function, s)) {
          incVars.put(function, s);
        }
      }
    }
  }

  private void handleEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {

    case AssumeEdge: {
      CExpression exp = ((CAssumeEdge) edge).getExpression();
      CFANode pre = edge.getPredecessor();

      Multimap<String, String> dep = exp.accept(new DependencyCollectingVisitor(pre));
      dependencies.addAll(dep);

      exp.accept(new BoolCollectingVisitor(pre));
      exp.accept(new NumberCollectingVisitor(pre));
      exp.accept(new IncCollectingVisitor(pre));

      break;
    }

    case DeclarationEdge: {
      CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
      if (!(declaration instanceof CVariableDeclaration)) { return; }

      CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
      String varName = vdecl.getName();
      String function = vdecl.isGlobal() ? null : edge.getPredecessor().getFunctionName();

      allVars.put(function, varName);

      CInitializer initializer = vdecl.getInitializer();
      if ((initializer == null) || !(initializer instanceof CInitializerExpression)) { return; }

      CExpression exp = ((CInitializerExpression) initializer).getExpression();
      if (exp == null) { return; }

      handleExpression(edge, exp, varName, function);

      break;
    }

    case StatementEdge: {
      CStatement statement = ((CStatementEdge) edge).getStatement();

      if (!(statement instanceof CAssignment)) { return; }

      CAssignment assignment = (CAssignment) statement;
      CRightHandSide rhs = assignment.getRightHandSide();
      CExpression lhs = assignment.getLeftHandSide();
      String varName = lhs.toASTString();
      String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();

      allVars.put(function, varName);

      if (rhs instanceof CExpression) {
        handleExpression(edge, ((CExpression) rhs), varName, function);

      } else if (rhs instanceof CFunctionCallExpression) {
        // use FUNCTION_RETURN_VARIABLE for RIGHT SIDE
        CFunctionCallExpression func = (CFunctionCallExpression) rhs;
        String functionName = func.getFunctionNameExpression().toASTString(); // TODO correct?

        if (cfa.getAllFunctionNames().contains(functionName)) {
          // TODO is this case really appearing or is it always handled as "functionCallEdge"?
          allVars.put(functionName, FUNCTION_RETURN_VARIABLE);
          dependencies.add(functionName, FUNCTION_RETURN_VARIABLE, function, varName);

        } else {
          // external function --> we can ignore this case completely
          // if var is used anywhere else, it should be handled.
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

      // functions can have more args than params used in the call
      assert args.size() >= params.size();

      for (int i = 0; i < params.size(); i++) {

        // build name for param and evaluate it
        // this variable is not global (->false)
        handleExpression(edge, args.get(i), params.get(i).getName(), innerFunctionName);
      }

      // create dependency for functionreturn
      CFunctionSummaryEdge func = functionCall.getSummaryEdge();
      CStatement statement = func.getExpression().asStatement();
      if (statement instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement call = (CFunctionCallAssignmentStatement) statement;
        CExpression lhs = call.getLeftHandSide();
        String varName = lhs.toASTString();
        String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
        allVars.put(innerFunctionName, FUNCTION_RETURN_VARIABLE);
        dependencies.add(innerFunctionName, FUNCTION_RETURN_VARIABLE, function, varName);

      } else if (statement instanceof CFunctionCallStatement) {
        // f(x); we can ignore it, there is no dependency
      }
      break;

    case FunctionReturnEdge: {
      // TODO does this edge appear? see statementEdge for functioncalls.
      break;
    }

    case ReturnStatementEdge: {
      // this is the 'x' from 'return (x);
      // adding a new temporary FUNCTION_RETURN_VARIABLE, that is not global (-> false)
      CReturnStatementEdge returnStatement = (CReturnStatementEdge) edge;
      CRightHandSide rhs = returnStatement.getExpression();
      if (rhs instanceof CExpression) {
        String function = edge.getPredecessor().getFunctionName();
        allVars.put(function, FUNCTION_RETURN_VARIABLE);
        handleExpression(edge, ((CExpression) rhs), FUNCTION_RETURN_VARIABLE,
            function);
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
  private void handleExpression(CFAEdge edge, CExpression exp, String varName,
      String function) {
    CFANode pre = edge.getPredecessor();

    DependencyCollectingVisitor dcv = new DependencyCollectingVisitor(pre);
    Multimap<String, String> dep = exp.accept(dcv);
    if (dep != null) {
      dep.put(function, varName);
      dependencies.addAll(dep);
    }

    BoolCollectingVisitor bcv = new BoolCollectingVisitor(pre);
    Multimap<String, String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, function, possibleBoolean, nonBooleanVars);

    NumberCollectingVisitor ncv = new NumberCollectingVisitor(pre);
    Multimap<String, String> possibleNumbers = exp.accept(ncv);
    handleResult(varName, function, possibleNumbers, nonSimpleNumberVars);

    IncCollectingVisitor icv = new IncCollectingVisitor(pre);
    Multimap<String, String> possibleIncs = exp.accept(icv);

    // assignment of number is allowed for incVars
    if (!(exp instanceof CIntegerLiteralExpression)) {
      handleResult(varName, function, possibleIncs, nonIncVars);
    }
  }

  /** adds the variable to notPossibleVars, if possibleVars is null.  */
  private void handleResult(String varName, String function,
      Multimap<String, String> possibleVars, Multimap<String, String> notPossibleVars) {
    if (possibleVars == null) {
      notPossibleVars.put(function, varName);
    }
  }

  private boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  @Override
  public String toString() {
    if (allVars == null) { return "VariableClassification is not build."; }

    StringBuilder str = new StringBuilder();
    str.append("\nALL  " + allVars.size() + "\n    " + allVars);
    str.append("\nBOOL  " + booleanVars.size() + "\n    " + booleanVars);
    str.append("\nSIMPLE NUMBER  " + simpleNumberVars.size() + "\n    " + simpleNumberVars);
    str.append("\nINCREMENT  " + incVars.size() + "\n    " + incVars);
    return str.toString();
  }

  /** This Visitor evaluates an Expression. It collects all variables.
   * a visit of IdExpression or CFieldReference returns a collection containing the varName,
   * a visit of CastExpression return the containing visit,
   * other visits return null. */
  private class DependencyCollectingVisitor implements
      CExpressionVisitor<Multimap<String, String>, NullPointerException> {

    private CFANode predecessor;

    public DependencyCollectingVisitor(CFANode pre) {
      this.predecessor = pre;
    }

    @Override
    public Multimap<String, String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);
      if (operand1 == null) {
        return operand2;
      } else if (operand2 == null) {
        return operand1;
      } else {
        operand1.putAll(operand2);
        return operand1;
      }
    }

    @Override
    public Multimap<String, String> visit(CCastExpression exp) throws NullPointerException {
      return exp.getOperand().accept(this);
    }

    @Override
    public Multimap<String, String> visit(CFieldReference exp) {
      String varName = exp.getFieldName();
      String function = isGlobal(exp) ? null : predecessor.getFunctionName();
      HashMultimap<String, String> ret = HashMultimap.create(1, 1);
      ret.put(function, varName);
      return ret;
    }

    @Override
    public Multimap<String, String> visit(CIdExpression exp) {
      String varName = exp.getName();
      String function = isGlobal(exp) ? null : predecessor.getFunctionName();
      HashMultimap<String, String> ret = HashMultimap.create(1, 1);
      ret.put(function, varName);
      return ret;
    }

    @Override
    public Multimap<String, String> visit(CCharLiteralExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CFloatLiteralExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CStringLiteralExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CTypeIdExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) {
      return exp.getOperand().accept(this);
    }
  }


  /** This Visitor evaluates an Expression. It also collects all variables.
   * Each visit-function returns
   * - null, if the expression is not boolean
   * - a collection, if the expression is boolean.
   * The collection contains all boolean vars. */
  private class BoolCollectingVisitor extends DependencyCollectingVisitor {

    public BoolCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
        if (operand1 != null) {
          nonBooleanVars.putAll(operand1);
        }
        if (operand2 != null) {
          nonBooleanVars.putAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case LOGICAL_AND:
      case LOGICAL_OR:
      case EQUALS:
      case NOT_EQUALS: // &&, ||, ==, != work with boolean operands
        operand1.putAll(operand2);
        return operand1;

      default: // +-*/ --> no boolean operators, a+b --> a and b are not boolean
        nonBooleanVars.putAll(operand1);
        nonBooleanVars.putAll(operand2);
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      BigInteger value = exp.getValue();
      if (BigInteger.ZERO.equals(value)
          || (allowOneAsBooleanValue && BigInteger.ONE.equals(value))) {
        return HashMultimap.create(0, 0);
      } else {
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) throws NullPointerException {
      Multimap<String, String> inner = exp.getOperand().accept(this);

      if (inner == null) {
        return null;
      } else if (UnaryOperator.NOT == exp.getOperator()) {
        // boolean operation, return inner vars
        return inner;
      } else { // PLUS, MINUS, etc --> not boolean
        nonBooleanVars.putAll(inner);
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression contains calculations
   * - a collection, if the expression is a number, unaryExp, == or != */
  private class NumberCollectingVisitor extends DependencyCollectingVisitor {

    public NumberCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonSimpleNumberVars.putAll(operand1);
        }
        if (operand2 != null) {
          nonSimpleNumberVars.putAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case EQUALS:
      case NOT_EQUALS: // ==, != work with numbers
        operand1.putAll(operand2);
        return operand1;

      default: // +-*/ --> no simple operators
        nonSimpleNumberVars.putAll(operand1);
        nonSimpleNumberVars.putAll(operand2);
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      return HashMultimap.create(0, 0);
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) throws NullPointerException {
      Multimap<String, String> inner = exp.getOperand().accept(this);

      if (inner == null) { return null; }

      switch (exp.getOperator()) {
      case PLUS: // simple calculations, no usage of another param
      case MINUS:
        return inner;
      default: // *, ~, etc --> not numeral
        nonSimpleNumberVars.putAll(inner);
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - null, if the expression contains calculations
   * - a collection, if the expression is a var or 1 */
  private class IncCollectingVisitor extends DependencyCollectingVisitor {

    public IncCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIncVars.putAll(operand1);
        }
        if (operand2 != null) {
          nonIncVars.putAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case EQUALS:
      case NOT_EQUALS: // ==, != work with all numbers
        operand1.putAll(operand2);
        return operand1;

      case PLUS:
        // 1+x
        if (exp.getOperand1() instanceof CIntegerLiteralExpression
            && BigInteger.ONE.equals(
                ((CIntegerLiteralExpression) exp.getOperand1()).getValue())) {
          assert operand1.isEmpty();
          return operand2;

          // x+1
        } else if (exp.getOperand2() instanceof CIntegerLiteralExpression
            && BigInteger.ONE.equals(
                ((CIntegerLiteralExpression) exp.getOperand2()).getValue())) {
          assert operand2.isEmpty();
          return operand1;

          // x+y, x+2
        } else {
          nonIncVars.putAll(operand1);
          nonIncVars.putAll(operand2);
          return null;
        }

      default: // +-*/ --> no simple operators
        nonIncVars.putAll(operand1);
        nonIncVars.putAll(operand2);
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      return HashMultimap.create(0, 0);
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) throws NullPointerException {
      Multimap<String, String> inner = exp.getOperand().accept(this);
      if (inner != null) { // increment is no unary operation, remove these vars
        nonIncVars.putAll(inner);
      }
      return null;
    }
  }

  /** This class stores dependencies between variables.
   * It sorts vars into partitions.
   * Dependent vars are in the same partition. Partitions are independent. */
  private class Dependencies {

    /** partitions, each of them contains vars */
    private List<Multimap<String, String>> partitions = Lists.newArrayList();

    /** map to get partition of a var */
    private Map<Pair<String, String>, Multimap<String, String>> varToPartition = Maps.newHashMap();

    public List<Multimap<String, String>> getPartitions() {
      return partitions;
    }

    public Multimap<String, String> getPartitionForVar(String function, String var) {
      return varToPartition.get(Pair.of(function, var));
    }

    /** This function creates a dependency between function1::var1 and function2::var2. */
    public void add(String function1, String var1, String function2, String var2) {
      Pair<String, String> first = Pair.of(function1, var1);
      Pair<String, String> second = Pair.of(function2, var2);

      // if both vars exists in some dependencies,
      // either ignore them or merge their partitions
      if (varToPartition.containsKey(first) && varToPartition.containsKey(second)) {
        Multimap<String, String> partition1 = varToPartition.get(first);
        Multimap<String, String> partition2 = varToPartition.get(second);

        // swap partitions, we create partitions in the order they are used
        if (partitions.lastIndexOf(partition1) > partitions.lastIndexOf(partition2)) {
          Multimap<String, String> tmp = partition2;
          partition2 = partition1;
          partition1 = tmp;
        }

        if (!partition1.equals(partition2)) {
          // merge partition1 and partition2
          partition1.putAll(partition2);

          // update vars from partition2 and delete partition2
          for (Entry<String, String> e : partition2.entries()) {
            Pair<String, String> var = Pair.of(e.getKey(), e.getValue());
            varToPartition.put(var, partition1);
          }
          partitions.remove(partition2);
        }

        // if only left side of dependency exists, add right side into same partition
      } else if (varToPartition.containsKey(first)) {
        Multimap<String, String> partition = varToPartition.get(first);
        partition.put(function2, var2);
        varToPartition.put(second, partition);

        // if only right side of dependency exists, add left side into same partition
      } else if (varToPartition.containsKey(second)) {
        Multimap<String, String> partition = varToPartition.get(second);
        partition.put(function1, var1);
        varToPartition.put(first, partition);

        // if none side is in any existing partition, create new partition
      } else {
        Multimap<String, String> partition = LinkedHashMultimap.create();
        partitions.add(partition);
        partition.put(function1, var1);
        partition.put(function2, var2);
        varToPartition.put(first, partition);
        varToPartition.put(second, partition);
      }
    }

    /** This function adds a group of vars to exactly one partition. */
    public void addAll(Multimap<String, String> vars) {
      if (vars == null || vars.isEmpty()) { return; }
      Iterator<Entry<String, String>> iter = vars.entries().iterator();

      // we use same function and varName for all other vars --> dependency
      Entry<String, String> entry = iter.next();
      String function = entry.getKey();
      String varName = entry.getValue();

      while (iter.hasNext()) {
        entry = iter.next();
        add(function, varName, entry.getKey(), entry.getValue());
      }
    }

    /** This function adds one single variable to the partitions.
     * This is the only method to create a partition with only one element. */
    public void addVar(String function, String varName) {
      Pair<String, String> var = Pair.of(function, varName);

      // if var exists, we can ignore it, otherwise create new partition for var
      if (!varToPartition.containsKey(var)) {
        Multimap<String, String> partition = LinkedHashMultimap.create(1, 1);
        partition.put(function, varName);
        partitions.add(partition);
        varToPartition.put(var, partition);
      }
    }

    /** This function adds all depending vars to the set, if necessary.
     * If A depends on B and A is part of the set, B is added to the set, and vice versa.
    * Example: If A is not boolean, B is not boolean. */
    public void solve(final Multimap<String, String> vars) {
      for (Multimap<String, String> partition : partitions) {

        // is at least one var from the partition part of vars
        boolean isDependency = false;
        for (Entry<String, String> var : partition.entries()) {
          if (vars.containsEntry(var.getKey(), var.getValue())) {
            isDependency = true;
            break;
          }
        }

        // add all dependend vars to vars
        if (isDependency) {
          vars.putAll(partition);
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder("[");
      for (Multimap<String, String> partition : partitions) {
        str.append(partition.toString() + ",\n");
      }
      str.append("]");
      return str.toString();
    }
  }
}
