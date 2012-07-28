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

import static org.sosy_lab.cpachecker.cpa.bdd.BDDTransferRelation.*;
import static org.sosy_lab.cpachecker.util.VariableClassification.FUNCTION_RETURN_VARIABLE;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.CInitializer;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
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
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.Multimap;

/** This Transfer Relation tracks variables and handles them as bitvectors. */
@Options(prefix = "cpa.bdd.vector")
public class BDDVectorTransferRelation implements TransferRelation {

  @Option(description = "initialize all variables to 0 when they are declared")
  private boolean initAllVars = false;

  @Option(description = "declare first bit of all vars, then second bit,...")
  private boolean initBitwise = true;

  @Option(description = "declare the bits of a var from 0 to N or from N to 0")
  private boolean initBitsIncreasing = true;

  @Option(description = "adding a new value to the state can be done from 0 to N or from N to 0")
  private boolean addIncreasing = false;

  @Option(description = "declare vars ordered in partitions")
  private boolean initPartitions = true;

  private final VariableClassification varClass;
  private final Map<Multimap<String, String>, String> partitionToTmpVar =
      new HashMap<Multimap<String, String>, String>();

  private final BitvectorManager bvmgr;
  private final NamedRegionManager rmgr;

  /** for statistics */
  private int createdPredicates;
  private int deletedPredicates;

  /** The Constructor of BDDVectorTransferRelation sets the NamedRegionManager
   * and the BitVectorManager. Both are used to build and manipulate BDDs,
   * that represent the regions. */
  public BDDVectorTransferRelation(NamedRegionManager manager,
      Configuration config, CFA cfa, BDDVectorPrecision precision)
      throws InvalidConfigurationException {
    config.inject(this);

    this.bvmgr = new BitvectorManager(config);
    this.rmgr = manager;

    assert cfa.getVarClassification().isPresent();
    this.varClass = cfa.getVarClassification().get();
    initVars(precision);
  }

  /** The BDDRegionManager orders the variables as they are declared
   *  (later vars are deeper in the BDD).
   *  This function declares those vars in the beginning of the analysis,
   *  so that we can choose between some orders. */
  private void initVars(BDDVectorPrecision precision) {
    int size = bvmgr.getBitSize();

    if (initPartitions) {
      for (Multimap<String, String> vars : varClass.getPartitions()) {
        createPredicates(size, vars, precision);
      }

    } else {
      Multimap<String, String> vars = varClass.getAllVars();
      createPredicates(size, vars, precision);
    }
  }

  /** This function declares variables for a given collection of vars.
   * The flag 'bitwise' chooses between initialing each var after each other
   * or bitwise overlapped (bit1 of all vars, then bit2 of all vars, etc). */
  private void createPredicates(int size, Multimap<String, String> vars, BDDVectorPrecision precision) {

    // add a temporary variable for each partition
    // (or for allVars, then this action is senseless, but simplifies code)
    String tmpVar = TMP_VARIABLE + "_" + partitionToTmpVar.size();
    partitionToTmpVar.put(vars, tmpVar);

    if (initBitwise) {
      // [a2, b2, c2, a1, b1, c1, a0, b0, c0]
      for (int i = 0; i < size; i++) {
        int index = initBitsIncreasing ? i : (size - i - 1);
        for (Entry<String, String> entry : vars.entries()) {
          if (precision.isTracking(entry.getKey(), entry.getValue())) {
            rmgr.createPredicate(buildVarName(entry.getKey(), entry.getValue()) + "@" + index);
          }
        }
        rmgr.createPredicate(tmpVar + "@" + index);
      }

    } else {
      // [a2, a1, a0, b2, b1, b0, c2, c1, c0]
      for (Entry<String, String> entry : vars.entries()) { // different loop order!
        for (int i = 0; i < size; i++) {
          if (precision.isTracking(entry.getKey(), entry.getValue())) {
            int index = initBitsIncreasing ? i : (size - i - 1);
            rmgr.createPredicate(buildVarName(entry.getKey(), entry.getValue()) + "@" + index);
          }
        }
        for (int i = 0; i < size; i++) {
          int index = initBitsIncreasing ? i : (size - i - 1);
          rmgr.createPredicate(tmpVar + "@" + index);
        }
      }
    }
  }

  @Override
  public Collection<BDDState> getAbstractSuccessors(
      AbstractState abstractState, Precision prec, CFAEdge cfaEdge)
      throws CPATransferException {
    BDDState state = (BDDState) abstractState;
    BDDVectorPrecision precision = (BDDVectorPrecision) prec;

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

    case StatementEdge:
      successor = handleStatementEdge(state, (CStatementEdge) cfaEdge, precision);
      break;

    case DeclarationEdge:
      successor = handleDeclarationEdge(state, (CDeclarationEdge) cfaEdge, precision);
      break;

    case MultiEdge:
      successor = state;
      Collection<BDDState> c = null;
      for (CFAEdge innerEdge : (MultiEdge) cfaEdge) {
        c = getAbstractSuccessors(successor, precision, innerEdge);
        if (c.isEmpty()) {
          successor = state; //TODO really correct??
        } else if (c.size() == 1) {
          successor = c.toArray(new BDDState[1])[0];
        } else {
          throw new AssertionError("only size 0 or 1 allowed");
        }
      }
      break;

    case FunctionCallEdge:
      successor = handleFunctionCallEdge(state, (CFunctionCallEdge) cfaEdge, precision);
      break;

    case FunctionReturnEdge:
      successor = handleFunctionReturnEdge(state, (CFunctionReturnEdge) cfaEdge, precision);
      break;

    case ReturnStatementEdge:
      successor = handleReturnStatementEdge(state, (CReturnStatementEdge) cfaEdge, precision);
      break;

    case BlankEdge:
    case CallToReturnEdge:
    default:
      successor = state;
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      assert !successor.getRegion().isFalse();
      return Collections.singleton(successor);
    }
  }

  /** This function handles statements like "a = 0;" and "b = !a;".
   * A region is build for the right side of the statement.
   * Then this region is assigned to the variable at the left side.
   * This equality is added to the BDDstate to get the next state. */
  private BDDState handleStatementEdge(BDDState state, CStatementEdge cfaEdge,
      BDDVectorPrecision precision) throws UnrecognizedCCodeException {
    CStatement statement = cfaEdge.getStatement();
    if (!(statement instanceof CAssignment)) { return state; }
    CAssignment assignment = (CAssignment) statement;

    CExpression lhs = assignment.getLeftHandSide();
    BDDState result = state;
    if (lhs instanceof CIdExpression || lhs instanceof CFieldReference
        || lhs instanceof CArraySubscriptExpression) {

      String function = isGlobal(lhs) ? null : state.getFunctionName();
      String varName = lhs.toASTString();
      if (precision.isTracking(function, varName)) {

        Region newRegion = state.getRegion();
        Region[] var = createPredicate(buildVarName(function, varName));
        CRightHandSide rhs = assignment.getRightHandSide();

        if (rhs instanceof CExpression) {
          CExpression exp = (CExpression) rhs;
          if (isUsedInExpression(function, varName, exp)) {
            // make tmp for assignment,
            // this is done to handle assignments like "a = !a;" as "tmp = !a; a = tmp;"
            String tmpVarName;
            if (initPartitions) {
              tmpVarName = partitionToTmpVar.get(varClass.getPartitionForVar(function, varName));
            } else {
              tmpVarName = TMP_VARIABLE;
            }
            Region[] tmp = createPredicate(tmpVarName);

            // make region for RIGHT SIDE and build equality of var and region
            BDDCExpressionVisitor ev = new BDDCExpressionVisitor(state, precision);
            Region[] regRHS = exp.accept(ev);
            newRegion = addEquality(tmp, regRHS, newRegion);

            // delete var, make tmp equal to (new) var, then delete tmp
            newRegion = removePredicate(newRegion, var);
            newRegion = addEquality(var, tmp, newRegion);
            newRegion = removePredicate(newRegion, tmp);

          } else {
            newRegion = removePredicate(newRegion, var);

            // make region for RIGHT SIDE and build equality of var and region
            BDDCExpressionVisitor ev = new BDDCExpressionVisitor(state, precision);
            Region[] regRHS = ((CExpression) rhs).accept(ev);
            newRegion = addEquality(var, regRHS, newRegion);
          }

        } else {
          // else if (rhs instanceof CFunctionCallExpression) {
          // call of external function: we know nothing, so we do nothing
          // TODO can we assume, that malloc returns something !=0?
          // are there some "save functions"?
          newRegion = removePredicate(newRegion, var);
        }

        result = new BDDState(rmgr, state.getFunctionCallState(), newRegion,
            state.getVars(), cfaEdge.getPredecessor().getFunctionName());
      }
    }

    assert !result.getRegion().isFalse();
    return result;
  }

  /** This function handles declarations like "int a = 0;" and "int b = !a;".
   * Regions are build for all Bits of the right side of the declaration,
   * if it is not null. Then these regions are assigned to the regions of
   * variable (bitvector) at the left side.
   * These equalities are added to the BDDstate to get the next state. */
  private BDDState handleDeclarationEdge(BDDState state, CDeclarationEdge cfaEdge,
      BDDVectorPrecision precision) throws UnrecognizedCCodeException {

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
      String function = vdecl.isGlobal() ? null : state.getFunctionName();
      String varName = vdecl.getName();
      String scopedVarName = buildVarName(function, varName);
      if (precision.isTracking(function, varName)) {
        Region[] var = createPredicate(scopedVarName);
        Region newRegion = removePredicate(state.getRegion(), var);

        // track vars, so we can delete them after returning from a function,
        // see handleFunctionReturnEdge(...) for detail.
        if (!vdecl.isGlobal()) {
          state.getVars().add(scopedVarName);
        }

        // initializer on RIGHT SIDE available, make region for it
        if (init != null) {
          BDDCExpressionVisitor ev = new BDDCExpressionVisitor(state, precision);
          Region[] regRHS = init.accept(ev);
          newRegion = addEquality(var, regRHS, newRegion);
          return new BDDState(rmgr, state.getFunctionCallState(), newRegion,
              state.getVars(), cfaEdge.getPredecessor().getFunctionName());
        }
      }
    }

    return state; // if we know nothing, we return the old state
  }

  /** This function handles functioncalls like "f(x)", that calls "f(int a)".
   * Therefore each arg ("x") is transformed into a region and assigned
   * to a param ("int a") of the function. The equalities of
   * all arg-param-pairs are added to the BDDstate to get the next state. */
  private BDDState handleFunctionCallEdge(BDDState state, CFunctionCallEdge cfaEdge,
      BDDVectorPrecision precision) throws UnrecognizedCCodeException {

    Region newRegion = state.getRegion();
    Set<String> newVars = new LinkedHashSet<String>();

    // overtake arguments from last functioncall into function,
    // get args from functioncall and make them equal with params from functionstart
    List<CExpression> args = cfaEdge.getArguments();
    List<CParameterDeclaration> params = cfaEdge.getSuccessor().getFunctionParameters();
    String innerFunctionName = cfaEdge.getSuccessor().getFunctionName();
    assert args.size() == params.size();

    for (int i = 0; i < args.size(); i++) {

      // make variable (predicate) for param, this variable is not global (->false)
      String varName = params.get(i).getName();
      String scopedVarName = buildVarName(innerFunctionName, varName);
      assert !newVars.contains(scopedVarName) : "variable used twice as param: " + scopedVarName;

      // make region for arg and build equality of var and arg
      if (precision.isTracking(innerFunctionName, varName)) {
        newVars.add(scopedVarName);
        Region[] var = createPredicate(scopedVarName);
        BDDCExpressionVisitor ev = new BDDCExpressionVisitor(state, precision);
        Region[] arg = args.get(i).accept(ev);
        newRegion = addEquality(var, arg, newRegion);
      }
    }

    return new BDDState(rmgr, state, newRegion, newVars, innerFunctionName);
  }

  /** This function handles functionReturns like "y=f(x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * left side ("y") is added to the new state.
   * Each variable from inside the function is removed from the BDDstate. */
  private BDDState handleFunctionReturnEdge(BDDState state, CFunctionReturnEdge cfaEdge,
      BDDVectorPrecision precision) {
    Region newRegion = state.getRegion();

    // delete variables from returning function,
    // this results in a smaller BDD and allows to call a function twice.
    for (String varName : state.getVars()) {
      newRegion = removePredicate(newRegion, createPredicate(varName));
    }

    // set result of function equal to variable on left side
    CFunctionSummaryEdge fnkCall = cfaEdge.getSummaryEdge();
    CStatement call = fnkCall.getExpression().asStatement();

    // make region (predicate) for RIGHT SIDE
    Region[] retVar = createPredicate(buildVarName(state.getFunctionName(), FUNCTION_RETURN_VARIABLE));

    // handle assignments like "y = f(x);"
    if (call instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) call;
      CExpression lhs = cAssignment.getLeftHandSide();

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      BDDState functionCall = state.getFunctionCallState();
      String function = isGlobal(lhs) ? null : functionCall.getFunctionName();
      String varName = lhs.toASTString();
      if (precision.isTracking(function, varName)) {
        Region[] var = createPredicate(buildVarName(function, varName));
        newRegion = removePredicate(newRegion, var);
        newRegion = addEquality(var, retVar, newRegion);
      }
    }

    // LAST ACTION: delete varname of right side
    newRegion = removePredicate(newRegion, retVar);

    return new BDDState(rmgr, state.getFunctionCallState().getFunctionCallState(),
        newRegion, state.getFunctionCallState().getVars(),
        cfaEdge.getSuccessor().getFunctionName());
  }

  /** This function handles functionStatements like "return (x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * evaluated right side ("x") is added to the new state. */
  private BDDState handleReturnStatementEdge(BDDState state, CReturnStatementEdge cfaEdge,
      BDDVectorPrecision precision) throws UnrecognizedCCodeException {

    // make variable (predicate) for returnStatement,
    // delete variable, if it was used before, this is done with an existential operator
    String scopedFuncName = buildVarName(state.getFunctionName(), FUNCTION_RETURN_VARIABLE);
    Region[] retvar = createPredicate(scopedFuncName);

    assert state.getRegion().equals(removePredicate(state.getRegion(), retvar)) : scopedFuncName
        + " was used twice in one trace??";

    // make region for RIGHT SIDE, this is the 'x' from 'return (x);
    CRightHandSide rhs = cfaEdge.getExpression();
    if (rhs instanceof CExpression) {
      BDDCExpressionVisitor ev = new BDDCExpressionVisitor(state, precision);
      Region[] regRHS = ((CExpression) rhs).accept(ev);
      Region newRegion = addEquality(retvar, regRHS, state.getRegion());
      return new BDDState(rmgr, state.getFunctionCallState(), newRegion,
          state.getVars(), cfaEdge.getPredecessor().getFunctionName());
    }
    return state;
  }

  /** This function handles assumptions like "if(a==b)" and "if(a!=0)".
   * A region is build for the assumption.
   * This region is added to the BDDstate to get the next state.
   * If the next state is False, the assumption is not fulfilled.
   * In this case NULL is returned. */
  private BDDState handleAssumption(BDDState state, CAssumeEdge cfaEdge,
      BDDVectorPrecision precision) throws UnrecognizedCCodeException {

    CExpression expression = cfaEdge.getExpression();
    BDDCExpressionVisitor ev = new BDDCExpressionVisitor(state, precision);
    Region[] operand = expression.accept(ev);

    if (operand == null) { // assumption cannot be evaluated
      return state;

    } else {

      Region evaluated = bvmgr.makeOr(operand);

      if (!cfaEdge.getTruthAssumption()) { // if false-branch
        evaluated = rmgr.makeNot(evaluated);
      }

      // get information from region into evaluated region
      Region newRegion = rmgr.makeAnd(state.getRegion(), evaluated);
      if (newRegion.isFalse()) { // assumption is not fulfilled / not possible
        return null;
      } else {
        return new BDDState(rmgr, state.getFunctionCallState(), newRegion,
            state.getVars(), cfaEdge.getPredecessor().getFunctionName());
      }
    }
  }

  /** This function builds the equality of left and right side and adds it to the environment.
   * If left or right side is null, the environment is returned unchanged. */
  private Region addEquality(Region[] leftSide, Region[] rightSide, Region environment) {
    if (leftSide == null || rightSide == null) {
      return environment;
    } else {
      final Region[] assignRegions = bvmgr.makeBinaryEqual(leftSide, rightSide);

      //      // TODO maybe faster??
      //      Region[] tmp = assignRegions;
      //      while (tmp.length > 1) {
      //        Region[] tmp2 = new Region[tmp.length / 2];
      //        for (int i = 0; i < tmp2.length; i++) {
      //          tmp2[i] = rmgr.makeAnd(tmp[2 * i], tmp[2 * i + 1]);
      //        }
      //        if (tmp.length != tmp2.length * 2) {
      //          assert tmp.length == tmp2.length * 2 + 1;
      //          tmp2[tmp2.length - 1] = rmgr.makeAnd(tmp2[tmp2.length - 1], tmp[tmp.length - 1]);
      //        }
      //        tmp = tmp2;
      //      }
      //
      //      assert tmp.length == 1;
      //      Region result1 = rmgr.makeAnd(environment, tmp[0]);

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

  /** This function returns regions containing bits of a variable.
   * returns regions for positions of a variable, s --> [s@0, s@1, s@2] */
  private Region[] createPredicate(String s) {
    int size = bvmgr.getBitSize();
    Region[] newRegions = new Region[size];
    for (int i = size - 1; i >= 0; i--) { // inverse order
      newRegions[i] = rmgr.createPredicate(s + "@" + i);
    }
    createdPredicates += size;
    return newRegions;
  }

  /** This function returns a region without a variable. */
  private Region removePredicate(Region region, Region... existing) {
    region = rmgr.makeExists(region, existing);
    return region;
  }

  /** This Visitor evaluates the visited expression and creates a region for it. */
  private class BDDCExpressionVisitor
      implements CExpressionVisitor<Region[], UnrecognizedCCodeException> {

    private String functionName;
    private BDDVectorPrecision precision;

    BDDCExpressionVisitor(BDDState state, BDDVectorPrecision prec) {
      this.functionName = state.getFunctionName();
      this.precision = prec;
    }

    /** This function returns regions containing bits of a variable.
     * The name of the variable is build from functionName and varName.
     * If the precision does not allow to track this variable, NULL is returned. */
    private Region[] makePredicate(CExpression exp, String functionName, BDDVectorPrecision precision) {
      String var = exp.toASTString();
      String function = isGlobal(exp) ? null : functionName;

      if (precision.isTracking(function, var)) {
        return createPredicate(buildVarName(function, var));
      } else {
        return null;
      }
    }

    @Override
    public Region[] visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {
      return makePredicate(exp, functionName, precision);
    }

    @Override
    public Region[] visit(CBinaryExpression exp) throws UnrecognizedCCodeException {
      Region[] operand1 = exp.getOperand1().accept(this);
      Region[] operand2 = exp.getOperand2().accept(this);

      if (operand1 == null || operand2 == null) { return null; }

      Region[] returnValue = null;
      switch (exp.getOperator()) {

      case BINARY_AND:
        returnValue = bvmgr.makeBinaryAnd(operand1, operand2);
        break;

      case LOGICAL_AND:
        returnValue = bvmgr.makeLogicalAnd(operand1, operand2);
        break;

      case BINARY_OR:
        returnValue = bvmgr.makeBinaryOr(operand1, operand2);
        break;

      case LOGICAL_OR:
        returnValue = bvmgr.makeLogicalOr(operand1, operand2);
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
    public Region[] visit(CCastExpression exp) throws UnrecognizedCCodeException {
      // we ignore casts, because Zero is Zero.
      return exp.getOperand().accept(this);
    }

    @Override
    public Region[] visit(CFieldReference exp) throws UnrecognizedCCodeException {
      return makePredicate(exp, functionName, precision);
    }

    @Override
    public Region[] visit(CIdExpression exp) throws UnrecognizedCCodeException {
      return makePredicate(exp, functionName, precision);
    }

    @Override
    public Region[] visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region[] visit(CFloatLiteralExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region[] visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      return bvmgr.makeNumber(exp.getValue());
    }

    @Override
    public Region[] visit(CStringLiteralExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region[] visit(CTypeIdExpression exp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Region[] visit(CUnaryExpression exp) throws UnrecognizedCCodeException {
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
        returnValue = bvmgr.makeSub(bvmgr.makeNumber(BigInteger.ZERO), operand);
        break;

      default:
        // *exp --> don't know anything
      }
      return returnValue;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state, List<AbstractState> states, CFAEdge cfaEdge,
      Precision precision) {
    // do nothing
    return null;
  }

  @Override
  public String toString() {
    return "Number of created predicates: " + createdPredicates +
        "\nNumber of deleted predicates: " + deletedPredicates +
        "\n" + rmgr.getStatistics();
  }
}
