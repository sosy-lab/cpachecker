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
package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.Preconditions;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AnalysisWithRefinableEnablerCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BDDCPARestrictionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CustomInstructionRequirementsExtractingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExceptionHandlingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithmWithARGReplay;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestrictedProgramDomainAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ResultCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar.PDRAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets.AggregatedReachedSetManager;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Factory class for the three core components of CPAchecker:
 * algorithm, cpa and reached set.
 */
@Options(prefix="analysis")
public class CoreComponentsFactory {

  @Option(secure=true, description="use assumption collecting algorithm")
  private boolean collectAssumptions = false;

  @Option(secure=true, name="algorithm.conditionAdjustment",
      description="use adjustable conditions algorithm")
  private boolean useAdjustableConditions = false;

  @Option(secure = true, name = "algorithm.pdr", description = "use PDR algorithm")
  private boolean usePDR = false;

  @Option(secure=true, name="algorithm.CEGAR",
      description = "use CEGAR algorithm for lazy counter-example guided analysis"
        + "\nYou need to specify a refiner with the cegar.refiner option."
        + "\nCurrently all refiner require the use of the ARGCPA.")
  private boolean useCEGAR = false;

  @Option(secure=true, description="use a second model checking run (e.g., with CBMC or a different CPAchecker configuration) to double-check counter-examples")
  private boolean checkCounterexamples = false;

  @Option(secure = true, description = "use counterexample check and the BDDCPA Restriction option")
  private boolean checkCounterexamplesWithBDDCPARestriction = false;

  @Option(secure=true, name="algorithm.BMC",
      description="use a BMC like algorithm that checks for satisfiability "
        + "after the analysis has finished, works only with PredicateCPA")
  private boolean useBMC = false;

  @Option(secure=true, name="algorithm.impact",
      description="Use McMillan's Impact algorithm for lazy interpolation")
  private boolean useImpactAlgorithm = false;

  @Option(secure=true, name="restartAfterUnknown",
      description="restart the analysis using a different configuration after unknown result")
  private boolean useRestartingAlgorithm = false;

  @Option(
    secure = true,
    name = "useParallelAnalyses",
    description =
        "Use analyses parallely. The resulting reachedset is the one of the first"
        + " analysis finishing in time. All other analyses are terminated."
  )
  private boolean useParallelAlgorithm = false;

  @Option(
    secure = true,
    name = "algorithm.termination",
    description = "Use termination algorithm to prove (non-)termination.")
  private boolean useTerminationAlgorithm = false;

  @Option(secure=true,
      description="memorize previously used (incomplete) reached sets after a restart of the analysis")
  private boolean memorizeReachedAfterRestart = false;

  @Option(secure=true, name="algorithm.analysisWithEnabler",
      description="use a analysis which proves if the program satisfies a specified property"
          + " with the help of an enabler CPA to separate differnt program paths")
  private boolean useAnalysisWithEnablerCPAAlgorithm = false;

  @Option(secure=true, name="algorithm.proofCheck",
      description="use a proof check algorithm to validate a previously generated proof")
  private boolean useProofCheckAlgorithm = false;

  @Option(secure=true, name="algorithm.propertyCheck",
      description = "do analysis and then check "
      + "if reached set fulfills property specified by ConfigurableProgramAnalysisWithPropertyChecker")
  private boolean usePropertyCheckingAlgorithm = false;

  @Option(secure=true, name="checkProof",
      description = "do analysis and then check analysis result")
  private boolean useResultCheckAlgorithm = false;

  @Option(secure=true, name="extractRequirements.customInstruction", description="do analysis and then extract pre- and post conditions for custom instruction from analysis result")
  private boolean useCustomInstructionRequirementExtraction = false;

  @Option(secure=true, name="restartAlgorithmWithARGReplay",
      description = "run a sequence of analysis, where the previous ARG is inserted into the current ARGReplayCPA.")
  private boolean useRestartAlgorithmWithARGReplay = false;

  @Option(secure=true, name="unknownIfUnrestrictedProgram",
      description="stop the analysis with the result unknown if the program does not satisfies certain restrictions.")
  private boolean unknownIfUnrestrictedProgram = false;

  @Option(
    secure = true,
    name = "algorithm.CBMC",
    description = "use CBMC as an external tool from CPAchecker"
  )
  boolean runCBMCasExternalTool = false;

  private final Configuration config;
  private final LogManager logger;
  private final @Nullable ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;

  private final ReachedSetFactory reachedSetFactory;
  private final CPABuilder cpaFactory;
  private final AggregatedReachedSets aggregatedReachedSets;
  private final @Nullable AggregatedReachedSetManager aggregatedReachedSetManager;

  public CoreComponentsFactory(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;

    config.inject(this);

    if (analysisNeedsShutdownManager()) {
      shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
      shutdownNotifier = shutdownManager.getNotifier();
    } else {
      shutdownManager = null;
      shutdownNotifier = pShutdownNotifier;
    }

    if (useTerminationAlgorithm) {
      aggregatedReachedSetManager = new AggregatedReachedSetManager();
      aggregatedReachedSetManager.addAggregated(pAggregatedReachedSets);
      aggregatedReachedSets = aggregatedReachedSetManager.asView();
    } else {
      aggregatedReachedSetManager = null;
      aggregatedReachedSets = pAggregatedReachedSets;
    }

    reachedSetFactory = new ReachedSetFactory(config);
    cpaFactory = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);

    if (checkCounterexamplesWithBDDCPARestriction) {
      checkCounterexamples = true;
    }
  }

  private boolean analysisNeedsShutdownManager() {
    // BMCAlgorithm needs to get a ShutdownManager that also affects the CPA it is used with.
    // We must not create such a new ShutdownManager if it is not needed,
    // because otherwise the GC will throw it away and shutdowns will NOT WORK!
    return !useProofCheckAlgorithm
        && !useRestartingAlgorithm
        && !useImpactAlgorithm
        && !useRestartAlgorithmWithARGReplay
        && !runCBMCasExternalTool
        && useBMC;
  }

  public Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa,
      final String programDenotation,
      final CFA cfa,
      final Specification pSpecification)
      throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    // TerminationAlgorithm requires hard coded specification.
    Specification specification;
    if (useTerminationAlgorithm) {
      specification = loadTerminationSpecification(cfa, pSpecification);
    } else {
      specification = pSpecification;
    }

    Algorithm algorithm;

    if (useProofCheckAlgorithm) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier, cfa, specification);

    } else if (useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm =
          RestartAlgorithm.create(
              config, logger, shutdownNotifier, specification, programDenotation, cfa);

    } else if (useImpactAlgorithm) {
      algorithm = new ImpactAlgorithm(config, logger, shutdownNotifier, cpa, cfa);

    } else if (useRestartAlgorithmWithARGReplay) {
      algorithm =
          new RestartAlgorithmWithARGReplay(config, logger, shutdownNotifier, cfa, specification);

    } else if (runCBMCasExternalTool) {
      algorithm = new ExternalCBMCAlgorithm(programDenotation, config, logger);

    } else if (useParallelAlgorithm) {
      algorithm =
          new ParallelAlgorithm(
              config,
              logger,
              shutdownNotifier,
              specification,
              cfa,
              programDenotation,
              aggregatedReachedSets);

    } else {
      algorithm = CPAAlgorithm.create(cpa, logger, config, shutdownNotifier);

      if (useAnalysisWithEnablerCPAAlgorithm) {
        algorithm = new AnalysisWithRefinableEnablerCPAAlgorithm(algorithm, cpa, cfa, logger, config, shutdownNotifier);
      }

      if (useCEGAR) {
        algorithm = new CEGARAlgorithm(algorithm, cpa, config, logger);
      }

      if (usePDR) {
        algorithm =
            new PDRAlgorithm(
                reachedSetFactory, cpa, algorithm, cfa, config, logger, shutdownNotifier);
      }

      if (useBMC) {
        verifyNotNull(shutdownManager);
        algorithm =
            new BMCAlgorithm(
                algorithm,
                cpa,
                config,
                logger,
                reachedSetFactory,
                shutdownManager,
                cfa,
                specification,
                aggregatedReachedSets);
      }

      if (checkCounterexamples) {
        if (cpa instanceof BAMCPA) {
          algorithm =
              new BAMCounterexampleCheckAlgorithm(
                  algorithm,
                  cpa,
                  config,
                  logger,
                  shutdownNotifier,
                  specification,
                  cfa,
                  programDenotation);
        } else {
          algorithm =
              new CounterexampleCheckAlgorithm(
                  algorithm,
                  cpa,
                  config,
                  specification,
                  logger,
                  shutdownNotifier,
                  cfa,
                  programDenotation);
        }
      }

      algorithm =
          ExceptionHandlingAlgorithm.create(
              config, algorithm, cpa, logger, shutdownNotifier, checkCounterexamples, useCEGAR);

      if (checkCounterexamplesWithBDDCPARestriction) {
        algorithm = new BDDCPARestrictionAlgorithm(algorithm, cpa, config, logger);
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
        algorithm =
            new ResultCheckAlgorithm(
                algorithm, cpa, cfa, config, logger, shutdownNotifier, specification);
      }

      if (useCustomInstructionRequirementExtraction) {
        algorithm = new CustomInstructionRequirementsExtractingAlgorithm(algorithm, cpa, config, logger, shutdownNotifier, cfa);
      }

      if (unknownIfUnrestrictedProgram) {
        algorithm = new RestrictedProgramDomainAlgorithm(algorithm, cfa);
      }

      if (useTerminationAlgorithm) {
        algorithm = new TerminationAlgorithm(
            config,
            logger,
            shutdownNotifier,
            cfa,
            reachedSetFactory,
            aggregatedReachedSetManager,
            specification,
            algorithm,
            cpa);
      }
    }

    return algorithm;
  }

  public ReachedSet createReachedSet() {
    ReachedSet reached = reachedSetFactory.create();

    if (useRestartingAlgorithm || useRestartAlgorithmWithARGReplay || useParallelAlgorithm) {
      // this algorithm needs an indirection so that it can change
      // the actual reached set instance on the fly
      if (memorizeReachedAfterRestart) {
        reached = new HistoryForwardingReachedSet(reached);
      } else {
        reached = new ForwardingReachedSet(reached);
      }
    }

    return reached;
  }

  public ConfigurableProgramAnalysis createCPA(final CFA cfa, final Specification pSpecification)
      throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");

    if (useRestartingAlgorithm || useParallelAlgorithm) {
      // hard-coded dummy CPA
      return LocationCPA.factory().set(cfa, CFA.class).setConfiguration(config).createInstance();
    }

    // TerminationAlgorithm requires hard coded specification.
    Specification specification;
    if (useTerminationAlgorithm) {
      specification = loadTerminationSpecification(cfa, pSpecification);
    } else {
      specification = pSpecification;
    }

    return cpaFactory.buildCPAs(cfa, specification, aggregatedReachedSets);
  }

  private Specification loadTerminationSpecification(CFA cfa, Specification originalSpecification)
      throws InvalidConfigurationException {
    Preconditions.checkState(useTerminationAlgorithm);
    Specification terminationSpecification =
        TerminationAlgorithm.loadTerminationSpecification(
            originalSpecification.getProperties(), cfa, config, logger);

    if (!originalSpecification.equals(Specification.alwaysSatisfied())
        && !originalSpecification.equals(terminationSpecification)) {
      throw new InvalidConfigurationException(
          originalSpecification + "is not usable with termination analysis");
    }

    return terminationSpecification;
  }
}
