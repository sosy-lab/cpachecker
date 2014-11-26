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
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
@SuppressWarnings("unused")
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


  private PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge) throws CPATransferException, InterruptedException {
    throw new RuntimeException("Not yet implemented");
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
    throw new RuntimeException("Not yet implemented");
  }





}
