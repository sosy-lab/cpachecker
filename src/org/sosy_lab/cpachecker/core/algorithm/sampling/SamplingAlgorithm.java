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
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
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

  @Option(secure = true, description = "The file where generated samples should be written to.")
  @FileOption(Type.OUTPUT_FILE)
  private Path outFile = Path.of("samples.json");

  @Option(
      secure = true,
      name = "initial.forward.config",
      description = "The configuration file to use for building the forward ARG.")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path initialForwardConfig;

  @Option(
      secure = true,
      name = "initial.backward.config",
      description = "The configuration file to use for building the backward ARG.")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path initialBackwardConfig;

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

    ConfigurationBuilder unrollingConfigBuilder =
        Configuration.builder()
            .copyFrom(pConfig)
            .setOption("cpa.predicate.direction", "FORWARD")
            .setOption("analysis.initialStatesFor", "ENTRY")
            .setOption("cpa.callstack.traverseBackwards", "false");
    forwardUnrollingAlgorithm =
        new SampleUnrollingAlgorithm(
            unrollingConfigBuilder.build(), pLogger, pShutdownManager, pCfa, pSpecification);

    unrollingConfigBuilder =
        Configuration.builder()
            .copyFrom(pConfig)
            .setOption("cpa.predicate.direction", "BACKWARD")
            .setOption("analysis.initialStatesFor", "TARGET")
            .setOption("cpa.callstack.traverseBackwards", "true");
    backwardUnrollingAlgorithm =
        new SampleUnrollingAlgorithm(
            unrollingConfigBuilder.build(), pLogger, pShutdownManager, pCfa, pSpecification);
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
    if (loops.size() != 1) {
      // TODO: Support multi-loop programs
      logger.log(Level.INFO, "Only single-loop programs are currently supported.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    Loop loop = loops.iterator().next();

    Collection<CFANode> targets = getTargetNodes();
    if (targets.size() != 1) {
      // TODO: Support multiple-error locations
      logger.log(Level.INFO, "Only programs with a single error location are currently supported.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    CFANode target = targets.iterator().next();

    // Build the reachedSet for the forward ARG
    Solver forwardSolver;
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

    // Build the reachedSet for the backward ARG
    Solver backwardsSolver;
    try {
      backwardsSolver = buildReachedSet(reachedSet, initialBackwardConfig, target);
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

    // Continuously collect samples until shutdown is requested
    ImmutableSet.Builder<Sample> samples = ImmutableSet.builder();
    Set<NoDuplicatesConstraint> constraints = new HashSet<>();
    while (!shutdownNotifier.shouldShutdown()) {
      // Collect positive samples using predicate-based sampling
      Set<Sample> positiveSamples = new HashSet<>();
      if (collectPositiveSamples) {
        positiveSamples =
            getInitialPositiveSamples(forwardReachedSet, forwardSolver, loop, constraints);
      }

      // Collect negative sampling using predicate-based sampling
      Set<Sample> negativeSamples = new HashSet<>();
      if (collectNegativeSamples) {
        negativeSamples =
            getInitialNegativeSamples(backwardReachedSet, backwardsSolver, loop, constraints);
      }

      // Shutdown if requested or neither positive nor negative samples can be found anymore
      if (shutdownNotifier.shouldShutdown()
          || (positiveSamples.isEmpty() && negativeSamples.isEmpty())) {
        break;
      }

      // Unroll positive samples using value-based sampling
      for (Sample sample : positiveSamples) {
        samples.addAll(forwardUnrollingAlgorithm.unrollSample(sample, loop));
      }

      // Unroll negative samples using backward sampling
      for (Sample sample : negativeSamples) {
        samples.addAll(backwardUnrollingAlgorithm.unrollSample(sample, loop));
      }

      // Export samples
      writeSamplesToFile(samples.build());
    }

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

  /**
   * Collect some initial positive samples, i.e. samples that may occur when entering the loop.
   *
   * <p>This function implements predicate-based sampling.
   */
  private Set<Sample> getInitialPositiveSamples(
      ReachedSet reachedSet, Solver solver, Loop loop, Set<NoDuplicatesConstraint> constraints)
      throws InterruptedException, SolverException {
    BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();

    // Collect some positive samples for each loop head.
    // Different loop heads potentially correspond to different paths to enter the loop and even if
    // some are redundant we collect at most numSamples * (numLoopHeads - 1) redundant samples which
    // should be small.
    Set<Sample> samples = new HashSet<>();
    for (CFANode loopHead : loop.getLoopHeads()) {
      ImmutableSet.Builder<BooleanFormula> formulaBuilder = ImmutableSet.builder();
      // TODO: We want a LocationMappedReachedSet inside the ForwardingReachedSet for maximum
      //  efficiency
      for (AbstractState state : reachedSet.getReached(loopHead)) {
        if (loopHead.equals(AbstractStates.extractLocation(state))) {
          PredicateAbstractState predicateState =
              AbstractStates.extractStateByType(state, PredicateAbstractState.class);
          BooleanFormula formula =
              bfmgr.and(
                  predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
                  predicateState.getPathFormula().getFormula());
          formulaBuilder.add(formula);
        }
      }
      samples.addAll(
          getSamplesForFormulas(
              formulaBuilder.build(), solver, loopHead, constraints, SampleClass.POSITIVE));
    }
    return samples;
  }

  private Set<Sample> getInitialNegativeSamples(
      ReachedSet reachedSet, Solver solver, Loop loop, Set<NoDuplicatesConstraint> constraints)
      throws InterruptedException, SolverException {
    BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();

    // Collect some negative samples for each potential loop exit.
    Set<Sample> samples = new HashSet<>();
    for (CFAEdge outgoing : loop.getOutgoingEdges()) {
      CFANode lastNode = outgoing.getPredecessor();

      // Extract path formulas at the considered node
      ImmutableSet.Builder<BooleanFormula> formulaBuilder = ImmutableSet.builder();
      // TODO: We want a LocationMappedReachedSet inside the ForwardingReachedSet for maximum
      //  efficiency
      for (AbstractState state : reachedSet.getReached(lastNode)) {
        if (lastNode.equals(AbstractStates.extractLocation(state))) {
          PredicateAbstractState predicateState =
              AbstractStates.extractStateByType(state, PredicateAbstractState.class);
          BooleanFormula formula =
              bfmgr.and(
                  predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
                  predicateState.getPathFormula().getFormula());
          formulaBuilder.add(formula);
        }
      }
      samples.addAll(
          getSamplesForFormulas(
              formulaBuilder.build(), solver, lastNode, constraints, SampleClass.NEGATIVE));
    }
    return samples;
  }

  private Set<Sample> getSamplesForFormulas(
      Set<BooleanFormula> pFormulas,
      Solver pSolver,
      CFANode pLocation,
      Set<NoDuplicatesConstraint> pConstraints,
      SampleClass pSampleClass)
      throws InterruptedException, SolverException {
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

        // Query solver for a random model, so we get easy access to the variable representations
        // (this is a hack)
        List<ValueAssignment> assignments = prover.getModelAssignments();
        Iterable<Formula> variableFormulas =
            FluentIterable.from(getRelevantAssignments(assignments, pLocation))
                .transform(ValueAssignment::getKey);

        // Collect some satisfying models
        while (collectedSamples < numInitialSamples) {
          List<ValueAssignment> model = samplingStrategy.getModel(fmgr, variableFormulas, prover);
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
          Iterable<ValueAssignment> relevantAssignments = getRelevantAssignments(model, pLocation);
          Sample sample = extractSampleFromModel(relevantAssignments, pLocation, pSampleClass);
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

  /**
   * Filter the given model for relevant assignments. Relevant for sampling are only the most recent
   * variable assignments to variables in the current function.
   */
  public static Iterable<ValueAssignment> getRelevantAssignments(
      Iterable<ValueAssignment> model, CFANode pLocation) {
    Map<String, Integer> highestIndizes = new HashMap<>();
    Map<String, ValueAssignment> latestAssignments = new HashMap<>();
    for (ValueAssignment assignment : model) {
      List<String> parts = Splitter.on("@").splitToList(assignment.getName());
      String qualifiedVar = parts.get(0);
      if (!qualifiedVar.contains("::")) {
        // Current assignment is a function result
        // TODO: Can this really not be an unqualified variable?
        continue;
      }
      String function = Splitter.on("::").splitToList(qualifiedVar).get(0);
      if (!function.equals(pLocation.getFunctionName())) {
        continue;
      }
      Integer index = Integer.valueOf(parts.get(1));
      if (index < highestIndizes.getOrDefault(qualifiedVar, 0)) {
        // We are interested in the most recent values of each variable
        continue;
      }
      highestIndizes.put(qualifiedVar, index);
      latestAssignments.put(qualifiedVar, assignment);
    }
    return latestAssignments.values();
  }

  public static Sample extractSampleFromModel(
      Iterable<ValueAssignment> assignments, CFANode location, SampleClass sampleClass) {
    Map<MemoryLocation, ValueAndType> variableValues = new HashMap<>();
    for (ValueAssignment assignment : assignments) {
      List<String> parts = Splitter.on("@").splitToList(assignment.getName());
      MemoryLocation var = MemoryLocation.fromQualifiedName(parts.get(0));

      Object value = assignment.getValue();
      ValueAndType valueAndType;
      if (value instanceof BigInteger) {
        valueAndType =
            new ValueAndType(new NumericValue((BigInteger) value), CNumericTypes.LONG_LONG_INT);
      } else {
        throw new AssertionError(
            "Unhandled type for value assignment: " + assignment.getValue().getClass());
      }
      variableValues.put(var, valueAndType);
    }
    return new Sample(variableValues, location, null, sampleClass);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    // Do nothing for now
    // TODO
  }
}
