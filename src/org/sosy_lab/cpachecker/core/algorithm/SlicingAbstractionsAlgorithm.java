/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;


/**
 * This is an implementation of the Slicing Abstractions idea
 * like in the papers:
 * "Slicing Abstractions" (doi:10.1007/978-3-540-75698-9_2)
 * "Splitting via Interpolants" (doi:10.1007/978-3-642-27940-9_13)
 */
public class SlicingAbstractionsAlgorithm implements Algorithm, StatisticsProvider {

  private final LogManager logger;

  private final ConfigurableProgramAnalysis cpa;
  private final Solver solver;
  private final PredicateAbstractionManager predAbsMgr;
  private final SlicingAbstractionsStrategy strategy;
  private final ARGBasedRefiner refiner;

  private final Algorithm cpaAlgorithm;
  private final AlgorithmStatus status;

  public SlicingAbstractionsAlgorithm(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier, ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    logger = pLogger;
    cpa = pCpa;
    cpaAlgorithm = CPAAlgorithm.create(cpa, logger, pConfig, pShutdownNotifier);
    status = AlgorithmStatus.SOUND_AND_PRECISE.withPrecise(true);

    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    predAbsMgr = predicateCpa.getPredicateManager();
    solver = predicateCpa.getSolver();
    PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();

    CFA cfa = predicateCpa.getCfa();
    MachineModel machineModel = cfa.getMachineModel();

    PathChecker pathChecker = new PathChecker(pConfig, logger, pShutdownNotifier, machineModel, pfmgr, solver);

    strategy = new SlicingAbstractionsStrategy(pConfig, solver, predAbsMgr, pathChecker);
    PredicateCPARefinerFactory factory = new PredicateCPARefinerFactory(pCpa);
    refiner =  factory.create(strategy);
  }


  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    cpaAlgorithm.run(pReachedSet);
    run0(pReachedSet);
    return status;
  }

  private void run0(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    CounterexampleInfo result = null;
    AbstractState targetState = getTargetState(pReachedSet);
    while (targetState != null) {
      ARGPath errorPath = ARGUtils.getOnePathTo((ARGState) targetState);
      assert pReachedSet.getWaitlist().size()==0;
      result = refiner.performRefinementForPath(new ARGReachedSet(pReachedSet), errorPath);
      if (!result.isSpurious()) {
        break;
      }
      targetState = getTargetState(pReachedSet);
    }
    if (result != null && !result.isSpurious()) {
      logger.log(Level.INFO, "Found counterexample!");
    } else {
      logger.log(Level.INFO, "All target states have been sliced!");
    }
  }
  private AbstractState getTargetState(ReachedSet pReachedSet) {
    for (AbstractState s : pReachedSet) {
      if (AbstractStates.isTargetState(s)) {
        return s;
      }
    }
    return null;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }

  private class Stats implements Statistics {

    @Override
    public String getName() {
      return "SlicingAbstractions Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

    }
  }

}
