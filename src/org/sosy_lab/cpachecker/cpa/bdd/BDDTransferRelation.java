// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/** This Transfer Relation tracks variables and handles them as bitvectors. */
public class BDDTransferRelation
    extends ForwardingTransferRelation<BDDState, BDDState, VariableTrackingPrecision> {

  private final int bitsize;
  private final VariableClassification varClass;
  private final BitvectorManager bvmgr;
  private final NamedRegionManager rmgr;
  private final PredicateManager predmgr;
  private final BitvectorComputer bvComputer;

  /**
   * The Constructor of BDDVectorTransferRelation sets the NamedRegionManager and the
   * BitVectorManager. Both are used to build and manipulate BDDs, that represent the regions.
   */
  public BDDTransferRelation(
      NamedRegionManager manager,
      BitvectorManager pBvmgr,
      PredicateManager pPredmgr,
      CFA cfa,
      int pBitsize,
      BitvectorComputer pBvComputer) {
    rmgr = manager;
    bvmgr = pBvmgr;
    predmgr = pPredmgr;
    bitsize = pBitsize;
    assert cfa.getVarClassification().isPresent();
    varClass = cfa.getVarClassification().orElseThrow();
    bvComputer = pBvComputer;
  }

  @Override
  protected Collection<BDDState> preCheck(BDDState pState, VariableTrackingPrecision pPrecision) {
    // no variables should be tracked
    if (pPrecision.isEmpty()) {
      return Collections.singleton(pState);
    }
    // the path is not fulfilled
    if (pState.getRegion().isFalse()) {
      return ImmutableList.of();
    }
    return null;
  }

  /**
   * This function handles statements like "a = 0;" and "b = !a;" and calls of external functions.
   */
  @Override
  protected BDDState handleStatementEdge(final CStatementEdge cfaEdge, final CStatement statement)
      throws UnsupportedCodeException {

    BDDState result = state;

    // normal assignment, "a = ..."
    if (statement instanceof CAssignment) {
      result = handleAssignment((CAssignment) statement, cfaEdge.getSuccessor(), cfaEdge);

      // call of external function, "scanf(...)" without assignment
      // internal functioncalls are handled as FunctionCallEdges
    } else if (statement instanceof CFunctionCallStatement) {
      result =
          handleExternalFunctionCall(
              result,
              cfaEdge.getSuccessor(),
              ((CFunctionCallStatement) statement)
                  .getFunctionCallExpression()
                  .getParameterExpressions());
    }

    assert !result.getRegion().isFalse();
    return result;
  }

  /**
   * This function handles statements like "a = 0;" and "b = !a;". A region is build for the right
   * side of the statement. Then this region is assigned to the variable at the left side. This
   * equality is added to the BDDstate to get the next state.
   */
  private BDDState handleAssignment(CAssignment assignment, CFANode successor, CFAEdge edge)
      throws UnsupportedCodeException {
    CExpression lhs = assignment.getLeftHandSide();

    final String varName;
    if (lhs instanceof CIdExpression) {
      varName = ((CIdExpression) lhs).getDeclaration().getQualifiedName();
    } else {
      varName = functionName + "::" + lhs;
    }

    final CType targetType = lhs.getExpressionType();

    // next line is a shortcut, not necessary
    if (!precision.isTracking(MemoryLocation.fromQualifiedName(varName), targetType, successor)) {
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
        String tmpVarName = predmgr.getTmpVariableForPartition(partition);
        final Region[] tmp =
            predmgr.createPredicateWithoutPrecisionCheck(
                tmpVarName, bvComputer.getBitsize(partition, targetType));
        final CFANode location = successor;

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS =
            bvComputer.evaluateVectorExpression(partition, exp, targetType, location, precision);
        newState = newState.addAssignment(tmp, regRHS);

        // delete var, make tmp equal to (new) var, then delete tmp
        final Region[] var =
            predmgr.createPredicate(
                scopeVar(lhs),
                targetType,
                successor,
                bvComputer.getBitsize(partition, targetType),
                precision);
        newState = newState.forget(var);
        newState = newState.addAssignment(var, tmp);
        newState = newState.forget(tmp);

      } else {
        final Region[] var =
            predmgr.createPredicate(
                scopeVar(lhs),
                targetType,
                successor,
                bvComputer.getBitsize(partition, targetType),
                precision);
        newState = newState.forget(var);
        final CFANode location = successor;

        // make region for RIGHT SIDE and build equality of var and region
        final Region[] regRHS =
            bvComputer.evaluateVectorExpression(
                partition, (CExpression) rhs, targetType, location, precision);
        newState = newState.addAssignment(var, regRHS);
      }
      return newState;

    } else if (rhs instanceof CFunctionCallExpression) {
      // handle params of functionCall, maybe there is a sideeffect
      newState =
          handleExternalFunctionCall(
              newState, successor, ((CFunctionCallExpression) rhs).getParameterExpressions());

      // call of external function: we know nothing, so we delete the value of the var
      // TODO can we assume, that malloc returns something !=0?
      // are there some "save functions"?

      final Region[] var =
          predmgr.createPredicate(
              scopeVar(lhs),
              targetType,
              successor,
              bitsize,
              precision); // is default bitsize enough?
      newState = newState.forget(var);

      return newState;

    } else {
      throw new AssertionError("unhandled assignment: " + edge.getRawStatement());
    }
  }

  /**
   * This function deletes all vars, that could be modified through a side-effect of the (external)
   * functionCall.
   */
  private BDDState handleExternalFunctionCall(
      BDDState currentState, CFANode successor, final List<CExpression> params) {

    for (final CExpression param : params) {

      /* special case: external functioncall with possible side-effect!
       * this is the only statement, where a pointer-operation is allowed
       * and the var can be boolean, intEqual or intAdd,
       * because we know, the variable can have a random (unknown) value after the functioncall.
       * example: "scanf("%d", &input);" */
      CExpression unpackedParam = param;
      while (unpackedParam instanceof CCastExpression) {
        unpackedParam = ((CCastExpression) param).getOperand();
      }
      if (unpackedParam instanceof CUnaryExpression
          && UnaryOperator.AMPER == ((CUnaryExpression) unpackedParam).getOperator()
          && ((CUnaryExpression) unpackedParam).getOperand() instanceof CIdExpression) {
        final CIdExpression id = (CIdExpression) ((CUnaryExpression) unpackedParam).getOperand();
        final Region[] var =
            predmgr.createPredicate(
                scopeVar(id),
                id.getExpressionType(),
                successor,
                bitsize,
                precision); // is default bitsize enough?
        currentState = currentState.forget(var);

      } else {
        // "printf("%d", output);" or "assert(exp);"
        // TODO: can we do something here?
      }
    }
    return currentState;
  }

  /**
   * This function handles declarations like "int a = 0;" and "int b = !a;". Regions are build for
   * all Bits of the right side of the declaration, if it is not null. Then these regions are
   * assigned to the regions of variable (bitvector) at the left side. These equalities are added to
   * the BDDstate to get the next state.
   */
  @Override
  protected BDDState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws UnsupportedCodeException {

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
      // delete variable, if it was initialized before i.e. in another block, with an existential
      // operator
      Partition partition = varClass.getPartitionForEdge(cfaEdge);
      Region[] var =
          predmgr.createPredicate(
              vdecl.getQualifiedName(),
              vdecl.getType(),
              cfaEdge.getSuccessor(),
              bvComputer.getBitsize(partition, vdecl.getType()),
              precision);
      BDDState newState = state.forget(var);

      // initializer on RIGHT SIDE available, make region for it
      if (init != null) {
        final Region[] rhs =
            bvComputer.evaluateVectorExpression(
                partition, init, vdecl.getType(), cfaEdge.getSuccessor(), precision);
        newState = newState.addAssignment(var, rhs);
      }

      return newState;
    }

    return state; // if we know nothing, we return the old state
  }

  /**
   * This function handles functioncalls like "f(x)", that calls "f(int a)". Therefore each arg
   * ("x") is transformed into a region and assigned to a param ("int a") of the function. The
   * equalities of all arg-param-pairs are added to the BDDstate to get the next state.
   */
  @Override
  protected BDDState handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> args,
      List<CParameterDeclaration> params,
      String calledFunction)
      throws UnsupportedCodeException {
    BDDState newState = state;

    // var_args cannot be handled: func(int x, ...) --> we only handle the first n parameters
    assert args.size() >= params.size();

    for (int i = 0; i < params.size(); i++) {

      // make variable (predicate) for param, this variable is not global
      final String varName = params.get(i).getQualifiedName();
      final CType targetType = params.get(i).getType();
      final Partition partition = varClass.getPartitionForParameterOfEdge(cfaEdge, i);
      final Region[] var =
          predmgr.createPredicate(
              varName,
              targetType,
              cfaEdge.getSuccessor(),
              bvComputer.getBitsize(partition, targetType),
              precision);
      final Region[] arg =
          bvComputer.evaluateVectorExpression(
              partition, args.get(i), targetType, cfaEdge.getSuccessor(), precision);
      newState = newState.addAssignment(var, arg);
    }

    return newState;
  }

  /**
   * This function handles functionReturns like "y=f(x)". The equality of the returnValue
   * (FUNCTION_RETURN_VARIABLE) and the left side ("y") is added to the new state. Each variable
   * from inside the function is removed from the BDDstate.
   */
  @Override
  protected BDDState handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String outerFunctionName) {
    BDDState newState = state;

    // set result of function equal to variable on left side
    final Partition partition = varClass.getPartitionForEdge(cfaEdge);

    // handle assignments like "y = f(x);"
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      final String returnVar =
          fnkCall.getFunctionEntry().getReturnVariable().orElseThrow().getQualifiedName();
      CFunctionCallAssignmentStatement cAssignment = (CFunctionCallAssignmentStatement) summaryExpr;
      CExpression lhs = cAssignment.getLeftHandSide();
      final int size = bvComputer.getBitsize(partition, lhs.getExpressionType());

      // make variable (predicate) for LEFT SIDE of assignment,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] var =
          predmgr.createPredicate(
              scopeVar(lhs), lhs.getExpressionType(), cfaEdge.getSuccessor(), size, precision);
      newState = newState.forget(var);

      // make region (predicate) for RIGHT SIDE
      final Region[] retVar =
          predmgr.createPredicate(
              returnVar,
              summaryExpr.getFunctionCallExpression().getExpressionType(),
              cfaEdge.getSuccessor(),
              size,
              precision);
      newState = newState.addAssignment(var, retVar);

      // remove returnVar from state,
      // all other function-variables were removed earlier (see handleReturnStatementEdge()).
      // --> now the state does not contain any variable from scope of called function.
      if (predmgr.getTrackedVars().contains(returnVar)) {
        newState = newState.forget(predmgr.createPredicateWithoutPrecisionCheck(returnVar));
      }

    } else {
      assert summaryExpr instanceof CFunctionCallStatement; // no assignment, nothing to do
    }

    return newState;
  }

  /**
   * This function handles functionStatements like "return (x)". The equality of the returnValue
   * (FUNCTION_RETURN_VARIABLE) and the evaluated right side ("x") is added to the new state.
   */
  @Override
  protected BDDState handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws UnsupportedCodeException {
    BDDState newState = state;
    String returnVar = "";

    if (cfaEdge.getExpression().isPresent()) {
      returnVar =
          ((CIdExpression) cfaEdge.asAssignment().orElseThrow().getLeftHandSide())
              .getDeclaration()
              .getQualifiedName();
      final Partition partition = varClass.getPartitionForEdge(cfaEdge);
      final CType functionReturnType =
          ((CFunctionDeclaration) cfaEdge.getSuccessor().getEntryNode().getFunctionDefinition())
              .getType()
              .getReturnType();

      // make region for RIGHT SIDE, this is the 'x' from 'return (x);
      final Region[] regRHS =
          bvComputer.evaluateVectorExpression(
              partition,
              cfaEdge.getExpression().orElseThrow(),
              functionReturnType,
              cfaEdge.getSuccessor(),
              precision);

      // make variable (predicate) for returnStatement,
      // delete variable, if it was used before, this is done with an existential operator
      final Region[] retvar =
          predmgr.createPredicate(
              returnVar,
              functionReturnType,
              cfaEdge.getSuccessor(),
              bvComputer.getBitsize(partition, functionReturnType),
              precision);
      newState = newState.forget(retvar);
      newState = newState.addAssignment(retvar, regRHS);
    }

    // delete variables from returning function,
    // we do not need them after this location, because the next edge is the functionReturnEdge.
    // this results in a smaller BDD and allows to call a function twice.
    for (String var : predmgr.getTrackedVars()) {
      if (isLocalVariableForFunction(var, functionName) && !returnVar.equals(var)) {
        newState = newState.forget(predmgr.createPredicateWithoutPrecisionCheck(var));
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
      for (String var : predmgr.getTrackedVars()) {
        if (isLocalVariableForFunction(var, functionName)) {
          newState = newState.forget(predmgr.createPredicateWithoutPrecisionCheck(var));
        }
      }
      return newState;
    }

    return state;
  }

  /**
   * This function handles assumptions like "if(a==b)" and "if(a!=0)". A region is build for the
   * assumption. This region is added to the BDDstate to get the next state. If the next state is
   * False, the assumption is not fulfilled. In this case NULL is returned.
   */
  @Override
  protected BDDState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws UnsupportedCodeException {

    Partition partition = varClass.getPartitionForEdge(cfaEdge);
    final Region[] operand =
        bvComputer.evaluateVectorExpression(
            partition, expression, CNumericTypes.INT, cfaEdge.getSuccessor(), precision);
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

  private BDDState handleAssumption(
      CAssumeEdge cfaEdge,
      CExpression expression,
      boolean truthAssumption,
      PointerState pPointerInfo)
      throws UnsupportedCodeException {

    Partition partition = varClass.getPartitionForEdge(cfaEdge);
    final Region[] operand =
        bvComputer.evaluateVectorExpressionWithPointerState(
            partition,
            expression,
            CNumericTypes.INT,
            cfaEdge.getSuccessor(),
            pPointerInfo,
            precision);
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

  /** This function returns, if the variable is used in the Expression. */
  private static boolean isUsedInExpression(String varName, CExpression exp) {
    return exp.accept(new VarCExpressionVisitor(varName));
  }

  private String scopeVar(final CExpression exp) {
    if (exp instanceof CIdExpression) {
      return ((CIdExpression) exp).getDeclaration().getQualifiedName();
    } else {
      return functionName + "::" + exp.toASTString();
    }
  }

  private static boolean isLocalVariableForFunction(String scopedVarName, String function) {
    // TODO this relies on implementation details of CDeclaration.getQualifiedName()
    // TODO this ignores variable names created from scopeVar() by calling toASTString().
    return scopedVarName.startsWith(function + "::");
  }

  /**
   * returns a canonical representation of a field reference, including functionname. return NULL if
   * the canonical name could not determined.
   */
  static @Nullable String getCanonicalName(CExpression expr) {
    String name = "";
    while (true) {
      if (expr instanceof CIdExpression) {
        return ((CIdExpression) expr).getDeclaration().getQualifiedName() + name;
      } else if (expr instanceof CFieldReference) {
        CFieldReference fieldRef = (CFieldReference) expr;
        name = (fieldRef.isPointerDereference() ? "->" : ".") + fieldRef.getFieldName() + name;
        expr = fieldRef.getFieldOwner();
      } else {
        return null;
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, Iterable<AbstractState> states, CFAEdge cfaEdge, Precision pPrecision)
      throws CPATransferException {
    BDDState bddState = (BDDState) pState;

    for (AbstractState otherState : states) {
      if (otherState instanceof PointerState) {
        super.setInfo(bddState, pPrecision, cfaEdge);
        bddState = strengthenWithPointerInformation((PointerState) otherState, cfaEdge);
        super.resetInfo();
        if (bddState == null) {
          return ImmutableList.of();
        }
      }
    }
    return Collections.singleton(bddState);
  }

  private BDDState strengthenWithPointerInformation(PointerState pPointerInfo, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {

    if (cfaEdge instanceof CAssumeEdge) {
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      return handleAssumption(
          assumeEdge, assumeEdge.getExpression(), assumeEdge.getTruthAssumption(), pPointerInfo);
    }

    // get target for LHS
    MemoryLocation target = null;
    ALeftHandSide leftHandSide = CFAEdgeUtils.getLeftHandSide(cfaEdge);
    if (leftHandSide instanceof CIdExpression) {
      target = MemoryLocation.forDeclaration(((CIdExpression) leftHandSide).getDeclaration());
    } else if (leftHandSide instanceof CPointerExpression) {
      ExplicitLocationSet explicitSet =
          getLocationsForLhs(pPointerInfo, (CPointerExpression) leftHandSide);
      if (explicitSet != null && explicitSet.getSize() == 1) {
        target = Iterables.getOnlyElement(explicitSet);
      }
    }

    // without a target, nothing can be done.
    if (target == null) {
      return state;
    }

    // get value for RHS
    MemoryLocation value = null;
    CType valueType = null;
    ARightHandSide rightHandSide = CFAEdgeUtils.getRightHandSide(cfaEdge);
    if (rightHandSide instanceof CIdExpression) {
      CIdExpression idExpr = (CIdExpression) rightHandSide;
      value = MemoryLocation.forDeclaration(idExpr.getDeclaration());
      valueType = idExpr.getDeclaration().getType();
    } else if (rightHandSide instanceof CPointerExpression) {
      CPointerExpression ptrExpr = (CPointerExpression) rightHandSide;
      value = getLocationForRhs(pPointerInfo, ptrExpr);
      valueType = ptrExpr.getExpressionType().getCanonicalType();
    }

    // without a value, nothing can be done.
    if (value == null) {
      return state;
    }

    final Partition partition = varClass.getPartitionForEdge(cfaEdge);
    int size = bvComputer.getBitsize(partition, valueType);

    final Region[] rhs =
        predmgr.createPredicate(
            target.getExtendedQualifiedName(), valueType, cfaEdge.getSuccessor(), size, precision);

    final Region[] evaluation =
        predmgr.createPredicate(
            value.getExtendedQualifiedName(), valueType, cfaEdge.getSuccessor(), size, precision);
    BDDState newState = state.forget(rhs);
    return newState.addAssignment(rhs, evaluation);
  }

  /** get all possible explicit targets for a pointer, or NULL if they are unknown. */
  static @Nullable ExplicitLocationSet getLocationsForLhs(
      PointerState pPointerInfo, CPointerExpression pPointer) throws UnrecognizedCodeException {
    LocationSet directLocation = PointerTransferRelation.asLocations(pPointer, pPointerInfo);
    if (!(directLocation instanceof ExplicitLocationSet)) {
      LocationSet indirectLocation =
          PointerTransferRelation.asLocations(pPointer.getOperand(), pPointerInfo);
      if (indirectLocation instanceof ExplicitLocationSet) {
        ExplicitLocationSet explicitSet = (ExplicitLocationSet) indirectLocation;
        if (explicitSet.getSize() == 1) {
          directLocation = pPointerInfo.getPointsToSet(Iterables.getOnlyElement(explicitSet));
        }
      }
    }
    if (directLocation instanceof ExplicitLocationSet) {
      return (ExplicitLocationSet) directLocation;
    }
    return null;
  }

  static @Nullable MemoryLocation getLocationForRhs(
      PointerState pPointerInfo, CPointerExpression pPointer) throws UnrecognizedCodeException {
    LocationSet fullSet = PointerTransferRelation.asLocations(pPointer.getOperand(), pPointerInfo);
    if (fullSet instanceof ExplicitLocationSet) {
      ExplicitLocationSet explicitSet = (ExplicitLocationSet) fullSet;
      if (explicitSet.getSize() == 1) {
        LocationSet pointsToSet =
            pPointerInfo.getPointsToSet(Iterables.getOnlyElement(explicitSet));
        if (pointsToSet instanceof ExplicitLocationSet) {
          ExplicitLocationSet explicitPointsToSet = (ExplicitLocationSet) pointsToSet;
          if (explicitPointsToSet.getSize() == 1) {
            return Iterables.getOnlyElement(explicitPointsToSet);
          }
        }
      }
    }
    return null;
  }
}
