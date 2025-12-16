// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaTransformationMetadata;
import org.sosy_lab.cpachecker.cfa.CfaTransformationMetadata.ProgramTransformation;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AnalysisWithRefinableEnablerCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ArrayAbstractionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BDDCPARestrictionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.CEGARAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleStoreAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CustomInstructionRequirementsExtractingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExceptionHandlingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.FaultLocalizationByImport;
import org.sosy_lab.cpachecker.core.algorithm.FaultLocalizationWithCoverage;
import org.sosy_lab.cpachecker.core.algorithm.FaultLocalizationWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.MPIPortfolioAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.NoopAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ProgramSplitAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RandomSamplingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RandomTestGeneratorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestrictedProgramDomainAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.SelectionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.TestCaseGeneratorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.UndefinedFunctionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.WitnessToACSLAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.DARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.IMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.pdr.PdrAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.composition.CompositionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DistributedSummarySynthesis;
import org.sosy_lab.cpachecker.core.algorithm.explainer.Explainer;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MporPreprocessingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpv.MPVAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpv.MPVReachedSet;
import org.sosy_lab.cpachecker.core.algorithm.parallel_bam.ParallelBAMAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.AlgorithmWithPropertyCheck;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ConfigReadingProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofCheckAndExtractCIRequirementsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ResultCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ConditionalVerifierAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ResidualProgramConstructionAfterAnalysisAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ResidualProgramConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.TestGoalToConditionConverterAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.slicing.SlicingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.NonTerminationWitnessValidator;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.TerminationWitnessValidator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets.AggregatedReachedSetManager;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToSafetyUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Factory class for the three core components of CPAchecker: algorithm, cpa and reached set. */
@Options(prefix = "analysis")
public class CoreComponentsFactory {

  @Option(
      secure = true,
      name = "disable",
      description = "stop CPAchecker after startup (internal option, not intended for users)")
  private boolean disableAnalysis = false;

  @Option(secure = true, description = "use assumption collecting algorithm")
  private boolean collectAssumptions = false;

  @Option(
      secure = true,
      name = "algorithm.conditionAdjustment",
      description = "use adjustable conditions algorithm")
  private boolean useAdjustableConditions = false;

  @Option(secure = true, name = "algorithm.pdr", description = "use PDR algorithm")
  private boolean usePDR = false;

  @Option(
      secure = true,
      name = "algorithm.CEGAR",
      description =
          "use CEGAR algorithm for lazy counter-example guided analysis"
              + "\nYou need to specify a refiner with the cegar.refiner option."
              + "\nCurrently all refiner require the use of the ARGCPA.")
  private boolean useCEGAR = false;

  @Option(
      secure = true,
      description =
          "use a second model checking run (e.g., with CBMC or a different CPAchecker"
              + " configuration) to double-check counter-examples")
  private boolean checkCounterexamples = false;

  @Option(secure = true, description = "use counterexample check and the BDDCPA Restriction option")
  private boolean checkCounterexamplesWithBDDCPARestriction = false;

  @Option(
      secure = true,
      description =
          "After an incomplete analysis constructs a residual program which contains all program"
              + " paths which are not fully explored")
  private boolean unexploredPathsAsProgram = false;

  @Option(
      secure = true,
      description = "Solely construct the residual program for a given condition/assumption.")
  private boolean constructResidualProgram = false;

  @Option(
      secure = true,
      description = "Construct a residual program from condition and verify residual program")
  private boolean asConditionalVerifier = false;

  @Option(secure = true, description = "Construct the program slice for the given configuration.")
  private boolean constructProgramSlice = false;

  @Option(
      secure = true,
      name = "algorithm.BMC",
      description =
          "use a BMC like algorithm that checks for satisfiability "
              + "after the analysis has finished, works only with PredicateCPA")
  private boolean useBMC = false;

  @Option(
      secure = true,
      name = "algorithm.IMC",
      description =
          "use McMillan's interpolation-based model checking algorithm, "
              + "works only with PredicateCPA and large-block encoding")
  private boolean useIMC = false;

  @Option(
      secure = true,
      name = "algorithm.DAR",
      description =
          "use dual approximated reachability model checking algorithm, "
              + "works only with PredicateCPA and large-block encoding")
  private boolean useDAR = false;

  @Option(
      secure = true,
      name = "algorithm.impact",
      description = "Use McMillan's Impact algorithm for lazy interpolation")
  private boolean useImpactAlgorithm = false;

  @Option(
      secure = true,
      name = "useCompositionAnalysis",
      description = "select an analysis from a set of analyses after unknown result")
  private boolean useCompositionAlgorithm = false;

  @Option(
      secure = true,
      name = "useRandomTestCaseGeneratorAlgorithm",
      description = "generate random test cases")
  private boolean useRandomTestCaseGeneratorAlgorithm = false;

  @Option(
      secure = true,
      name = "useTestCaseGeneratorAlgorithm",
      description = "generate test cases for covered test targets")
  private boolean useTestCaseGeneratorAlgorithm = false;

  @Option(
      secure = true,
      name = "restartAfterUnknown",
      description = "restart the analysis using a different configuration after unknown result")
  private boolean useRestartingAlgorithm = false;

  @Option(
      secure = true,
      name = "selectAnalysisHeuristically",
      description = "Use heuristics to select the analysis")
  private boolean useHeuristicSelectionAlgorithm = false;

  @Option(
      secure = true,
      name = "useParallelAnalyses",
      description =
          "Use analyses parallely. The resulting reachedset is the one of the first"
              + " analysis finishing in time. All other analyses are terminated.")
  private boolean useParallelAlgorithm = false;

  @Option(secure = true, description = "converts a witness to an ACSL annotated program")
  private boolean useWitnessToACSLAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.MPI",
      description =
          "Use MPI for running analyses in new subprocesses. The resulting reachedset "
              + "is the one of the first analysis returning in time. All other mpi-processes will "
              + "get aborted.")
  private boolean useMPIProcessAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.termination",
      description =
          "Use termination algorithm to prove (non-)termination. This needs the TerminationCPA as"
              + " root CPA and an automaton CPA with termination_as_reach.spc in the tree of CPAs.")
  private boolean useTerminationAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.terminationToSafety",
      description =
          "Use termination-to-safety algorithm to prove (non-)termination. This needs the"
              + " TerminationToReachCPA,PredicateCPA, LocationCPA and CallStackCPA in the"
              + " CompositeCPA.")
  private boolean useTerminationToSafetyAlgorithm = false;

  @Option(
      secure = true,
      name = "useArrayAbstraction",
      description = "Use array abstraction by program transformation.")
  private boolean useArrayAbstraction = false;

  @Option(
      secure = true,
      name = "alwaysStoreCounterexamples",
      description =
          "If not already done by the analysis,"
              + " store a found counterexample in the ARG for later re-use. Does nothing"
              + " if no ARGCPA is used")
  private boolean forceCexStore = false;

  @Option(
      secure = true,
      name = "split.program",
      description = "Split program in subprograms which can be analyzed separately afterward")
  private boolean splitProgram = false;

  @Option(
      secure = true,
      description =
          "memorize previously used (incomplete) reached sets after a restart of the analysis")
  private boolean memorizeReachedAfterRestart = false;

  @Option(
      secure = true,
      name = "algorithm.analysisWithEnabler",
      description =
          "use an analysis which proves if the program satisfies a specified property"
              + " with the help of an enabler CPA to separate differnt program paths")
  private boolean useAnalysisWithEnablerCPAAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.proofCheck",
      description = "use a proof check algorithm to validate a previously generated proof")
  private boolean useProofCheckAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.proofCheckReadConfig",
      description =
          "use a proof check algorithm to validate a previously generated proof"
              + "and read the configuration for checking from the proof")
  private boolean useProofCheckAlgorithmWithStoredConfig = false;

  @Option(
      secure = true,
      name = "algorithm.proofCheckAndGetHWRequirements",
      description =
          "use a proof check algorithm to validate a previously generated proof"
              + "and extract requirements on a (reconfigurable) HW from the proof")
  private boolean useProofCheckAndExtractCIRequirementsAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.proofCheckWithARGCMCStrategy",
      description =
          "use a proof check algorithm that using pcc.strategy=arg.ARG_CMCStrategy to validate a"
              + " previously generated proof")
  private boolean useProofCheckWithARGCMCStrategy = false;

  @Option(
      secure = true,
      name = "algorithm.propertyCheck",
      description =
          "do analysis and then check if reached set fulfills property specified by"
              + " ConfigurableProgramAnalysisWithPropertyChecker")
  private boolean usePropertyCheckingAlgorithm = false;

  @Option(
      secure = true,
      name = "checkProof",
      description = "do analysis and then check analysis result")
  private boolean useResultCheckAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.nonterminationWitnessCheck",
      description =
          "use nontermination witness validator to check a violation witness for termination")
  private boolean useNonTerminationWitnessValidation = false;

  @Option(
      secure = true,
      name = "algorithm.terminationWitnessCheck",
      description =
          "use termination witness validator to check a correctness witness for termination")
  private boolean useTerminationWitnessValidation = false;

  @Option(
      secure = true,
      name = "algorithm.undefinedFunctionCollector",
      description = "collect undefined functions")
  private boolean useUndefinedFunctionCollector = false;

  @Option(
      secure = true,
      name = "extractRequirements.customInstruction",
      description =
          "do analysis and then extract pre- and post conditions for custom instruction from"
              + " analysis result")
  private boolean useCustomInstructionRequirementExtraction = false;

  @Option(
      secure = true,
      name = "algorithm.useParallelBAM",
      description = "run the parallel BAM algortihm.")
  private boolean useParallelBAM = false;

  @Option(
      secure = true,
      name = "unknownIfUnrestrictedProgram",
      description =
          "stop the analysis with the result unknown if the program does not satisfies certain"
              + " restrictions.")
  private boolean unknownIfUnrestrictedProgram = false;

  @Option(
      secure = true,
      name = "preprocessing.MPOR",
      description =
          "Use Modular Partial Order Reduction (MPOR) algorithm to sequentialize a concurrent C"
              + " program. This algorithm transforms the input program into a sequential program"
              + " that preserves the properties of the original concurrent program.\n"
              + "If the sequentialized program should be analyzed inside CPAchecker"
              + " directly, set 'algorithm.MPOR.preprocessing.runAnalysis=true' too.")
  private boolean useMporPreprocessing = false;

  @Option(
      secure = true,
      name = "preprocessing.preferOriginalCfaOverSequentialized",
      description =
          "In case the CFA was modified in a pre-processing step (e.g., by sequentialization), if"
              + " this option is set to true the original CFA is used instead of the modified one"
              + " for the analysis. This is useful when the pre-processing should be done for"
              + " multiple algorithms in a parallel portfolio, but some of them should analyze the"
              + " original CFA. For example, when using some analyses which support concurrency"
              + " natively alongside analyses which need sequentialization in a parallel portfolio"
              + " we want the analyses which natively support concurrency to use the original CFA.")
  private boolean preferOriginalCfaOverSequentialized = false;

  @Option(
      secure = true,
      name = "algorithm.MPV",
      description = "use MPV algorithm for checking multiple properties")
  private boolean useMPV = false;

  @Option(
      secure = true,
      name = "algorithm.faultLocalization.by_coverage",
      description = "for found property violation, perform fault localization with coverage")
  private boolean useFaultLocalizationWithCoverage = false;

  @Option(
      secure = true,
      name = "algorithm.faultLocalization.by_traceformula",
      description = "for found property violation, perform fault localization with trace formulas")
  private boolean useFaultLocalizationWithTraceFormulas = false;

  @Option(
      secure = true,
      name = "algorithm.faultLocalization.by_distance",
      description = "Use fault localization with distance metrics")
  private boolean useFaultLocalizationWithDistanceMetrics = false;

  @Option(
      secure = true,
      name = "algorithm.distributedSummarySynthesis",
      description =
          "Use distributed summary synthesis. This decomposes the input program into smaller units"
              + " that are analyzed concurrently. See https://doi.org/10.1145/3660766 for details.")
  private boolean useDistributedSummarySynthesis = false;

  @Option(
      secure = true,
      name = "algorithm.importFaults",
      description = "Import faults stored in a JSON format.")
  private boolean useImportFaults = false;

  @Option(
      secure = true,
      name = "algorithm.useSamplingAlgorithm",
      description =
          "Generate samples using the provided algorithm. Currently this "
              + "only works using the configuration "
              + "'config/valueAnalysis-NoCegar.properties' as an algorithm."
              + "Ideally never use this option directly, but only through "
              + "a configuration")
  private boolean useSamplingAlgorithm = false;

  @Option(
      secure = true,
      name = "algorithm.copyCFA",
      description =
          "Everything constructed in the CoreComponentsFactory is done on a copy of the original"
              + " CFA, if the option is set to true. One of the possible use-cases are"
              + " modifications of the CFA by the algorithm. For example, if we run algorithms in"
              + " parallel or in sequence, they are expected to get the CFA corresponding to the"
              + " original program. Hence, to prevent these modifications from influencing other"
              + " algorithms, each algorithm can claim  a copy of the original CFA to modify.")
  private boolean copyCFA = false;

  @Option(secure = true, description = "Enable converting test goals to conditions.")
  private boolean testGoalConverter;

  private final Configuration config;
  private final LogManager logger;
  private final @Nullable ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;
  private CFA cfa;
  private final CFA oldCfa;

  private final ReachedSetFactory reachedSetFactory;
  private final CPABuilder cpaFactory;
  private final AggregatedReachedSets aggregatedReachedSets;
  private final @Nullable AggregatedReachedSetManager aggregatedReachedSetManager;

  public CoreComponentsFactory(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      AggregatedReachedSets pAggregatedReachedSets,
      CFA pCFA)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;

    config.inject(this);
    oldCfa = checkNotNull(pCFA);

    if (copyCFA) {
      cfa = ImmutableCFA.copyOf(checkNotNull(pCFA), pConfig, logger);
    } else {
      cfa = checkNotNull(pCFA);
    }

    if (analysisNeedsShutdownManager()) {
      shutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
      shutdownNotifier = shutdownManager.getNotifier();
    } else {
      shutdownManager = null;
      shutdownNotifier = pShutdownNotifier;
    }

    // Allow for deactivating pre-processing steps like the sequentialization in inner analyses
    // which do not need it.
    CfaTransformationMetadata transformationMetadata =
        cfa.getMetadata().getTransformationMetadata();

    // Whenever we want to use the original CFA instead of a pre-processed one, we retrieve it here.
    // This is necessary to pre-process the CFA only once, e.g., by sequentialization, but still
    // allow analyses which do not need the pre-processed CFA to use the original one. For example,
    // when using some analyses which support concurrency natively alongside analyses which need
    // sequentialization in a parallel portfolio we want the analyses which natively support
    // concurrency to use the original CFA.
    if (preferOriginalCfaOverSequentialized
        && transformationMetadata != null
        && transformationMetadata
            .transformation()
            .equals(ProgramTransformation.SEQUENTIALIZATION_ATTEMPTED)) {
      cfa = transformationMetadata.originalCfa();
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
    return !disableAnalysis
        && !useProofCheckAlgorithm
        && !useProofCheckAlgorithmWithStoredConfig
        && !useRestartingAlgorithm
        && !useImpactAlgorithm
        && (useBMC || useIMC || useDAR);
  }

  private boolean analysisSequentializesCfa() {
    return useMporPreprocessing && !preferOriginalCfaOverSequentialized;
  }

  /**
   * This method can be used in case the factory constructs a new copy of cfa on which it operates.
   * This way, caller algorithms can get hold of this copy.
   *
   * @return cfa used in this instance of the factory
   */
  public CFA getCfa() {
    return cfa;
  }

  public Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa, final Specification specification)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    logger.log(Level.FINE, "Creating algorithms");

    if (disableAnalysis) {
      return NoopAlgorithm.INSTANCE;
    }

    Algorithm algorithm;

    if (useUndefinedFunctionCollector) {
      logger.log(Level.INFO, "Using undefined function collector");
      algorithm = new UndefinedFunctionCollectorAlgorithm(config, logger, shutdownNotifier, cfa);
    } else if (analysisSequentializesCfa()) {
      // Wrap the inner algorithm into one which pre-processes the CFA with MPOR sequentialization.
      // Only in case the CFA is not already sequentialized, since in that case we are somewhere
      // inside a nested algorithm inside of the `MporPreprocessingAlgorithm`.
      // In such a case we want to continue creating the algorithm with the already sequentialized
      // CFA.
      //
      // This is usefull in order to be able to write `analysis.preprocessing.MPOR=true` in the
      // existing configuration and have all (sub-)analyses automatically operate on the
      // sequentialized CFA no matter how deep they are nested. In particular this works for
      // parallel compositions, sequential compositions, and restart algorithm.
      algorithm =
          new MporPreprocessingAlgorithm(config, logger, shutdownNotifier, cfa, specification);
    } else if (useNonTerminationWitnessValidation) {
      logger.log(Level.INFO, "Using validator for violation witnesses for termination");
      algorithm =
          new NonTerminationWitnessValidator(
              cfa, config, logger, shutdownNotifier, specification.getSpecificationAutomata());
    } else if (useProofCheckAlgorithmWithStoredConfig) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ConfigReadingProofCheckAlgorithm(
              config, logger, shutdownNotifier, cfa, specification);
    } else if (useProofCheckAlgorithm || useProofCheckWithARGCMCStrategy) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier, cfa, specification);

    } else if (useProofCheckAndExtractCIRequirementsAlgorithm) {
      logger.log(Level.INFO, "Using Proof Check Algorithm");
      algorithm =
          new ProofCheckAndExtractCIRequirementsAlgorithm(
              cpa, config, logger, shutdownNotifier, cfa, specification);
    } else if (asConditionalVerifier) {
      logger.log(Level.INFO, "Using Conditional Verifier");
      algorithm =
          new ConditionalVerifierAlgorithm(config, logger, shutdownNotifier, specification, cfa);
    } else if (useHeuristicSelectionAlgorithm) {
      logger.log(Level.INFO, "Using heuristics to select analysis");
      algorithm = new SelectionAlgorithm(cfa, shutdownNotifier, config, specification, logger);
    } else if (useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm = RestartAlgorithm.create(config, logger, shutdownNotifier, specification, cfa);
    } else if (useCompositionAlgorithm) {
      logger.log(Level.INFO, "Using Composition Algorithm");
      algorithm = new CompositionAlgorithm(config, logger, shutdownNotifier, specification, cfa);

    } else if (useImpactAlgorithm) {
      algorithm = new ImpactAlgorithm(config, logger, shutdownNotifier, cpa, cfa);

    } else if (useParallelAlgorithm) {
      algorithm =
          new ParallelAlgorithm(
              config, logger, shutdownNotifier, specification, cfa, aggregatedReachedSets);

    } else if (useWitnessToACSLAlgorithm) {
      algorithm = new WitnessToACSLAlgorithm(config, logger, shutdownNotifier, cfa);

    } else if (useMPIProcessAlgorithm) {
      algorithm = new MPIPortfolioAlgorithm(config, logger, shutdownNotifier, specification);

    } else if (useFaultLocalizationWithDistanceMetrics) {
      algorithm = new Explainer(config, logger, shutdownNotifier, specification, cfa);
    } else if (useArrayAbstraction) {
      algorithm =
          new ArrayAbstractionAlgorithm(config, logger, shutdownNotifier, specification, cfa);
    } else if (useRandomTestCaseGeneratorAlgorithm) {
      algorithm =
          new RandomTestGeneratorAlgorithm(config, logger, shutdownNotifier, cfa, specification);
    } else {
      if (useTerminationToSafetyAlgorithm) {
        TerminationToSafetyUtils.shareTheSolverBetweenCPAs(cpa);
      }

      algorithm = CPAAlgorithm.create(cpa, logger, config, shutdownNotifier);

      if (testGoalConverter) {
        algorithm =
            new TestGoalToConditionConverterAlgorithm(
                config, logger, shutdownNotifier, cfa, algorithm, cpa);
      }

      if (constructResidualProgram) {
        algorithm =
            new ResidualProgramConstructionAlgorithm(
                cfa, config, logger, shutdownNotifier, specification, cpa, algorithm);
      }

      if (constructProgramSlice) {
        logger.log(
            Level.INFO,
            "Constructing program slice. (Sub-)analysis will stop after this"
                + " and ignore other algorithms in this configuration.");
        algorithm = new SlicingAlgorithm(logger, shutdownNotifier, config, cfa, specification);
      }

      if (useParallelBAM) {
        algorithm = new ParallelBAMAlgorithm(cpa, config, logger, shutdownNotifier);
      }

      if (useAnalysisWithEnablerCPAAlgorithm) {
        algorithm =
            new AnalysisWithRefinableEnablerCPAAlgorithm(
                algorithm, cpa, cfa, logger, config, shutdownNotifier);
      }

      if (useCEGAR) {
        algorithm =
            new CEGARAlgorithmFactory(algorithm, cpa, logger, config, shutdownNotifier)
                .newInstance();
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
                specification,
                aggregatedReachedSets);
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

      if (useIMC) {
        verifyNotNull(shutdownManager);
        algorithm =
            new IMCAlgorithm(
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

      if (useDAR) {
        verifyNotNull(shutdownManager);
        algorithm =
            new DARAlgorithm(
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

      if (useTerminationAlgorithm) {
        algorithm =
            new TerminationAlgorithm(
                config,
                logger,
                shutdownNotifier,
                cfa,
                reachedSetFactory,
                aggregatedReachedSetManager,
                algorithm,
                cpa);
      }

      if (useTerminationWitnessValidation) {
        logger.log(Level.INFO, "Using validator for correctness witnesses for termination");
        algorithm =
            new TerminationWitnessValidator(
                cfa,
                cpa,
                config,
                logger,
                shutdownNotifier,
                specification.getPathToSpecificationAutomata().keySet(),
                specification);
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
            new TestCaseGeneratorAlgorithm(
                algorithm, cfa, config, cpa, logger, shutdownNotifier, specification);
      }

      if (collectAssumptions) {
        algorithm =
            new AssumptionCollectorAlgorithm(algorithm, cpa, config, logger, cfa, shutdownNotifier);
      }

      if (useAdjustableConditions) {
        algorithm = new RestartWithConditionsAlgorithm(algorithm, cpa, config, logger);
      }

      if (splitProgram) {
        algorithm = new ProgramSplitAlgorithm(algorithm, cpa, config, logger, shutdownNotifier);
      }

      if (usePropertyCheckingAlgorithm) {
        if (!(cpa instanceof PropertyCheckerCPA propertyCheckerCPA)) {
          throw new InvalidConfigurationException(
              "Property checking algorithm requires CPAWithPropertyChecker as Top CPA");
        }
        algorithm = new AlgorithmWithPropertyCheck(algorithm, logger, propertyCheckerCPA);
      }

      if (useResultCheckAlgorithm) {
        algorithm =
            new ResultCheckAlgorithm(
                algorithm, cfa, config, logger, shutdownNotifier, specification);
      }
      if (useCustomInstructionRequirementExtraction) {
        algorithm =
            new CustomInstructionRequirementsExtractingAlgorithm(
                algorithm, cpa, config, logger, shutdownNotifier, cfa);
      }

      if (unexploredPathsAsProgram) {
        algorithm =
            new ResidualProgramConstructionAfterAnalysisAlgorithm(
                cfa, algorithm, config, logger, shutdownNotifier, specification);
      }

      if (unknownIfUnrestrictedProgram) {
        algorithm = new RestrictedProgramDomainAlgorithm(algorithm, cfa);
      }

      if (cpa instanceof ARGCPA && forceCexStore) {
        algorithm =
            new CounterexampleStoreAlgorithm(algorithm, cpa, config, logger, cfa.getMachineModel());
      }

      if (useMPV) {
        algorithm = new MPVAlgorithm(cpa, config, logger, shutdownNotifier, specification, cfa);
      }

      if (useDistributedSummarySynthesis) {
        algorithm =
            new DistributedSummarySynthesis(
                config,
                logger,
                cfa,
                ShutdownManager.createWithParent(shutdownNotifier),
                specification);
      }
      if (useFaultLocalizationWithCoverage) {
        algorithm = new FaultLocalizationWithCoverage(algorithm, shutdownNotifier, logger, config);
      }
      if (useFaultLocalizationWithTraceFormulas) {
        algorithm =
            new FaultLocalizationWithTraceFormula(algorithm, config, logger, cfa, shutdownNotifier);
      }
      if (useImportFaults) {
        algorithm = new FaultLocalizationByImport(config, algorithm, cfa, logger);
      }

      if (useSamplingAlgorithm) {
        algorithm =
            new RandomSamplingAlgorithm(algorithm, config, logger, shutdownNotifier, cfa, cpa);
      }
    }

    return algorithm;
  }

  /**
   * Creates an instance of a {@link ReachedSet}. The better way to construct reached set is to use
   * createInitializedReachedSet ! If this method needs to be used, use initializeReachedSet
   * afterward to initialize it.
   *
   * @param cpa The CPA whose abstract states will be stored in this reached set.
   */
  public ReachedSet createReachedSet(ConfigurableProgramAnalysis cpa) {
    ReachedSet reached = reachedSetFactory.create(cpa);

    if (useCompositionAlgorithm
        || useRestartingAlgorithm
        || useHeuristicSelectionAlgorithm
        || useParallelAlgorithm
        || asConditionalVerifier
        || useFaultLocalizationWithDistanceMetrics
        || useArrayAbstraction) {
      // this algorithm needs an indirection so that it can change
      // the actual reached set instance on the fly
      if (memorizeReachedAfterRestart) {
        reached = new HistoryForwardingReachedSet(reached);
      } else {
        reached = new ForwardingReachedSet(reached);
      }
    }
    if (useMPV) {
      reached = new MPVReachedSet(reached);
    }

    return reached;
  }

  /**
   * Initializes the {@link ReachedSet} with the initial states from the current CFA. The better way
   * to construct and initialize a reached set is to use createInitializedReachedSet !
   *
   * @param cpa The CPA whose abstract states will be stored in this reached set.
   */
  public void initializeReachedSet(
      ReachedSet pReachedSet, CFANode pInitialNode, ConfigurableProgramAnalysis cpa)
      throws InterruptedException, CPAException {
    if (copyCFA) {
      if (!oldCfa.getMainFunction().equals(pInitialNode)) {
        throw new CPAException(
            "If the copying of CFA is set, the analysis can only start from the initial state of"
                + " the CFA");
      }
      pInitialNode = cfa.getMainFunction();
    }
    AbstractState initialState =
        cpa.getInitialState(pInitialNode, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(pInitialNode, StateSpacePartition.getDefaultPartition());
    pReachedSet.add(initialState, initialPrecision);
  }

  /**
   * Initializes the {@link ReachedSet} with the initial states from the current CFA.
   *
   * @param cpa The CPA whose abstract states will be stored in this reached set.
   */
  public ReachedSet createInitializedReachedSet(
      ConfigurableProgramAnalysis cpa, CFANode pInitialNode)
      throws InterruptedException, CPAException {
    ReachedSet reachedSet = createReachedSet(cpa);
    initializeReachedSet(reachedSet, pInitialNode, cpa);
    return reachedSet;
  }

  public ConfigurableProgramAnalysis createCPA(final Specification pSpecification)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    logger.log(Level.FINE, "Creating CPAs");

    if (useCompositionAlgorithm
        || useRestartingAlgorithm
        || useHeuristicSelectionAlgorithm
        || useParallelAlgorithm
        || useProofCheckAlgorithmWithStoredConfig
        || useProofCheckWithARGCMCStrategy
        || asConditionalVerifier
        || useNonTerminationWitnessValidation
        || useUndefinedFunctionCollector
        || constructProgramSlice
        || useFaultLocalizationWithDistanceMetrics
        || useArrayAbstraction
        || useRandomTestCaseGeneratorAlgorithm
        || analysisSequentializesCfa()) {
      // hard-coded dummy CPA
      return LocationCPA.factory().set(cfa, CFA.class).setConfiguration(config).createInstance();
    }

    return cpaFactory.buildCPAs(cfa, pSpecification, aggregatedReachedSets);
  }
}
