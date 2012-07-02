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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
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
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class VariableClassification {

  /** name for return-variables, it is used for function-returns. */
  private static final String FUNCTION_RETURN_VARIABLE = "__CPAchecker_return_var";

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
      allVars = HashMultimap.create();
      nonBooleanVars = HashMultimap.create();
      nonSimpleNumberVars = HashMultimap.create();
      nonIncVars = HashMultimap.create();

      dependencies = new Dependencies();

      booleanVars = HashMultimap.create();
      simpleNumberVars = HashMultimap.create();
      incVars = HashMultimap.create();

      // fill maps
      collectVars();
    }
  }

  /** This function returns a collection of (functionName, varNames). */
  public Multimap<String, String> getBooleanVars() {
    build();
    return booleanVars;
  }

  /** This function returns a collection of (functionName, varNames). */
  public Multimap<String, String> getNonBooleanVars() {
    build();
    return nonBooleanVars;
  }

  /** This function iterates over all edges of the cfa, collects all variables
   * and orders them into different sets, i.e. booleanVars and nonBooleanVars. */
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

    // we know all non-X-Vars, now build the opposite X-Vars.
    for (String function : allVars.keySet()) {
      Collection<String> vars = allVars.get(function);

      for (String s : vars) {
        if (!nonBooleanVars.containsEntry(function, s)) {
          booleanVars.put(function, s);
        }
      }

      for (String s : vars) {
        if (!nonSimpleNumberVars.containsEntry(function, s)
            && nonBooleanVars.containsEntry(function, s)) {
          simpleNumberVars.put(function, s);
        }
      }

      for (String s : vars) {
        if (!nonIncVars.containsEntry(function, s)
            && nonBooleanVars.containsEntry(function, s)) {
          incVars.put(function, s);
        }
      }
    }
  }

  private void handleEdge(CFAEdge edge) {
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

      if (rhs instanceof CExpression) {


        allVars.put(function, varName);
        handleExpression(edge, ((CExpression) rhs), varName, function);

      } else if (rhs instanceof CFunctionCallExpression) {
        // use FUNCTION_RETURN_VARIABLE for RIGHT SIDE
        CFunctionCallExpression func = (CFunctionCallExpression) rhs;
        String functionName = func.getFunctionNameExpression().toASTString(); // TODO correct?

        if (cfa.getAllFunctionNames().contains(functionName)) {
          dependencies.add(functionName, FUNCTION_RETURN_VARIABLE, function, varName);

        } else {
          // external function --> we assume, that var is not boolean
          // System.out.println("found external function: " + functionName);
          nonBooleanVars.put(function, varName);
          nonSimpleNumberVars.put(function, varName);
          nonIncVars.put(function, varName);
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

        // build name for param and evaluate it
        // this variable is not global (->false)
        handleExpression(edge, args.get(i), params.get(i).getName(), innerFunctionName);
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

  /** evaluates an expression and adds containing vars to the sets.
   * @param function */
  private void handleExpression(CFAEdge edge, CExpression exp, String varName,
      String function) {

    BoolCollectingVisitor bcv = new BoolCollectingVisitor(edge.getPredecessor());
    Multimap<String, String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, function, possibleBoolean, nonBooleanVars);

    NumberCollectingVisitor ncv = new NumberCollectingVisitor(edge.getPredecessor());
    Multimap<String, String> possibleNumbers = exp.accept(ncv);
    handleResult(varName, function, possibleNumbers, nonSimpleNumberVars);

    IncCollectingVisitor icv = new IncCollectingVisitor(edge.getPredecessor());
    Multimap<String, String> possibleIncs = exp.accept(icv);
    handleResult(varName, function, possibleIncs, nonIncVars);
  }

  private void handleResult(String varName, String function,
      Multimap<String, String> possibleVars, Multimap<String, String> notPossibleVars) {
    if (possibleVars == null) {
      notPossibleVars.put(function, varName);
    } else {
      for (Entry<String, String> entry : possibleVars.entries()) {
        dependencies.add(function, varName, entry.getKey(), entry.getValue());
      }
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
    StringBuilder str = new StringBuilder();

    if (allVars == null) {
      str.append("VariableClassification is not build.");
      return str.toString();
    }

    str.append("\nALL  " + allVars.size() + "\n    " + allVars);

    //    str.append("\nBOOL DEPENDENCIES\n    ");
    //    for (Entry<String, String> entry : boolDependencies.entrySet()) {
    //      str.append(entry.getKey() + " --> " + entry.getValue() + "\n    ");
    //    }
    //    str.append("\nNONBOOL\n    " + Arrays.toString(nonBooleanVars.toArray()).replace(", ", ",\n    "));
    str.append("\nBOOL  " + booleanVars.size() + "\n    " + booleanVars);

    //    str.append("\nSIMPLE NUMBER DEPENDENCIES\n    ");
    //    for (Entry<String, String> entry : simpleNumberDependencies.entrySet()) {
    //      str.append(entry.getKey() + " --> " + entry.getValue() + "\n    ");
    //    }
    //    str.append("\nNO SIMPLE NUMBER\n    " + Arrays.toString(nonSimpleNumberVars.toArray()).replace(", ", ",\n    "));
    str.append("\nSIMPLE NUMBER  " + simpleNumberVars.size() + "\n    " + simpleNumberVars);

    //    str.append("\nINCREMENT DEPENDENCIES\n    ");
    //    for (Entry<String, String> entry : incDependencies.entrySet()) {
    //      str.append(entry.getKey() + " --> " + entry.getValue() + "\n    ");
    //    }
    //    str.append("\nNON INCREMENT\n    " + Arrays.toString(nonIncVars.toArray()).replace(", ", ",\n    "));
    str.append("\nINCREMENT  " + incVars.size() + "\n    " + incVars);

    return str.toString();
  }

  /** This Visitor evaluates an Expression. It collects all variables.
   * a visit of IdExpression or CFieldReference returns a collection containing the varName,
   * a visit of CastExpression return the containing visit,
   * other visits return null. */
  private class VarCollectingVisitor implements
      CExpressionVisitor<Multimap<String, String>, NullPointerException> {

    private CFANode predecessor;

    public VarCollectingVisitor(CFANode pre) {
      this.predecessor = pre;
    }

    @Override
    public Multimap<String, String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      exp.getOperand1().accept(this);
      exp.getOperand2().accept(this);
      return null;
    }

    @Override
    public Multimap<String, String> visit(CCastExpression exp) throws NullPointerException {
      return exp.getOperand().accept(this);
    }

    @Override
    public Multimap<String, String> visit(CFieldReference exp) {
      String varName = exp.getFieldName();
      String function = isGlobal(exp) ? null : predecessor.getFunctionName();
      allVars.put(function, varName);
      HashMultimap<String, String> ret = HashMultimap.create(1, 1);
      ret.put(function, varName);
      return ret;
    }

    @Override
    public Multimap<String, String> visit(CIdExpression exp) {
      String varName = exp.getName();
      String function = isGlobal(exp) ? null : predecessor.getFunctionName();
      allVars.put(function, varName);
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
    public Multimap<String, String> visit(CUnaryExpression exp) throws NullPointerException {
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
      if (BigInteger.ZERO.equals(value) || BigInteger.ONE.equals(value)) {
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
  private class NumberCollectingVisitor extends VarCollectingVisitor {

    public NumberCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+123 --> a is not boolean
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
  private class IncCollectingVisitor extends VarCollectingVisitor {

    public IncCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) throws NullPointerException {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);

      // operand1 contains exactly one var
      // operand2 is empty (numeral ONE)
      // operator is PLUS
      if (operand1 != null && operand2 != null
          && operand1.size() == 1 && operand2.isEmpty()
          && BinaryOperator.PLUS.equals(exp.getOperator())) {
        return operand1;

      } else {
        if (operand1 != null) {
          nonIncVars.putAll(operand1);
        }
        if (operand2 != null) {
          nonIncVars.putAll(operand2);
        }
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      if (BigInteger.ONE.equals(exp.getValue())) {
        return HashMultimap.create(0, 0);
      } else {
        return null;
      }
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

    private Set<Collection<Pair<String, String>>> partitions = new HashSet<Collection<Pair<String, String>>>();
    private Map<Pair<String, String>, Collection<Pair<String, String>>> varToPartition =
        new HashMap<Pair<String, String>, Collection<Pair<String, String>>>();

    /** This function creates a dependency between function1::var1 and function2::var2. */
    public void add(String function1, String var1, String function2, String var2) {
      Pair<String, String> first = Pair.of(function1, var1);
      Pair<String, String> second = Pair.of(function2, var2);

      // if both vars exists in some dependencies,
      // either ignore them or merge their partitions
      if (varToPartition.containsKey(first) && varToPartition.containsKey(second)) {
        Collection<Pair<String, String>> partition1 = varToPartition.get(first);
        Collection<Pair<String, String>> partition2 = varToPartition.get(first);

        if (!partition1.equals(partition2)) {
          // merge partition1 and partition2
          partition1.addAll(partition2);

          // update vars from partition2 and delete partition2
          for (Pair<String, String> var : partition2) {
            varToPartition.remove(var);
            varToPartition.put(var, partition1);
          }
          partitions.remove(partition2);
        }

        // if only left side of dependency exists, add right side into same partition
      } else if (varToPartition.containsKey(first)) {
        Collection<Pair<String, String>> partition = varToPartition.get(first);
        partition.add(second);
        varToPartition.put(second, partition);

        // if only right side of dependency exists, add left side into same partition
      } else if (varToPartition.containsKey(second)) {
        Collection<Pair<String, String>> partition = varToPartition.get(second);
        partition.add(first);
        varToPartition.put(first, partition);

        // if none side is in any existing partition, create new partition
      } else {
        Collection<Pair<String, String>> partition = new HashSet<Pair<String, String>>();
        partitions.add(partition);
        partition.add(first);
        partition.add(second);
        varToPartition.put(first, partition);
        varToPartition.put(second, partition);
      }
    }

    /** This function adds all depending vars to the set, if necessary.
     * If A depends on B and A is part of the set, B is added to the set, and vice versa.
    * Example: If A is not boolean, B is not boolean. */
    public void solve(final Multimap<String, String> vars) {
      for (Collection<Pair<String, String>> partition : partitions) {

        // is at least one var from the partition part of vars
        boolean isDependency = false;
        for (Pair<String, String> var : partition) {
          if (vars.containsEntry(var.getFirst(), var.getSecond())) {
            isDependency = true;
            break;
          }
        }

        // add all dependend vars to vars
        if (isDependency) {
          for (Pair<String, String> var : partition) {
            vars.put(var.getFirst(), var.getSecond());
          }
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder("{");
      for (Collection<Pair<String, String>> partition : partitions) {
        str.append("[");
        for (Pair<String, String> var : partition) {
          str.append(var.getFirst() + "::" + var.getSecond() + ", ");
        }
        str.append("], \n");
      }
      str.append("}");
      return str.toString();
    }
  }
}
