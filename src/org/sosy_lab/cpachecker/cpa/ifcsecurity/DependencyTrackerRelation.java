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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.FunctionCallStatementDependancy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.VariableDependancy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.AllTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DepPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DependencyPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.ImplicitDependencyPrecision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * CPA-Transfer-Relation for tracking which variables/functions are depends on which other variables/functions
 */
public class DependencyTrackerRelation
    extends ForwardingTransferRelation<DependencyTrackerState, DependencyTrackerState, Precision> {


  @SuppressWarnings("unused")
  private LogManager logger;

  public DependencyTrackerRelation(LogManager pLogger) {
    logger = pLogger;
  }

  private DependencyTrackerState cloneWithPrecision(CFAEdge pCfaEdge) {
    /*
     * Clone the state
     */
    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = state.clone();
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    /*
     * Apply Precision on Clone
     */
    for (Entry<Variable, SortedSet<Variable>> entry : stateDeps.entrySet()) {
      Variable lhsVar = entry.getKey();
      SortedSet<Variable> lhsDeps = resultDeps.get(lhsVar);
      SortedSet<Variable> rhsVars = entry.getValue();
      for (Variable rhsVar : rhsVars) {
        removeDep(lhsDeps, pCfaEdge, lhsVar, rhsVar);
      }
    }
    result.setPrec((DepPrecision) getPrecision());
    /*
     * Return
     */
    return result;
  }

  private void addDep(SortedSet<Variable> pLhsDeps, CFAEdge pCfaEdge, Variable pLhsVar,
      Variable pRhsVar) {
    /*
     * Add only entities that are trackable
     */
    DependencyPrecision prec = (DependencyPrecision) getPrecision();
    addDep(prec, pLhsDeps, pCfaEdge, pLhsVar, pRhsVar);
  }

  private void addDep(DependencyPrecision pPrec, SortedSet<Variable> pLhsDeps, CFAEdge pCfaEdge, Variable pLhsVar,
      Variable pRhsVar) {
    /*
     * Add only entities that are trackable
     */
    if (pPrec.isTracked(pCfaEdge.getSuccessor(), pLhsVar, pRhsVar)) {
      pLhsDeps.add(pRhsVar);
    }
  }

  private void removeDep(SortedSet<Variable> pLhsDeps, CFAEdge pCfaEdge, Variable pLhsVar,
      Variable pRhsVar) {
    /*
     * Remove all rhs entities that are not trackable
     */
    DependencyPrecision prec = (DependencyPrecision) getPrecision();
    removeDep(prec, pLhsDeps, pCfaEdge, pLhsVar, pRhsVar);
  }

  private void removeDep(DependencyPrecision pPrec, SortedSet<Variable> pLhsDeps, CFAEdge pCfaEdge, Variable pLhsVar,
      Variable pRhsVar) {
    /*
     * Remove all rhs entities that are not trackable
     */
    if (!pPrec.isTracked(pCfaEdge.getSuccessor(), pLhsVar, pRhsVar)) {
      pLhsDeps.remove(pRhsVar);
    }
  }


  private void setDeps(Map<Variable, SortedSet<Variable>> pResultDeps, Variable pLhsVar,
      SortedSet<Variable> pLhsDeps) {
    /*
     * Storage Reduction
     * Add (pLhsVar, pLhsDeps) only iff pLhsDeps contains entries
     */
    if (pLhsDeps.size() != 0) {
      pResultDeps.put(pLhsVar, pLhsDeps);
    } else {
      pResultDeps.remove(pLhsVar);
    }
  }

  private SortedSet<Variable> getDeps(Map<Variable, SortedSet<Variable>> pDeps, CFAEdge pCfaEdge,
      Variable pLhsVar) {
    DependencyPrecision prec = (DependencyPrecision) getPrecision();
    return getDeps(prec, pDeps, pCfaEdge, pLhsVar);
  }

  private SortedSet<Variable> getDeps(DependencyPrecision pPrec, Map<Variable, SortedSet<Variable>> pDeps, CFAEdge pCfaEdge,
      Variable pLhsVar) {
    SortedSet<Variable> rhsDeps = initializeDeps(pPrec, pCfaEdge, pLhsVar);
    if (pDeps.containsKey(pLhsVar)) {
      rhsDeps = pDeps.get(pLhsVar);
    }
    return rhsDeps;
  }

  private SortedSet<Variable> initializeDeps(CFAEdge pCfaEdge, Variable pLhsVar) {
    DependencyPrecision prec = (DependencyPrecision) getPrecision();
    return initializeDeps(prec, pCfaEdge, pLhsVar);
  }

  private SortedSet<Variable> initializeDeps(DependencyPrecision pPrec, CFAEdge pCfaEdge, Variable pLhsVar) {
    SortedSet<Variable> rhsDeps = new TreeSet<>();
    addDep(pPrec, rhsDeps, pCfaEdge, pLhsVar, pLhsVar);
    return rhsDeps;
  }



  @Override
  protected DependencyTrackerState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression,
      boolean pTruthAssumption)
      throws CPATransferException {
    /*
     * Just copy
     */
    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    return result;
  }

  @Override
  protected DependencyTrackerState handleBlankEdge(BlankEdge pCfaEdge) {
    /*
     * Just copy
     */
    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    return result;
  }

  @Override
  protected DependencyTrackerState handleDeclarationEdge(CDeclarationEdge pCfaEdge,
      CDeclaration pDecl)
      throws CPATransferException {


    if (pDecl instanceof CVariableDeclaration) { return handleDeclarationEdge_VariableDeclaration(pCfaEdge,
        (CVariableDeclaration) pDecl); }
    if (pDecl instanceof CFunctionDeclaration) { return handleDeclarationEdge_FunctionDeclaration(pCfaEdge,
        (CFunctionDeclaration) pDecl); }
    if (pDecl instanceof CTypeDeclaration) {
      //DO NOTHING
    }
    /*
     * Default: Return
     */
    return cloneWithPrecision(pCfaEdge);
  }

  private DependencyTrackerState handleDeclarationEdge_VariableDeclaration(CDeclarationEdge pCfaEdge,
      CVariableDeclaration pDecl)
      throws CPATransferException {

    /*
     * TYPE a[=B];
     *
     * Dep'(a)=a union [Union_(b in fv(B)) Dep(b)]
     */
    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    /*
     * a
     */
    String lhsName = pDecl.getQualifiedName();
    Variable lhsVar = new Variable(lhsName);

    /*
     * Dep'(a)=a
     */
    SortedSet<Variable> lhsDeps = initializeDeps(pCfaEdge, lhsVar);

    if (pDecl.getInitializer() != null) {

      /*
       * fv(B)={..,b_i,..}
       */
      CInitializer initializer = pDecl.getInitializer();
      CInitializerExpression initializerExpr = (CInitializerExpression) initializer;
      VariableDependancy visitor = new VariableDependancy();
      initializerExpr.getExpression().accept(visitor);
      SortedSet<Variable> initializerVars = visitor.getResult();

      for (Variable initializerVar : initializerVars) {
        /*
         * b_i
         */

        /*
         * Dep(b_i)
         */
        SortedSet<Variable> rhsDeps = getDeps(stateDeps, pCfaEdge, initializerVar);

        /*
         * Dep'(a)=Dep(a) union Dep(b_i)
         */
        for (Variable rhsDep : rhsDeps) {
          addDep(lhsDeps, pCfaEdge, lhsVar, rhsDep);
        }
      }
    }

    /*
     * Add Dep(a)
     */
    setDeps(resultDeps, lhsVar, lhsDeps);

    /*
     * Return
     */
    return result;
  }

  private DependencyTrackerState handleDeclarationEdge_FunctionDeclaration(CDeclarationEdge pCfaEdge,
      CFunctionDeclaration pDecl) {

    /*
     * TYPE f(..., TYPE p_i,..);
     *
     * Dep(f)=f union [p_i];
     * Dep(p_i)=p_i
     */
    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    /*
     * f
     */
    String functionName = pDecl.getQualifiedName();
    Variable functionVar = new Variable(functionName);

    /*
     * Dep'(f)=f
     */
    SortedSet<Variable> functionDeps = initializeDeps(pCfaEdge, functionVar);

    /*
     * p=[..,p_i,..]
     */
    List<CParameterDeclaration> params = pDecl.getParameters();
    for (CParameterDeclaration param : params) {
      /*
       * p_i
       */
      CVariableDeclaration paramDecl = param.asVariableDeclaration();
      String paramName = paramDecl.getQualifiedName();
      Variable paramVar = new Variable(paramName);
      /*
       * Dep'(p_i)=p_i
       */
      SortedSet<Variable> paramDeps = initializeDeps(pCfaEdge, paramVar);

      /*
       * Dep'(f)=Dep(f) union p_i
       */
      addDep(functionDeps, pCfaEdge, functionVar, paramVar);

      /*
       * Add Dep(p_i)
       */
      setDeps(resultDeps, paramVar, paramDeps);
    }

    /*
     * Add Dep(f)
     */
    setDeps(resultDeps, functionVar, functionDeps);
    /*
     * Return
     */
    return result;
  }

  @Override
  protected DependencyTrackerState handleFunctionCallEdge(CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments, List<CParameterDeclaration> pParameters,
      String pCalledFunctionName) throws CPATransferException {

    /*
     *  call f(..,a_i,..)                      #Signature: TYPE f(..,TYPE p_i,..);
     *
     *  Dep(p_i)=Dep(a_i)
     */
    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    for (int i = 0; i < pParameters.size(); i++) {

      /*
       * p_i
       */
      CParameterDeclaration paramDecl = pParameters.get(i);
      String paramName = paramDecl.getQualifiedName();
      Variable paramVar = new Variable(paramName);

      /*
       * Dep(p_i)=p_i
       */
      SortedSet<Variable> paramDeps = initializeDeps(pCfaEdge, paramVar);

      /*
       * fv(a_i)={..,b_i,..}
       */
      CExpression paramExpr = pArguments.get(i);
      VariableDependancy visitor = new VariableDependancy();
      paramExpr.accept(visitor);
      SortedSet<Variable> paramExprVars = visitor.getResult();

      /*
       * b_i
       */
      for (Variable paramExprVar : paramExprVars) {

        /*
         * Dep(b_i)
         */
        SortedSet<Variable> paramExprVarDeps = getDeps(stateDeps, pCfaEdge, paramExprVar);

        /*
         * Dep'(p_i)=Dep(p_i) union Dep(b_i)
         */
        for (Variable paramExprVarDep : paramExprVarDeps) {
          addDep(paramDeps, pCfaEdge, paramVar, paramExprVarDep);
        }
      }

      /*
       * Add Dep(p_i)
       */
      setDeps(resultDeps, paramVar, paramDeps);
    }

    /*
     * Return
     */
    return result;
  }

  @Override
  protected DependencyTrackerState handleFunctionReturnEdge(CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCall pSummaryExpr, String pCallerFunctionName)
      throws CPATransferException {

    if (pSummaryExpr instanceof CFunctionCallAssignmentStatement) { return handleFR_FCAS(pCfaEdge,
        pFnkCall, (CFunctionCallAssignmentStatement) pSummaryExpr, pCallerFunctionName); }
    if (pSummaryExpr instanceof CFunctionCallStatement) { return handleFR_FCS(pCfaEdge, pFnkCall,
        (CFunctionCallStatement) pSummaryExpr, pCallerFunctionName); }
    /*
     * Default: Return
     */
    return cloneWithPrecision(pCfaEdge);
  }

  @SuppressWarnings("unused")
  private DependencyTrackerState handleFR_clear(DependencyTrackerState result, CFAEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCall pSummaryExpr, String pCallerFunctionName)
      throws UnsupportedCodeException {
    /*
     * Dep(f::retval)=f::retval;
     * Dep(p_i)=p_i;
     * Dep(f)=f;
     *
     */
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    if (pFnkCall.getFunctionEntry().getReturnVariable().isPresent()) {

      /*
       * f::retval
       */
      String fnkRetvalName =
          pFnkCall.getFunctionEntry().getReturnVariable().get().getQualifiedName();
      Variable fnkRetVal = new Variable(fnkRetvalName);

      /*
       * Dep'(f::retval)=f::retval
       */
      SortedSet<Variable> fnkRetValDeps = initializeDeps(pCfaEdge, fnkRetVal);
      setDeps(resultDeps, fnkRetVal, fnkRetValDeps);
    }

    List<CParameterDeclaration> paramDefs = pFnkCall.getFunctionEntry().getFunctionParameters();
    for (CParameterDeclaration paramDef : paramDefs) {

      /*
       * p_i
       */
      String paramDefName = paramDef.asVariableDeclaration().getQualifiedName();
      Variable paramDefVar = new Variable(paramDefName);

      /*
       * Dep'(p_i) = p_i
       */
      SortedSet<Variable> paramDefDeps = initializeDeps(pCfaEdge, paramDefVar);
      setDeps(resultDeps, paramDefVar, paramDefDeps);
    }

    /*
     * f
     */
    Variable functionVar = new Variable(functionName);
    /*
     * Dep'(f) = f
     */
    SortedSet<Variable> functionDeps = initializeDeps(pCfaEdge, functionVar);
    setDeps(resultDeps, functionVar, functionDeps);

    /*
     * Return
     */
    return result;
  }

  @SuppressWarnings("unused")
  private DependencyTrackerState handleFR_FCAS(CFAEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCallAssignmentStatement pSummaryExpr,
      String pCallerFunctionName)
      throws UnsupportedCodeException {

    /*
     * l=f(a_i);
     *
     * Dep(l)=l union  Dep(f::retval) ;
     *
     * Clear:
     * Dep(f::retval)=f::retval;
     * Dep(p_i)=p_i;
     * Dep(f)=f;
     *
     */
    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();


    if (pFnkCall.getFunctionEntry().getReturnVariable().isPresent()) {
      /*
       * f::retval
       */
      String fnkRetvalName =
          pFnkCall.getFunctionEntry().getReturnVariable().get().getQualifiedName();
      Variable fnkRetVal = new Variable(fnkRetvalName);

      /*
       * Dep'(f::retval)
       */
      SortedSet<Variable> fnkRetValDeps = getDeps(stateDeps, pCfaEdge, fnkRetVal);

      /*
       * l
       */
      CExpression lhsExpr = pSummaryExpr.getLeftHandSide();
      VariableDependancy visitor = new VariableDependancy();
      lhsExpr.accept(visitor);
      SortedSet<Variable> lhsVars = visitor.getResult();

      for (Variable lhsVar : lhsVars) {
        /*
         * Dep'(l) = empty
         */
        SortedSet<Variable> lhsDeps = new TreeSet<>();
        /*
         * Dep'(l) = l
         */
        addDep(lhsDeps, pCfaEdge, lhsVar, lhsVar);


        /*
         * Dep'(l) = Dep(l) union Dep(f::retval)
         */
        for (Variable fnkRetValDep : fnkRetValDeps) {
          addDep(lhsDeps, pCfaEdge, lhsVar, fnkRetValDep);
        }
        setDeps(resultDeps, lhsVar, lhsDeps);
      }
    }

//  Not sure
//  result = handleFR_clear(result, pCfaEdge, pFnkCall, pSummaryExpr, pCallerFunctionName);

    /*
     * Return
     */
    return result;
  }

  @SuppressWarnings("unused")
  private DependencyTrackerState handleFR_FCS(CFAEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCallStatement pSummaryExpr,
      String pCallerFunctionName)
      throws UnsupportedCodeException {

    /*
    * Clear:
    * Dep(f::retval)=f::retval;
    * Dep(p_i)=p_i;
    * Dep(f)=f;
    *
    */
    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
//  Not sure
//  result = handleFR_clear(result, pCfaEdge, pFnkCall, pSummaryExpr, pCallerFunctionName);

    /*
     * Return
     */
    return result;
  }

  @Override
  protected DependencyTrackerState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge)
      throws CPATransferException {
    /*
     * Just copy
     */
    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    return result;
  }


  @Override
  protected DependencyTrackerState handleStatementEdge(CStatementEdge pCfaEdge,
      CStatement pStatement)
      throws CPATransferException {
    if (pStatement instanceof CExpressionAssignmentStatement) { return handleStmtCEAS(pCfaEdge,
        (CExpressionAssignmentStatement) pStatement); }
    if (pStatement instanceof CExpressionStatement) {
      //DO NOTHING
    }
    if (pStatement instanceof CFunctionCallAssignmentStatement) { return handleStmtFCAS(pCfaEdge,
        (CFunctionCallAssignmentStatement) pStatement); }
    if (pStatement instanceof CFunctionCallStatement) { return handleStmtFCS(pCfaEdge,
        (CFunctionCallStatement) pStatement); }
    /*
     * Default: Return
     */
    return cloneWithPrecision(pCfaEdge);
  }

  private DependencyTrackerState handleStmtCEAS(CFAEdge pCfaEdge,
      CExpressionAssignmentStatement pStatement)
      throws CPATransferException {
    /*
     * a[=B];
     *
     * Dep'(a)=a union [Union_(b in fv(B)) Dep(b)]
     */
    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    VariableDependancy visitor;

    CExpression lhsExpr = pStatement.getLeftHandSide();
    visitor = new VariableDependancy();
    lhsExpr.accept(visitor);
    SortedSet<Variable> lhsVars = visitor.getResult();

    /*
     * fv(B)={..,b_i,..}
     */
    CExpression rhsExpr = pStatement.getRightHandSide();
    visitor = new VariableDependancy();
    rhsExpr.accept(visitor);
    SortedSet<Variable> rhsVars = visitor.getResult();

    for (Variable lhsVar : lhsVars) {
      /*
       * a
       */

      /*
       * Dep'(a)=a
       */
      SortedSet<Variable> lhsDeps = initializeDeps(pCfaEdge, lhsVar);

      for (Variable rhsVar : rhsVars) {
        /*
         * b_i
         */

        /*
         * Dep(b_i)
         */
        SortedSet<Variable> rhsDeps = getDeps(stateDeps, pCfaEdge, rhsVar);

        /*
         * Dep'(a)=Dep(a) union Dep(b_i)
         */
        for (Variable rhsDep : rhsDeps) {
          addDep(lhsDeps, pCfaEdge, lhsVar, rhsDep);
        }
      }

      /*
       * Add Dep(a)
       */
      setDeps(resultDeps, lhsVar, lhsDeps);
    }

    /*
     * Return
     */
    return result;
  }

  private DependencyTrackerState handleStmtFCAS(CFAEdge pCfaEdge,
      CFunctionCallAssignmentStatement pStatement)
      throws CPATransferException {

    /*
     * l=f(..,a_i,..);                             #Signature: TYPE f(..,TYPE p_i,..);
     *
     * Dep(l)=l union  Dep(f) union Dep(p_i) union Dep(a_i) ;
     * Dep(f)=f union Dep(a_i);
     * Dep(p_i)=p_i union Dep(a_i);
     *
     */
    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    FunctionCallStatementDependancy visitor;

    CLeftHandSide lhsExpr = pStatement.getLeftHandSide();
    visitor = new FunctionCallStatementDependancy();
    lhsExpr.accept(visitor);
    SortedSet<Variable> lhsVars = visitor.getResult();

    CFunctionCallExpression functionCallExpr = pStatement.getFunctionCallExpression();
    CExpression functionExpr = functionCallExpr.getFunctionNameExpression();
    visitor = new FunctionCallStatementDependancy();
    functionExpr.accept(visitor);
    SortedSet<Variable> functionVars = visitor.getResult();

    List<CParameterDeclaration> paramDefs =
        pStatement.getRightHandSide().getDeclaration().getParameters();
    List<CExpression> paramExprs = pStatement.getRightHandSide().getParameterExpressions();

    assert ((!(paramDefs != null && paramExprs != null))
        || (paramDefs.size() == paramExprs.size()));

    for (Variable lhsVar : lhsVars) {
      /*
       * l
       */

      /*
       * Dep(l)=l
       */
      SortedSet<Variable> lhsDeps = initializeDeps(pCfaEdge, lhsVar);

      for (Variable functionVar : functionVars) {
        /*
         * f
         */

        /*
         * Dep(f)
         */
        SortedSet<Variable> functionVarDeps = getDeps(stateDeps, pCfaEdge, functionVar);

        /*
         * Dep'(l)=Dep(l) union Dep(f)
         */
        for (Variable functionVarDep : functionVarDeps) {
          addDep(lhsDeps, pCfaEdge, lhsVar, functionVarDep);
        }

        for (int i = 0; i < paramDefs.size(); i++) {

          /*
           * p_i
           */
          String paramName = paramDefs.get(i).asVariableDeclaration().getQualifiedName();
          Variable paramDefVar = new Variable(paramName);

          /*
           * Dep'(p_i)=p_i
           */
          SortedSet<Variable> paramDeps = initializeDeps(pCfaEdge, paramDefVar);

          /*
           * Dep'(f)=Dep(f) union Dep(p_i)
           */
          addDep(functionVarDeps, pCfaEdge, functionVar, paramDefVar);
          /*
           * Dep'(l)=Dep(l) union Dep(p_i)
           */
          addDep(lhsDeps, pCfaEdge, lhsVar, paramDefVar);

          CExpression paramExpr = paramExprs.get(i);
          visitor = new FunctionCallStatementDependancy();
          paramExpr.accept(visitor);
          SortedSet<Variable> paramExprVars = visitor.getResult();

          for (Variable paramExprVar : paramExprVars) {
            /*
             * a_i
             */

            /*
             * Dep(a_i)={..,b_i,..}
             */
            SortedSet<Variable> paramExprVarDeps = getDeps(stateDeps, pCfaEdge, paramExprVar);

            for (Variable paramExprVarDep : paramExprVarDeps) {
              /*
               * b_i
               */

              /*
               * Dep'(p_i)=Dep(p_i) union Dep(b_i)
               */
              addDep(paramDeps, pCfaEdge, paramDefVar, paramExprVarDep);

              /*
               * Dep'(f)=Dep(f) union Dep(b_i)
               */
              addDep(functionVarDeps, pCfaEdge, functionVar, paramExprVarDep);

              /*
               * Dep'(l)=Dep(l) union Dep(b_i)
               */
              addDep(lhsDeps, pCfaEdge, lhsVar, paramExprVarDep);
            }
          }

          /*
           * Add Dep(p_i)
           */
          setDeps(resultDeps, paramDefVar, paramDeps);
        }

        /*
         * Add Dep(f)
         */
        setDeps(resultDeps, functionVar, functionVarDeps);
      }

      /*
       * Add Dep(l)
       */
      setDeps(resultDeps, lhsVar, lhsDeps);
    }

    /*
     * Return
     */
    return result;
  }

  private DependencyTrackerState handleStmtFCS(CFAEdge pCfaEdge,
      CFunctionCallStatement pStatement)
      throws CPATransferException {

    /*
     *  f(..,a_i,..)                      #Signature: TYPE f(..,TYPE p_i,..);
     *
     *
     *  Dep(p_i)=p_i union Dep(a_i)
     *  Dep(f)= f union Dep(p_i)
     *
     */

    Map<Variable, SortedSet<Variable>> stateDeps = state.getDependencies();

    DependencyTrackerState result = cloneWithPrecision(pCfaEdge);
    Map<Variable, SortedSet<Variable>> resultDeps = result.getDependencies();

    FunctionCallStatementDependancy visitor;

    CFunctionCallExpression functionCallExpr = pStatement.getFunctionCallExpression();
    CExpression functionExpr = functionCallExpr.getFunctionNameExpression();
    visitor = new FunctionCallStatementDependancy();
    functionExpr.accept(visitor);
    SortedSet<Variable> functionVars = visitor.getResult();

    List<CParameterDeclaration> paramDefs =
        pStatement.getFunctionCallExpression().getDeclaration().getParameters();
    List<CExpression> paramExprs = pStatement.getFunctionCallExpression().getParameterExpressions();

    assert (!(paramDefs != null && paramExprs != null) || (paramDefs.size() == paramExprs.size()));

    for (Variable functionVar : functionVars) {
      /*
       * f
       */

      /*
       * Dep(f)
       */
      SortedSet<Variable> functionVarDeps = getDeps(stateDeps, pCfaEdge, functionVar);

      for (int i = 0; i < paramDefs.size(); i++) {

        /*
         * p_i
         */
        String paramName = paramDefs.get(i).asVariableDeclaration().getQualifiedName();
        Variable paramDefVar = new Variable(paramName);

        /*
         * Dep'(p_i)=p_i
         */
        SortedSet<Variable> paramDeps = initializeDeps(pCfaEdge, paramDefVar);

        /*
         * Dep'(f)=Dep(f) union Dep(p_i)
         */
        addDep(functionVarDeps, pCfaEdge, functionVar, paramDefVar);

        CExpression paramExpr = paramExprs.get(i);
        visitor = new FunctionCallStatementDependancy();
        paramExpr.accept(visitor);
        SortedSet<Variable> paramExprVars = visitor.getResult();

        for (Variable paramExprVar : paramExprVars) {
          /*
           * a_i
           */

          /*
           * Dep(a_i)={..,b_i,..}
           */
          SortedSet<Variable> paramExprVarDeps = getDeps(stateDeps, pCfaEdge, paramExprVar);

          for (Variable paramExprVarDep : paramExprVarDeps) {
            /*
             * b_i
             */

            /*
             * Dep'(p_i)=Dep(p_i) union Dep(b_i)
             */
            addDep(paramDeps, pCfaEdge, paramDefVar, paramExprVarDep);

            /*
             * Dep'(f)=Dep(f) union Dep(b_i)
             */
            addDep(functionVarDeps, pCfaEdge, functionVar, paramExprVarDep);
          }
        }

        /*
         * Add Dep(p_i)
         */
        setDeps(resultDeps, paramDefVar, paramDeps);
      }

      /*
       * Add Dep(f)
       */
      setDeps(resultDeps, functionVar, functionVarDeps);
    }

    /*
     * Return
     */
    return result;
  }

  @Override
  protected DependencyTrackerState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    /*
     * f::retval[=b];     *
     * Dep(f::retval)=f::retval union [Dep(b)]
     *
     */
    if (pCfaEdge.asAssignment().isPresent()) {
      CAssignment statement = pCfaEdge.asAssignment().get();
      if (statement instanceof CExpressionAssignmentStatement) {
        return handleStmtCEAS(pCfaEdge,(CExpressionAssignmentStatement) statement);
      }
      if (statement instanceof CFunctionCallAssignmentStatement) {
        return handleStmtFCAS(pCfaEdge,(CFunctionCallAssignmentStatement) statement);
      }
    }
    /*
     * Default Return
     */
    return cloneWithPrecision(pCfaEdge);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    assert pState instanceof DependencyTrackerState;
    DependencyTrackerState state = (DependencyTrackerState) pState;

    for (AbstractState aState : pOtherStates) {
      /*
       * 1. Further Statement Adaptions
       */
      strengthen_Statements(state, aState, pCfaEdge, pPrecision);
      /*
       * 2. Control Context Adaption
       */
      strengthen_Context(state, aState, pCfaEdge, pPrecision);
    }

    return Collections.singleton(state);
  }

  @SuppressWarnings("unused")
  protected void strengthen_Statements(DependencyTrackerState pState, AbstractState aState,
      CFAEdge pCfaEdge, Precision pPrecision) {

    /*
     * Further Statement Adaptions
     */

    if (aState instanceof PointerState) {

      /*
       * PointerState
       *
       * a->b         Dep(a)
       * b->Mem[x]    Dep(b)
       *
       * Dep'(a)= Dep(a) union Dep(b)
       * Dep'(b)= Dep(a) union Dep(b)
       *
       */

      DependencyPrecision prec = (DependencyPrecision) pPrecision;
      Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

      PointerState pointerState = (PointerState) aState;
      Map<MemoryLocation, LocationSet> pointsToMap = pointerState.getPointsToMap();
      for (Entry<MemoryLocation, LocationSet> pointsTo : pointsToMap.entrySet()) {

        /*
         * a
         */
        String aName = pointsTo.getKey().getIdentifier();
        Variable aVar = new Variable(aName);

        /*
         * Dep(a)
         */
        SortedSet<Variable> aDeps = stateDeps.get(aVar);

        LocationSet locationSet = pointsTo.getValue();
        for (Variable bVar : stateDeps.keySet()) {

          /*
           * b
           */


          /*
           * a -> b?
           */
          if (locationSet.mayPointTo(MemoryLocation.valueOf(bVar.toString()))) {

            /*
             * Dep(b)
             */
            SortedSet<Variable> bDeps = stateDeps.get(bVar);

            /*
             * Dep(a) != Dep(b)
             */
            if (!(aDeps.equals(bDeps))) {

              /*
               * nDep'(a)= empty
               * nDep'(b)= empty
               */
              SortedSet<Variable> aNewDeps = new TreeSet<>(bDeps);
              SortedSet<Variable> bNewDeps = new TreeSet<>(bDeps);

              /*
               * nDep'(a)= nDep(a) union Dep(a)
               * nDep'(b)= nDep(b) union Dep(a)
               */
              for(Variable aRHSVar:aDeps){
                addDep(prec,aNewDeps,pCfaEdge,aVar,aRHSVar);
                addDep(prec,bNewDeps,pCfaEdge,bVar,aRHSVar);
              }

              /*
               * nDep'(a)= nDep(a) union Dep(b)
               * nDep'(b)= nDep(b) union Dep(b)
               */
              for(Variable bRHSVar:bDeps){
                addDep(prec,aNewDeps,pCfaEdge,aVar,bRHSVar);
                addDep(prec,bNewDeps,pCfaEdge,bVar,bRHSVar);
              }

              /*
               * Add nDep(a)
               */
              setDeps(stateDeps, aVar, aNewDeps);

              /*
               * Add nDep(b)
               */
              setDeps(stateDeps, bVar, bNewDeps);
            }
          }
        }
      }
    }
  }

  protected void strengthen_Context(DependencyTrackerState pState,
      AbstractState aState,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    //Step 2

    if (aState instanceof ControlDependencyTrackerState) {
      ControlDependencyTrackerState cdState=(ControlDependencyTrackerState) aState;
      if (pCfaEdge instanceof CDeclarationEdge) {
        CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
        CDeclaration decl = declEdge.getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          strengthen_Context_DeclarationEdge(pState, cdState, pCfaEdge, pPrecision, decl);
        }
        if (decl instanceof CFunctionDeclaration) {
          strengthen_Context_DeclarationEdge(pState, cdState, pCfaEdge, pPrecision, decl);
        }
        if (decl instanceof CTypeDeclaration) {
          //DO NOTHING
        }
      }
      if (pCfaEdge instanceof CFunctionCallEdge) {
        strengthen_Context_FunctionCallEdge(pState, cdState, (CFunctionCallEdge)pCfaEdge, pPrecision);
      }
      if (pCfaEdge instanceof CFunctionReturnEdge) {
        CFunctionReturnEdge returnEdge = (CFunctionReturnEdge) pCfaEdge;
        CStatement statement = returnEdge.getSummaryEdge().getExpression();
        if (statement instanceof CFunctionCallAssignmentStatement) {
          strengthen_Context_FunctionReturnEdge(pState, cdState, pCfaEdge, pPrecision,
              (CFunctionCallAssignmentStatement) statement);
        }
        if (statement instanceof CFunctionCallStatement) {
          //DO NOTHING
        }
      }
      if (pCfaEdge instanceof CStatementEdge) {
        CStatementEdge statementEdge = ((CStatementEdge) pCfaEdge);
        CStatement statement = statementEdge.getStatement();
        if (statement instanceof CExpressionAssignmentStatement) {
          strengthen_Context_ExpressionAssignmentStatement(pState, cdState, pCfaEdge, pPrecision,
              (CExpressionAssignmentStatement) statement);
        }
        if (statement instanceof CExpressionStatement) {
          //DO NOTHING
        }
        if (statement instanceof CFunctionCallAssignmentStatement) {
          strengthen_Context_FunctionCallAssignmentStatement(pState, cdState, pCfaEdge, pPrecision,
              (CFunctionCallAssignmentStatement) statement);
        }
        if (statement instanceof CFunctionCallStatement) {
          strengthen_Context_FunctionCallStatement(pState, cdState, pCfaEdge, pPrecision,
              (CFunctionCallStatement) statement);
        }
      }
      if (pCfaEdge instanceof CReturnStatementEdge) {
        CReturnStatementEdge rstatementEdge = ((CReturnStatementEdge) pCfaEdge);
        if (rstatementEdge.asAssignment().isPresent()) {

          CAssignment statement = rstatementEdge.asAssignment().get();

          if (statement instanceof CExpressionAssignmentStatement) {
            strengthen_Context_ExpressionAssignmentStatement(pState, cdState, pCfaEdge, pPrecision,
                (CExpressionAssignmentStatement) statement);
          }
          if (statement instanceof CFunctionCallAssignmentStatement) {
            strengthen_Context_FunctionCallAssignmentStatement(pState, cdState, pCfaEdge, pPrecision,
                (CFunctionCallAssignmentStatement) statement);
          }
        }
      }

    }
  }

  private void strengthen_Context_DeclarationEdge(DependencyTrackerState pState,
      ControlDependencyTrackerState pCDState,
      CFAEdge pCfaEdge, Precision pPrecision, CDeclaration pDecl) {

    /*
     * ControlDependancyTrackerState => Declaration
     * if(g_i){
     *  TYPE l=b
     * }
     *
     *  Dep(l)=Dep(l) union g_i
     *
     */
    DependencyPrecision prec = (DependencyPrecision) pPrecision;
    Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

    /*
     * l
     */
    String lhsName = pDecl.getQualifiedName();
    Variable lhsVar = new Variable(lhsName);

    /*
     * Dep(l)
     */
    SortedSet<Variable> lhsDeps = getDeps(prec, stateDeps,pCfaEdge,lhsVar);

    for (Entry<CFANode, SortedSet<Variable>> entry: pCDState.getContexts().entrySet()) {
      SortedSet<Variable> guardVars = entry.getValue();
      for (Variable guardVar : guardVars) {

        /*
         * g_i
         */

        /*
         * Dep(l)=Dep'(l) union g_i
         */
        addDep(prec,lhsDeps,pCfaEdge,lhsVar,guardVar);
      }
    }


    /*
     * Add Dep(l)
     */
    setDeps(stateDeps, lhsVar, lhsDeps);
  }

  @SuppressWarnings("unused")
  private void strengthen_Context_FunctionCallEdge(DependencyTrackerState pState,
      ControlDependencyTrackerState pCDState,
      CFunctionCallEdge pCfaEdge, Precision pPrecision)
      throws CPATransferException {

      /*
       * ControlDependancyTrackerState => CFunctionCallEdge
       *
       * if(g_i){
       *  call f(..,a_i,..)                      #Signature: TYPE f(..,TYPE p_i,..);
       * }
       *
       * Dep'(p_i)=Dep(p_i) union g_i
       */

      DependencyPrecision prec = (DependencyPrecision) pPrecision;
      Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

      List<CParameterDeclaration> parameters = pCfaEdge.getSuccessor().getFunctionParameters();

      for (int i = 0; i < parameters.size(); i++) {

        /*
         * p_i
         */
        CParameterDeclaration paramDecl = parameters.get(i);
        String paramName = paramDecl.getQualifiedName();
        Variable paramVar = new Variable(paramName);

        /*
         * Dep(p_i)=p_i
         */
        SortedSet<Variable> paramDeps = getDeps(prec, stateDeps, pCfaEdge, paramVar);

        for (Entry<CFANode, SortedSet<Variable>> entry: pCDState.getContexts().entrySet()) {
          SortedSet<Variable> guardVars = entry.getValue();
          for (Variable guardVar : guardVars) {

            /*
             * g_i
             */

            /*
             * Dep'(p_i)=Dep(p_i) union g_i
             */
            addDep(prec,paramDeps,pCfaEdge,paramVar,guardVar);
          }
        }


        /*
         * Add Dep(p_i)
         */
        setDeps(stateDeps, paramVar, paramDeps);
      }

  }

  @SuppressWarnings("unused")
  private void strengthen_Context_FunctionReturnEdge(DependencyTrackerState pState,
      ControlDependencyTrackerState pCDState,
      CFAEdge pCfaEdge, Precision pPrecision, CFunctionCallAssignmentStatement pStatement)
      throws CPATransferException {

    /*
     * ControlDependancyTrackerState => CFunctionReturenEdge with CFunctionCallAssignmentStatement
     * if(g_i){
     *  l=f(a_i)
     * }
     *
     * Dep'(l)=Dep(l) union  g_i ;
     *
     */


    DependencyPrecision prec = (DependencyPrecision) pPrecision;
    Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

    CExpression lhsExpr = pStatement.getLeftHandSide();
    VariableDependancy visitor = new VariableDependancy();
    lhsExpr.accept(visitor);
    SortedSet<Variable> lhsVars = visitor.getResult();
    for (Variable lhsVar : lhsVars) {
      /*
       * l
       */

      /*
       * Dep(l)
       */
      SortedSet<Variable> lhsDeps = getDeps(prec, stateDeps,pCfaEdge, lhsVar);

      for (Entry<CFANode, SortedSet<Variable>> entry: pCDState.getContexts().entrySet()) {
        SortedSet<Variable> guardVars = entry.getValue();
        for (Variable guardVar : guardVars) {

          /*
           * g_i
           */

          /*
           * Dep'(l)=Dep(l) union g_i
           */
          addDep(prec,lhsDeps,pCfaEdge,lhsVar,guardVar);
        }
      }

      /*
       * Add Dep(l)
       */
      setDeps(stateDeps, lhsVar, lhsDeps);
    }

  }

  private void strengthen_Context_ExpressionAssignmentStatement(DependencyTrackerState pState,
      ControlDependencyTrackerState pCDState,
      CFAEdge pCfaEdge, Precision pPrecision, CExpressionAssignmentStatement pStatement)
      throws CPATransferException {
    /*
     * ControlDependancyTrackerState => Statement
     * if(g_i){
     *  l=b
     * }
     *
     * Dep'(l)=Dep(l) union g_i
     *
     */
    DependencyPrecision prec = (DependencyPrecision) pPrecision;
    Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

    CExpression lhsExpr = pStatement.getLeftHandSide();
    VariableDependancy visitor = new VariableDependancy();
    lhsExpr.accept(visitor);
    SortedSet<Variable> lhsVars = visitor.getResult();

    for (Variable lhsVar : lhsVars) {
      /*
       * l
       */

      /*
       * Dep(l)
       */
      SortedSet<Variable> lhsDeps = getDeps(prec,stateDeps,pCfaEdge,lhsVar);

      for (Entry<CFANode, SortedSet<Variable>> entry: pCDState.getContexts().entrySet()) {
        SortedSet<Variable> guardVars = entry.getValue();
          for (Variable guardVar : guardVars) {

            /*
             * g_i
             */

            /*
             * Dep(l)=Dep'(l) union g_i
             */
            addDep(prec,lhsDeps,pCfaEdge,lhsVar,guardVar);
          }
        }
      /*
       * Add Dep(l)
       */
      setDeps(stateDeps, lhsVar, lhsDeps);
    }
  }

  private void strengthen_Context_FunctionCallAssignmentStatement(DependencyTrackerState pState,
      ControlDependencyTrackerState pCDState,
      CFAEdge pCfaEdge, Precision pPrecision, CFunctionCallAssignmentStatement pStatement)
      throws CPATransferException {

      /*
       * if(g_i){             #Signature: TYPE f(..,TYPE p_i,..);
       *  l=f(a_i);
       * }
       *
       * Dep'(l)=Dep(l) union g_i ;
       * Dep'(f)=Dep(f) union g_i ;
       * Dep'(p_i)=Dep(p_i) union g_i ;
       *
       */

        DependencyPrecision prec = (DependencyPrecision) pPrecision;
        Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

        FunctionCallStatementDependancy visitor;

        CLeftHandSide lhsExpr = pStatement.getLeftHandSide();
        visitor = new FunctionCallStatementDependancy();
        lhsExpr.accept(visitor);
        SortedSet<Variable> lhsVars = visitor.getResult();

        CFunctionCallExpression functionCallExpr = pStatement.getFunctionCallExpression();
        CExpression functionExpr = functionCallExpr.getFunctionNameExpression();
        visitor = new FunctionCallStatementDependancy();
        functionExpr.accept(visitor);
        SortedSet<Variable> functionVars = visitor.getResult();

        List<CParameterDeclaration> paramDefs =
            pStatement.getRightHandSide().getDeclaration().getParameters();
        List<CExpression> paramExprs = pStatement.getRightHandSide().getParameterExpressions();

    assert (!(paramDefs != null && paramExprs != null) || (paramDefs.size() == paramExprs.size()));

        for (Variable lhsVar : lhsVars) {
          /*
           * l
           */

          /*
           * Dep(l)
           */
          SortedSet<Variable> lhsDeps = getDeps(prec, stateDeps,pCfaEdge, lhsVar);

          for (Variable functionVar : functionVars) {
            /*
             * f
             */

            /*
             * Dep(f)
             */
            SortedSet<Variable> functionVarDeps = getDeps(prec, stateDeps, pCfaEdge, functionVar);

            for (int i = 0; i < paramDefs.size(); i++) {

              /*
               * p_i
               */
              String paramName = paramDefs.get(i).asVariableDeclaration().getQualifiedName();
              Variable paramDefVar = new Variable(paramName);

              /*
               * Dep'(p_i)
               */
              SortedSet<Variable> paramDeps = getDeps(prec, stateDeps, pCfaEdge, paramDefVar);

              for (Entry<CFANode, SortedSet<Variable>> entry: pCDState.getContexts().entrySet()) {
                SortedSet<Variable> guardVars = entry.getValue();
                  for (Variable guardVar : guardVars) {

                    /*
                     * g_i
                     */

                    /*
                     * Dep'(p_i)=Dep(p_i) union g_i
                     */
                    addDep(prec,paramDeps,pCfaEdge,paramDefVar,guardVar);
                    /*
                     * Dep'(f)=Dep(f) union g_i
                     */
                    addDep(prec,functionVarDeps,pCfaEdge,functionVar,guardVar);
                    /*
                     * Dep'(l)=Dep(l) union g_i
                     */
                    addDep(prec,lhsDeps,pCfaEdge,lhsVar,guardVar);
                  }
                }
              /*
               * Add Dep(p_i)
               */
              setDeps(stateDeps, paramDefVar, paramDeps);
            }

            /*
             * Add Dep(f)
             */
            setDeps(stateDeps, functionVar, functionVarDeps);
          }

          /*
           * Add Dep(l)
           */
          setDeps(stateDeps, lhsVar, lhsDeps);
        }
  }



  @SuppressWarnings("unused")
  private void strengthen_Context_FunctionCallStatement(DependencyTrackerState pState,
      ControlDependencyTrackerState pCDState,
      CFAEdge pCfaEdge, Precision pPrecision, CFunctionCallStatement pStatement)
      throws CPATransferException {

    /*
     * ControlDependancyTrackerState => CFunctionCallStatement
     *
     * if(g_j){
     *  f(..,a_i,..)                      #Signature: TYPE f(..,TYPE p_i,..);
     * }
     *
     * Dep'(p_i) = Dep(p_i) union  g_j ;
     * Dep'(f) = Dep(f) union  g_j ;
     *
     */

    DependencyPrecision prec = (DependencyPrecision) pPrecision;
    Map<Variable, SortedSet<Variable>> stateDeps = pState.getDependencies();

    FunctionCallStatementDependancy visitor;

    CFunctionCallExpression functionCallExpr = pStatement.getFunctionCallExpression();
    CExpression functionExpr = functionCallExpr.getFunctionNameExpression();
    visitor = new FunctionCallStatementDependancy();
    functionExpr.accept(visitor);
    SortedSet<Variable> functionVars = visitor.getResult();

    List<CParameterDeclaration> paramDefs =
        pStatement.getFunctionCallExpression().getDeclaration().getParameters();

    for (Variable functionVar : functionVars) {
      /*
       * f
       */

      /*
       * Dep(f)
       */
      SortedSet<Variable> functionVarDeps = getDeps(prec, stateDeps, pCfaEdge, functionVar);

      for (int i = 0; i < paramDefs.size(); i++) {

        /*
         * p_i
         */
        String paramName = paramDefs.get(i).asVariableDeclaration().getQualifiedName();
        Variable paramDefVar = new Variable(paramName);

        /*
         * Dep(p_i)
         */
        SortedSet<Variable> paramDeps = getDeps(prec, stateDeps, pCfaEdge, paramDefVar);

        for (Entry<CFANode, SortedSet<Variable>> entry: pCDState.getContexts().entrySet()) {
          SortedSet<Variable> guardVars = entry.getValue();
            for (Variable guardVar : guardVars) {

              /*
               * g_i
               */

              /*
               * Dep'(p_i)=Dep(p_i) union g_i
               */
              addDep(prec,paramDeps,pCfaEdge,paramDefVar,guardVar);

              /*
               * Dep'(f)=Dep(f) union g_i
               */
              addDep(prec,functionVarDeps,pCfaEdge,functionVar,guardVar);
            }
          }
        /*
         * Add Dep(p_i)
         */
        setDeps(stateDeps, paramDefVar, paramDeps);
      }

      /*
       * Add Dep(f)
       */
      setDeps(stateDeps, functionVar, functionVarDeps);
    }
  }

  @SuppressWarnings("unused")
  protected void strengthen_Precision(DependencyTrackerState pState,
      AbstractState aState,
      CFAEdge pCfaEdge, Precision pPrecision) {
    //Step 3
    DepPrecision precision=null;
    if (pPrecision instanceof ImplicitDependencyPrecision) {
      precision=(ImplicitDependencyPrecision) pPrecision;
    }
    if (pPrecision instanceof AllTrackingPrecision) {
      precision=(AllTrackingPrecision) pPrecision;
    }

    Map<Variable, SortedSet<Variable>> deps = pState.getDependencies();
    SortedSet<Variable> removeset = new TreeSet<>();
    for (Entry<Variable, SortedSet<Variable>> dep : deps.entrySet()) {
      Variable var = dep.getKey();
      SortedSet<Variable> vars = dep.getValue();
      if (!(precision.isTrackingNecessary(var, vars))) {
        removeset.add(var);
      }
    }
    for (Variable rvar : removeset) {
      deps.remove(rvar);
    }
  }



}
