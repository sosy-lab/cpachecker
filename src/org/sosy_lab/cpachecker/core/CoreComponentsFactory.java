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
package org.sosy_lab.cpachecker.core;

import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BDDCPARestrictionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.PredicatedAnalysisAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ResultCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Factory class for the three core components of CPAchecker:
 * algorithm, cpa and reached set.
 */
@Options(prefix="analysis")
public class CoreComponentsFactory {

  @Option(description="use assumption collecting algorithm")
  private boolean collectAssumptions = false;

  @Option(name="algorithm.conditionAdjustment",
      description="use adjustable conditions algorithm")
  private boolean useAdjustableConditions = false;

  @Option(name="algorithm.CEGAR",
      description = "use CEGAR algorithm for lazy counter-example guided analysis"
        + "\nYou need to specify a refiner with the cegar.refiner option."
        + "\nCurrently all refiner require the use of the ARGCPA.")
  private boolean useCEGAR = false;

  @Option(description="use a second model checking run (e.g., with CBMC or a different CPAchecker configuration) to double-check counter-examples")
  private boolean checkCounterexamples = false;

  @Option(name="checkCounterexamplesWithBDDCPARestriction",
      description="use counterexample check and the BDDCPA Restriction option")
  private boolean useBDDCPARestriction = false;

  @Option(name="algorithm.BMC",
      description="use a BMC like algorithm that checks for satisfiability "
        + "after the analysis has finished, works only with PredicateCPA")
  private boolean useBMC = false;

  @Option(name="algorithm.impact",
      description="Use McMillan's Impact algorithm for lazy interpolation")
  private boolean useImpactAlgorithm = false;

  @Option(name="restartAfterUnknown",
      description="restart the analysis using a different configuration after unknown result")
  private boolean useRestartingAlgorithm = false;

  @Option(name="predicatedAnalysis",
      description="use a predicated analysis which proves if the program satisfies a specified property"
          + " with the help of a PredicateCPA to separate differnt program paths")
  private boolean usePredicatedAnalysisAlgorithm = false;

  @Option(name="algorithm.proofCheck",
      description="use a proof check algorithm to validate a previously generated proof")
  private boolean useProofCheckAlgorithm = false;

  @Option(name="algorithm.propertyCheck",
      description = "do analysis and then check "
      + "if reached set fulfills property specified by ConfigurableProgramAnalysisWithPropertyChecker")
  private boolean usePropertyCheckingAlgorithm = false;

  @Option(name="checkProof",
      description = "do analysis and then check analysis result")
  private boolean useResultCheckAlgorithm = false;

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final ReachedSetFactory reachedSetFactory;
  private final CPABuilder cpaFactory;

  public CoreComponentsFactory(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    config.inject(this);

    reachedSetFactory = new ReachedSetFactory(config, logger);
    cpaFactory = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
  }

  public Algorithm createAlgorithm(final ConfigurableProgramAnalysis cpa,
      final String programDenotation, final CFA cfa, @Nullable final MainCPAStatistics stats)
      throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm;

    if (useProofCheckAlgorithm) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm = new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier);
    } else if (useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm = new RestartAlgorithm(config, logger, shutdownNotifier, programDenotation, cfa);

    } else if (useImpactAlgorithm) {
      algorithm = new ImpactAlgorithm(config, logger, shutdownNotifier, cpa, cfa);

    } else {
      algorithm = CPAAlgorithm.create(cpa, logger, config, shutdownNotifier);

      if(usePredicatedAnalysisAlgorithm){
        algorithm = new PredicatedAnalysisAlgorithm(algorithm, cpa, cfa, logger, config, shutdownNotifier);
      }

      if (useCEGAR) {
        algorithm = new CEGARAlgorithm(algorithm, cpa, config, logger);
      }

      if (useBMC) {
        algorithm = new BMCAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, shutdownNotifier, cfa);
      }

      if (checkCounterexamples) {
        algorithm = new CounterexampleCheckAlgorithm(algorithm, cpa, config, logger, shutdownNotifier, cfa, programDenotation);
      }

      if (useBDDCPARestriction) {
        algorithm = new BDDCPARestrictionAlgorithm(algorithm, cpa, config, logger, shutdownNotifier, cfa, programDenotation);
      }

      if (collectAssumptions) {
        algorithm = new AssumptionCollectorAlgorithm(algorithm, cpa, config, logger);
      }

      if (useAdjustableConditions) {
        algorithm = new RestartWithConditionsAlgorithm(algorithm, cpa, config, logger);
      }

      if (usePropertyCheckingAlgorithm) {
        if (!(cpa instanceof PropertyCheckerCPA)) {
          throw new InvalidConfigurationException(
              "Property checking algorithm requires CPAWithPropertyChecker as Top CPA");
        }
        algorithm =
            new AlgorithmWithPropertyCheck(algorithm, logger, (PropertyCheckerCPA) cpa);
      }

      if (useResultCheckAlgorithm) {
        algorithm = new ResultCheckAlgorithm(algorithm, cpa, cfa, config, logger, shutdownNotifier);
      }
    }

    if (stats != null && algorithm instanceof StatisticsProvider) {
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

  public ConfigurableProgramAnalysis createCPA(final CFA cfa,
      @Nullable final MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");
    if (stats != null) {
      stats.cpaCreationTime.start();
    }
    try {

      if (useRestartingAlgorithm) {
        // hard-coded dummy CPA
        return LocationCPA.factory().set(cfa, CFA.class).createInstance();
      }

      ConfigurableProgramAnalysis cpa = cpaFactory.buildCPAs(cfa);

      if (stats != null && cpa instanceof StatisticsProvider) {
        ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
      }
      return cpa;

    } finally {
      if (stats != null) {
        stats.cpaCreationTime.stop();
      }
    }
  }
}
