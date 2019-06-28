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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
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
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;

public class SLTransferRelation
    extends ForwardingTransferRelation<Collection<SLState>, SLState, Precision>
    implements SLHeapDelegate {

  private final LogManager logger;
  private FormulaManagerView fm;
  private PathFormulaManager pfm;
  private BooleanFormulaManager bfm;
  private IntegerFormulaManager ifm;
  private BitvectorFormulaManager bvfm;
  private Solver solver;
  private final SLVisitor slVisitor;

  private String currentFunctionScope;
  private PathFormula pathFormula;
  private PathFormula pathFormulaPrev;

  /**
   * Modeling a heap of chars.
   */
  private Map<Formula, Formula> heap;

  private Map<Formula, BigInteger> allocationSizes;
  private Map<String, Formula> stack;
  private CFAEdge edge;


  public SLTransferRelation(LogManager pLogger) {
    logger = pLogger;
    slVisitor = new SLVisitor(this);
    allocationSizes = new HashMap<>();
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
    heap = new HashMap<>(state.getHeap());
    stack = new HashMap<>(state.getStack());
    edge = pCfaEdge;
  }

  @Override
  protected Collection<SLState> postProcessing(Collection<SLState> pSuccessor, CFAEdge pEdge) {
    String info = "";
    info += pEdge.getCode() + "\n";
    info += pSuccessor.iterator().next() + "\n";
    info += "---------------------------";
    logger.log(Level.INFO, info);
    return pSuccessor;
  }

  @Override
  protected void resetInfo() {
    super.resetInfo();
    pathFormula = null;
    pathFormulaPrev = null;
    heap = null;
    edge = null;
  }


  @Override
  protected List<SLState>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    boolean isTarget = false;
    try {
      isTarget = pDecl.accept(slVisitor);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return ImmutableList.of(new SLState(pathFormula, heap, stack, isTarget));
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
    return ImmutableList.of(new SLState(pathFormula, heap, stack, isTarget));
  }

  @Override
  protected Collection<SLState> handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    setFunctionScope(null);
    return ImmutableList.of(new SLState(pathFormula, heap, stack, false));
  }

  @Override
  protected Set<SLState> handleBlankEdge(BlankEdge pCfaEdge) {
    return Collections.singleton(new SLState(pathFormula, heap, stack, false));
  }

  public void setPathFormulaManager(PathFormulaManager pPfm) {
    pfm = pPfm;
  }

  public void setSolver(Solver pSolver) {
    solver = pSolver;
  }

  public void setFormulaManager(FormulaManagerView pFMgr) {
    fm = pFMgr;
    bfm = fm.getBooleanFormulaManager();
    ifm = fm.getIntegerFormulaManager();
    // slfm = fm.getSLFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();
  }

  private String getSSAVarName(String pVarName) {
    return currentFunctionScope + "::" + pVarName;
  }

  private Formula getFormulaForVarName(String pVarName) {
    String var = getSSAVarName(pVarName);
    CType type = pathFormula.getSsa().getType(var);
    Formula f = pfm.makeFormulaForVariable(pathFormula, var, type);
    return f;
  }

  // -------------------------------------------------------------------------------------------------
  // Delegate methods starting
  // here.------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------
  @Override
  public void addToHeap(String pVarName, BigInteger size) {
    for (int i = 0; i < size.intValueExact(); i++) {
      Formula f = getFormulaForVarName(pVarName);
      if (i > 0) {
        f = bvfm.add((BitvectorFormula) f, bvfm.makeBitvector(size.bitLength(), i));
      } else {
        allocationSizes.put(f, size);
      }
      heap.put(f, ifm.makeNumber(0));
    }
  }

  @Override
  public void removeFromHeap(Formula pAddrFormula) {
    BigInteger size = allocationSizes.get(pAddrFormula);
    for (int i = 0; i < size.intValueExact(); i++) {
      if (i == 0) {
        heap.remove(pAddrFormula);
      } else {
        Formula tmp =
            bvfm.add((BitvectorFormula) pAddrFormula, bvfm.makeBitvector(size.bitLength(), i));
        heap.remove(tmp);
      }

    }

  }

  /**
   * Determines the numeric value of the @CExpression used as a parameter in a malloc() function
   * call.
   */
  @SuppressWarnings("resource")
  @Override
  public BigInteger getAllocationSize(CExpression pExp) throws Exception {
    Formula f = pfm.expressionToFormula(pathFormulaPrev, pExp, edge);
    final String tmpVarName = "0_allocationSize";
    f = fm.makeEqual(bvfm.makeVariable(32, tmpVarName), f);

    ProverEnvironment env = null;
    try {
      env = solver.newProverEnvironment();
      env.addConstraint(pathFormulaPrev.getFormula());
      env.addConstraint((BooleanFormula) f);
      if (!env.isUnsat()) {
        List<ValueAssignment> assignments = env.getModelAssignments();
        for (ValueAssignment a : assignments) {
          if (a.getKey().toString().equals(tmpVarName)) {
            return (BigInteger) a.getValue();
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      if (env != null) {
        env.close();
      }
    }
    logger.log(
        Level.SEVERE,
        "Numeric value of expression " + pExp.toString() + " could not be determined.");
    return null;
  }

  @Override
  public void setFunctionScope(String pScope) {
    currentFunctionScope = pScope;
  }

  @SuppressWarnings("resource")
  @Override
  public Formula checkAllocation(CExpression pAddrExp, CExpression pOffset, CExpression pValExp)
      throws Exception {
    Formula fAddr = null;
    Formula fVal = null;
    try {
      fAddr = pfm.expressionToFormula(pathFormulaPrev, pAddrExp, edge);
      if (pOffset != null) {
        Formula fOffset = pfm.expressionToFormula(pathFormulaPrev, pOffset, edge);
        fAddr = fm.makePlus(fAddr, fOffset);
      }
      if (pValExp != null) {
        fVal = pfm.expressionToFormula(pathFormulaPrev, pValExp, edge);
      }
    } catch (UnrecognizedCodeException e1) {
      return null;
    }

    if (heap.containsKey(fAddr)) {
      if (fVal != null) {
        heap.put(fAddr, fVal);
      }
      // Syntactical check for performance.
      return fAddr;
    }

    // Semantical check.
    for (Formula formulaOnHeap : heap.keySet()) {
      ProverEnvironment env = solver.newProverEnvironment();
      env.addConstraint(pathFormulaPrev.getFormula());
      Formula tmp = fm.makeEqual(fAddr, formulaOnHeap);
      env.addConstraint((BooleanFormula) tmp);
      if (!env.isUnsat()) {
        env.close();
        if (fVal != null) {
          heap.put(formulaOnHeap, fVal);
        }
        return formulaOnHeap;
      }
      env.close();
    }
    return null;
  }
}
