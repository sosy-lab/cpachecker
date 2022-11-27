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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.NestingAlgorithm;
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
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "samplingAlgorithm")
public class SamplingAlgorithm extends NestingAlgorithm {

  @Option(secure = true, description = "The number of initial samples to collect before unrolling.")
  private int numInitialSamples = 5;

  private final CFA cfa;
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
    cfa = pCfa;

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

    // Create algorithm for building the ARG
    Algorithm builderAlgorithm;
    ConfigurableProgramAnalysis builderCPA;
    ReachedSet builderReachedSet;
    Solver solver;
    Path builderConfig = Path.of("config", "valueAnalysis-predicateAnalysis-Cegar-ABEl.properties");
    try {
      Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> buildARG =
          super.createAlgorithm(
              builderConfig,
              extractLocation(reachedSet.getFirstState()),
              cfa,
              ShutdownManager.createWithParent(shutdownNotifier),
              AggregatedReachedSets.empty(),
              ImmutableSet.of("analysis.useSamplingAlgorithm"),
              Sets.newHashSet());
      builderAlgorithm = buildARG.getFirst();
      builderCPA = buildARG.getSecond();
      builderReachedSet = buildARG.getThird();
      solver =
          CPAs.retrieveCPAOrFail(builderCPA, PredicateCPA.class, SamplingAlgorithm.class)
              .getSolver();
    } catch (InvalidConfigurationException e) {
      logger.log(
          Level.WARNING,
          "Could not create algorithm for building ARG due to invalid configuration");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not load configuration for building ARG");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    // Run analysis to create ARG
    reachedSet.setDelegate(builderReachedSet);
    logger.log(Level.INFO, "Building ARG...");
    builderAlgorithm.run(reachedSet);
    shutdownNotifier.shutdownIfNecessary();

    ImmutableSet.Builder<Sample> samples = ImmutableSet.builder();

    // Collect some positive samples using predicate-based and value-based sampling
    for (Loop loop : cfa.getLoopStructure().get().getAllLoops()) {
      for (Sample sample : getInitialPositiveSamples(reachedSet, solver, loop, numInitialSamples)) {
        samples.addAll(forwardUnrollingAlgorithm.unrollSample(sample, loop));
      }
    }

    // Now collect some negative samples for each target location
    for (CFANode targetNode : getTargetNodes()) {
      // Create algorithm for building the backward ARG
      Algorithm backwardsBuilderAlgorithm;
      ConfigurableProgramAnalysis backwardsBuilderCPA;
      ReachedSet backwardsBuilderReachedSet;
      Solver backwardsSolver;
      Path backwardsBuilderConfig = Path.of("config", "predicateAnalysisBackward.properties");
      try {
        Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> buildBackwardARG =
            super.createAlgorithm(
                backwardsBuilderConfig,
                targetNode,
                cfa,
                ShutdownManager.createWithParent(shutdownNotifier),
                AggregatedReachedSets.empty(),
                ImmutableSet.of("analysis.useSamplingAlgorithm"),
                Sets.newHashSet());
        backwardsBuilderAlgorithm = buildBackwardARG.getFirst();
        backwardsBuilderCPA = buildBackwardARG.getSecond();
        backwardsBuilderReachedSet = buildBackwardARG.getThird();
        backwardsSolver =
            CPAs.retrieveCPAOrFail(backwardsBuilderCPA, PredicateCPA.class, SamplingAlgorithm.class)
                .getSolver();
      } catch (InvalidConfigurationException e) {
        logger.log(
            Level.WARNING,
            "Could not create algorithm for building backward ARG due to invalid configuration");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not load configuration for building backward ARG");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }

      // Run analysis to create backward ARG
      reachedSet.setDelegate(backwardsBuilderReachedSet);
      logger.log(Level.INFO, "Building backward ARG...");
      backwardsBuilderAlgorithm.run(reachedSet);
      shutdownNotifier.shutdownIfNecessary();

      // Collect some negative samples using predicate-based and backward sampling
      for (Loop loop : cfa.getLoopStructure().get().getAllLoops()) {
        for (Sample sample :
            getInitialNegativeSamples(reachedSet, backwardsSolver, loop, numInitialSamples)) {
          samples.addAll(backwardUnrollingAlgorithm.unrollSample(sample, loop));
        }
      }
    }

    samples.build();

    // TODO: Then build samples and write to file

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private ImmutableSet<CFANode> getTargetNodes() throws InterruptedException {
    TargetLocationProvider tlp = new TargetLocationProviderImpl(shutdownNotifier, logger, cfa);
    Specification spec;
    try {
      spec =
          Specification.fromFiles(
              ImmutableSet.of(Path.of("config", "specification", "default.spc")),
              cfa,
              globalConfig,
              logger,
              shutdownNotifier);
    } catch (InvalidConfigurationException pE) {
      logger.log(Level.WARNING, "Could not determine target due to invalid configuration");
      return ImmutableSet.of();
    }
    return tlp.tryGetAutomatonTargetLocations(cfa.getMainFunction(), spec);
  }

  /**
   * Collect some initial positive samples, i.e. samples that may occur when entering the loop.
   *
   * <p>This function implements predicate-based sampling.
   */
  private Set<Sample> getInitialPositiveSamples(
      ForwardingReachedSet reachedSet, Solver solver, Loop loop, int numSamples)
      throws InterruptedException, SolverException {
    BooleanFormulaManager bfmgr = solver.getFormulaManager().getBooleanFormulaManager();

    // Collect some positive samples for each loop head.
    // Different loop heads potentially correspond to different paths to enter the loop and even if
    // some are redundant we collect at most numSamples * (numLoopHeads - 1) redundant samples which
    // should be small.
    Set<Sample> samples = new HashSet<>();
    for (CFANode loopHead : loop.getLoopHeads()) {
      // Extract path formulas at the loop head
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
      ImmutableSet<BooleanFormula> formulas = formulaBuilder.build();

      // Next, get satisfying models from SMT solver
      try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        for (BooleanFormula formula : formulas) {
          List<List<ValueAssignment>> models = new ArrayList<>(numSamples);
          prover.push(formula);
          while (models.size() < numSamples) {
            if (prover.isUnsat()) {
              logger.logf(
                  Level.INFO,
                  "Exhausted all satisfying models, collected only %s samples",
                  models.size());
              break;
            }
            models.add(prover.getModelAssignments());
            List<BooleanFormula> modelFormulas = new ArrayList<>();
            for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
              modelFormulas.add(modelAssignment.getAssignmentAsFormula());
            }
            prover.addConstraint(bfmgr.not(bfmgr.and(modelFormulas)));

            samples.add(extractSampleFromModel(prover.getModelAssignments(), loopHead));
          }
          prover.pop();
        }
      }
    }
    return samples;
  }

  private Set<Sample> getInitialNegativeSamples(
      ForwardingReachedSet reachedSet, Solver solver, Loop loop, int numSamples)
      throws InterruptedException, SolverException {
    BooleanFormulaManager bfmgr = solver.getFormulaManager().getBooleanFormulaManager();

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
      ImmutableSet<BooleanFormula> formulas = formulaBuilder.build();

      // Next, get satisfying models from SMT solver
      try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
        for (BooleanFormula formula : formulas) {
          List<List<ValueAssignment>> models = new ArrayList<>(numSamples);
          prover.push(formula);
          while (models.size() < numSamples) {
            if (prover.isUnsat()) {
              logger.logf(
                  Level.INFO,
                  "Exhausted all satisfying models, collected only %s samples",
                  models.size());
              break;
            }
            models.add(prover.getModelAssignments());
            List<BooleanFormula> modelFormulas = new ArrayList<>();
            for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
              modelFormulas.add(modelAssignment.getAssignmentAsFormula());
            }
            prover.addConstraint(bfmgr.not(bfmgr.and(modelFormulas)));

            samples.add(extractSampleFromModel(prover.getModelAssignments(), lastNode));
          }
          prover.pop();
        }
      }
    }
    return samples;
  }

  private Sample extractSampleFromModel(List<ValueAssignment> model, CFANode location) {
    Map<MemoryLocation, ValueAndType> variableValues = new HashMap<>();
    for (ValueAssignment assignment : model) {
      String varName = assignment.getName();
      List<String> parts = Splitter.on("@").splitToList(varName);
      if (!parts.get(0).contains("::")) {
        // Current assignment is a function result
        // TODO: Can this really not be an unqualified variable?
        continue;
      }
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
    return new Sample(variableValues, location);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    // Do nothing for now
    // TODO
  }
}
