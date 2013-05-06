/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.VariableClassification.FUNCTION_RETURN_VARIABLE;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/** This Transfer Relation tracks variables and handles them as bitvectors. */
@Options(prefix = "cpa.bdd")
public class BDDTransferRelation implements TransferRelation {

  @Option(name = "logfile", description = "Dump tracked variables to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("BDDCPA_tracked_variables.log");

  private static final String TMP_VARIABLE = "__CPAchecker_tmp_var";

  @Option(description = "initialize all variables to 0 when they are declared")
  private boolean initAllVars = false;

  @Option(description = "declare first bit of all vars, then second bit,...")
  private boolean initBitwise = true;

  @Option(description = "declare the bits of a var from 0 to N or from N to 0")
  private boolean initBitsIncreasing = true;

  @Option(description = "adding a new value to the state can be done from 0 to N or from N to 0")
  private boolean addIncreasing = false;

  @Option(description = "declare vars partitionwise")
  private boolean initPartitions = true;

  @Option(description = "declare partitions ordered")
  private boolean initPartitionsOrdered = true;

  @Option(description = "default bitsize for values and vars")
  private int bitsize = 32;

  @Option(description = "use a smaller bitsize for all vars, that have only intEqual values")
  private boolean compressIntEqual = true;

  private final LogManager logger;

  private final VariableClassification varClass;
  private final Map<Multimap<String, String>, String> varsToTmpVar = new HashMap<>();

  /** This map is used for scoping vars. It contains all used vars of a function. */
  private final Multimap<String, Region> functionToVars = LinkedHashMultimap.create();

  private final BitvectorManager bvmgr;
  private final NamedRegionManager rmgr;

  /** This map contains tuples (int, region[]) for each intEqual-partition. */
  private final Map<Partition, Map<BigInteger, Region[]>> intToRegionsMap = new HashMap<>();

  /** Contains the varNames of all really tracked vars.
   * This set may differ from the union of all partitions,
   * because not every variable, that appears in the sourcecode,
   * is analyzed or even reachable. */
  private final Multimap<String, String> trackedVars = LinkedHashMultimap.create();

  /** The Constructor of BDDVectorTransferRelation sets the NamedRegionManager
   * and the BitVectorManager. Both are used to build and manipulate BDDs,
   * that represent the regions. */
  public BDDTransferRelation(NamedRegionManager manager, LogManager pLogger,
      Configuration config, BDDRegionManager rmgr, CFA cfa, BDDPrecision precision)
      throws InvalidConfigurationException {
    config.inject(this);

    this.logger = pLogger;
    this.bvmgr = new BitvectorManager(config, rmgr);
    this.rmgr = manager;

    assert cfa.getVarClassification().isPresent();
    this.varClass = cfa.getVarClassification().get();

    if (initPartitions) {
      initVars(precision, cfa);
    }

    if (compressIntEqual) {
      initMappingIntToRegions();
    }
  }

  /** The BDDRegionManager orders the variables as they are declared
   *  (later vars are deeper in the BDD).
   *  This function declares those vars in the beginning of the analysis,
   *  so that we can choose between some orders. */
  private void initVars(BDDPrecision precision, CFA cfa) {
    List<Partition> partitions;
    if (initPartitionsOrdered) {
      BDDPartitionOrderer d = new BDDPartitionOrderer(cfa);
      partitions = d.getOrderedPartitions();
    } else {
      partitions = varClass.getPartitions();
    }

    for (Partition partition : partitions) {
      createPredicates(partition.getVars(), precision, partitionToBitsize(partition));
    }
  }

  /** This function declares variables for a given collection of vars.
   *
   * The value 'bitsize' chooses how much bits are used for each var.
   * The varname is build as "varname@pos". */
  private void createPredicates(Multimap<String, String> vars,
      BDDPrecision precision, int bitsize) {

    // add a temporary variable for each partition
    String tmpVar = TMP_VARIABLE + "_" + varsToTmpVar.size();
    varsToTmpVar.put(vars, tmpVar);

    if (bitsize == 1) {
      boolean isTrackingSomething = false;
      for (Entry<String, String> entry : vars.entries()) { // different loop order!
        if (precision.isTracking(entry.getKey(), entry.getValue())) {
          createPredicateDirectly(entry.getKey(), entry.getValue());
          isTrackingSomething = true;
        }
      }
      if (isTrackingSomething) {
        createPredicateDirectly(null, tmpVar);
      }

    } else {
      assert bitsize > 1 : "you need at least one bit for a variable.";

      // bitvectors [a2, a1, a0]
      // 'initBitwise' chooses between initialing each var separately or bitwise overlapped.
      if (initBitwise) {

        // [a2, b2, c2, a1, b1, c1, a0, b0, c0]
        boolean isTrackingSomething = false;
        for (int i = 0; i < bitsize; i++) {
          int index = initBitsIncreasing ? i : (bitsize - i - 1);
          for (Entry<String, String> entry : vars.entries()) {
            if (precision.isTracking(entry.getKey(), entry.getValue())) {
              createPredicateDirectly(entry.getKey(), entry.getValue(), index);
              isTrackingSomething = true;
            }
          }
          if (isTrackingSomething) {
            createPredicateDirectly(null, tmpVar, index);
          }
        }

      } else {
        // [a2, a1, a0, b2, b1, b0, c2, c1, c0]
        boolean isTrackingSomething = false;
        for (Entry<String, String> entry : vars.entries()) { // different loop order!
          if (precision.isTracking(entry.getKey(), entry.getValue())) {
            for (int i = 0; i < bitsize; i++) {
              int index = initBitsIncreasing ? i : (bitsize - i - 1);
              createPredicateDirectly(entry.getKey(), entry.getValue(), index);
            }
            isTrackingSomething = true;
          }
        }
        if (isTrackingSomething) {
          for (int i = 0; i < bitsize; i++) {
            int index = initBitsIncreasing ? i : (bitsize - i - 1);
            createPredicateDirectly(null, tmpVar, index);
          }
        }
      }
    }
  }

  @Override
  public Collection<BDDState> getAbstractSuccessors(
      AbstractState abstractState, Precision prec, CFAEdge cfaEdge)
      throws CPATransferException {
    BDDState state = (BDDState) abstractState;
    BDDPrecision precision = (BDDPrecision) prec;

    if (precision.isDisabled()) {
      // this means that no variables should be tracked
      return Collections.singleton(state);
    }

    if (state.getRegion().isFalse()) { return Collections.emptyList(); }

    BDDState successor = null;

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge:
      successor = handleAssumption(state, (CAssumeEdge) cfaEdge, precision);
      break;

    case FunctionCallEdge:
      successor = handleFunctionCallEdge(state, (CFunctionCallEdge) cfaEdge, precision);
      break;

    case FunctionReturnEdge:
      successor = handleFunctionReturnEdge(state, (CFunctionReturnEdge) cfaEdge, precision);
      break;

    case MultiEdge:
      successor = state;
      for (CFAEdge innerEdge : (MultiEdge) cfaEdge) {
        successor = handleSimpleEdge(successor, precision, innerEdge);
      }
      break;

    default:
      successor = handleSimpleEdge(state, precision, cfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      assert !successor.getRegion().isFalse();
      return Collections.singleton(successor);
    }
  }

  /** This function handles simple edges like Declarations, Statements,
   * ReturnStatements and BlankEdges. */
  private BDDState handleSimpleEdge(
      BDDState state, BDDPrecision prec, CFAEdge cfaEdge) {

    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      return handleDeclarationEdge(state, (CDeclarationEdge) cfaEdge, prec);

    case StatementEdge:
      return handleStatementEdge(state, (CStatementEdge) cfaEdge, prec);

    case ReturnStatementEdge:
      return handleReturnStatementEdge(state, (CReturnStatementEdge) cfaEdge, prec);

    case BlankEdge:
    case CallToReturnEdge:
      return state; // nothing to do

    default:
      throw new AssertionError("unknown edgeType");
    }
  }

  /** This function handles statements like "a = 0;" and "b = !a;" and
   * calls of external functions. */
  private BDDState handleStatementEdge(final BDDState state, final CStatementEdge cfaEdge,
      final BDDPrecision precision) {
    final CStatement statement = cfaEdge.getStatement();
    BDDState result = state;

    // normal assignment, "a = ..."
    if (statement instanceof CAssignment) {
      result = handleAssignment(cfaEdge, state, (CAssignment) statement, precision);

      // call of external function, "scanf(...)" without assignment
      // internal functioncalls are handled as FunctionCallEdges
    } else if (statement instanceof CFunctionCallStatement) {
      final Region newRegion = handleExternalFunctionCall(cfaEdge, result, state.getRegion(),
          ((CFunctionCallStatement) statement).getFunctionCallExpression().getParameterExpressions(),
          precision);
      result = new BDDState(rmgr, newRegion);

    }

    assert !result.getRegion().isFalse();
    return result;
  }

  /** This function handles statements like "a = 0;" and "b = !a;".
   * A region is build for the right side of the statement.
   * Then this region is assigned to the variable at the left side.
   * This equality is added to the BDDstate to get the next state. */
  private BDDState handleAssignment(CFAEdge cfaEdge, BDDState state, CAssignment assignment, BDDPrecision precision) {
    Region newRegion = state.getRegion();
    CExpression lhs = assignment.getLeftHandSide();
    if (!(lhs instanceof CIdExpression)) { return state; }

    final String functionName = cfaEdge.getPredecessor().getFunctionName();
    final String scopedFunctionName = isGlobal(lhs) ? null : functionName;
    final String varName = lhs.toASTString();

    // next line is a shortcut, not necessary
    if (!precision.isTracking(scopedFunctionName, varName)) { return state; }

    CRightHandSide rhs = assignment.getRightHandSide();
    if (rhs instanceof CExpression) {
      CExpression exp = (CExpression) rhs;
      final Partition partition = varClass.getPartitionForEdge(cfaEdge);
      final int size = partitionToBitsize(partition);

      if (isUsedInExpression(scopedFunctionName, varName, exp)) {
        // make tmp for assignment,
        // this is done to handle assignments like "a = !a;" as "tmp = !a; a = tmp;"
        String tmpVarName;
        if (initPartitions) {
          tmpVarName = varsToTmpVar.get(varClass.getPartitionForVar(scopedFunctionName, varName).getVars());
        } else {
          tmpVarName = TMP_VARIABLE;
        }

        final Region[] tmp = createPredicates(null, tmpVarName, size, null);

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS = evaluateVectorExpression(functionName, precision, partition, exp, size);
        newRegion = addEquality(tmp, regRHS, newRegion);

        // delete var, make tmp equal to (new) var, then delete tmp
        final Region[] var = createPredicates(scopedFunctionName, varName, size, precision);
        newRegion = removePredicate(newRegion, var);
        newRegion = addEquality(var, tmp, newRegion);
        newRegion = removePredicate(newRegion, tmp);

      } else {
        final Region[] var = createPredicates(scopedFunctionName, varName, size, precision);
        newRegion = removePredicate(newRegion, var);

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS = evaluateVectorExpression(functionName, precision, partition, (CExpression) rhs, size);
        newRegion = addEquality(var, regRHS, newRegion);
      }
      return new BDDState(rmgr, newRegion);

    } else if (rhs instanceof CFunctionCallExpression) {
      // handle params of functionCall, maybe there is a sideeffect
      newRegion = handleExternalFunctionCall(cfaEdge, state, newRegion,
          ((CFunctionCallExpression) rhs).getParameterExpressions(), precision);

      // call of external function: we know nothing, so we delete the value of the var
      // TODO can we assume, that malloc returns something !=0?
      // are there some "save functions"?

      final Partition partition = varClass.getPartitionForEdge(cfaEdge, -1); // -1 is the var of the assignmnt
      final Region[] var = createPredicates(scopedFunctionName, varName, partitionToBitsize(partition), precision);
      newRegion = removePredicate(newRegion, var);

      return new BDDState(rmgr, newRegion);

    } else {
      throw new AssertionError("unhandled assignment: " + cfaEdge.getRawStatement());
    }
  }

  /** This function deletes all vars, that could be modified
   * through a side-effect of the (external) functionCall. */
  private Region handleExternalFunctionCall(final CFAEdge cfaEdge,
      final BDDState state, Region newRegion,
      final List<CExpression> params, final BDDPrecision precision) {

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
        final String function = isGlobal(id) ? null : cfaEdge.getPredecessor().getFunctionName();
        final String varName = id.getName();
        final Partition partition = varClass.getPartitionForEdge(cfaEdge, i);

        final Region[] var = createPredicates(function, varName, partitionToBitsize(partition), precision);
        newRegion = removePredicate(newRegion, var);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO: can we do something here?
      }
    }
    return newRegion;
  }

  /** This function handles declarations like "int a = 0;" and "int b = !a;".
   * Regions are build for all Bits of the right side of the declaration,
   * if it is not null. Then these regions are assigned to the regions of
   * variable (bitvector) at the left side.
   * These equalities are added to the BDDstate to get the next state. */
  private BDDState handleDeclarationEdge(BDDState state, CDeclarationEdge cfaEdge,
      BDDPrecision precision) {

    CDeclaration decl = cfaEdge.getDeclaration();

    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      CInitializer initializer = vdecl.getInitializer();

      CExpression init = null;
      if (initializer == null && initAllVars) { // auto-initialize variables to zero
        init = CDefaults.forType(decl.getType(), decl.getFileLocation());
      } else if (initializer instanceof CInitializerExpression) {
        init = ((CInitializerExpression) initializer).getExpression();
      }

      // make variable (predicate) for LEFT SIDE of declaration,
      // delete variable, if it was initialized before i.e. in another block, with an existential operator
      final String functionName = cfaEdge.getPredecessor().getFunctionName();
      final String scopedFunctionName = vdecl.isGlobal() ? null : functionName;
      final String varName = vdecl.getName();

      Partition partition = varClass.getPartitionForEdge(cfaEdge);
      final int size = partitionToBitsize(partition);

      Region[] var = createPredicates(scopedFunctionName, varName, size, precision);
      Region newRegion = removePredicate(state.getRegion(), var);

      // track vars, so we can delete them after returning from a function,
      // see handleFunctionReturnEdge(...) for detail.
      if (var != null && !vdecl.isGlobal()) {
        assert scopedFunctionName != null;
        for (int i = 0; i < var.length; i++) {
          functionToVars.put(scopedFunctionName, var[i]);
        }
      }

      // initializer on RIGHT SIDE available, make region for it
      if (init != null) {
        final Region[] rhs = evaluateVectorExpression(functionName, precision, partition, init, size);
        newRegion = addEquality(var, rhs, newRegion);
        return new BDDState(rmgr, newRegion);
      }
    }

    return state; // if we know nothing, we return the old state
  }

  /** This function handles functioncalls like "f(x)", that calls "f(int a)".
   * Therefore each arg ("x") is transformed into a region and assigned
   * to a param ("int a") of the function. The equalities of
   * all arg-param-pairs are added to the BDDstate to get the next state. */
  private BDDState handleFunctionCallEdge(BDDState state, CFunctionCallEdge cfaEdge,
      BDDPrecision precision) throws UnrecognizedCCodeException {
    Region newRegion = state.getRegion();

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    final List<CExpression> args = cfaEdge.getArguments();
    final List<CParameterDeclaration> params = cfaEdge.getSuccessor().getFunctionParameters();

    // var_args cannot be handled: func(int x, ...) --> we only handle the first n parameters
    assert args.size() >= params.size();

    final String innerFunctionName = cfaEdge.getSuccessor().getFunctionName();
    final String outerFunctionName = cfaEdge.getPredecessor().getFunctionName();

    for (int i = 0; i < params.size(); i++) {

      // make variable (predicate) for param, this variable is not global
      String varName = params.get(i).getName();
      Partition partition = varClass.getPartitionForEdge(cfaEdge, i);
      final int size = partitionToBitsize(partition);
      final Region[] var = createPredicates(innerFunctionName, varName, size, precision);
      final Region[] arg = evaluateVectorExpression(outerFunctionName, precision, partition, args.get(i), size);
      newRegion = addEquality(var, arg, newRegion);
      if (var != null) {
        for (int j = 0; j < var.length; j++) {
          functionToVars.put(innerFunctionName, var[j]);
        }
      }
    }

    return new BDDState(rmgr, newRegion);
  }

  /** This function handles functionReturns like "y=f(x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * left side ("y") is added to the new state.
   * Each variable from inside the function is removed from the BDDstate. */
  private BDDState handleFunctionReturnEdge(BDDState state, CFunctionReturnEdge cfaEdge,
      BDDPrecision precision) {
    Region newRegion = state.getRegion();

    final String innerFunctionName = cfaEdge.getPredecessor().getFunctionName();

    // delete variables from returning function,
    // this results in a smaller BDD and allows to call a function twice.
    Collection<Region> innerVars = functionToVars.get(innerFunctionName);
    if (innerVars.size() > 0) {
      newRegion = removePredicate(newRegion, innerVars.toArray(new Region[0]));
    }

    // set result of function equal to variable on left side
    CFunctionSummaryEdge fnkCall = cfaEdge.getSummaryEdge();
    CStatement call = fnkCall.getExpression().asStatement();
    final Partition partition = varClass.getPartitionForEdge(cfaEdge);
    final int size = partitionToBitsize(partition);

    // handle assignments like "y = f(x);"
    if (call instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) call;
      CExpression lhs = cAssignment.getLeftHandSide();

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      final String outerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      final String function = isGlobal(lhs) ? null : outerFunctionName;
      final String varName = lhs.toASTString();
      // make region (predicate) for RIGHT SIDE
      final Region[] retVar = createPredicates(
          innerFunctionName, FUNCTION_RETURN_VARIABLE, size, precision);
      final Region[] var = createPredicates(function, varName, size, precision);
      newRegion = removePredicate(newRegion, var);
      newRegion = addEquality(var, retVar, newRegion);

      // LAST ACTION: delete varname of right side
      newRegion = removePredicate(newRegion, retVar);

    } else if (call instanceof CFunctionCallStatement) {
      final Region[] retVar = createPredicates(
          innerFunctionName, FUNCTION_RETURN_VARIABLE, size, precision);
      newRegion = removePredicate(newRegion, retVar);

    } else {
      assert false;
    }

    return new BDDState(rmgr, newRegion);
  }

  /** This function handles functionStatements like "return (x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * evaluated right side ("x") is added to the new state. */
  private BDDState handleReturnStatementEdge(BDDState state, CReturnStatementEdge cfaEdge,
      BDDPrecision precision) {
    CRightHandSide rhs = cfaEdge.getExpression();
    if (rhs instanceof CExpression) {

      Region newRegion = state.getRegion();
      Partition partition = varClass.getPartitionForEdge(cfaEdge);
      final int size = partitionToBitsize(partition);
      final String functionName = cfaEdge.getPredecessor().getFunctionName();

      // make variable (predicate) for returnStatement,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] retvar = createPredicates(functionName, FUNCTION_RETURN_VARIABLE, size, precision);

      // make region for RIGHT SIDE, this is the 'x' from 'return (x);
      final Region[] regRHS = evaluateVectorExpression(functionName, precision, partition, (CExpression) rhs, size);
      newRegion = addEquality(retvar, regRHS, newRegion);

      return new BDDState(rmgr, newRegion);
    }
    return state;
  }

  /** This function handles assumptions like "if(a==b)" and "if(a!=0)".
   * A region is build for the assumption.
   * This region is added to the BDDstate to get the next state.
   * If the next state is False, the assumption is not fulfilled.
   * In this case NULL is returned. */
  private BDDState handleAssumption(BDDState state, CAssumeEdge cfaEdge,
      BDDPrecision precision) throws UnrecognizedCCodeException {

    CExpression expression = cfaEdge.getExpression();
    final String functionName = cfaEdge.getPredecessor().getFunctionName();

    Partition partition = varClass.getPartitionForEdge(cfaEdge);
    final Region[] operand = evaluateVectorExpression(functionName, precision, partition, expression, partitionToBitsize(partition));
    if (operand == null) { return state; } // assumption cannot be evaluated
    Region evaluated = bvmgr.makeOr(operand);

    if (!cfaEdge.getTruthAssumption()) { // if false-branch
      evaluated = rmgr.makeNot(evaluated);
    }

    // get information from region into evaluated region
    Region newRegion = rmgr.makeAnd(state.getRegion(), evaluated);
    if (newRegion.isFalse()) { // assumption is not fulfilled / not possible
      return null;
    } else {
      return new BDDState(rmgr, newRegion);
    }
  }

  /** This function returns a bitvector, that represents the expression.
   * The partition chooses the compression of the bitvector. */
  private Region[] evaluateVectorExpression(final String functionName,
      final BDDPrecision precision, final Partition partition, final CExpression exp, final int size) {
    return exp.accept(new BDDVectorCExpressionVisitor(
        functionName, precision, partition, size));
  }

  /** This function builds the equality of left and right side and adds it to the environment.
   * If left or right side is null, the environment is returned unchanged. */
  private Region addEquality(@Nullable Region[] leftSide, @Nullable Region[] rightSide, Region environment) {
    if (leftSide == null || rightSide == null) {
      return environment;
    } else {
      final Region[] assignRegions = bvmgr.makeBinaryEqual(leftSide, rightSide);

      Region result;

      if (addIncreasing) {
        result = assignRegions[0];
        for (int i = 1; i < assignRegions.length; i++) {
          result = rmgr.makeAnd(result, assignRegions[i]);
        }
      } else {
        result = assignRegions[assignRegions.length - 1];
        for (int i = assignRegions.length - 2; i >= 0; i--) {
          result = rmgr.makeAnd(result, assignRegions[i]);
        }
      }

      result = rmgr.makeAnd(environment, result);

      return result;
    }
  }

  /** This function returns, if the variable is used in the Expression. */
  private static boolean isUsedInExpression(String function, String varName, CExpression exp) {
    return exp.accept(new VarCExpressionVisitor(function, varName));
  }

  private static boolean isGlobal(CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  private static String buildVarName(String function, String var) {
    if (function == null) {
      return var;
    } else {
      return function + "::" + var;
    }
  }

  /** This function returns a region for a variable.
   * This function does not track any statistics. */
  private Region createPredicateDirectly(@Nullable final String functionName,
      final String varName) {
    return rmgr.createPredicate(buildVarName(functionName, varName));
  }

  /** This function returns a region for a variable with an index.
   * This function does not track any statistics. */
  private Region createPredicateDirectly(@Nullable final String functionName,
      final String varName, final int index) {
    return createPredicateDirectly(functionName, varName + "@" + index);
  }

  /** This function returns regions containing bits of a variable.
   * returns regions for positions of a variable, s --> [s@2, s@1, s@0].
   * If the variable is not tracked by the the precision, Null is returned. */
  private Region[] createPredicates(final String functionName,
      final String varName, final int size, final BDDPrecision precision) {
    if (precision != null && !precision.isTracking(functionName, varName)) {
      return null;
    }
    trackedVars.put(functionName, varName);
    final Region[] newRegions = new Region[size];
    if (size == 1) {
      newRegions[0] = createPredicateDirectly(functionName, varName);
    } else {
      for (int i = size - 1; i >= 0; i--) { // inverse order
        newRegions[i] = createPredicateDirectly(functionName, varName, i);
      }
    }
    return newRegions;
  }

  /** This function returns a region without a variable. */
  private Region removePredicate(final Region region, @Nullable final Region... existing) {
    if (existing == null) {
      return region;
    }
    return rmgr.makeExists(region, existing);
  }

  /** This function creates a mapping of intEqual partitions to a mapping of number to bitvector.
   * This allows to compress big numbers to a small number of bits in the BDD. */
  private void initMappingIntToRegions() {
    for (Partition partition : Sets.difference(
        varClass.getIntEqualPartitions(), varClass.getBooleanPartitions())) {
      int size = partitionToBitsize(partition);
      Map<BigInteger, Region[]> currentMapping = new HashMap<>();
      int i = 0;
      for (BigInteger num : partition.getValues()) {
        currentMapping.put(num, bvmgr.makeNumber(BigInteger.valueOf(i), size));
        i++;
      }
      intToRegionsMap.put(partition, currentMapping);
    }
  }

  /** This function returns the bitsize for vars of a partition.
   * For a boolean var the value is 1.
   *
   * Compression for IntEqual-vars:
   * For N different values there are N+1 possible values for a var
   * (one for each value and one for the whole rest).
   * For N+1 different values we need at least log_2(N+1) bits in the representation. */
  private int partitionToBitsize(Partition partition) {
    if (partition == null) {
      // we know nothing about the partition, so do not track it with BDDCPA
      return 0;
    } else if (varClass.getBooleanPartitions().contains(partition)) {
      return 1;
    } else if (compressIntEqual && varClass.getIntEqualPartitions().contains(partition)) {
      return (int) Math.ceil(Math.log(partition.getValues().size() + 1) / Math.log(2));
    } else {
      return bitsize;
    }
  }

  /** This function returns a representation of a number as bitvector.
   * The bitvector is not the binary representation of the number and
   * can be completely different. */
  private Region[] mapIntToRegions(BigInteger num, Partition partition) {
    return intToRegionsMap.get(partition).get(num);
  }

  /** This Visitor evaluates the visited expression and creates a region for it. */
  private class BDDVectorCExpressionVisitor
      implements CExpressionVisitor<Region[], RuntimeException> {

    private final String functionName;
    private final BDDPrecision precision;
    private final Partition partition;
    private final int size;
    private final boolean compress;

    BDDVectorCExpressionVisitor(final String function,
        final BDDPrecision prec, final Partition partition, final int size) {
      this.functionName = function;
      this.precision = prec;
      this.partition = partition;
      this.size = size;
      this.compress = compressIntEqual &&
          !varClass.getBooleanPartitions().contains(partition) &&
          varClass.getIntEqualPartitions().contains(partition);
    }

    /** This function returns regions containing bits of a variable.
     * The name of the variable is build from functionName and varName.
     * If the precision does not allow to track this variable, NULL is returned. */
    private Region[] makePredicate(CExpression exp, String functionName, BDDPrecision precision) {
      String var = exp.toASTString();
      String function = isGlobal(exp) ? null : functionName;
      return createPredicates(function, var, size, precision);
    }

    @Override
    public Region[] visit(CArraySubscriptExpression exp) {
      return makePredicate(exp, functionName, precision);
    }

    @Override
    public Region[] visit(CBinaryExpression exp) {

      // for numeral values
      Region[] operand1;
      BigInteger val1 = VariableClassification.getNumber(exp.getOperand1());
      if (compress && val1 != null) {
        operand1 = mapIntToRegions(val1, partition);
        assert operand1 != null;
      } else {
        operand1 = exp.getOperand1().accept(this);
      }

      // for numeral values
      BigInteger val2 = VariableClassification.getNumber(exp.getOperand2());
      Region[] operand2;
      if (compress && val2 != null) {
        operand2 = mapIntToRegions(val2, partition);
        assert operand2 != null;
      } else {
        operand2 = exp.getOperand2().accept(this);
      }

      if (operand1 == null || operand2 == null) { return null; }

      Region[] returnValue = null;
      switch (exp.getOperator()) {

      case BINARY_AND:
        returnValue = bvmgr.makeBinaryAnd(operand1, operand2);
        break;

      case BINARY_OR:
        returnValue = bvmgr.makeBinaryOr(operand1, operand2);
        break;

      case EQUALS:
        returnValue = bvmgr.makeLogicalEqual(operand1, operand2);
        break;

      case NOT_EQUALS:
        returnValue = bvmgr.makeNot(bvmgr.makeLogicalEqual(operand1, operand2));
        break;

      case BINARY_XOR:
        returnValue = bvmgr.makeXor(operand1, operand2);
        break;

      case PLUS:
        returnValue = bvmgr.makeAdd(operand1, operand2);
        break;

      case MINUS:
        returnValue = bvmgr.makeSub(operand1, operand2);
        break;

      case LESS_THAN:
        returnValue = bvmgr.makeLess(operand1, operand2);
        break;

      case LESS_EQUAL: // A<=B <--> !(B<A)
        returnValue = bvmgr.makeNot(bvmgr.makeLess(operand2, operand1));
        break;

      case GREATER_THAN: // A>B <--> B<A
        returnValue = bvmgr.makeLess(operand2, operand1);
        break;

      case GREATER_EQUAL:// A>=B <--> !(A<B)
        returnValue = bvmgr.makeNot(bvmgr.makeLess(operand1, operand2));
        break;

      case MULTIPLY:
      case DIVIDE:
      case MODULO:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        // a*b, a<<b, etc --> don't know anything
      }
      return returnValue;
    }

    @Override
    public Region[] visit(CCastExpression exp) {
      // we ignore casts, because Zero is Zero.
      return exp.getOperand().accept(this);
    }

    @Override
    public Region[] visit(CFieldReference exp) {
      return null;
    }

    @Override
    public Region[] visit(CIdExpression exp) {
      return makePredicate(exp, functionName, precision);
    }

    @Override
    public Region[] visit(CCharLiteralExpression exp) {
      return null;
    }

    @Override
    public Region[] visit(CFloatLiteralExpression exp) {
      return null;
    }

    @Override
    public Region[] visit(CIntegerLiteralExpression exp) {
      if (compress) {
        return mapIntToRegions(exp.getValue(), partition);
      } else {
        return bvmgr.makeNumber(exp.getValue(), size);
      }
    }

    @Override
    public Region[] visit(CStringLiteralExpression exp) {
      return null;
    }

    @Override
    public Region[] visit(CTypeIdExpression exp) {
      return null;
    }

    @Override
    public Region[] visit(CTypeIdInitializerExpression exp) {
      return null;
    }

    @Override
    public Region[] visit(CUnaryExpression exp) {

      // for numeral values
      BigInteger val = VariableClassification.getNumber(exp);
      if (compress && val != null) {
        return mapIntToRegions(val, partition);
      }

      // for vars
      Region[] operand = exp.getOperand().accept(this);
      if (operand == null) { return null; }

      Region[] returnValue = null;
      switch (exp.getOperator()) {
      case NOT:
        returnValue = bvmgr.makeNot(operand);
        break;

      case PLUS: // +X == X
        returnValue = operand;
        break;

      case MINUS: // -X == (0-X)
        returnValue = bvmgr.makeSub(bvmgr.makeNumber(BigInteger.ZERO, size), operand);
        break;

      default:
        // *exp --> don't know anything
      }
      return returnValue;
    }
  }

  /** This Visitor evaluates the visited expression and
   * returns iff the given variable is used in it. */
  private static class VarCExpressionVisitor implements CExpressionVisitor<Boolean, RuntimeException> {

    private String functionName;
    private String varName;

    VarCExpressionVisitor(String function, String var) {
      this.functionName = function;
      this.varName = var;
    }

    private Boolean handle(CExpression exp, String functionName) {
      String var = exp.toASTString();
      String function = isGlobal(exp) ? null : functionName;

      if (functionName == null) {
        return function == null && varName.equals(var);
      } else {
        return functionName.equals(function) && varName.equals(var);
      }
    }

    @Override
    public Boolean visit(CArraySubscriptExpression exp) {
      return handle(exp, functionName);
    }

    @Override
    public Boolean visit(CBinaryExpression exp) {
      return exp.getOperand1().accept(this) || exp.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CFieldReference exp) {
      return handle(exp, functionName);
    }

    @Override
    public Boolean visit(CIdExpression exp) {
      return handle(exp, functionName);
    }

    @Override
    public Boolean visit(CCharLiteralExpression exp) {
      return false;
    }

    @Override
    public Boolean visit(CFloatLiteralExpression exp) {
      return false;
    }

    @Override
    public Boolean visit(CIntegerLiteralExpression exp) {
      return false;
    }

    @Override
    public Boolean visit(CStringLiteralExpression exp) {
      return false;
    }

    @Override
    public Boolean visit(CTypeIdExpression exp) {
      return false;
    }

    @Override
    public Boolean visit(CTypeIdInitializerExpression exp) {
      return false;
    }

    @Override
    public Boolean visit(CUnaryExpression exp) {
      return exp.getOperand().accept(this);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state, List<AbstractState> states, CFAEdge cfaEdge,
      Precision precision) {
    // do nothing
    return null;
  }

  /** THis function writes some information about tracked variables, number of partitions,... */
  void printStatistics(final PrintStream out) {
    final Set<Partition> booleans = varClass.getBooleanPartitions();
    final Set<Partition> intEquals = varClass.getIntEqualPartitions();
    final Set<Partition> intAdds = varClass.getIntAddPartitions();

    int numOfBooleans = varClass.getBooleanVars().size();

    int numOfIntEquals = 0;
    final Set<Partition> realIntEquals= Sets.difference(intEquals, booleans);
    for (Partition p : realIntEquals) {
      numOfIntEquals += p.getVars().size();
    }

    int numOfIntAdds = 0;
    final Set<Partition> realIntAdds = Sets.difference(intAdds, Sets.union(booleans, intEquals));
    for (Partition p : realIntAdds) {
      numOfIntAdds += p.getVars().size();
    }

    Multimap<String, String> trackedBooleans = LinkedHashMultimap.create();
    Multimap<String, String> trackedIntEquals = LinkedHashMultimap.create();
    Multimap<String, String> trackedIntAdds = LinkedHashMultimap.create();
    for (Entry<String, String> var : trackedVars.entries()) {
      if (varClass.getBooleanVars().containsEntry(var.getKey(), var.getValue())) {
        trackedBooleans.put(var.getKey(), var.getValue());
      } else if (varClass.getIntEqualVars().containsEntry(var.getKey(), var.getValue())) {
        trackedIntEquals.put(var.getKey(), var.getValue());
      } else if (varClass.getIntAddVars().containsEntry(var.getKey(), var.getValue())) {
        trackedIntAdds.put(var.getKey(), var.getValue());
      } else {
        // ignore other vars, they are either function_return_vars or tmp_vars
      }
    }

    if (dumpfile != null) { // option -noout
      try (Writer w = Files.openOutputFile(dumpfile)) {
        w.append("Boolean\n\n");
        w.append(trackedBooleans.toString());
        w.append("\n\nIntEqual\n\n");
        w.append(trackedIntEquals.toString());
        w.append("\n\nIntAdd\n\n");
        w.append(trackedIntAdds.toString());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write tracked variables for BDDCPA to file");
      }
    }

    out.println(String.format("Number of boolean vars:           %d (of %d)", trackedBooleans.size(), numOfBooleans));
    out.println(String.format("Number of intEqual vars:          %d (of %d)", trackedIntEquals.size(), numOfIntEquals));
    out.println(String.format("Number of intAdd vars:            %d (of %d)", trackedIntAdds.size(), numOfIntAdds));
    out.println(String.format("Number of all vars:               %d (of %d)",
        trackedBooleans.size() + trackedIntEquals.size() + trackedIntAdds.size(), varClass.getAllVars().size()));
    out.println("Number of boolean partitions:     " + booleans.size());
    out.println("Number of intEqual partitions:    " + realIntEquals.size());
    out.println("Number of intAdd partitions:      " + realIntAdds.size());
    out.println("Number of all partitions:         " + varClass.getPartitions().size());
    rmgr.printStatistics(out);
  }
}
