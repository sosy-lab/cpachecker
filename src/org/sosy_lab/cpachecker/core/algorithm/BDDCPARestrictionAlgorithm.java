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

import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.ImmutableList;

@Options(prefix="counterexample")
public class BDDCPARestrictionAlgorithm implements Algorithm, StatisticsProvider {

  @Option(description="The files where the BDDCPARestrictionAlgorithm should write the presence conditions for the counterexamples to.")
  @FileOption(Type.OUTPUT_FILE)
  private Path presenceConditionFile = Paths.get("ErrorPath.%d.presenceCondition.txt");

  private final Algorithm algorithm;
  private final LogManager logger;
  private final ConfigurableProgramAnalysis cpa;

  private final NamedRegionManager manager;
  private Region errorSummary;

  public BDDCPARestrictionAlgorithm(Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa, String filename) throws InvalidConfigurationException, CPAException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);

    BDDCPA bddCpa = CPAs.retrieveCPA(pCpa, BDDCPA.class);
    if (bddCpa == null) {
      throw new InvalidConfigurationException("BDD CPA needed for BDDCPARestrictionAlgorithm");
    }
    cpa = pCpa;
    logger.log(Level.INFO, "using the BDDCPA Restriction Algorithm");

    manager = bddCpa.getManager();
    errorSummary = manager.makeFalse();
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean sound = true;

    while (reached.hasWaitingState()) {
      sound &= algorithm.run(reached);
      assert ARGUtils.checkARG(reached);

      final AbstractState lastState = reached.getLastState();
      if (!(lastState instanceof Targetable)
          || !((Targetable)lastState).isTarget()) {
        // no target state
        break;
      }

      // BDD specials
      final BDDState bddErrorState = AbstractStates.extractStateByType(lastState, BDDState.class);
      final Region errorBdd = bddErrorState.getRegion();
      final String errorBddStr = manager.dumpRegion(errorBdd);

      logger.log(Level.INFO, "ErrorBDD:", errorBddStr);
      errorSummary = manager.makeOr(errorBdd, errorSummary);

      if (presenceConditionFile != null && cpa instanceof ARGCPA) {
        CounterexampleInfo counterEx = ((ARGCPA)cpa).getCounterexamples().get(lastState);
        if (counterEx != null) {
          counterEx.addFurtherInformation(errorBddStr, presenceConditionFile);
        }
      }

      logger.log(Level.INFO, "ErrorSummary:", manager.dumpRegion(errorSummary));

      final Region negatedErrorBdd = manager.makeNot(errorBdd);
      for (AbstractState s : ImmutableList.copyOf(reached.getWaitlist())) {
        BDDState bddState = AbstractStates.extractStateByType(s, BDDState.class);
        if (bddState != null) {
          bddState.addConstraintToState(negatedErrorBdd);

          if (bddState.getRegion().isFalse()) {
            reached.removeOnlyFromWaitlist(s);
          }
        }
      }
      // END BDD specials
    }

    return sound;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
  }
}
