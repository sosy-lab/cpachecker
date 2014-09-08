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
package org.sosy_lab.cpachecker.cpa.wp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;


/**
 *
 */
@Options
public class WpTransferRelation extends SingleEdgeTransferRelation {

  final Timer postTimer = new Timer();
  final Timer satCheckTimer = new Timer();
  final Timer pathFormulaTimer = new Timer();
  final Timer strengthenTimer = new Timer();
  final Timer strengthenCheckTimer = new Timer();
  final Timer abstractionCheckTimer = new Timer();
  final Timer pathFormulaCheckTimer = new Timer();
  int numSatChecksFalse = 0;
  int numSatChecksTotal = 0;

  private final LogManager logger;
  private final PathFormulaManager pathFormulaManager;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  public WpTransferRelation(Configuration pConfig, LogManager pLogger, PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFmgr, Solver pSolver) throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    pathFormulaManager = pPathFormulaManager;
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    solver = pSolver;
  }

//  @Override
//  protected WpAbstractState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
//      throws CPATransferException {
//
//    BooleanFormula stmtFormula = pathFormulaManager.makeAnd(
//        pathFormulaManager.makeEmptyPathFormula(), stmt).getFormula();
//
//    BooleanFormula assumeFormula = pathFormulaManager.makeAnd(
//        pathFormulaManager.makeEmptyPathFormula(), assume).getFormula();
//
//    BooleanFormula query = formulaManagerView.uninstantiate(booleanManager.and(stmtFormula, assumeFormula));
//    boolean contra = solver.isUnsat(query);
//
//  }

  private PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge) throws CPATransferException, InterruptedException {
    pathFormulaTimer.start();
    try {
      // compute new pathFormula with the operation on the edge
      return pathFormulaManager.makeAnd(pathFormula, edge);
    } finally {
      pathFormulaTimer.stop();
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {

    WpAbstractState state = (WpAbstractState) pState;
    CFANode backwardsLoc = pCfaEdge.getPredecessor();

    // TODO: Check whether abstraction is false.
    // Such elements might get created when precision adjustment computes an abstraction.
    // if (state.getAbstractionFormula().isFalse()) { return Collections.emptySet(); }

    // calculate weakest precondition
    PathFormula pathFormula = convertEdgeToPathFormula(state.getPathFormula(), pCfaEdge);
    logger.log(Level.ALL, "New path formula (WP) is", pathFormula);

    // TODO: check whether to do abstraction
    /// boolean doAbstraction = blk.isBlockEnd(edge, pathFormula);

    return createState(state, pathFormula, backwardsLoc, false);
  }

  private Collection<WpAbstractState> handleNonAbstractionFormulaLocation(
      PathFormula pathFormula, WpAbstractState predState) throws InterruptedException {

    boolean satCheck = true; // (satCheckBlockSize > 0) && (pathFormula.getLength() >= satCheckBlockSize);
    // TODO: Implement a heuristic for SAT checks

    if (satCheck) {
      numSatChecksTotal++;
      satCheckTimer.start();

      boolean unsat = solver.isUnsat(predState.getPathFormula().getFormula());

      satCheckTimer.stop();

      if (unsat) {
        numSatChecksFalse++;
        logger.log(Level.ALL, "PathFormula is unsatisfiable.");
        return Collections.emptySet();
      }
    }

    return Collections.singleton(new WpAbstractState(pathFormula));
  }

  private Collection<? extends WpAbstractState> createState(
      WpAbstractState predState, PathFormula pathFormula,
      CFANode loc, boolean doAbstraction) throws InterruptedException {
    if (doAbstraction) {
      throw new RuntimeException("Not implemented");
    } else {
      return handleNonAbstractionFormulaLocation(pathFormula, predState);
    }
  }


}
