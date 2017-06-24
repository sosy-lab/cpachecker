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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** This Transfer Relation tracks variables and handles them as bitvectors. */
public class BDDTransferRelation extends ForwardingTransferRelation<BDDState, BDDState, VariableTrackingPrecision> {

  private final int bitsize;
  private final boolean compressIntEqual;
  private final VariableClassification varClass;
  private final BitvectorManager bvmgr;
  private final NamedRegionManager rmgr;
  private final PredicateManager predmgr;
  private final MachineModel machineModel;

  /**
   * The Constructor of BDDVectorTransferRelation sets the NamedRegionManager and the
   * BitVectorManager. Both are used to build and manipulate BDDs, that represent the regions.
   */
  public BDDTransferRelation(
      NamedRegionManager manager,
      BitvectorManager bvmgr,
      PredicateManager pPredmgr,
      CFA cfa,
      int pBitsize,
      boolean pCompressIntEqual) {
    this.machineModel = cfa.getMachineModel();
    this.rmgr = manager;
    this.bvmgr = bvmgr;
    this.predmgr = pPredmgr;
    bitsize = pBitsize;
    compressIntEqual = pCompressIntEqual;
    assert cfa.getVarClassification().isPresent();
    this.varClass = cfa.getVarClassification().get();
  }

  @Override
  protected Collection<BDDState> preCheck(BDDState state, VariableTrackingPrecision precision) {
    // no variables should be tracked
    if (precision.isEmpty()) {
      return Collections.singleton(state);
    }
    // the path is not fulfilled
    if (state.getRegion().isFalse()) {
      return Collections.emptyList();
    }
    return null;
  }

  /** This function handles statements like "a = 0;" and "b = !a;" and
   * calls of external functions. */
  @Override
  protected BDDState handleStatementEdge(final CStatementEdge cfaEdge, final CStatement statement)
      throws UnsupportedCCodeException {

    BDDState result = state;

    // normal assignment, "a = ..."
    if (statement instanceof CAssignment) {
      result = handleAssignment((CAssignment) statement, cfaEdge.getSuccessor(), cfaEdge);

      // call of external function, "scanf(...)" without assignment
      // internal functioncalls are handled as FunctionCallEdges
    } else if (statement instanceof CFunctionCallStatement) {
      result = handleExternalFunctionCall(result, cfaEdge.getSuccessor(),
              ((CFunctionCallStatement) statement).getFunctionCallExpression().getParameterExpressions());
    }

    assert !result.getRegion().isFalse();
    return result;
  }

  /** This function handles statements like "a = 0;" and "b = !a;".
   * A region is build for the right side of the statement.
   * Then this region is assigned to the variable at the left side.
   * This equality is added to the BDDstate to get the next state. */
  private BDDState handleAssignment(CAssignment assignment, CFANode successor, CFAEdge edge)
      throws UnsupportedCCodeException {
    CExpression lhs = assignment.getLeftHandSide();

    if (!(lhs instanceof CIdExpression)) {
      return state;
    }

    final CType targetType = lhs.getExpressionType();
    final String varName = ((CIdExpression) lhs).getDeclaration().getQualifiedName();

    // next line is a shortcut, not necessary
    if (!precision.isTracking(MemoryLocation.valueOf(varName), targetType, successor)) {
      return state;
    }

    BDDState newState = state;
    CRightHandSide rhs = assignment.getRightHandSide();
    if (rhs instanceof CExpression) {
      final CExpression exp = (CExpression) rhs;
      final Partition partition = varClass.getPartitionForEdge(edge);

      if (isUsedInExpression(varName, exp)) {
        // make tmp for assignment,
        // this is done to handle assignments like "a = !a;" as "tmp = !a; a = tmp;"
        String tmpVarName = predmgr.getTmpVariableForVars(partition.getVars());
        final Region[] tmp = predmgr.createPredicateWithoutPrecisionCheck(tmpVarName, getBitsize(partition, targetType));

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS = evaluateVectorExpression(partition, exp, targetType, successor);
        newState = newState.addAssignment(tmp, regRHS);

        // delete var, make tmp equal to (new) var, then delete tmp
        final Region[] var = predmgr.createPredicate(scopeVar(lhs), targetType, successor, getBitsize(partition, targetType), precision);
        newState = newState.forget(var);
        newState = newState.addAssignment(var, tmp);
        newState = newState.forget(tmp);

      } else {
        final Region[] var = predmgr.createPredicate(scopeVar(lhs), targetType, successor, getBitsize(partition, targetType), precision);
        newState = newState.forget(var);

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS = evaluateVectorExpression(partition, (CExpression) rhs, targetType, successor);
        newState = newState.addAssignment(var, regRHS);
      }
      return newState;

    } else if (rhs instanceof CFunctionCallExpression) {
      // handle params of functionCall, maybe there is a sideeffect
      newState = handleExternalFunctionCall(newState, successor,
              ((CFunctionCallExpression) rhs).getParameterExpressions());

      // call of external function: we know nothing, so we delete the value of the var
      // TODO can we assume, that malloc returns something !=0?
      // are there some "save functions"?

      final Region[] var = predmgr.createPredicate(scopeVar(lhs), targetType, successor, bitsize, precision); // is default bitsize enough?
      newState = newState.forget(var);

      return newState;

    } else {
      throw new AssertionError("unhandled assignment: " + edge.getRawStatement());
    }
  }

  /** This function deletes all vars, that could be modified
   * through a side-effect of the (external) functionCall. */
  private BDDState handleExternalFunctionCall(BDDState currentState, CFANode successor, final List<CExpression> params) {

    for (final CExpression param : params) {

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      if (param instanceof CUnaryExpression &&
              UnaryOperator.AMPER == ((CUnaryExpression) param).getOperator() &&
              ((CUnaryExpression) param).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) param).getOperand();
        final Region[] var = predmgr.createPredicate(scopeVar(id), id.getExpressionType(), successor, bitsize, precision); // is default bitsize enough?
        currentState = currentState.forget(var);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO: can we do something here?
      }
    }
    return state;
  }

  /** This function handles declarations like "int a = 0;" and "int b = !a;".
   * Regions are build for all Bits of the right side of the declaration,
   * if it is not null. Then these regions are assigned to the regions of
   * variable (bitvector) at the left side.
   * These equalities are added to the BDDstate to get the next state. */
  @Override
  protected BDDState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws UnsupportedCCodeException {

    if (decl instanceof CVariableDeclaration) {
      CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      if (vdecl.getType().isIncomplete()) {
        // Variables of such types cannot store values, only their address can be taken.
        // We can ignore them.
        return state;
      }

      CInitializer initializer = vdecl.getInitializer();
      CExpression init = null;
      if (initializer instanceof CInitializerExpression) {
        init = ((CInitializerExpression) initializer).getExpression();
      }

      // make variable (predicate) for LEFT SIDE of declaration,
      // delete variable, if it was initialized before i.e. in another block, with an existential operator
      Partition partition = varClass.getPartitionForEdge(cfaEdge);
      Region[] var = predmgr.createPredicate(vdecl.getQualifiedName(), vdecl.getType(), cfaEdge.getSuccessor(), getBitsize(partition, vdecl.getType()), precision);
      BDDState newState = state.forget(var);

      // initializer on RIGHT SIDE available, make region for it
      if (init != null) {
        final Region[] rhs = evaluateVectorExpression(partition, init, vdecl.getType(), cfaEdge.getSuccessor());
        newState = newState.addAssignment(var, rhs);
        return newState;
      }
    }

    return state; // if we know nothing, we return the old state
  }

  /** This function handles functioncalls like "f(x)", that calls "f(int a)".
   * Therefore each arg ("x") is transformed into a region and assigned
   * to a param ("int a") of the function. The equalities of
   * all arg-param-pairs are added to the BDDstate to get the next state. */
  @Override
  protected BDDState handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> args,
      List<CParameterDeclaration> params,
      String calledFunction)
      throws UnsupportedCCodeException {
    BDDState newState = state;

    // var_args cannot be handled: func(int x, ...) --> we only handle the first n parameters
    assert args.size() >= params.size();

    for (int i = 0; i < params.size(); i++) {

      // make variable (predicate) for param, this variable is not global
      final String varName = params.get(i).getQualifiedName();
      final CType targetType = params.get(i).getType();
      final Partition partition = varClass.getPartitionForParameterOfEdge(cfaEdge, i);
      final Region[] var = predmgr.createPredicate(varName, targetType, cfaEdge.getSuccessor(), getBitsize(partition, targetType), precision);
      final Region[] arg = evaluateVectorExpression(partition, args.get(i), targetType, cfaEdge.getSuccessor());
      newState = newState.addAssignment(var, arg);
    }

    return newState;
  }

  /** This function handles functionReturns like "y=f(x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * left side ("y") is added to the new state.
   * Each variable from inside the function is removed from the BDDstate. */
  @Override
  protected BDDState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
                                              CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String outerFunctionName) {
    BDDState newState = state;

    // set result of function equal to variable on left side
    final Partition partition = varClass.getPartitionForEdge(cfaEdge);

    // handle assignments like "y = f(x);"
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      final String returnVar = fnkCall.getFunctionEntry().getReturnVariable().get().getQualifiedName();
      CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) summaryExpr;
      CExpression lhs = cAssignment.getLeftHandSide();
      final int size = getBitsize(partition, lhs.getExpressionType());

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] var = predmgr.createPredicate(scopeVar(lhs), lhs.getExpressionType(), cfaEdge.getSuccessor(), size, precision);
      newState = newState.forget(var);

      // make region (predicate) for RIGHT SIDE
      final Region[] retVar = predmgr.createPredicate(returnVar, summaryExpr.getFunctionCallExpression().getExpressionType(), cfaEdge.getSuccessor(),  size, precision);
      newState = newState.addAssignment(var, retVar);

      // remove returnVar from state,
      // all other function-variables were removed earlier (see handleReturnStatementEdge()).
      // --> now the state does not contain any variable from scope of called function.
      if  (predmgr.getTrackedVars().containsKey(returnVar)) {
        newState = newState.forget(predmgr.createPredicateWithoutPrecisionCheck(
                returnVar, predmgr.getTrackedVars().get(returnVar)));
      }

    } else {
      assert summaryExpr instanceof CFunctionCallStatement; // no assignment, nothing to do
    }

    return newState;
  }

  /** This function handles functionStatements like "return (x)".
   * The equality of the returnValue (FUNCTION_RETURN_VARIABLE) and the
   * evaluated right side ("x") is added to the new state. */
  @Override
  protected BDDState handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws UnsupportedCCodeException {
    BDDState newState = state;
    String returnVar = "";

    if (cfaEdge.getExpression().isPresent()) {
      returnVar = ((CIdExpression)cfaEdge.asAssignment().get().getLeftHandSide()).getDeclaration().getQualifiedName();
      final Partition partition = varClass.getPartitionForEdge(cfaEdge);
      final CType functionReturnType = ((CFunctionDeclaration) cfaEdge.getSuccessor().getEntryNode()
              .getFunctionDefinition()).getType().getReturnType();

      // make region for RIGHT SIDE, this is the 'x' from 'return (x);
      final Region[] regRHS = evaluateVectorExpression(partition, cfaEdge.getExpression().get(), functionReturnType, cfaEdge.getSuccessor());

      // make variable (predicate) for returnStatement,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] retvar = predmgr.createPredicate(returnVar, functionReturnType, cfaEdge.getSuccessor(),
              getBitsize(partition, functionReturnType), precision);
      newState = newState.forget(retvar);
      newState = newState.addAssignment(retvar, regRHS);
    }

    // delete variables from returning function,
    // we do not need them after this location, because the next edge is the functionReturnEdge.
    // this results in a smaller BDD and allows to call a function twice.
    for (String var : predmgr.getTrackedVars().keySet()) {
      if (isLocalVariableForFunction(var, functionName) && !returnVar.equals(var)) {
        newState = newState.forget(predmgr.createPredicateWithoutPrecisionCheck(var, predmgr.getTrackedVars().get(var)));
      }
    }

    return newState;
  }

  @Override
  protected BDDState handleBlankEdge(BlankEdge cfaEdge) {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      assert "default return".equals(cfaEdge.getDescription())
              || "skipped unnecessary edges".equals(cfaEdge.getDescription());

      // delete variables from returning function,
      // we do not need them after this location, because the next edge is the functionReturnEdge.
      // this results in a smaller BDD and allows to call a function twice.
      BDDState newState = state;
      for (String var : predmgr.getTrackedVars().keySet()) {
        if (isLocalVariableForFunction(var, functionName)) {
          newState = newState.forget(predmgr.createPredicateWithoutPrecisionCheck(var, predmgr.getTrackedVars().get(var)));
        }
      }
      return newState;
    }

    return state;
  }

  /** This function handles assumptions like "if(a==b)" and "if(a!=0)".
   * A region is build for the assumption.
   * This region is added to the BDDstate to get the next state.
   * If the next state is False, the assumption is not fulfilled.
   * In this case NULL is returned. */
  @Override
  protected BDDState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws UnsupportedCCodeException {

    Partition partition = varClass.getPartitionForEdge(cfaEdge);
    final Region[] operand = evaluateVectorExpression(partition, expression, CNumericTypes.INT, cfaEdge.getSuccessor());
    if (operand == null) {
      return state;
    } // assumption cannot be evaluated
    Region evaluated = bvmgr.makeOr(operand);

    if (!truthAssumption) { // if false-branch
      evaluated = rmgr.makeNot(evaluated);
    }

    // get information from region into evaluated region
    Region newRegion = rmgr.makeAnd(state.getRegion(), evaluated);
    if (newRegion.isFalse()) { // assumption is not fulfilled / not possible
      return null;
    } else {
      return new BDDState(rmgr, bvmgr, newRegion);
    }
  }

  /** This function returns a bitvector, that represents the expression.
   * The partition chooses the compression of the bitvector. */
  private @Nullable Region[] evaluateVectorExpression(
      final Partition partition,
      final CExpression exp,
      final CType targetType,
      final CFANode location)
      throws UnsupportedCCodeException {
    final boolean compress = (partition != null) && compressIntEqual
            && varClass.getIntEqualPartitions().contains(partition);
    if (varClass.getIntBoolPartitions().contains(partition)) {
      Region booleanResult = exp.accept(new BDDBooleanExpressionVisitor(predmgr, rmgr, precision, location));
      return (booleanResult == null) ? null : new Region[]{booleanResult};
    } else if (compress) {
      return exp.accept(new BDDCompressExpressionVisitor(predmgr, precision, getBitsize(partition, null), location, bvmgr, partition));
    } else {
      Region[] value = exp.accept(new BDDVectorCExpressionVisitor(predmgr, precision, bvmgr, machineModel, location));
      if (value != null) {
        // cast to correct length
        final CType sourceType = exp.getExpressionType().getCanonicalType();
        value = bvmgr.toBitsize(
                machineModel.getSizeof(targetType) * machineModel.getSizeofCharInBits(),
                sourceType instanceof CSimpleType && machineModel.isSigned((CSimpleType) sourceType),
                value);
      }
      return value;
    }
  }

  /** This function returns, if the variable is used in the Expression. */
  private static boolean isUsedInExpression(String varName, CExpression exp) {
    return exp.accept(new VarCExpressionVisitor(varName));
  }

  private static String scopeVar(final CExpression exp) {
    if (exp instanceof CIdExpression) {
      return ((CIdExpression) exp).getDeclaration().getQualifiedName();
    } else {
      // TODO function name?
      return exp.toASTString();
    }
  }

  private static boolean isLocalVariableForFunction(String scopedVarName, String function) {
    // TODO this relies on implementation details of CDeclaration.getQualifiedName()
    // TODO this ignores variable names created from scopeVar() by calling toASTString().
    return scopedVarName.startsWith(function + "::");
  }

  /** This function returns the bitsize for vars of a partition.
   * For a boolean var the value is 1.
   *
   * Compression for IntEqual-vars:
   * For N different values (maybe plus 2 for additional Zero and One)
   * of M different variables there are N+M possible values for a variable
   * (one for each value and one for each (maybe uninitialized) variable).
   * For N+M different values we need at least log_2(N+M) bits in the representation.
   *
   * For other variables, the lengthof the CType is used. */
  protected int getBitsize(@Nullable Partition partition, @Nullable CType type) {
    if (partition == null) {
      // we know nothing about the partition, so do not track it with BDDCPA
      return 0;
    } else if (varClass.getIntBoolPartitions().contains(partition)) {
      return 1;
    } else if (compressIntEqual && varClass.getIntEqualPartitions().contains(partition)) {
      final Set<BigInteger> values = partition.getValues();
      int N = values.size();
      if (!values.contains(BigInteger.ZERO)) {
        N++;
      }
      if (!values.contains(BigInteger.ONE)) {
        N++;
      }
      int M = partition.getVars().size();
      return (int) Math.ceil(Math.log(N + M) / Math.log(2));
    } else {
      return machineModel.getSizeof(type) * machineModel.getSizeofCharInBits();
    }
  }

  /** This Visitor evaluates the visited expression and
   * returns iff the given variable is used in it. */
  private static class VarCExpressionVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

    private String varName;

    VarCExpressionVisitor(String var) {
      this.varName = var;
    }

    private Boolean handle(CExpression exp) {
      return varName.equals(exp.toASTString());
    }

    @Override
    public Boolean visit(CArraySubscriptExpression exp) {
      return handle(exp);
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
    public Boolean visit(CComplexCastExpression exp) {
      // TODO check if only the part of the operand should be evaluated which the
      // expression casts to
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CFieldReference exp) {
      return handle(exp);
    }

    @Override
    public Boolean visit(CIdExpression exp) {
      return varName.equals(exp.getDeclaration().getQualifiedName());
    }

    @Override
    public Boolean visit(CUnaryExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CPointerExpression exp) {
      return exp.getOperand().accept(this);
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      return false;
    }
  }
}
