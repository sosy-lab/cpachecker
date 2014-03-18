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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bdd.BDDState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

@Options(prefix="counterexample")
public class BDDCPARestrictionAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final ARGCPA cpa;

  public BDDCPARestrictionAlgorithm(Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa, String filename) throws InvalidConfigurationException, CPAException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("ARG CPA needed for counterexample check");
    }
    cpa = (ARGCPA)pCpa;
    logger.log(Level.INFO, "using the BDDCPA Restriction Algorithm");
  }

  //items for the BDD_extension
  NamedRegionManager manager = null;
  Region errorSummary = null;
  int errorsHit =0;

  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean sound = true;

    while (reached.hasWaitingState()) {
      sound &= algorithm.run(reached);
      assert ARGUtils.checkARG(reached);

      AbstractState lastState = reached.getLastState();
      if (lastState != null && !(lastState instanceof ARGState)) {
        // no analysis possible
        break;
      }
      ARGState errorState = (ARGState)lastState;

      // BDD specials
      Region errorBDD = null;
      for (AbstractState x : ((CompositeState)errorState.getWrappedState()).getWrappedStates()) {
        if (x instanceof BDDState) {
          errorBDD = ((BDDState) x).getRegion();
          //logger.log(Level.INFO,"BDD: " + ((BDDState) x).toString());
          if (manager==null) {
            manager = ((BDDState)x).getManager();
            errorSummary=manager.makeFalse();
          }
          logger.log(Level.INFO, "errorBDD:" + (errorBDD==null?"null":manager.dumpRegion(errorBDD)));
          errorSummary=manager.makeOr(errorBDD, errorSummary);
          errorsHit++;
          CounterexampleInfo counterEx = cpa.getCounterexamples().get(errorState);
          if (counterEx != null) {
            counterEx.addFurtherInformation(manager.dumpRegion(errorBDD), Paths.get("output","errorPath.%d.presenceCondition.txt"));
          }
        }
      }
      logger.log(Level.INFO, "ErrorSummary:" + (errorSummary==null?"null":manager.dumpRegion(errorSummary)));

      //TODO: would be better to delete states that should not be explored further from the waitlist
      for (AbstractState x : reached.getWaitlist()) {
        ARGState xart = (ARGState)x;
        BDDState fvstate = null;
        for (AbstractState y : ((CompositeState)xart.getWrappedState()).getWrappedStates()) {
          if (y instanceof BDDState) {
            fvstate = (BDDState)y;
          }
        }
        if (fvstate != null) {
          fvstate.addConstraintToState(manager.makeNot(errorBDD));
        }
      }
    }
    // END BDD specials

    return sound;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }
}
