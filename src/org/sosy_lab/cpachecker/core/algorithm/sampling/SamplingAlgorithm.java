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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
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
  private final SampleUnrollingAlgorithm unrollingAlgorithm;

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
    unrollingAlgorithm =
        new SampleUnrollingAlgorithm(pConfig, pLogger, pShutdownManager, pCfa, pSpecification);
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
          "Could not create algorithm for collecting initial samples due to invalid configuration");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not load configuration for initial sampling");
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
        samples.addAll(unrollingAlgorithm.unrollSample(sample, loop));
      }
    }

    // TODO: Now get initial negative samples and unroll backwards

    // TODO: Then build samples and write to file

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
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
