/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.sl;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;

public class SLTransferRelation
    extends ForwardingTransferRelation<Collection<SLState>, SLState, Precision>
    implements SLFormulaBuilder {

  private final LogManager logger;
  private final Solver solver;
  private final PathFormulaManager pfm;
  private final FormulaManagerView fm;
  private final BitvectorFormulaManager bvfm;

  private final SLVisitor slVisitor;
  private final SLHeapDelegate memDel;

  private PathFormula pathFormula;
  private CFAEdge edge;


  public SLTransferRelation(
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPfm,
      SLHeapDelegate pMemDel) {
    logger = pLogger;
    solver = pSolver;
    fm = solver.getFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();
    pfm = pPfm;
    memDel = pMemDel;
    slVisitor = new SLVisitor(this, pMemDel);
  }

  @Override
  protected void
      setInfo(AbstractState pAbstractState, Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);

    // pathFormulaPred = pfm.makeAnd(state.getPathFormula(), bfm.makeTrue()); // clone
    try {
      pathFormula = pfm.makeAnd(state.getPathFormula(), pCfaEdge);
    } catch (CPATransferException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    edge = pCfaEdge;
    slVisitor.setPathFormulae(pathFormula, state.getPathFormula());
  }

  @Override
  protected Collection<SLState> postProcessing(Collection<SLState> pSuccessor, CFAEdge pEdge) {
    for (SLState slState : pSuccessor) {

      Set<CSimpleDeclaration> vars = pEdge.getSuccessor().getOutOfScopeVariables();
      for (CSimpleDeclaration outOfScopeVar : vars) {
        CType varType = outOfScopeVar.getType();
        Formula f = getFormulaForVariableName(outOfScopeVar.getName(), true, pathFormula);
        if (varType instanceof CPointerType || varType instanceof CArrayType) {
          // Check if a pointer to allocated heap memory is dropped.
          try {
            Formula loc = memDel.checkAllocation(f, pathFormula);
            if (loc != null) {
              // Check if a copy of the dropped heap pointer exists.
              boolean eqFound = false;
              for (CSimpleDeclaration inScopePtr : slVisitor.getInScopePtrs()) {
                Formula varFormula =
                    getFormulaForVariableName(inScopePtr.getName(), true, pathFormula);
                if (!varFormula.equals(f)
                    && memDel.checkEquivalence(loc, varFormula, pathFormula)) {
                  eqFound = true;
                  break;
                }
              }
              if (!eqFound) { //
                slState.setTarget(SLStateError.UNFREED_MEMORY);
              }
            }

          } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
          }
        }

        // Remove from heap.
        try {
          CExpression e = SLHeapDelegate.createSymbolicLocation(outOfScopeVar);
          Formula loc = getFormulaForExpression(e, pathFormula);
          memDel.removeFromHeap(loc);
          if (varType instanceof CArrayType) {
            memDel.removeFromHeap(f);
          }
        } catch (Exception e1) {
          logger.log(Level.SEVERE, e1.getMessage());
        }
      }

      String info = "";
      info += pEdge.getCode() + "\n";
      info += slState + "\n";
      info += "---------------------------";
      logger.log(Level.INFO, info);
    }

    return pSuccessor;
  }

  @Override
  protected void resetInfo() {
    super.resetInfo();
    slVisitor.setPathFormulae(null, null);
    pathFormula = null;
    edge = null;
  }

  @Override
  protected @Nullable Collection<SLState>
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {
    SLStateError error = null;
    try {
      error = pExpression.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(new SLState(pathFormula, memDel.getHeap(), error));
  }

  @Override
  protected Collection<SLState> handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    // TODO Auto-generated method stub. Not yet implemented.
    // return super.handleFunctionCallEdge(pCfaEdge, pArguments, pParameters, pCalledFunctionName);
    return Collections
        .singleton(new SLState(pathFormula, memDel.getHeap(), null));
  }

  @Override
  protected Collection<SLState> handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    // TODO Auto-generated method stub. Not yet implemented.
    // return super.handleFunctionReturnEdge(pCfaEdge, pFnkCall, pSummaryExpr, pCallerFunctionName);
    return Collections
        .singleton(new SLState(pathFormula, memDel.getHeap(), null));
  }

  @Override
  protected List<SLState>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    SLStateError error = null;
    try {
      error = pDecl.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(new SLState(pathFormula, memDel.getHeap(), error));
  }

  @Override
  protected List<SLState>
      handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
          throws CPATransferException {

    SLStateError error = null;
    try {
      error = pStatement.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(new SLState(pathFormula, memDel.getHeap(), error));
  }

  @Override
  protected Collection<SLState> handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return ImmutableList.of(new SLState(pathFormula, memDel.getHeap(), null));
  }

  @Override
  protected Set<SLState> handleBlankEdge(BlankEdge pCfaEdge) {
    return Collections
        .singleton(new SLState(pathFormula, memDel.getHeap(), null));
  }

  @Override
  public BigInteger getValueForCExpression(CExpression pExp, PathFormula pContext)
      throws Exception {
    Formula f = pfm.expressionToFormula(pContext, pExp, edge);
    final String dummyVarName = "0_allocationSize";
    f = fm.makeEqual(bvfm.makeVariable(32, dummyVarName), f);

    try(ProverEnvironment env = solver.newProverEnvironment()) {
      env.addConstraint(pContext.getFormula());
      env.addConstraint((BooleanFormula) f);
      if (!env.isUnsat()) {
        List<ValueAssignment> assignments = env.getModelAssignments();
        for (ValueAssignment a : assignments) {
          if (a.getKey().toString().equals(dummyVarName)) {
            return (BigInteger) a.getValue();
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    logger.log(
        Level.SEVERE,
        "Numeric value of expression " + pExp.toString() + " could not be determined.");
    return null;
  }


  @Override
  public Formula
      getFormulaForVariableName(String pVariable, boolean addFctScope, PathFormula pContext) {

    String var = addFctScope ? functionName + "::" + pVariable : pVariable;
    CType type = pContext.getSsa().getType(var);
    return pfm.makeFormulaForVariable(pContext, var, type);
  }

  @Override
  public Formula getFormulaForExpression(CExpression pExp, PathFormula pContext)
      throws UnrecognizedCodeException {
    return pfm.expressionToFormula(pContext, pExp, edge);
  }
}
