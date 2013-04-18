/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options
public class ResultCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class ResultCheckStatistics implements Statistics {

    private Timer checkTimer = new Timer();
    private Timer analysisTimer = new Timer();
    private int stopChecks = 0;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      pOut.println("Number of checks:           " + stopChecks);
      pOut.println("Time for Analysis:          " + analysisTimer);
      pOut.println("Time for Result Check:      " + checkTimer);

      if (checkTimer.getSumTime() != 0) {
        pOut.println("Speed up checking:        " + ((float) analysisTimer.getSumTime()) / checkTimer.getSumTime());
        System.out.println(((float) analysisTimer.getSumTime()) / checkTimer.getSumTime());//TODO remove
      }

    }

    @Override
    public String getName() {
      return "ResultCheckAlgorithm";
    }

  }

  @Option(
      name = "pcc.proofType",
      description = "defines proof representation, either abstract reachability graph or set of reachable abstract states",
      values = { "ARG", "SET", "PSET" })
  private String pccType = "ARG";
  private LogManager logger;
  private Configuration config;
  private Algorithm analysisAlgorithm;
  private ConfigurableProgramAnalysis cpa;
  private CFA analyzedProgram;
  private ResultCheckStatistics stats;

  public ResultCheckAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, CFA pCfa,
      Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    analysisAlgorithm = pAlgorithm;
    analyzedProgram = pCfa;
    cpa = pCpa;
    logger = pLogger;
    config = pConfig;
    stats = new ResultCheckStatistics();
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    boolean result = false;

    logger.log(Level.INFO, "Start analysis.");
    stats.analysisTimer.start();
    try {
      result = analysisAlgorithm.run(pReachedSet);
    } finally {
      stats.analysisTimer.stop();
      logger.log(Level.INFO, "Analysis stopped.");
    }

    if (result && pReachedSet.getWaitlist().size() == 0) {
      logger.log(Level.INFO, "Analysis successful.", "Start checking analysis result");
      try {
        stats.checkTimer.start();

        if (pccType.equals("ARG")) {

          ARGState first = (ARGState) pReachedSet.getFirstState();
          ReachedSet newS = null;
          try {
            newS = new CoreComponentsFactory(config, logger).createReachedSet();
          } catch (InvalidConfigurationException e) {
            return false;
          }
          newS.add(cpa.getInitialState(analyzedProgram.getMainFunction()),
              cpa.getInitialPrecision(analyzedProgram.getMainFunction()));


          result = checkARG(newS, first);

        } else if (pccType.equals("SET")) {
          result = checkResult(pReachedSet);
        }
      } finally {
        stats.checkTimer.stop();
        logger.log(Level.INFO, "Stop checking analysis result.");
      }

      if (result) {
        logger.log(Level.INFO, "Analysis result checked successfully.");
        return true;
      }
      logger.log(Level.INFO, "Analysis result could not be checked.");

    } else {
      logger.log(Level.WARNING, "Analysis incomplete.");
    }

    return false;
  }

  private boolean checkARG(final ReachedSet reachedSet, ARGState rootState) throws CPAException, InterruptedException {
    ProofChecker checker = (ProofChecker) cpa;

    //TODO does not account for strengthen yet (proof check will fail if strengthen is needed to explain successor states)
    logger.log(Level.INFO, "Proof check algorithm started");

    AbstractState initialState = reachedSet.popFromWaitlist();
    Precision initialPrecision = reachedSet.getPrecision(initialState);

    logger.log(Level.FINE, "Checking root state");

    if (!(checker.isCoveredBy(initialState, rootState) && checker.isCoveredBy(rootState, initialState))) {
      logger.log(Level.WARNING, "Root state of proof is invalid.");
      return false;
    }

    reachedSet.add(rootState, initialPrecision);

    Set<ARGState> postponedStates = new HashSet<>();

    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    Set<ARGState> inWaitlist = new HashSet<>();
    inWaitlist.add(rootState);

    boolean unexploredParent;

    do {
      for (ARGState e : postponedStates) {
        if (!reachedSet.contains(e.getCoveringState())) {
          logger.log(Level.WARNING, "Covering state", e.getCoveringState(), "was not found in reached set");
          return false;
        }
        reachedSet.reAddToWaitlist(e);
      }
      postponedStates.clear();

      while (reachedSet.hasWaitingState()) {
        CPAchecker.stopIfNecessary();

        ARGState state = (ARGState) reachedSet.popFromWaitlist();
        inWaitlist.remove(state);

        logger.log(Level.FINE, "Looking at state", state);

        if (state.isTarget()) { return false; }

        if (state.isCovered()) {

          logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
          ARGState coveringState = state.getCoveringState();

          if (!reachedSet.contains(coveringState)) {
            postponedStates.add(state);
            continue;
          }

          if (!isCoveringCycleFree(state)) {
            logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
            return false;
          }
          if (!checker.isCoveredBy(state, coveringState)) {

            logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
            return false;
          }

        } else {
          Collection<ARGState> successors = state.getChildren();
          logger.log(Level.FINER, "Checking abstract successors", successors);
          if (!checker.areAbstractSuccessors(state, null, successors)) {
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
          for (ARGState e : successors) {
            unexploredParent = false;
            for (ARGState p : e.getParents()) {
              if (!reachedSet.contains(p) || inWaitlist.contains(p)) {
                waitingForUnexploredParents.add(e);
                unexploredParent = true;
                break;
              }
            }
            if (unexploredParent) {
              continue;
            }
            if (reachedSet.contains(e)) {
              // state unknown parent of e
              logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
              return false;
            } else {
              waitingForUnexploredParents.remove(e);
              reachedSet.add(e, initialPrecision);
              inWaitlist.add(e);
            }
          }
        }
      }
    } while (!postponedStates.isEmpty());

    return waitingForUnexploredParents.isEmpty();
  }

  private boolean isCoveringCycleFree(ARGState pState) {
    HashSet<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) { return false; }
    }
    return true;
  }


  private boolean checkResult(ReachedSet pReachedSet) {
    /*also restrict stop to elements of same location as analysis does*/
    StopOperator stop = cpa.getStopOperator();
    Collection<AbstractState> result = pReachedSet.asCollection();
    Precision initialPrec = cpa.getInitialPrecision(analyzedProgram.getMainFunction());

    // check if initial element covered
    try {
      stats.stopChecks++;
      AbstractState initialState = cpa.getInitialState(analyzedProgram.getMainFunction());
      if (!stop.stop(initialState, pReachedSet.getReached(initialState), initialPrec)) {
        logger.log(Level.FINE, "Cannot check that initial element is covered by result.");
        return false;
      }
    } catch (CPAException e) {
      logger.logException(Level.FINE, e, "Stop check failed for initial element.");
      return false;
    }


    // check if elements form transitive closure
    Collection<? extends AbstractState> successors;
    for (AbstractState state : result) {
      try {
        successors = cpa.getTransferRelation().getAbstractSuccessors(state, initialPrec, null);
        for (AbstractState succ : successors) {
          stats.stopChecks++;
          if (!stop.stop(succ, pReachedSet.getReached(succ), initialPrec)) {
            logger.log(Level.FINE, "Cannot check that result is transitive closure.", "Successor ", succ,
                "of element ", state, "not covered by result.");
            return false;
          }
        }

      } catch (CPATransferException | InterruptedException e) {
        logger.logException(Level.FINE, e, "Computation of successors failed.");
        return false;
      } catch (CPAException e) {
        logger.logException(Level.FINE, e, "Stop check failed for successor.");
        return false;
      }
    }
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (analysisAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) analysisAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
