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

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Options(prefix = "cfa.variableClassification")
public class VariableClassification {

  @Option(name = "logfile", description = "Dump variable classification to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("VariableClassification.log");

  @Option(description = "Print some information about the variable classification.")
  private boolean printStatsOnStartup = false;

  /** name for return-variables, it is used for function-returns. */
  public static final String FUNCTION_RETURN_VARIABLE = "__CPAchecker_return_var";

  /** normally a boolean value would be 0 or 1,
   * however there are cases, where the values are only 0 and 1,
   * but the variable is not boolean at all: "int x; if(x!=0 && x!= 1){}".
   * so we allow only 0 as boolean value, and not 1. */
  private boolean allowOneAsBooleanValue = false;
  private Timer buildTimer = new Timer();

  private Multimap<String, String> allVars = null;

  private Multimap<String, String> nonBooleanVars;
  private Multimap<String, String> nonIntEqualVars;
  private Multimap<String, String> nonIntAddVars;

  private Dependencies dependencies;

  private Multimap<String, String> booleanVars;
  private Multimap<String, String> intEqualVars;
  private Multimap<String, String> intAddVars;

  private Set<Partition> booleanPartitions;
  private Set<Partition> intEqualPartitions;
  private Set<Partition> intAddPartitions;

  private CFA cfa;
  private final LogManager logger;

  public VariableClassification(CFA cfa, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    this.cfa = cfa;
    this.logger = pLogger;

    if (printStatsOnStartup) {
      build();
      printStats();
    }

  }

  private void printStats() {
    final Set<Partition> booleans = getBooleanPartitions();
    final Set<Partition> intEquals = getIntEqualPartitions();
    final Set<Partition> intAdds = getIntAddPartitions();

    int numOfBooleans = getBooleanVars().size();

    int numOfIntEquals = 0;
    final Set<Partition> realIntEquals = Sets.difference(intEquals, booleans);
    for (Partition p : realIntEquals) {
      numOfIntEquals += p.getVars().size();
    }

    int numOfIntAdds = 0;
    final Set<Partition> realIntAdds = Sets.difference(intAdds, Sets.union(booleans, intEquals));
    for (Partition p : realIntAdds) {
      numOfIntAdds += p.getVars().size();
    }

    final String prefix = "\nVC ";
    StringBuilder str = new StringBuilder("VariableClassification Statistics\n");
    Joiner.on(prefix).appendTo(str, new String[] {
        "---------------------------------",
        "number of boolean vars:  " + numOfBooleans,
        "number of intEqual vars: " + numOfIntEquals,
        "number of intAdd vars:   " + numOfIntAdds,
        "number of all vars:      " + allVars.size(),
        "number of boolean partitions:  " + booleans.size(),
        "number of intEqual partitions: " + realIntEquals.size(),
        "number of intAdd partitions:   " + realIntAdds.size(),
        "number of all partitions:      " + getPartitions().size(),
        "time for building classification: " + buildTimer });
    str.append("\n---------------------------------\n");

    logger.log(Level.INFO, str.toString());
  }

  /** This function does the whole work:
   * creating all maps, collecting vars, solving dependencies.
   * The function runs only once, after that it does nothing. */
  private void build() {
    if (allVars == null) {

      buildTimer.start();

      // init maps
      allVars = LinkedHashMultimap.create();
      nonBooleanVars = LinkedHashMultimap.create();
      nonIntEqualVars = LinkedHashMultimap.create();
      nonIntAddVars = LinkedHashMultimap.create();

      dependencies = new Dependencies();

      booleanVars = LinkedHashMultimap.create();
      intEqualVars = LinkedHashMultimap.create();
      intAddVars = LinkedHashMultimap.create();

      booleanPartitions = new HashSet<>();
      intEqualPartitions = new HashSet<>();
      intAddPartitions = new HashSet<>();

      // fill maps
      collectVars();

      // we have collected the nonBooleanVars, lets build the needed booleanVars.
      buildOpposites();

      // add last vars to dependencies,
      // this allows to get partitions for all vars,
      // otherwise only dependent vars are in the partitions
      for (Entry<String, String> var : allVars.entries()) {
        dependencies.addVar(var.getKey(), var.getValue());
      }

      buildTimer.stop();

      if (dumpfile != null) { // option -noout
        try (Writer w = Files.openOutputFile(dumpfile)) {
          w.append("Boolean\n");
          w.append(booleanVars.toString());
          w.append("\n\nIntEqual\n\n");
          w.append(intEqualVars.toString());
          w.append("\n\nIntAdd\n\n");
          w.append(intAddVars.toString());
          w.append("\n\nALL\n\n");
          w.append(allVars.toString());
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write variable classification to file");
        }
      }
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

  /** This function returns a collection of partitions.
   * Each partition contains only boolean vars. */
  public Set<Partition> getBooleanPartitions() {
    build();
    return booleanPartitions;
  }

  /** This function returns a collection of (functionName, varNames).
   * This collection contains all vars, that are only assigned or compared
   * with integer values. The collection also includes some boolean vars,
   * because they can be assigned, too.
   * There are NO mathematical calculations (add, sub, mult) with these vars. */
  public Multimap<String, String> getIntEqualVars() {
    build();
    return intEqualVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only vars,
   * that are only assigned or compared with integer values. */
  public Set<Partition> getIntEqualPartitions() {
    build();
    return intEqualPartitions;
  }

  /** This function returns a collection of (functionName, varNames).
   * This collection contains all vars, that are only used in simple calculations
   * (+, -, <, >, <=, >=, ==, !=, &, &&, |, ||, ^).
   * The collection includes all boolean vars and simple numbers, too. */
  public Multimap<String, String> getIntAddVars() {
    build();
    return intAddVars;
  }

  /** This function returns a collection of partitions.
   * Each partition contains only vars, that are used in simple calculations. */
  public Set<Partition> getIntAddPartitions() {
    build();
    return intAddPartitions;
  }

  /** This function returns a collection of partitions.
   * A partition contains all vars, that are dependent from each other. */
  public List<Partition> getPartitions() {
    build();
    return dependencies.getPartitions();
  }

  /** This function returns a partition containing all vars,
   * that are dependent with the given variable. */
  public Partition getPartitionForVar(String function, String var) {
    build();
    return dependencies.getPartitionForVar(function, var);
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge. */
  public Partition getPartitionForEdge(CFAEdge edge) {
    return getPartitionForEdge(edge, 0);
  }

  /** This function returns a partition containing all vars,
   * that are dependent from a given CFAedge.
   * The index is 0 for all edges, except functionCalls,
   * where it is the position of the param.
   * For the left-hand-side of the assignment of external functionCalls use -1. */
  public Partition getPartitionForEdge(CFAEdge edge, int index) {
    build();
    return dependencies.getPartitionForEdge(edge, index);
  }

  /** This function iterates over all edges of the cfa, collects all variables
   * and orders them into different sets, i.e. nonBoolean and nonIntEuqalNumber. */
  private void collectVars() {
    Collection<CFANode> nodes = cfa.getAllNodes();
    for (CFANode node : nodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        handleEdge(edge);
      }
    }

    // if a value is not boolean, all dependent vars are not boolean and viceversa
    dependencies.solve(nonBooleanVars);
    dependencies.solve(nonIntEqualVars);
    dependencies.solve(nonIntAddVars);
  }

  /** This function builds the opposites of each non-x-vars-collection. */
  private void buildOpposites() {
    for (final String function : allVars.keySet()) {
      for (final String s : allVars.get(function)) {

        if (!nonBooleanVars.containsEntry(function, s)) {
          booleanVars.put(function, s);
          booleanPartitions.add(getPartitionForVar(function, s));
        }

        if (!nonIntEqualVars.containsEntry(function, s)) {
          intEqualVars.put(function, s);
          intEqualPartitions.add(getPartitionForVar(function, s));
        }

        if (!nonIntAddVars.containsEntry(function, s)) {
          intAddVars.put(function, s);
          intAddPartitions.add(getPartitionForVar(function, s));
        }
      }
    }
  }

  private void handleEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {

    case AssumeEdge: {
      CExpression exp = ((CAssumeEdge) edge).getExpression();
      CFANode pre = edge.getPredecessor();

      DependencyCollectingVisitor dcv = new DependencyCollectingVisitor(pre);
      Multimap<String, String> dep = exp.accept(dcv);
      dependencies.addAll(dep, dcv.getValues(), edge, 0);

      exp.accept(new BoolCollectingVisitor(pre));
      exp.accept(new IntEqualCollectingVisitor(pre, false));
      exp.accept(new IntAddCollectingVisitor(pre));

      break;
    }

    case DeclarationEdge: {
      handleDeclarationEdge((CDeclarationEdge) edge);
      break;
    }

    case StatementEdge: {
      final CStatement statement = ((CStatementEdge) edge).getStatement();

      // normal assignment of variable, rightHandSide can be expression or (external) functioncall
      if (statement instanceof CAssignment) {
        handleAssignment(edge, (CAssignment) statement);

        // pure external functioncall
      } else if (statement instanceof CFunctionCallStatement) {
        handleExternalFunctionCall(edge, ((CFunctionCallStatement) statement).
            getFunctionCallExpression().getParameterExpressions());
      }

      break;
    }

    case FunctionCallEdge: {
      handleFunctionCallEdge((CFunctionCallEdge) edge);
      break;
    }

    case FunctionReturnEdge: {
      String innerFunctionName = edge.getPredecessor().getFunctionName();
      dependencies.addVar(innerFunctionName, FUNCTION_RETURN_VARIABLE);
      Partition partition = getPartitionForVar(innerFunctionName, FUNCTION_RETURN_VARIABLE);
      partition.addEdge(edge, 0);
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

    case MultiEdge:
      for (CFAEdge innerEdge : (MultiEdge) edge) {
        handleEdge(innerEdge);
      }
      break;

    case BlankEdge:
    case CallToReturnEdge:
      // other cases are not interesting
      break;

    default:
      throw new AssertionError("Unknoewn edgeType: " + edge.getEdgeType());
    }
  }

  /** This function handles a declaration with an optional initializer.
   * Only simple types are handled. */
  private void handleDeclarationEdge(final CDeclarationEdge edge) {
    CDeclaration declaration = edge.getDeclaration();
    if (!(declaration instanceof CVariableDeclaration)) { return; }

    CVariableDeclaration vdecl = (CVariableDeclaration) declaration;
    String varName = vdecl.getName();
    String function = vdecl.isGlobal() ? null : edge.getPredecessor().getFunctionName();

    // "connect" the edge with its partition
    HashMultimap<String, String> var = HashMultimap.create(1, 1);
    var.put(function, varName);
    dependencies.addAll(var, new HashSet<BigInteger>(), edge, 0);

    // only simple types (int, long) are allowed for booleans, ...
    if (!(vdecl.getType() instanceof CSimpleType)) {
      nonBooleanVars.put(function, varName);
      nonIntEqualVars.put(function, varName);
      nonIntAddVars.put(function, varName);
    }

    IAInitializer initializer = vdecl.getInitializer();
    if ((initializer == null) || !(initializer instanceof CInitializerExpression)) { return; }

    CExpression exp = ((CInitializerExpression) initializer).getExpression();
    if (exp == null) { return; }

    handleExpression(edge, exp, varName, function);
  }

  /** This function handles normal assignments of vars. */
  private void handleAssignment(final CFAEdge edge, final CAssignment assignment) {
    CRightHandSide rhs = assignment.getRightHandSide();
    CExpression lhs = assignment.getLeftHandSide().getExpression();
    String varName = lhs.toASTString();
    String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();

    // only simple types (int, long) are allowed for booleans, ...
    if (!(lhs instanceof CIdExpression && lhs.getExpressionType() instanceof CSimpleType)) {
      nonBooleanVars.put(function, varName);
      nonIntEqualVars.put(function, varName);
      nonIntAddVars.put(function, varName);
    }

    dependencies.addVar(function, varName);

    if (rhs instanceof CExpression) {
      handleExpression(edge, ((CExpression) rhs), varName, function);

    } else if (rhs instanceof CFunctionCallExpression) {
      // use FUNCTION_RETURN_VARIABLE for RIGHT SIDE
      CFunctionCallExpression func = (CFunctionCallExpression) rhs;
      String functionName = func.getFunctionNameExpression().toASTString(); // TODO correct?

      if (cfa.getAllFunctionNames().contains(functionName)) {
        // TODO is this case really appearing or is it always handled as "functionCallEdge"?
        dependencies.add(functionName, FUNCTION_RETURN_VARIABLE, function, varName);

      } else {
        // external function
        Partition partition = getPartitionForVar(function, varName);
        partition.addEdge(edge, -1); // negative value, because all positives are used for params
      }

      handleExternalFunctionCall(edge, func.getParameterExpressions());

    } else {
      throw new AssertionError("unhandled assignment: " + edge.getRawStatement());
    }
  }

  /** This function handles the call of an external function
   * without an assignment of the result.
   * example: "printf("%d", output);" or "assert(exp);" */
  private void handleExternalFunctionCall(final CFAEdge edge, final List<CExpression> params) {
    for (int i = 0; i < params.size(); i++) {
      final CExpression param = params.get(i);

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      if (param instanceof CUnaryExpression &&
          UnaryOperator.AMPER == ((CUnaryExpression) param).getOperator() &&
          ((CUnaryExpression) param).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) param).getOperand();
        final String function = isGlobal(id) ? null : edge.getPredecessor().getFunctionName();
        final String varName = id.getName();

        dependencies.addVar(function, varName);
        Partition partition = getPartitionForVar(function, varName);
        partition.addEdge(edge, i);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO do we need the edge? ignore it?

        CFANode pre = edge.getPredecessor();
        DependencyCollectingVisitor dcv = new DependencyCollectingVisitor(pre);
        Multimap<String, String> dep = param.accept(dcv);
        dependencies.addAll(dep, dcv.getValues(), edge, i);

        param.accept(new BoolCollectingVisitor(pre));
        param.accept(new IntEqualCollectingVisitor(pre, false));
        param.accept(new IntAddCollectingVisitor(pre));
      }
    }
  }

  /** This function puts each param in same partition than its arg.
   * If there the functionresult is assigned, it is also handled. */
  private void handleFunctionCallEdge(CFunctionCallEdge edge) {

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    List<CExpression> args = edge.getArguments();
    List<CParameterDeclaration> params = edge.getSuccessor().getFunctionParameters();
    String innerFunctionName = edge.getSuccessor().getFunctionName();

    // functions can have more args than params used in the call
    assert args.size() >= params.size();

    for (int i = 0; i < params.size(); i++) {
      CParameterDeclaration param = params.get(i);
      String varName = param.getName();

      // only simple types (int, long) are allowed for booleans, ...
      if (!(param.getType() instanceof CSimpleType)) {
        nonBooleanVars.put(innerFunctionName, varName);
        nonIntEqualVars.put(innerFunctionName, varName);
        nonIntAddVars.put(innerFunctionName, varName);
      }

      // build name for param and evaluate it
      // this variable is not global (->false)
      handleExpression(edge, args.get(i), varName, innerFunctionName, i);
    }

    // create dependency for functionreturn
    CFunctionSummaryEdge func = edge.getSummaryEdge();
    CStatement statement = func.getExpression().asStatement();

    // a=f();
    if (statement instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement call = (CFunctionCallAssignmentStatement) statement;
      CExpression lhs = call.getLeftHandSide().getExpression();
      String varName = lhs.toASTString();
      String function = isGlobal(lhs) ? null : edge.getPredecessor().getFunctionName();
      dependencies.add(innerFunctionName, FUNCTION_RETURN_VARIABLE, function, varName);

      // f(); without assignment
    } else if (statement instanceof CFunctionCallStatement) {
      // next line is not necessary, but we do it for completeness, TODO correct?
      dependencies.addVar(innerFunctionName, FUNCTION_RETURN_VARIABLE);
    }
  }

  /** evaluates an expression and adds containing vars to the sets. */
  private void handleExpression(CFAEdge edge, CExpression exp, String varName,
      String function) {
    handleExpression(edge, exp, varName, function, 0);
  }

  /** evaluates an expression and adds containing vars to the sets.
   * the id is the position of the expression in the edge,
   * it is 0 for all edges except a FuntionCallEdge. */
  private void handleExpression(CFAEdge edge, CExpression exp, String varName,
      String function, int id) {
    CFANode pre = edge.getPredecessor();

    DependencyCollectingVisitor dcv = new DependencyCollectingVisitor(pre);
    Multimap<String, String> dep = exp.accept(dcv);
    if (dep == null) {
      dep = HashMultimap.create(1, 1);
    }
    dep.put(function, varName);
    dependencies.addAll(dep, dcv.getValues(), edge, id);

    BoolCollectingVisitor bcv = new BoolCollectingVisitor(pre);
    Multimap<String, String> possibleBoolean = exp.accept(bcv);
    handleResult(varName, function, possibleBoolean, nonBooleanVars);

    IntEqualCollectingVisitor ncv = new IntEqualCollectingVisitor(pre, true);
    Multimap<String, String> possibleIntEqualVars = exp.accept(ncv);
    handleResult(varName, function, possibleIntEqualVars, nonIntEqualVars);

    IntAddCollectingVisitor icv = new IntAddCollectingVisitor(pre);
    Multimap<String, String> possibleIntAddVars = exp.accept(icv);
    handleResult(varName, function, possibleIntAddVars, nonIntAddVars);
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

  /** returns the value of a (nested) IntegerLiteralExpression
   * or null for everything else. */
  public static BigInteger getNumber(CExpression exp) {
    if (exp instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression) exp).getValue();

    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unExp = (CUnaryExpression) exp;
      BigInteger value = getNumber(unExp.getOperand());
      if (value == null) { return null; }
      switch (unExp.getOperator()) {
      case PLUS:
        return value;
      case MINUS:
        return value.negate();
      default:
        return null;
      }

    } else if (exp instanceof CCastExpression) {
      return getNumber(((CCastExpression) exp).getOperand());

    } else {
      return null;
    }
  }

  /** returns true, if the expression contains a casted or negated binaryExpression. */
  private boolean isNestedBinaryExp(CExpression exp) {
    if (exp instanceof CBinaryExpression) {
      return true;

    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unExp = (CUnaryExpression) exp;
      return (UnaryOperator.NOT == unExp.getOperator()) &&
          isNestedBinaryExp(unExp.getOperand());

    } else if (exp instanceof CCastExpression) {
      return isNestedBinaryExp(((CCastExpression) exp).getOperand());

    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    build();

    StringBuilder str = new StringBuilder();
    str.append("\nALL  " + allVars.size() + "\n    " + allVars);
    str.append("\nBool  " + booleanVars.size() + "\n    " + booleanVars);
    str.append("\nIntEqual  " + intEqualVars.size() + "\n    " + intEqualVars);
    str.append("\nIntAdd  " + intAddVars.size() + "\n    " + intAddVars);
    return str.toString();
  }

  /** This Visitor evaluates an Expression. It collects all variables.
   * a visit of IdExpression or CFieldReference returns a collection containing the varName,
   * a visit of CastExpression return the containing visit,
   * other visits return null.
  * The Visitor collects all numbers used in the expression. */
  private class DependencyCollectingVisitor implements
      CExpressionVisitor<Multimap<String, String>, RuntimeException> {

    private CFANode predecessor;
    private Set<BigInteger> values = new TreeSet<>();

    public DependencyCollectingVisitor(CFANode pre) {
      this.predecessor = pre;
    }

    public Set<BigInteger> getValues() {
      return values;
    }

    @Override
    public Multimap<String, String> visit(CArraySubscriptExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) {

      // for numeral values
      BigInteger val1 = getNumber(exp.getOperand1());
      Multimap<String, String> operand1;
      if (val1 == null) {
        operand1 = exp.getOperand1().accept(this);
      } else {
        values.add(val1);
        operand1 = null;
      }

      // for numeral values
      BigInteger val2 = getNumber(exp.getOperand2());
      Multimap<String, String> operand2;
      if (val2 == null) {
        operand2 = exp.getOperand2().accept(this);
      } else {
        values.add(val2);
        operand2 = null;
      }

      // handle vars from operands
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
    public Multimap<String, String> visit(CCastExpression exp) {
      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CFieldReference exp) {
      String varName = exp.toASTString(); // TODO "(*p).x" vs "p->x"
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
      values.add(exp.getValue());
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
    public Multimap<String, String> visit(CTypeIdInitializerExpression exp) {
      return null;
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) {
      BigInteger val = getNumber(exp);
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        values.add(val);
        return null;
      }
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
    public Multimap<String, String> visit(CFieldReference exp) {
      nonBooleanVars.putAll(super.visit(exp));
      return null;
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) {
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

      case EQUALS:
      case NOT_EQUALS: // ==, != work with boolean operands
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
    public Multimap<String, String> visit(CUnaryExpression exp) {
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
  private class IntEqualCollectingVisitor extends DependencyCollectingVisitor {

    /** this flag only allows vars and values, no calculations */
    private boolean onlyOneExp;

    public IntEqualCollectingVisitor(CFANode pre, boolean onlyOneExp) {
      super(pre);
      this.onlyOneExp = onlyOneExp;
    }

    @Override
    public Multimap<String, String> visit(CCastExpression exp) {
      BigInteger val = getNumber(exp.getOperand());
      if (val == null) {
        return exp.getOperand().accept(this);
      } else {
        return HashMultimap.create(0, 0);
      }
    }

    @Override
    public Multimap<String, String> visit(CFieldReference exp) {
      nonIntEqualVars.putAll(super.visit(exp));
      return null;
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) {

      // for numeral values
      BigInteger val1 = getNumber(exp.getOperand1());
      Multimap<String, String> operand1;
      if (val1 == null) {
        operand1 = exp.getOperand1().accept(this);
      } else {
        operand1 = HashMultimap.create(0, 0);
      }

      // for numeral values
      BigInteger val2 = getNumber(exp.getOperand2());
      Multimap<String, String> operand2;
      if (val2 == null) {
        operand2 = exp.getOperand2().accept(this);
      } else {
        operand2 = HashMultimap.create(0, 0);
      }

      // handle vars from operands
      if (onlyOneExp || operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIntEqualVars.putAll(operand1);
        }
        if (operand2 != null) {
          nonIntEqualVars.putAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case EQUALS:
      case NOT_EQUALS: // ==, != work with numbers
        operand1.putAll(operand2);
        return operand1;

      default: // +-*/ --> no simple operators
        nonIntEqualVars.putAll(operand1);
        nonIntEqualVars.putAll(operand2);
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      return HashMultimap.create(0, 0);
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) {

      // if exp is numeral
      BigInteger val = getNumber(exp);
      if (val != null) { return HashMultimap.create(0, 0); }

      // if exp is binary expression
      Multimap<String, String> inner = exp.getOperand().accept(this);
      if (isNestedBinaryExp(exp)) { return inner; }

      // if exp is unknown
      if (inner == null) { return null; }

      // if exp is a simple var
      switch (exp.getOperator()) {
      case PLUS: // this is no calculation, no usage of another param
        return inner;
      default: // *, ~, etc --> not numeral
        nonIntEqualVars.putAll(inner);
        return null;
      }
    }
  }


  /** This Visitor evaluates an Expression.
   * Each visit-function returns
   * - a collection, if the expression is a var or a simple mathematical
   *   calculation (add, sub, <, >, <=, >=, ==, !=, !),
   * - else null */
  private class IntAddCollectingVisitor extends DependencyCollectingVisitor {

    public IntAddCollectingVisitor(CFANode pre) {
      super(pre);
    }

    @Override
    public Multimap<String, String> visit(CCastExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Multimap<String, String> visit(CFieldReference exp) {
      nonIntAddVars.putAll(super.visit(exp));
      return null;
    }

    @Override
    public Multimap<String, String> visit(CBinaryExpression exp) {
      Multimap<String, String> operand1 = exp.getOperand1().accept(this);
      Multimap<String, String> operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { // a+0.2 --> no simple number
        if (operand1 != null) {
          nonIntAddVars.putAll(operand1);
        }
        if (operand2 != null) {
          nonIntAddVars.putAll(operand2);
        }
        return null;
      }

      switch (exp.getOperator()) {

      case PLUS:
      case MINUS:
      case LESS_THAN:
      case LESS_EQUAL:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case EQUALS:
      case NOT_EQUALS:
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
        // this calculations work with all numbers
        operand1.putAll(operand2);
        return operand1;

      default: // *, /, %, shift --> no simple calculations
        nonIntAddVars.putAll(operand1);
        nonIntAddVars.putAll(operand2);
        return null;
      }
    }

    @Override
    public Multimap<String, String> visit(CIntegerLiteralExpression exp) {
      return HashMultimap.create(0, 0);
    }

    @Override
    public Multimap<String, String> visit(CUnaryExpression exp) {
      Multimap<String, String> inner = exp.getOperand().accept(this);
      if (inner == null) { return null; }

      switch (exp.getOperator()) {
      case PLUS:
      case MINUS:
      case NOT:
        return inner;
      default: // *, ~, etc --> not simple
        nonIntAddVars.putAll(inner);
        return null;
      }
    }
  }

  /** A Partition is a Wrapper for some vars, values and edges.
   * After merging two Partitions, they wrap the same internals,
   * so adding a value to the first modifies the other one, too. */
  public class Partition {

    private Multimap<String, String> vars = LinkedHashMultimap.create();
    private Set<BigInteger> values = Sets.newTreeSet();
    private Multimap<CFAEdge, Integer> edges = HashMultimap.create();

    private Map<Pair<String, String>, Partition> varToPartition;
    private Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition;

    public Partition(Map<Pair<String, String>, Partition> varToPartition,
        Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition) {
      this.varToPartition = varToPartition;
      this.edgeToPartition = edgeToPartition;
    }

    public Multimap<String, String> getVars() {
      return vars;
    }

    public Set<BigInteger> getValues() {
      return values;
    }

    public Multimap<CFAEdge, Integer> getEdges() {
      return edges;
    }

    /** adds the var to the partition and also to the global set of all vars. */
    public void add(String function, String varName) {
      vars.put(function, varName);
      allVars.put(function, varName);
      varToPartition.put(Pair.of(function, varName), this);
    }

    public void addValues(Set<BigInteger> newValues) {
      values.addAll(newValues);
    }

    public void addEdge(CFAEdge edge, int index) {
      edges.put(edge, index);
      edgeToPartition.put(Pair.of(edge, index), this);
    }

    /** copies all data from other to current partition */
    public void merge(Partition other) {
      assert this.varToPartition == other.varToPartition;

      this.vars.putAll(other.vars);
      this.values.addAll(other.values);
      this.edges.putAll(other.edges);

      // update mapping of vars
      for (Entry<String, String> var : other.vars.entries()) {
        varToPartition.put(Pair.of(var.getKey(), var.getValue()), this);
      }

      // update mapping of edges
      for (Entry<CFAEdge, Integer> edge : other.edges.entries()) {
        edgeToPartition.put(Pair.of(edge.getKey(), edge.getValue()), this);
      }
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Partition) {
        Partition p = (Partition) other;
        return this.vars == p.vars;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return vars.hashCode();
    }

    @Override
    public String toString() {
      return vars.toString() + " --> " + Arrays.toString(values.toArray());
    }
  }

  /** This class stores dependencies between variables.
   * It sorts vars into partitions.
   * Dependent vars are in the same partition. Partitions are independent. */
  private class Dependencies {

    /** partitions, each of them contains vars */
    private List<Partition> partitions = Lists.newArrayList();

    /** map to get partition of a var */
    private Map<Pair<String, String>, Partition> varToPartition = Maps.newHashMap();

    /** table to get a partition for a edge. */
    Map<Pair<CFAEdge, Integer>, Partition> edgeToPartition = Maps.newHashMap();

    public List<Partition> getPartitions() {
      return partitions;
    }

    public Partition getPartitionForVar(String function, String var) {
      return varToPartition.get(Pair.of(function, var));
    }

    public Partition getPartitionForEdge(CFAEdge edge, int index) {
      return edgeToPartition.get(Pair.of(edge, index));
    }

    /** This function creates a dependency between function1::var1 and function2::var2. */
    public void add(String function1, String var1, String function2, String var2) {
      Pair<String, String> first = Pair.of(function1, var1);
      Pair<String, String> second = Pair.of(function2, var2);

      // if both vars exists in some dependencies,
      // either ignore them or merge their partitions
      Partition partition1 = varToPartition.get(first);
      Partition partition2 = varToPartition.get(second);
      if (partition1 != null && partition2 != null) {

        // swap partitions, we create partitions in the order they are used
        if (partitions.lastIndexOf(partition1) > partitions.lastIndexOf(partition2)) {
          Partition tmp = partition2;
          partition2 = partition1;
          partition1 = tmp;
        }

        if (!partition1.equals(partition2)) {
          partition1.merge(partition2);
          partitions.remove(partition2);
        }

        // if only left side of dependency exists, add right side into same partition
      } else if (partition1 != null) {
        partition1.add(function2, var2);

        // if only right side of dependency exists, add left side into same partition
      } else if (partition2 != null) {
        partition2.add(function1, var1);

        // if none side is in any existing partition, create new partition
      } else {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(function1, var1);
        partition.add(function2, var2);
        partitions.add(partition);
      }
    }

    /** This function adds a group of vars to exactly one partition.
     * The values are stored in the partition.
     * The partition is "connected" with the expression. */
    public void addAll(Multimap<String, String> vars, Set<BigInteger> values,
        CFAEdge edge, int index) {
      if (vars == null || vars.isEmpty()) { return; }

      Iterator<Entry<String, String>> iter = vars.entries().iterator();

      // we use same function and varName for all other vars --> dependency
      Entry<String, String> entry = iter.next();
      String function = entry.getKey();
      String varName = entry.getValue();

      // first add one single var
      addVar(function, varName);

      // then add all other vars, they are dependent from the first var
      while (iter.hasNext()) {
        entry = iter.next();
        add(function, varName, entry.getKey(), entry.getValue());
      }

      Partition partition = getPartitionForVar(function, varName);
      partition.addValues(values);
      partition.addEdge(edge, index);
    }

    /** This function adds one single variable to the partitions.
     * This is the only method to create a partition with only one element. */
    public void addVar(String function, String varName) {
      Pair<String, String> var = Pair.of(function, varName);

      // if var exists, we can ignore it, otherwise create new partition for var
      if (!varToPartition.containsKey(var)) {
        Partition partition = new Partition(varToPartition, edgeToPartition);
        partition.add(function, varName);
        partitions.add(partition);
      }
    }

    /** This function adds all depending vars to the set, if necessary.
     * If A depends on B and A is part of the set, B is added to the set, and vice versa.
    * Example: If A is not boolean, B is not boolean. */
    public void solve(final Multimap<String, String> vars) {
      for (Partition partition : partitions) {

        // is at least one var from the partition part of vars
        boolean isDependency = false;
        for (Entry<String, String> var : partition.getVars().entries()) {
          if (vars.containsEntry(var.getKey(), var.getValue())) {
            isDependency = true;
            break;
          }
        }

        // add all dependend vars to vars
        if (isDependency) {
          vars.putAll(partition.getVars());
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder str = new StringBuilder("[");
      for (Partition partition : partitions) {
        str.append(partition.toString() + ",\n");
      }
      str.append("]\n\n");

      //      for (Pair<CFAEdge, Integer> edge : edgeToPartition.keySet()) {
      //        str.append(edge.getFirst().getRawStatement() + " :: "
      //            + edge.getSecond() + " --> " + edgeToPartition.get(edge) + "\n");
      //      }
      return str.toString();
    }
  }
}
