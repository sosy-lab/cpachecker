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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

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
      MachineModel pMachineModel) {
    logger = pLogger;
    solver = pSolver;
    fm = solver.getFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();
    pfm = pPfm;
    memDel = new SLHeapDelegateImpl(logger, solver, pMachineModel, this);
    slVisitor = new SLVisitor(memDel);
  }

  @Override
  protected void
      setInfo(AbstractState pAbstractState, Precision pAbstractPrecision, CFAEdge pCfaEdge) {
    super.setInfo(pAbstractState, pAbstractPrecision, pCfaEdge);
    try {
      pathFormula = pfm.makeAnd(getPredPathFormula(), pCfaEdge);
    } catch (CPATransferException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    edge = pCfaEdge;
  }

  @Override
  protected Collection<SLState> postProcessing(Collection<SLState> pSuccessor, CFAEdge pEdge) {
    for (SLState slState : pSuccessor) {

      Set<CSimpleDeclaration> vars = pEdge.getSuccessor().getOutOfScopeVariables();
      for (CSimpleDeclaration outOfScopeVar : vars) {
        try {
          SLStateError error = memDel.handleOutOfScopeVariable(outOfScopeVar);
          if (error != null) {
            slState.setTarget(error);
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
        .of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), error));
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
        .singleton(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), null));
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
        .singleton(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), null));
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
        .of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), error));
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
        .of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), error));
  }

  @Override
  protected Collection<SLState> handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return ImmutableList.of(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), null));
  }

  @Override
  protected Set<SLState> handleBlankEdge(BlankEdge pCfaEdge) {
    return Collections
        .singleton(new SLState(pathFormula, memDel.getHeap(), memDel.getStack(), null));
  }

  @Override
  public BigInteger getValueForCExpression(CExpression pExp, boolean usePredContext)
      throws Exception {
    PathFormula context = usePredContext ? getPredPathFormula() : pathFormula;
    Formula f = pfm.expressionToFormula(context, pExp, edge);
    final String dummyVarName = "0_allocationSize"; // must not be a valid C variable name.
    f = fm.makeEqual(bvfm.makeVariable(32, dummyVarName), f);

    try (ProverEnvironment env = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      env.addConstraint(context.getFormula());
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
      getFormulaForVariableName(String pVariable, boolean usePredContext) {
    PathFormula context = usePredContext ? getPredPathFormula() : pathFormula;
    // String var = addFctScope ? functionName + "::" + pVariable : pVariable;
    CType type = context.getSsa().getType(pVariable);
    return pfm.makeFormulaForVariable(context, pVariable, type);
  }

  @Override
  public Formula getFormulaForDeclaration(CSimpleDeclaration pDecl) {
    return pfm.makeFormulaForVariable(pathFormula, pDecl.getQualifiedName(), pDecl.getType());
  }

  @Override
  public Formula getFormulaForExpression(CExpression pExp, boolean usePredContext)
      throws UnrecognizedCodeException {
    PathFormula context = usePredContext ? getPredPathFormula() : pathFormula;
    return pfm.expressionToFormula(context, pExp, edge);
  }

  @Override
  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public PathFormula getPredPathFormula() {
    return state.getPathFormula();
  }

  @Override
  public void updateSSAMap(SSAMap pMap) {
    pathFormula = pfm.makeNewPathFormula(pathFormula, pMap); // TODO
  }
}
