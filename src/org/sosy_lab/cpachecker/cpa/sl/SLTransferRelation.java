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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;

public class SLTransferRelation
    extends ForwardingTransferRelation<Collection<SLState>, SLState, Precision>
    implements SLSolverDelegate {

  private final LogManager logger;
  private final Solver solver;
  private final PathFormulaManager pfm;
  private final FormulaManagerView fm;
  private final BooleanFormulaManager bfm;
  private final BitvectorFormulaManager bvfm;

  private final SLVisitor slVisitor;
  private final SLMemoryDelegate memDel;

  private PathFormula pathFormula;
  private PathFormula pathFormulaPrev;
  private CFAEdge edge;


  public SLTransferRelation(
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPfm,
      SLMemoryDelegate pMemDel) {
    logger = pLogger;
    solver = pSolver;
    fm = solver.getFormulaManager();
    bfm = fm.getBooleanFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();
    pfm = pPfm;
    memDel = pMemDel;
    slVisitor = new SLVisitor(this, pMemDel);
  }

  @Override
  protected void
      setInfo(AbstractState pAbstractState, Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
    pathFormulaPrev = pfm.makeAnd(state.getPathFormula(), bfm.makeTrue());
    try {
      pathFormula = pfm.makeAnd(state.getPathFormula(), pCfaEdge);
    } catch (CPATransferException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    edge = pCfaEdge;

  }

  @Override
  protected Collection<SLState> postProcessing(Collection<SLState> pSuccessor, CFAEdge pEdge) {
    for (SLState slState : pSuccessor) {
      Set<CSimpleDeclaration> vars = pEdge.getSuccessor().getOutOfScopeVariables();
      for (CSimpleDeclaration var : vars) {
        Formula fHeap = getFormulaForVariableName(var.getName(), false, true);
        try {
          Formula loc = memDel.checkAllocation(this, fHeap);
          slState.setTarget(loc != null);
        } catch (Exception e) {
          logger.log(Level.SEVERE, e.getMessage());
        }
        memDel.removeFromStack(getFormulaForVariableName(var.getName(), false, false));
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
    pathFormula = null;
    pathFormulaPrev = null;
    edge = null;
  }

  @Override
  protected @Nullable Collection<SLState>
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {
    boolean isTarget = true;
    try {
      isTarget = pExpression.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), isTarget));
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
        .singleton(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), false));
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
        .singleton(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), false));
  }

  @Override
  protected List<SLState>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    boolean isTarget = true;
    try {
      isTarget = pDecl.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), isTarget));
  }

  @Override
  protected List<SLState>
      handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
          throws CPATransferException {

    boolean isTarget = true;
    try {
      isTarget = pStatement.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList
        .of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), isTarget));
  }

  @Override
  protected Collection<SLState> handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return ImmutableList.of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), false));
  }

  @Override
  protected Set<SLState> handleBlankEdge(BlankEdge pCfaEdge) {
    return Collections
        .singleton(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), false));
  }

  @Override
  public BigInteger getValueForCExpression(CExpression pExp) throws Exception {
    Formula f = pfm.expressionToFormula(pathFormulaPrev, pExp, edge);
    final String dummyVarName = "0_allocationSize";
    f = fm.makeEqual(bvfm.makeVariable(32, dummyVarName), f);

    try(ProverEnvironment env = solver.newProverEnvironment()) {
      env.addConstraint(pathFormulaPrev.getFormula());
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
  public boolean checkEquivalence(Formula pF0, Formula pF1) {
    try (ProverEnvironment env = solver.newProverEnvironment()) {
      env.addConstraint(pathFormulaPrev.getFormula());
      BooleanFormula tmp = fm.makeEqual(pF0, pF1);
      env.addConstraint(tmp);
      return !env.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return false;
  }

  @Override
  public Formula
      getFormulaForVariableName(String pVariable, boolean isGlobal, boolean addSSAIndex) {
      String var = isGlobal ? pVariable : functionName + "::" + pVariable;
      CType type = pathFormula.getSsa().getType(var);
    Formula f;
    if (addSSAIndex) {
      f = pfm.makeFormulaForVariable(pathFormula, var, type);
    } else {
      f = pfm.makeFormulaForUninstantiatedVariable(var, type, null, false);
    }
      return f;
  }

  @Override
  public Formula getFormulaForExpression(CExpression pExp, boolean onLHS)
      throws UnrecognizedCodeException {
    return onLHS
        ? pfm.expressionToFormula(pathFormula, pExp, edge)
        : pfm.expressionToFormula(pathFormulaPrev, pExp, edge);
  }
}
