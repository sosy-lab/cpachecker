// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.NestingAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
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

  public SamplingAlgorithm(
      Configuration pConfig, LogManager pLogger, ShutdownManager pShutdownManager, CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownManager.getNotifier(), Specification.alwaysSatisfied());
    pConfig.inject(this);
    cfa = pCfa;
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

    if (cfa.getAllLoopHeads().isEmpty()) {
      logger.log(Level.INFO, "Program contains no loops, nothing to do for SamplingAlgorithm.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    Set<Sample> samples = getInitialSamples(reachedSet, numInitialSamples);
    for (Sample sample : samples) {
      // TODO: Unroll samples
    }

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /**
   * Collect some initial samples, i.e. positive samples that may occur when entering the loop for
   * the first time.
   */
  private Set<Sample> getInitialSamples(ForwardingReachedSet reachedSet, int numSamples)
      throws InterruptedException, CPAException, SolverException {
    // Create algorithm for finding initial sample
    Algorithm sampleAlgorithm;
    ConfigurableProgramAnalysis sampleCPA;
    ReachedSet sampleReachedSet;
    Solver solver;
    Path samplingConfig =
        Path.of("config", "valueAnalysis-predicateAnalysis-Cegar-ABEl.properties");
    // TODO: Generate specification based on loop structure
    Path samplingSpec = Path.of("..", "sample_config.spc");
    try {
      Specification initialSamplingSpecification =
          Specification.fromFiles(
              ImmutableSet.of(samplingSpec), cfa, globalConfig, logger, shutdownNotifier);
      Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> findInitialSample =
          createNextAlgorithm(
              samplingConfig,
              extractLocation(reachedSet.getFirstState()),
              cfa,
              ShutdownManager.createWithParent(shutdownNotifier),
              null,
              initialSamplingSpecification);
      sampleAlgorithm = findInitialSample.getFirst();
      sampleCPA = findInitialSample.getSecond();
      sampleReachedSet = findInitialSample.getThird();
      solver =
          CPAs.retrieveCPAOrFail(sampleCPA, PredicateCPA.class, SamplingAlgorithmProto.class)
              .getSolver();
    } catch (InvalidConfigurationException e) {
      logger.log(
          Level.WARNING,
          "Could not create algorithm for collecting initial samples due to invalid configuration");
      return ImmutableSet.of();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not load configuration for initial sampling");
      return ImmutableSet.of();
    }

    // Run analysis until loop is entered.
    // This is done by considering the loop start as target location.
    reachedSet.setDelegate(sampleReachedSet);
    logger.log(Level.INFO, "Collecting initial samples");
    sampleAlgorithm.run(sampleReachedSet);
    shutdownNotifier.shutdownIfNecessary();
    // If the loop can never be entered there is no need for a loop invariant
    if (!sampleReachedSet.wasTargetReached()) {
      logger.logf(Level.WARNING, "Loop not reachable, failed to generate samples");
      return ImmutableSet.of();
    }

    // The config used for finding initial samples already creates a counterexample, so there is no
    // need to build another target path.
    ARGState lastState = (ARGState) reachedSet.getLastState();
    assert lastState.isTarget();
    ARGPath targetPath = lastState.getCounterexampleInformation().orElseThrow().getTargetPath();

    // Extract initial samples from target path
    return generateSamplesFromTargetPath(targetPath, solver, numSamples);
  }

  private Set<Sample> generateSamplesFromTargetPath(
      ARGPath targetPath, Solver solver, int numSamples)
      throws InterruptedException, SolverException {
    // Create formula for target location
    BooleanFormulaManager bfmgr = solver.getFormulaManager().getBooleanFormulaManager();
    List<BooleanFormula> pathFormulas = new ArrayList<>();
    List<AbstractState> abstractionStates =
        from(targetPath.asStatesList())
            .skip(1)
            .filter(PredicateAbstractState::containsAbstractionState)
            .transform(state -> (AbstractState) state)
            .toList();
    for (PredicateAbstractState e :
        AbstractStates.projectToType(abstractionStates, PredicateAbstractState.class)) {
      // Conjuncting block formula of last abstraction and current path formula
      // works regardless of state is an abstraction state or not.
      BooleanFormula pathFormula =
          bfmgr.and(
              e.getAbstractionFormula().getBlockFormula().getFormula(),
              e.getPathFormula().getFormula());
      pathFormulas.add(pathFormula);
    }
    BooleanFormula formula = bfmgr.and(pathFormulas);

    // Collect models
    List<List<ValueAssignment>> models = new ArrayList<>();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
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
      }
      prover.pop();
    }

    // Extract samples from models
    Set<Sample> samples = new HashSet<>();
    for (List<ValueAssignment> model : models) {
      Map<MemoryLocation, Object> variableValues = new HashMap<>();
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
        variableValues.put(var, value);
      }
      samples.add(new Sample(formula, solver.getFormulaManager(), variableValues));
    }
    return samples;
  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createNextAlgorithm(
      Path singleConfigFileName,
      CFANode pInitialNode,
      CFA pCfa,
      ShutdownManager singleShutdownManager,
      ReachedSet currentReachedSet,
      Specification pSpecification)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {
    AggregatedReachedSets aggregatedReachedSets;
    if (currentReachedSet != null) {
      aggregatedReachedSets = AggregatedReachedSets.singleton(currentReachedSet);
    } else {
      aggregatedReachedSets = AggregatedReachedSets.empty();
    }

    return super.createAlgorithmWithSpecification(
        singleConfigFileName,
        pInitialNode,
        pCfa,
        singleShutdownManager,
        aggregatedReachedSets,
        pSpecification,
        ImmutableSet.of("analysis.useSamplingAlgorithm"),
        Sets.newHashSet());
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    // Do nothing for now
    // TODO
  }
}
