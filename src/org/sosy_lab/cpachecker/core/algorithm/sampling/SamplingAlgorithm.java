// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.NestingAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "samplingAlgorithm")
public class SamplingAlgorithm extends NestingAlgorithm {

  private enum Strategy {
    CONSTRAINED_UNIFORM,
    SMT_RANDOM
  }

  private record NoDuplicatesConstraint(
      Sample sample, BooleanFormula formula, FormulaManagerView fmgr) {}

  @Option(secure = true, description = "The number of initial samples to collect before unrolling.")
  private int numInitialSamples = 5;

  @Option(secure = true, description = "Whether positive samples should be collected.")
  private boolean collectPositiveSamples = true;

  @Option(secure = true, description = "Whether negative samples should be collected.")
  private boolean collectNegativeSamples = true;

  @Option(secure = true, description = "Whether test-based sampling should be used")
  private boolean useTestBasedSampling = false;

  @Option(secure = true, description = "The file where generated samples should be written to.")
  @FileOption(Type.OUTPUT_FILE)
  private Path outFile = Path.of("samples.json");

  @Option(
      secure = true,
      description = "The configuration file to use for building the forward ARG.")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path initialForwardConfig;

  @Option(
      secure = true,
      description = "The configuration file to use for unrolling positive samples.")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path forwardUnrollingConfig;

  @Option(
      secure = true,
      description = "The configuration file to use for building the backward ARG.")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path initialBackwardConfig;

  @Option(
      secure = true,
      description = "The configuration file to use for unrolling negative samples.")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path backwardUnrollingConfig;

  @Option(secure = true, description = "The strategy to use for initial sample generation.")
  private Strategy strategy = Strategy.SMT_RANDOM;

  private final CFA cfa;
  private final Specification samplingSpecification;
  private final SamplingStrategy samplingStrategy;
  private final SampleUnrollingAlgorithm forwardUnrollingAlgorithm;
  private final SampleUnrollingAlgorithm backwardUnrollingAlgorithm;

  public SamplingAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownManager pShutdownManager,
      CFA pCfa,
      Specification pSpecification)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownManager.getNotifier(), Specification.alwaysSatisfied());
    pConfig.inject(this);
    if (!collectPositiveSamples && !collectNegativeSamples) {
      throw new InvalidConfigurationException(
          "At least one of samplingAlgorithm.collectPositiveSamples"
              + " and samplingAlgorithm.collectNegativeSamples must be true.");
    }

    cfa = pCfa;
    samplingSpecification = pSpecification;

    samplingStrategy =
        switch (strategy) {
          case CONSTRAINED_UNIFORM -> new ConstrainedUniformSamplingStrategy(pConfig);
          case SMT_RANDOM -> new SMTRandomSamplingStrategy();
        };

    try {
      Configuration forwardConfig =
          Configuration.builder()
              .copyFrom(pConfig)
              .clearOption("analysis.useSamplingAlgorithm")
              .loadFromFile(forwardUnrollingConfig)
              .build();
      forwardUnrollingAlgorithm =
          new SampleUnrollingAlgorithm(
              forwardConfig, pLogger, pShutdownManager, pCfa, pSpecification);

      Configuration backwardConfig =
          Configuration.builder()
              .copyFrom(pConfig)
              .clearOption("analysis.useSamplingAlgorithm")
              .loadFromFile(backwardUnrollingConfig)
              .build();
      backwardUnrollingAlgorithm =
          new SampleUnrollingAlgorithm(
              backwardConfig, pLogger, pShutdownManager, pCfa, pSpecification);
    } catch (IOException pE) {
      throw new InvalidConfigurationException("Could not load unrolling config", pE);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    try {
      return run0(reachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver failure: " + e.getMessage(), e);
    }
  }

  private AlgorithmStatus run0(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, SolverException {
    Preconditions.checkArgument(
        pReachedSet instanceof ForwardingReachedSet,
        "SamplingAlgorithm needs ForwardingReachedSet");
    ForwardingReachedSet reachedSet = (ForwardingReachedSet) pReachedSet;

    if (cfa.getLoopStructure().isEmpty()) {
      logger.log(Level.INFO, "Program contains no loops, nothing to do for SamplingAlgorithm.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    Collection<Loop> loops = cfa.getLoopStructure().orElseThrow().getAllLoops();

    Collection<CFANode> targets = getTargetNodes();
    if (targets.size() != 1) {
      // TODO: Support multiple-error locations
      logger.log(Level.INFO, "Only programs with a single error location are currently supported.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    CFANode target = targets.iterator().next();

    // Prepare generation of initial positive samples using predicate-based sampling
    Solver forwardSolver;
    Map<CFANode, ImmutableSet<BooleanFormula>> formulasForPositiveSamples;
    Multimap<Loop, Formula> relevantVariablesPositive;
    // Build the reachedSet for the forward ARG.
    // This is always necessary to collect the relevant variables.
    try {
      forwardSolver =
          buildReachedSet(
              reachedSet, initialForwardConfig, extractLocation(pReachedSet.getFirstState()));
    } catch (InvalidConfigurationException e) {
      logger.log(
          Level.WARNING,
          "Could not create algorithm for building ARG due to invalid configuration");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not load configuration for building ARG");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    ReachedSet forwardReachedSet = reachedSet.getDelegate();
    BooleanFormulaManagerView bfmgr = forwardSolver.getFormulaManager().getBooleanFormulaManager();

    // Collect formulas and relevant variables for each loop
    ImmutableMap.Builder<CFANode, ImmutableSet<BooleanFormula>> positiveFormulaBuilder =
        ImmutableMap.builder();
    ImmutableListMultimap.Builder<Loop, Formula> relevantVariablePosBuilder =
        ImmutableListMultimap.builder();
    for (Loop loop : loops) {
      ImmutableSet.Builder<Formula> relevantVariableBuilder = ImmutableSet.builder();
      for (CFANode node : loop.getLoopNodes()) {
        ImmutableSet<BooleanFormula> formulas =
            collectFormulasFromReachedSet(forwardReachedSet, node, bfmgr);
        relevantVariableBuilder.addAll(
            getRelevantVariablesForFormulas(forwardSolver, node, formulas));
        if (loop.getLoopHeads().contains(node)) {
          positiveFormulaBuilder.put(node, formulas);
        }
      }
      relevantVariablePosBuilder.putAll(loop, relevantVariableBuilder.build());
    }
    formulasForPositiveSamples = positiveFormulaBuilder.buildOrThrow();
    relevantVariablesPositive = relevantVariablePosBuilder.build();
    if (relevantVariablesPositive.isEmpty()) {
      // No relevant variables means nothing to do or a bug
      // TODO: What do you mean "or a bug"?
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    // Prepare generation of initial negative samples using predicate-based sampling
    Solver backwardSolver = null;
    Map<CFANode, ImmutableSet<BooleanFormula>> formulasForNegativeSamples = ImmutableMap.of();
    Multimap<Loop, Formula> relevantVariablesNegative = ImmutableListMultimap.of();
    if (collectNegativeSamples) {
      // Build the reachedSet for the backward ARG
      try {
        backwardSolver = buildReachedSet(reachedSet, initialBackwardConfig, target);
      } catch (InvalidConfigurationException e) {
        logger.log(
            Level.WARNING,
            "Could not create algorithm for building backward ARG due to invalid configuration");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not load configuration for building backward ARG");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
      ReachedSet backwardReachedSet = reachedSet.getDelegate();
      FormulaManagerView backwardFmgr = backwardSolver.getFormulaManager();
      BooleanFormulaManagerView backwardBfmgr = backwardFmgr.getBooleanFormulaManager();

      // Collect formulas and relevant variables for each loop
      ImmutableMap.Builder<CFANode, ImmutableSet<BooleanFormula>> negativeFormulaBuilder =
          ImmutableMap.builder();
      ImmutableListMultimap.Builder<Loop, Formula> relevantVariableNegBuilder =
          ImmutableListMultimap.builder();
      for (Loop loop : loops) {
        ImmutableSet.Builder<Formula> relevantVariableBuilder = ImmutableSet.builder();
        for (CFANode node : loop.getLoopNodes()) {
          ImmutableSet<BooleanFormula> formulas =
              collectFormulasFromReachedSet(backwardReachedSet, node, backwardBfmgr);
          relevantVariableBuilder.addAll(
              getRelevantVariablesForFormulas(backwardSolver, node, formulas));
          if (loop.getLoopHeads().contains(node)) {
            negativeFormulaBuilder.put(node, formulas);
          }
        }
        // The relevant variables are the same for positive and for negative samples, but we need to
        // avoid using formulas from other solver contexts and SSA indices are different between
        // forward and backward analysis.
        relevantVariableNegBuilder.putAll(loop, relevantVariableBuilder.build());
      }
      formulasForNegativeSamples = negativeFormulaBuilder.buildOrThrow();
      relevantVariablesNegative = relevantVariableNegBuilder.build();
      if (relevantVariablesNegative.isEmpty()) {
        // No relevant variables means nothing to do or a bug
        // TODO: What do you mean "or a bug"?
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
    }

    // Continuously collect samples until shutdown is requested
    ImmutableSet.Builder<Sample> samples = ImmutableSet.builder();
    Set<NoDuplicatesConstraint> constraints = new HashSet<>();
    logger.log(Level.INFO, "Collecting samples...");
    while (!shutdownNotifier.shouldShutdown()) {
      // Collect positive samples using predicate-based sampling
      Set<Sample> positiveSamples = new HashSet<>();
      if (collectPositiveSamples) {
        for (Entry<CFANode, ImmutableSet<BooleanFormula>> entry :
            formulasForPositiveSamples.entrySet()) {
          CFANode loopHead = entry.getKey();
          ImmutableSet<BooleanFormula> formulas = entry.getValue();
          Loop loop = getLoopForLoopHead(loopHead, loops);
          positiveSamples.addAll(
              getSamplesForFormulas(
                  formulas,
                  forwardSolver,
                  loopHead,
                  relevantVariablesPositive.get(loop),
                  constraints,
                  SampleClass.POSITIVE));
        }
        logger.logf(Level.FINER, "Collected %s initial positive samples", positiveSamples.size());
      }

      // Collect negative sampling using predicate-based sampling
      Set<Sample> negativeSamples = new HashSet<>();
      if (collectNegativeSamples) {
        for (Entry<CFANode, ImmutableSet<BooleanFormula>> entry :
            formulasForNegativeSamples.entrySet()) {
          CFANode loopHead = entry.getKey();
          ImmutableSet<BooleanFormula> formulas = entry.getValue();
          Loop loop = getLoopForLoopHead(loopHead, loops);
          negativeSamples.addAll(
              getSamplesForFormulas(
                  formulas,
                  backwardSolver,
                  loopHead,
                  relevantVariablesNegative.get(loop),
                  constraints,
                  SampleClass.NEGATIVE));
        }
        logger.logf(Level.FINER, "Collected %s initial negative samples", negativeSamples.size());
      }

      // Shutdown if requested or neither positive nor negative samples can be found anymore
      if (shutdownNotifier.shouldShutdown()
          || (positiveSamples.isEmpty() && negativeSamples.isEmpty())) {
        break;
      }

      // Unroll positive samples using value-based sampling
      for (Sample sample : positiveSamples) {
        samples.addAll(
            forwardUnrollingAlgorithm.unrollSample(
                sample, getLoopForLoopHead(sample.getLocation(), loops)));
      }

      // Unroll negative samples using backward sampling
      for (Sample sample : negativeSamples) {
        samples.addAll(
            backwardUnrollingAlgorithm.unrollSample(
                sample, getLoopForLoopHead(sample.getLocation(), loops)));
      }

      // Use test-based sampling to potentially find negative samples
      if (useTestBasedSampling) {
        for (Loop loop : loops) {
          // Create an unknown sample
          List<ValueAssignment> model;
          try (ProverEnvironment prover =
              forwardSolver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            model =
                samplingStrategy.getModel(
                    forwardSolver.getFormulaManager(), relevantVariablesPositive.get(loop), prover);
          }
          for (CFANode loopHead : loop.getLoopHeads()) {
            Sample unknownSample =
                SampleUtils.extractSampleFromRelevantAssignments(
                    model, loopHead, SampleClass.UNKNOWN);

            // Unroll unknown sample, add to known samples if they can be classified
            Set<Sample> unrolledSamples =
                forwardUnrollingAlgorithm.unrollSample(unknownSample, loop);
            samples.addAll(
                unrolledSamples.stream()
                    .filter(s -> s.getSampleClass() != SampleClass.UNKNOWN)
                    .toList());
          }
        }
      }

      // Export samples
      writeSamplesToFile(samples.build());
    }
    logger.log(Level.INFO, "Finished sample collection");

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /**
   * Collects the states reachable from the initial location and returns the Solver instance used
   * during the analysis.
   *
   * <p>The caller can retrieve the constructed ReachedSet from the ForwardingReachedSet passed as
   * argument.
   */
  private Solver buildReachedSet(
      ForwardingReachedSet pReachedSet, Path pConfigFile, CFANode pInitialNode)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    // Create components for building the reachedSet
    NestedAnalysis components =
        super.createAlgorithm(
            pConfigFile,
            pInitialNode,
            cfa,
            ShutdownManager.createWithParent(shutdownNotifier),
            AggregatedReachedSets.empty(),
            ImmutableSet.of("analysis.useSamplingAlgorithm"),
            new CopyOnWriteArrayList<>());
    Algorithm algorithm = components.algorithm();
    ConfigurableProgramAnalysis cpa = components.cpa();
    ReachedSet reached = components.reached();
    Solver solver =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, SamplingAlgorithm.class).getSolver();

    // Run analysis to create backward ARG
    pReachedSet.setDelegate(reached);
    logger.log(Level.INFO, "Building reachedSet...");
    algorithm.run(pReachedSet);
    shutdownNotifier.shutdownIfNecessary();
    return solver;
  }

  private void writeSamplesToFile(Set<Sample> samples) {
    StringJoiner sj = new StringJoiner(",\n", "[\n", "]\n");
    for (Sample sample : samples) {
      sj.add(sample.export());
    }
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(sj.toString());
    } catch (IOException e) {
      logger.log(Level.WARNING, "Export of produced samples failed");
    }
  }

  private ImmutableSet<CFANode> getTargetNodes() {
    TargetLocationProvider tlp = new TargetLocationProviderImpl(shutdownNotifier, logger, cfa);
    return tlp.tryGetAutomatonTargetLocations(cfa.getMainFunction(), samplingSpecification);
  }

  /** Extract the formulas that may hold at the given location from the reached set. */
  private ImmutableSet<BooleanFormula> collectFormulasFromReachedSet(
      ReachedSet reachedSet, CFANode location, BooleanFormulaManagerView bfmgr) {
    ImmutableSet.Builder<BooleanFormula> formulaBuilder = ImmutableSet.builder();
    // TODO: We want a LocationMappedReachedSet inside the ForwardingReachedSet for maximum
    //  efficiency
    for (AbstractState state : reachedSet.getReached(location)) {
      if (location.equals(AbstractStates.extractLocation(state))) {
        PredicateAbstractState predicateState =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);
        BooleanFormula formula =
            bfmgr.and(
                predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
                predicateState.getPathFormula().getFormula());
        formulaBuilder.add(formula);
      }
    }
    return formulaBuilder.build();
  }

  private Set<Sample> getSamplesForFormulas(
      Set<BooleanFormula> pFormulas,
      Solver pSolver,
      CFANode pLocation,
      Collection<Formula> relevantVariables,
      Set<NoDuplicatesConstraint> pConstraints,
      SampleClass pSampleClass)
      throws InterruptedException, SolverException {
    assert !Iterables.isEmpty(relevantVariables);
    Set<Sample> samples = new HashSet<>();
    FormulaManagerView fmgr = pSolver.getFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    try (ProverEnvironment prover = pSolver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (BooleanFormula formula : pFormulas) {
        int collectedSamples = 0;
        prover.push(formula);

        // Add constraints to prevent previously found samples from being found again
        for (NoDuplicatesConstraint constraint : pConstraints) {
          if (constraint.sample().getSampleClass().equals(pSampleClass)
              && constraint.sample().getLocation().equals(pLocation)) {
            BooleanFormula constraintFormula =
                fmgr.translateFrom(constraint.formula(), constraint.fmgr());
            prover.addConstraint(constraintFormula);
          }
        }
        if (prover.isUnsat()) {
          // Formula is UNSAT even without additional constraints, there is nothing to do
          logger.log(Level.INFO, "Encountered unsatisfiable formula, no samples exist");
          prover.pop();
          continue;
        }

        // Collect some satisfying models
        while (collectedSamples < numInitialSamples) {
          List<ValueAssignment> model = samplingStrategy.getModel(fmgr, relevantVariables, prover);
          if (model == null) {
            logger.logf(
                Level.INFO,
                "Exhausted all satisfying models, collected only %s samples for formula %s",
                collectedSamples,
                formula);
            break;
          }
          collectedSamples++;

          // Extract sample
          Iterable<ValueAssignment> relevantAssignments =
              SampleUtils.getRelevantAssignments(model, pLocation);
          Sample sample =
              SampleUtils.extractSampleFromModel(relevantAssignments, pLocation, pSampleClass);
          samples.add(sample);

          // Add constraint to avoid getting the same model again
          Set<BooleanFormula> relevantFormulas =
              FluentIterable.from(relevantAssignments)
                  .transform(ValueAssignment::getAssignmentAsFormula)
                  .toSet();
          BooleanFormula constraint = bfmgr.not(bfmgr.and(relevantFormulas));
          prover.addConstraint(constraint);
          pConstraints.add(new NoDuplicatesConstraint(sample, constraint, fmgr));
        }

        prover.pop();
      }
    }
    return samples;
  }

  private ImmutableSet<Formula> getRelevantVariablesForFormulas(
      Solver pSolver, CFANode pLocation, Set<BooleanFormula> pFormulas)
      throws InterruptedException, SolverException {
    ImmutableSet.Builder<Formula> relevantVariableBuilder = ImmutableSet.builder();

    try (ProverEnvironment prover = pSolver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      for (BooleanFormula formula : pFormulas) {
        prover.push(formula);
        if (!prover.isUnsat()) {
          List<ValueAssignment> assignments = prover.getModelAssignments();
          for (ValueAssignment relevantVariableAssignment :
              SampleUtils.getRelevantAssignments(assignments, pLocation)) {
            relevantVariableBuilder.add(relevantVariableAssignment.getKey());
          }
        }
        prover.pop();
      }
    }
    return relevantVariableBuilder.build();
  }

  private @Nullable Loop getLoopForLoopHead(CFANode pLoopHead, Collection<Loop> pLoops) {
    for (Loop loop : pLoops) {
      if (loop.getLoopHeads().contains(pLoopHead)) {
        return loop;
      }
    }
    return null;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    // Do nothing for now
    // TODO
  }
}
