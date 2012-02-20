/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.FeatureVarsRestrictionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.mcmillan.McMillanAlgorithm;
import org.sosy_lab.cpachecker.mcmillan.McMillanAlgorithmWithWaitlist;

/**
 * Factory class for the three core components of CPAchecker:
 * algorithm, cpa and reached set.
 */
@Options(prefix="analysis")
class CoreComponentsFactory {

  @Option(description="use assumption collecting algorithm")
  private boolean useAssumptionCollector = false;

  @Option(description="use adjustable conditions algorithm")
  private boolean useAdjustableConditions = false;

  @Option(description = "use CEGAR algorithm for lazy counter-example guided analysis"
        + "\nYou need to specify a refiner with the cegar.refiner option."
        + "\nCurrently all refiner require the use of the ARTCPA.")
  private boolean useRefinement = false;

  @Option(description="use CBMC to double-check counter-examples")
  private boolean useCBMC = false;

  @Option(description="use CBMC and the FeatureVars Restriction option")
  private boolean useFeatureVarsRestriction = false;

  @Option(description="use a BMC like algorithm that checks for satisfiability "
        + "after the analysis has finished, works only with PredicateCPA")
  private boolean useBMC = false;

  @Option(name="useMcMillan",
      description="Use McMillans algorithm for lazy interpolation")
  private boolean useMcMillan = false;

  @Option(description="Use alternate implementation of McMillans algorithm for lazy interpolation")
  private boolean useMcMillanWithWaitlist = false;

  @Option(name="restartAfterUnknown",
      description="restart the algorithm using a different CPA after unknown result")
  private boolean useRestartingAlgorithm = false;

  @Option(description="use a proof check algorithm to validate a previously generated proof")
  private boolean useProofCheckAlgorithm = false;

  private final Configuration config;
  private final LogManager logger;

  private final ReachedSetFactory reachedSetFactory;
  private final CPABuilder cpaFactory;

  public CoreComponentsFactory(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;

    config.inject(this);

    reachedSetFactory = new ReachedSetFactory(config, logger);
    cpaFactory = new CPABuilder(config, logger, reachedSetFactory);
  }

  public Algorithm createAlgorithm(final ConfigurableProgramAnalysis cpa,
      final String filename, final CFA cfa, final MainCPAStatistics stats)
      throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm;

    if(useProofCheckAlgorithm) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm = new ProofCheckAlgorithm(cpa, config, logger);
    } else if (useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm = new RestartAlgorithm(config, logger, filename, cfa);

    } else if (useMcMillan) {
      if (useMcMillanWithWaitlist) {
        throw new InvalidConfigurationException("Cannot use both implementations of McMillan's algorithm");
      }
      algorithm = new McMillanAlgorithm(config, logger, cpa);

    } else if (useMcMillanWithWaitlist) {
      if (useMcMillan) {
        throw new InvalidConfigurationException("Cannot use both implementations of McMillan's algorithm");
      }
      algorithm = new McMillanAlgorithmWithWaitlist(config, logger, cpa);

    } else {
      algorithm = new CPAAlgorithm(cpa, logger);

      if (useRefinement) {
        algorithm = new CEGARAlgorithm(algorithm, cpa, config, logger);
      }

      if (useBMC) {
        algorithm = new BMCAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, cfa);
      }

      if (useCBMC) {
        algorithm = new CounterexampleCheckAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, cfa);
      }

      if (useFeatureVarsRestriction) {
        algorithm = new FeatureVarsRestrictionAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, cfa);
      }

      if (useAssumptionCollector) {
        algorithm = new AssumptionCollectorAlgorithm(algorithm, cpa, config, logger);
      }

      if (useAdjustableConditions) {
        algorithm = new RestartWithConditionsAlgorithm(algorithm, cpa, config, logger);
      }
    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }

  public ReachedSet createReachedSet() {
    ReachedSet reached = reachedSetFactory.create();

    if (useRestartingAlgorithm) {
      // this algorithm needs an indirection so that it can change
      // the actual reached set instance on the fly
      reached = new ForwardingReachedSet(reached);
    }

    return reached;
  }

  public ConfigurableProgramAnalysis createCPA(final CFA cfa, final MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");
    stats.cpaCreationTime.start();
    try {

      if (useRestartingAlgorithm) {
        // hard-coded dummy CPA
        return LocationCPA.factory().set(cfa, CFA.class).createInstance();
      }

      ConfigurableProgramAnalysis cpa = cpaFactory.buildCPAs(cfa);

      if (cpa instanceof StatisticsProvider) {
        ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
      }
      return cpa;

    } finally {
      stats.cpaCreationTime.stop();
    }
  }
}
