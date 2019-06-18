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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SLFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;

/**
 *
 */
public class SLCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SLCPA.class);
  }

  // private final CFA cfa;
  // private final Solver solver;
  // private final SLPrecisionAdjustment prec;

  // private final FormulaManagerView fMgr;
  // private final SLFormulaManagerView slMgr;
  // private final IntegerFormulaManagerView intMgr;


  private final SLTransferRelation transfer;
  private final FormulaManagerView fm;
  private final IntegerFormulaManager ifm;
  // private final BooleanFormulaManager bfm;
  private final PathFormulaManager pfm;
  private final SLFormulaManagerView slfm;

  private SLCPA(
      CFA pCfa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    super("sep", "sep", new FlatLatticeDomain(), new SLTransferRelation(pLogger));

    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    fm = solver.getFormulaManager();
    ifm = fm.getIntegerFormulaManager();
    // bfm = fm.getBooleanFormulaManager();
    pfm =
        new PathFormulaManagerImpl(
            fm,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);

    slfm = fm.getSLFormulaManager();
    transfer = (SLTransferRelation) getTransferRelation();
    transfer.setPathFormulaManager(pfm);
    transfer.setSolver(solver);
    transfer.setFormulaManager(fm);

  }

  @Override
  public SLState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    PathFormula stack = pfm.makeEmptyPathFormula();
    // BooleanFormula heap = slfm.makeEmptyHeap(ifm.makeNumber(42), ifm.makeNumber(0));
    return new SLState(stack);
  }
}

// FormulaManagerView fManager = solver.getFormulaManager();
//
// SLFormulaManager slMgr = fManager.getSLFormulaManager();
// IntegerFormulaManager intMgr = fManager.getIntegerFormulaManager();
// IntegerFormula x = intMgr.makeVariable("x");
// IntegerFormula five = intMgr.makeNumber(5);
// slMgr.makePointsTo(x, five);
