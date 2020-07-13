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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.ProverEnvironment;

//TODO implement shutdown notifier handling.
public class SLTransferRelation
    extends ForwardingTransferRelation<Collection<SLState>, SLState, Precision> {

  private final LogManager logger;

  private final Solver solver;
  private final SLVisitor slVisitor;
  private final SLHeapDelegate memDel;

  public SLTransferRelation(
      LogManager pLogger,
      SLStatistics pStats,
      Solver pSolver,
      PathFormulaManager pPfm,
      MachineModel pMachineModel) {
    logger = pLogger;
    solver = pSolver;
    memDel = new SLHeapDelegateImpl(logger, pStats, pSolver, pMachineModel, pPfm);
    slVisitor = new SLVisitor(memDel);
  }

  @Override
  protected void
      setInfo(AbstractState pAbstractState, Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
    state = state.copyWithoutErrors();
    memDel.setContext(state, pCfaEdge, functionName);
  }

  @Override
  protected Collection<SLState> postProcessing(Collection<SLState> pSuccessor, CFAEdge pEdge) {
    for (SLState slState : pSuccessor) {
      slState.setPathFormula(memDel.getPathFormula());
      Set<CSimpleDeclaration> vars = pEdge.getSuccessor().getOutOfScopeVariables();
      for (CSimpleDeclaration outOfScopeVar : vars) {
        try {
          SLStateError error = memDel.handleOutOfScopeVariable(outOfScopeVar);
          if (error != null) {
            slState.addError(error);
          }
        } catch (Exception e) {
          logger.log(Level.SEVERE, "OutOfScopeVariable: " + e.getMessage());
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
    memDel.clearContext();
  }

  @Override
  protected @Nullable Collection<SLState>
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {
    SLStateError error = null;
    try {
      error = pExpression.accept(slVisitor);
      if (error != null) {
        state.addError(error);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }

    ProverEnvironment prover = solver.newProverEnvironment();
    boolean unsat = false;
    try {
      prover.addConstraint(state.getPathFormula().getFormula());
      unsat = prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    if(unsat) {
      return Collections.emptyList();
    }
    return ImmutableList.of(state);
  }

  @Override
  protected Collection<SLState> handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {

    for (CParameterDeclaration cParameterDeclaration : pParameters) {
      SLStateError error = null;
      try {
        error = cParameterDeclaration.accept(slVisitor);
        if (error != null) {
          state.addError(error);
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, e.getMessage());
      }
    }
    return ImmutableList.of(state);
  }

  @Override
  protected Collection<SLState> handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    CIdExpression idExp = (CIdExpression) pSummaryExpr.getFunctionCallExpression().getFunctionNameExpression();
    String callee = idExp.getName();
    memDel.handleFunctionReturn(callee);
    return Collections
        .singleton(state);
  }

  @Override
  protected List<SLState>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    SLStateError error = null;
    try {
      error = pDecl.accept(slVisitor);
      if (error != null) {
        state.addError(error);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(state);
  }

  @Override
  protected List<SLState>
      handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
          throws CPATransferException {

    SLStateError error = null;
    try {
      error = pStatement.accept(slVisitor);
      if (error != null) {
        state.addError(error);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(state);
  }

  @Override
  protected Collection<SLState> handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return ImmutableList.of(state);
  }

  @Override
  protected Set<SLState> handleBlankEdge(BlankEdge pCfaEdge) {
    return Collections.singleton(state);
  }
}
