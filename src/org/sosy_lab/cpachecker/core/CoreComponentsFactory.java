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
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.CEGARAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CustomInstructionRequirementsExtractingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExceptionHandlingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.InterleavedAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ProgramSplitAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestrictedProgramDomainAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.TestCaseGeneratorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.UndefinedFunctionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.PdrAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.parallel_bam.ParallelBAMAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ConfigReadingProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofCheckAndExtractCIRequirementsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ResultCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ConditionalVerifierAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ResidualProgramConstructionAfterAnalysisAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ResidualProgramConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.NonTerminationWitnessValidator;
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

  @Option(secure = true, description="After an incomplete analysis constructs a residual program which contains all program paths which are not fully explored")
  private boolean unexploredPathsAsProgram = false;

  @Option(secure = true, description="Solely construct the residual program for a given condition/assumption.")
  private boolean constructResidualProgram = false;

  @Option(secure = true, description="Construct a residual program from condition and verify residual program")
  private boolean asConditionalVerifier = false;

  @Option(secure=true, name="algorithm.BMC",
      description="use a BMC like algorithm that checks for satisfiability "
        + "after the analysis has finished, works only with PredicateCPA")
  private boolean useBMC = false;

  @Option(secure=true, name="algorithm.impact",
      description="Use McMillan's Impact algorithm for lazy interpolation")
  private boolean useImpactAlgorithm = false;

  @Option(secure = true, name = "useInterleavedAnalyses",
      description = "start different analyses interleaved and continue after unknown result")
    private boolean useInterleavedAlgorithm = false;

  @Option(secure = true, name = "useTestCaseGeneratorAlgorithm",
      description = "generate test cases for covered test targets")
    private boolean useTestCaseGeneratorAlgorithm = false;
  
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

  @Option(
    secure = true,
    name = "split.program",
    description = "Split program in subprograms which can be analyzed separately afterwards"
  )
  private boolean splitProgram = false;

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

  @Option(secure=true, name="algorithm.proofCheckReadConfig",
      description="use a proof check algorithm to validate a previously generated proof"
          + "and read the configuration for checking from the proof")
  private boolean useProofCheckAlgorithmWithStoredConfig = false;

  @Option(secure=true, name="algorithm.proofCheckAndGetHWRequirements",
      description="use a proof check algorithm to validate a previously generated proof"
      + "and extract requirements on a (reconfigurable) HW from the proof")
  private boolean useProofCheckAndExtractCIRequirementsAlgorithm = false;

  @Option(secure=true, name="algorithm.proofCheckWithARGCMCStrategy",
      description="use a proof check algorithm that using pcc.strategy=arg.ARG_CMCStrategy to validate a previously generated proof")
  private boolean useProofCheckWithARGCMCStrategy = false;

  @Option(secure=true, name="algorithm.propertyCheck",
      description = "do analysis and then check "
      + "if reached set fulfills property specified by ConfigurableProgramAnalysisWithPropertyChecker")
  private boolean usePropertyCheckingAlgorithm = false;

  @Option(secure=true, name="checkProof",
      description = "do analysis and then check analysis result")
  private boolean useResultCheckAlgorithm = false;

  @Option(
    secure = true,
    name = "algorithm.nonterminationWitnessCheck",
    description =
        "use nontermination witness validator to check a violation witness for termination"
  )
  private boolean useNonTerminationWitnessValidation = false;

  @Option(
      secure = true,
      name = "algorithm.undefinedFunctionCollector",
      description = "collect undefined functions")
  private boolean useUndefinedFunctionCollector = false;

  @Option(secure=true, name="extractRequirements.customInstruction", description="do analysis and then extract pre- and post conditions for custom instruction from analysis result")
  private boolean useCustomInstructionRequirementExtraction = false;

  @Option(secure = true, name = "algorithm.useParallelBAM", description = "run the parallel BAM algortihm.")
  private boolean useParallelBAM = false;

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

    reachedSetFactory = new ReachedSetFactory(config, logger);
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
        && !useProofCheckAlgorithmWithStoredConfig
        && !useRestartingAlgorithm
        && !useImpactAlgorithm
        && !runCBMCasExternalTool
        && useBMC;
  }

  public Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa,
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

    if (useUndefinedFunctionCollector) {
      logger.log(Level.INFO, "Using undefined function collector");
      algorithm =
          new UndefinedFunctionCollectorAlgorithm(cfa, config, logger);
    } else if (useNonTerminationWitnessValidation) {
      logger.log(Level.INFO, "Using validator for violation witnesses for termination");
      algorithm =
          new NonTerminationWitnessValidator(
              cfa, config, logger, shutdownNotifier, pSpecification.getSpecificationAutomata());
    } else if(useProofCheckAlgorithmWithStoredConfig) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ConfigReadingProofCheckAlgorithm(config, logger, shutdownNotifier, cfa, specification);
    } else if (useProofCheckAlgorithm || useProofCheckWithARGCMCStrategy) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier, cfa, specification);

    } else if (useProofCheckAndExtractCIRequirementsAlgorithm) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ProofCheckAndExtractCIRequirementsAlgorithm(cpa, config, logger, shutdownNotifier,
              cfa, specification);
    } else if (asConditionalVerifier) {
      logger.log(Level.INFO, "Using Conditional Verifier");
      algorithm = new ConditionalVerifierAlgorithm(config, logger, shutdownNotifier, specification, cfa);
    } else if (useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm = RestartAlgorithm.create(config, logger, shutdownNotifier, specification, cfa);
    } else if (useInterleavedAlgorithm) {
      logger.log(Level.INFO, "Using Interleaved Algorithm");
      algorithm = new InterleavedAlgorithm(config, logger, shutdownNotifier, specification, cfa);

    } else if (useImpactAlgorithm) {
      algorithm = new ImpactAlgorithm(config, logger, shutdownNotifier, cpa, cfa);

    } else if (runCBMCasExternalTool) {
      if (cfa.getFileNames().size() > 1) {
        throw new InvalidConfigurationException(
            "Cannot use CBMC as analysis with more than one input file");
      }
      algorithm = new ExternalCBMCAlgorithm(cfa.getFileNames().get(0), config, logger);

    } else if (useParallelAlgorithm) {
      algorithm =
          new ParallelAlgorithm(
              config,
              logger,
              shutdownNotifier,
              specification,
              cfa,
              aggregatedReachedSets);

    } else {
      algorithm = CPAAlgorithm.create(cpa, logger, config, shutdownNotifier);

      if (constructResidualProgram) {
        algorithm = new ResidualProgramConstructionAlgorithm(cfa, config, logger, shutdownNotifier,
            specification, cpa, algorithm);
      }

      if (useParallelBAM) {
        algorithm = new ParallelBAMAlgorithm(cpa, config, logger, shutdownNotifier);
      }

      if (useAnalysisWithEnablerCPAAlgorithm) {
        algorithm = new AnalysisWithRefinableEnablerCPAAlgorithm(algorithm, cpa, cfa, logger, config, shutdownNotifier);
      }

      if (useCEGAR) {
        algorithm = new CEGARAlgorithmFactory(algorithm, cpa, logger, config).newInstance();
      }

      if (usePDR) {
        algorithm =
            new PdrAlgorithm(
                algorithm,
                cpa,
                config,
                logger,
                reachedSetFactory,
                shutdownNotifier,
                cfa,
                specification);
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
                  algorithm, cpa, config, logger, shutdownNotifier, specification, cfa);
        } else {
          algorithm =
              new CounterexampleCheckAlgorithm(
                  algorithm, cpa, config, specification, logger, shutdownNotifier, cfa);
        }
      }

      algorithm =
          ExceptionHandlingAlgorithm.create(
              config, algorithm, cpa, logger, shutdownNotifier, checkCounterexamples, useCEGAR);

      if (checkCounterexamplesWithBDDCPARestriction) {
        algorithm = new BDDCPARestrictionAlgorithm(algorithm, cpa, config, logger);
      }
      
      if (useTestCaseGeneratorAlgorithm) {
        algorithm =
            new TestCaseGeneratorAlgorithm(algorithm, cfa, config, cpa, logger, shutdownNotifier);
      }

      if (collectAssumptions) {
        algorithm = new AssumptionCollectorAlgorithm(algorithm, cpa, config, logger);
      }

      if (useAdjustableConditions) {
        algorithm = new RestartWithConditionsAlgorithm(algorithm, cpa, config, logger);
      }

      if (splitProgram) {
        algorithm = new ProgramSplitAlgorithm(algorithm, cpa, config, logger, shutdownNotifier);
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

      if (unexploredPathsAsProgram) {
        algorithm = new ResidualProgramConstructionAfterAnalysisAlgorithm(cfa, algorithm, config, logger, shutdownNotifier, specification);
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

    if (useInterleavedAlgorithm || useRestartingAlgorithm || useParallelAlgorithm || asConditionalVerifier) {
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

    if (useInterleavedAlgorithm
        || useRestartingAlgorithm
        || useParallelAlgorithm
        || useProofCheckAlgorithmWithStoredConfig
        || useProofCheckWithARGCMCStrategy
        || asConditionalVerifier
        || useNonTerminationWitnessValidation
        || useUndefinedFunctionCollector) {
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
