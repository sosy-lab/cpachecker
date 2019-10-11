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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.formula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class FormulaRelation
    extends ForwardingTransferRelation<FormulaState, FormulaState, Precision> {

  // TODO replace the formula relation by a more sophisticated version in the near future
  @SuppressWarnings("unused")
  private Configuration config;
  private Solver solver;

  public Solver getSolver() {
    return solver;
  }

  private CtoFormulaConverter converter;
  private SSAMapBuilder ssaBuilder;

  protected SSAMapBuilder getSsaBuilder() {
    return ssaBuilder;
  }

  private FormulaManagerView formulaManager;

  public FormulaManagerView getFormulaManager() {
    return formulaManager;
  }

  private PathFormulaManager pathFormulaManager;

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }

  public CtoFormulaConverter getConverter() {
    return this.converter;
  }

  // private Map<CFANode, PathFormula> computedPathFormulae = new HashMap<>();
  // private Map<CFANode, Boolean> explored = new HashMap<>();

  //
  @SuppressWarnings("unused")
  private LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  CtoFormulaTypeHandler typeHandler;
  /**
   * Internal Variable: Control Dependencies
   */
  // private Map<CFANode, TreeSet<CFANode>> rcd;

  public FormulaRelation(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {

    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    solver = Solver.create(config, logger, pShutdownNotifier);
    formulaManager = solver.getFormulaManager();

    FormulaEncodingOptions options = new FormulaEncodingOptions(config);
    typeHandler = new CtoFormulaTypeHandler(pLogger, pCfa.getMachineModel());
    converter =
        new CtoFormulaConverter(
            options,
            formulaManager,
            pCfa.getMachineModel(),
            pCfa.getVarClassification(),
            logger,
            shutdownNotifier,
            typeHandler,
            AnalysisDirection.FORWARD);

    ssaBuilder = SSAMap.emptySSAMap().builder();

    pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManager,
            config,
            logger,
            shutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
  }

  public FormulaState makeInitial() {
    FormulaState initialstate = new FormulaState(pathFormulaManager.makeEmptyPathFormula(), this);
    return initialstate;
  }

  @Override
  protected FormulaState
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {
    PathFormula newformula1 = null;
    try {
      newformula1 = pathFormulaManager.makeAnd(state.getPathFormula(), pCfaEdge);
    } catch (InterruptedException e) {
    }
    FormulaState result = new FormulaState(newformula1, this);
    result.whilebefore = state.whilebefore;
    result.formulabefore = state.formulabefore;
    if (pCfaEdge.getPredecessor() != null) {
      CFANode pre = pCfaEdge.getPredecessor();
      int length = pre.getNumEnteringEdges();
      for (int i = 0; i < length; i++) {
        CFAEdge preEdge = pre.getEnteringEdge(i);
        if (preEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
            && preEdge.getDescription().contains("while")) {

          // WHILE

          if (pCfaEdge.getTruthAssumption()) {
            try {
              if (state.whilebefore == null) {
                // TODO: Enhance logging
                throw new CPATransferException(
                    "Abort the computation, since an interal error occured");
              }
              SSAMap map1 = state.whilebefore;
              SSAMapBuilder mb1 = map1.builder();
              for (String v : map1.allVariables()) {
                mb1.setIndex(v, mb1.getType(v), mb1.getIndex(v) + 1);
              }
              SSAMap newmap1 = mb1.build();

              newformula1 =
                  new PathFormula(
                      state.formulabefore.getFormula(),
                      newmap1,
                      state.formulabefore.getPointerTargetSet(),
                      state.formulabefore.getLength());

              newformula1 = pathFormulaManager.makeAnd(newformula1, pCfaEdge);

            } catch (InterruptedException e) {

            }

            result = new FormulaState(newformula1, this);
            result.whilebefore = state.whilebefore;
            result.formulabefore = state.formulabefore;
          } else {
            // The non-loop part is not considered here
          }
        }
        // For each case
        // TODO: remove redundant code
        else if (pre.isLoopStart()) {
          // Since we are in a for each case, there is a statement executed before. hence, look two
          // statements up:
          for (int j = 0; j < preEdge.getPredecessor().getNumEnteringEdges(); j++) {
            CFAEdge prepreEdge = preEdge.getPredecessor().getEnteringEdge(j);
            if (prepreEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
                && prepreEdge.getDescription().contains("for")) {
              // For loop
              if (pCfaEdge.getTruthAssumption()) {
                try {
                  SSAMap map1 = state.whilebefore;
                  SSAMapBuilder mb1 = map1.builder();
                  for (String v : map1.allVariables()) {
                    mb1.setIndex(v, mb1.getType(v), mb1.getIndex(v) + 1);
                  }
                  SSAMap newmap1 = mb1.build();

                  if (state.formulabefore == null) {
                    throw new CPATransferException(
                        "The formulaBefore needs to be set, hence the transformation is aborted");
                  }
                  newformula1 =
                      new PathFormula(
                          state.formulabefore.getFormula(),
                          newmap1,
                          state.formulabefore.getPointerTargetSet(),
                          state.formulabefore.getLength());

                  newformula1 = pathFormulaManager.makeAnd(newformula1, pCfaEdge);

                } catch (InterruptedException e) {

                }

                result = new FormulaState(newformula1, this);
                result.whilebefore = state.whilebefore;
              } else {
                // The non-loop part is not considered here
              }
            }
          }
        }

      }
    }
    return result;
  }

  @Override
  protected FormulaState handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  protected FormulaState handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  protected FormulaState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    PathFormula newformula1 = null;

    try {

      newformula1 = pathFormulaManager.makeAnd(state.getPathFormula(), pCfaEdge);

    } catch (InterruptedException e) {

    }
    FormulaState result = new FormulaState(newformula1, this);

    result.whilebefore = state.whilebefore;
    result.formulabefore = state.formulabefore;
    // Workaround for for-loops
    for (int i = 0; i < pCfaEdge.getPredecessor().getNumEnteringEdges(); i++) {
      if (pCfaEdge.getPredecessor().getEnteringEdge(i).getEdgeType().equals(CFAEdgeType.BlankEdge)
          && pCfaEdge.getPredecessor().getEnteringEdge(i).getDescription().contains("for")) {
        result.formulabefore = state.formulabefore;
      }
    }
    return result;
  }

  @Override
  protected FormulaState handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    PathFormula newformula1 = null;

    try {

      newformula1 = pathFormulaManager.makeAnd(state.getPathFormula(), pCfaEdge);

    } catch (InterruptedException e) {

    }
    FormulaState result = new FormulaState(newformula1, this);
    result.whilebefore = state.whilebefore;
    result.formulabefore = state.formulabefore;
    // Workaround for for-loops
    for (int i = 0; i < pCfaEdge.getPredecessor().getNumEnteringEdges(); i++) {
      if (pCfaEdge.getPredecessor().getEnteringEdge(i).getEdgeType().equals(CFAEdgeType.BlankEdge)
          && pCfaEdge.getPredecessor().getEnteringEdge(i).getDescription().contains("for")) {
        result.formulabefore = state.formulabefore;
      }
    }
    return result;
  }

  @Override
  protected FormulaState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    // See Strengthen
    return state.clone();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected FormulaState handleBlankEdge(BlankEdge pCfaEdge) {
    if (pCfaEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
        && pCfaEdge.getDescription().contains("while")) {
      FormulaState result = state.clone();
      SSAMap map1 = state.path1.getSsa();
      SSAMapBuilder mb1 = map1.builder();
      SSAMap newmap1 = mb1.build();
      result.whilebefore = newmap1;
      result.formulabefore = state.path1;
      return result;
    }

    if (pCfaEdge.getSuccessor() != null) {
      CFANode post = pCfaEdge.getSuccessor();
      int length = post.getNumEnteringEdges();
      for (int i = 0; i < length; i++) {
        CFAEdge preEdge = post.getEnteringEdge(i);
        if (preEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
            && preEdge.getDescription().contains("while")) {
          FormulaState result = state.clone();
          SSAMap map1 = state.path1.getSsa();
          SSAMapBuilder mb1 = map1.builder();

          SSAMap whileafter1 = mb1.build();
          // TODO MAKE EQUALITIES
          List<MapsDifference.Entry<String, Integer>> symbolDifferences1 = new ArrayList<>();
          SSAMap.merge(
              state.whilebefore,
              whileafter1,
              MapsDifference.collectMapsDifferenceTo(symbolDifferences1));

          BooleanFormula p1add = result.getPathFormula().getFormula();

          int length1 = result.getPathFormula().getLength();

          for (MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences1) {
            String v = symbolDifference.getKey();
            int index1 = symbolDifference.getLeftValue().orElse(1);
            int index2 = symbolDifference.getRightValue().orElse(1);
            if (index1 + 1 == index2 && state.whilebefore.containsVariable(v)) {
              // Consider History of Not rewritten variables
              p1add = formulaManager.makeAnd(p1add, makeEqual(v, v, index2, index1));

              length1++;
            }
          }

          result.path1 =
              new PathFormula(
                  p1add,
                  whileafter1,
                  PointerTargetSet.emptyPointerTargetSet(),
                  length1);

          // result.whilebefore = null;
          return result;
        }
      }

    }

    // COde for for-loops:
    // TODO: remove redundant code
    if (pCfaEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
        && pCfaEdge.getDescription().contains("for")) {
      FormulaState result = state.clone();
      SSAMap map1 = state.path1.getSsa();
      SSAMapBuilder mb1 = map1.builder();
      SSAMap newmap1 = mb1.build();
      result.whilebefore = newmap1;
      result.formulabefore = state.path1;
      return result;
    }

    if (pCfaEdge.getSuccessor() != null) {
      CFANode post = pCfaEdge.getSuccessor();
      int length = post.getNumEnteringEdges();
      for (int i = 0; i < length; i++) {
        CFAEdge preEdge = post.getEnteringEdge(i);
        if (preEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
            && preEdge.getDescription().contains("for")) {
          FormulaState result = state.clone();
          SSAMap map1 = state.path1.getSsa();
          SSAMapBuilder mb1 = map1.builder();

          SSAMap whileafter1 = mb1.build();
          // TODO MAKE EQUALITIES
          List<MapsDifference.Entry<String, Integer>> symbolDifferences1 = new ArrayList<>();
          SSAMap.merge(
              state.whilebefore,
              whileafter1,
              MapsDifference.collectMapsDifferenceTo(symbolDifferences1));

          BooleanFormula p1add = result.getPathFormula().getFormula();

          int length1 = result.getPathFormula().getLength();

          for (MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences1) {
            String v = symbolDifference.getKey();
            int index1 = symbolDifference.getLeftValue().orElse(1);
            int index2 = symbolDifference.getRightValue().orElse(1);
            if (index1 + 1 == index2 && state.whilebefore.containsVariable(v)) {
              // Consider History of Not rewritten variables
              p1add = formulaManager.makeAnd(p1add, makeEqual(v, v, index2, index1));

              length1++;
            }
          }

          result.path1 =
              new PathFormula(
                  p1add,
                  whileafter1,
                  PointerTargetSet.emptyPointerTargetSet(),
                  length1);

          // result.whilebefore = null;
          return result;
        }
      }

    }

    return state.clone();
  }

  @Override
  protected FormulaState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge)
      throws CPATransferException {
    return state.clone();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    assert pState instanceof FormulaState;

    FormulaState cstate = (FormulaState) pState;

    // MERGING INTO WHILE

    cstate.path1 =
        new PathFormula(
            formulaManager.simplify(cstate.getPathFormula().getFormula()),
            cstate.getPathFormula().getSsa(),
            cstate.getPathFormula().getPointerTargetSet(),
            cstate.getPathFormula().getLength());
    if (cstate.isUnsat()) {
      // Dead Code
      return Collections.emptyList();
    } else {
      //
      return Collections.singleton(pState);
    }
  }

  public BooleanFormula makeEqual(String v1, String v2, int i1, int i2) {
    FormulaType<?> vartype = converter.getFormulaTypeFromCType(CNumericTypes.INT);

    Formula f1 = formulaManager.makeVariable(vartype, v1, i1);

    Formula f2 = formulaManager.makeVariable(vartype, v2, i2);
    Formula result = formulaManager.makeEqual(f1, f2);

    return (BooleanFormula) result;
  }

  public BooleanFormula makeEqualforBothPaths(Variable v, int i) {
    return makeEqual(v.toString(), v.toString(), i, i);
  }

  public BooleanFormula makeEqualforBothPaths(List<Variable> pVars, int i) {
    BooleanFormula result = formulaManager.getBooleanFormulaManager().makeBoolean(true);
    for (Variable v : pVars) {
      BooleanFormula tmp = makeEqualforBothPaths(v, i);
      result = formulaManager.makeAnd(tmp, result);
    }
    return result;
  }

  public BooleanFormula makeEqualforBothPaths(List<Variable> pVars, SSAMap pMap) {
    BooleanFormula result = formulaManager.getBooleanFormulaManager().makeBoolean(true);
    for (Variable v : pVars) {
      int i = pMap.getIndex(v.toString());
      BooleanFormula tmp = makeEqualforBothPaths(v, i);
      result = formulaManager.makeAnd(tmp, result);
    }
    return result;
  }

}
