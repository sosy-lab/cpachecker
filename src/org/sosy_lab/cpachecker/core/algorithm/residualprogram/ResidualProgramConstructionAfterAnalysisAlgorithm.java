// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class ResidualProgramConstructionAfterAnalysisAlgorithm
    extends ResidualProgramConstructionAlgorithm implements StatisticsProvider {

  private final CFA cfa;
  private final Algorithm innerAlgorithm;

  private final Collection<Statistics> stats = new ArrayList<>();

  private static boolean isStop(AbstractState e) {
    AssumptionStorageState ass = AbstractStates.extractStateByType(e, AssumptionStorageState.class);
    return ass != null && ass.isStop();
  }

  public ResidualProgramConstructionAfterAnalysisAlgorithm(
      final CFA pCfa,
      final Algorithm pAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdown,
      final Specification pSpec)
      throws InvalidConfigurationException {
    super(pCfa, pConfig, pLogger, pShutdown, pSpec);

    cfa = pCfa;
    innerAlgorithm = pAlgorithm;

    if (innerAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) innerAlgorithm).collectStatistics(stats);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Preconditions.checkArgument(
        pReachedSet.getFirstState() instanceof ARGState,
        "Top most abstract state must be an ARG state");
    Preconditions.checkArgument(
        AbstractStates.extractLocation(pReachedSet.getFirstState()) != null,
        "Require location information to build residual program");

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    try {
      logger.log(Level.INFO, "Start analysis");
      status = status.update(innerAlgorithm.run(pReachedSet));
    } catch (InfeasibleCounterexampleException | RefinementFailedException e) {
      // ignore
    }

    if (!pReachedSet.hasWaitingState()
        && !from(pReachedSet).anyMatch(ResidualProgramConstructionAfterAnalysisAlgorithm::isStop)) {
      logger.log(Level.INFO, "Analysis complete");
      // analysis alone succeeded
      return status;
    }

    logger.log(
        Level.INFO,
        "Analysis incomplete, some states could not be explored. Generate residual program.");
    ARGState argRoot = (ARGState) pReachedSet.getFirstState();

    CFANode mainFunction = AbstractStates.extractLocation(argRoot);
    assert (mainFunction != null);

    Path assumptionAutomaton = null;

    if (usesParallelCompositionOfProgramAndCondition()) {
      try {
        assumptionAutomaton = TempFile.builder().prefix("assumptions").suffix("txt").create();

        try (Writer automatonWriter =
            IO.openOutputFile(assumptionAutomaton, Charset.defaultCharset())) {
          AssumptionCollectorAlgorithm.writeAutomaton(
              automatonWriter,
              argRoot,
              computeRelevantStates(pReachedSet),
              ImmutableSet.copyOf(pReachedSet.getWaitlist()),
              0,
              true,
              true);
        }
      } catch (IOException e1) {
        throw new CPAException(
            "Could not generate assumption automaton needed to generate residual program", e1);
      }
    }

    Pair<ARGState, ReachedSet> result =
        prepareARGToConstructResidualProgram(mainFunction, assumptionAutomaton);

    if (result == null || result.getFirst() == null) {
      throw new CPAException("Failed to build structure of residual program");
    }

    argRoot = result.getFirst();

    Set<ARGState> addPragma;
    try {
      statistic.collectPragmaPointsTimer.stop();
      switch (getStrategy()) {
        case COMBINATION:
          addPragma = getAllTargetStates(result.getSecond());
          break;
        case SLICING:
          addPragma = getAllTargetStatesNotFullyExplored(pReachedSet, result.getSecond());
          break;
        default: // CONDITION no effect
          addPragma = null;
      }
    } finally {
      statistic.collectPragmaPointsTimer.stop();
    }

    if (!writeResidualProgram(argRoot, addPragma)) {
      throw new CPAException("Failed to write residual program.");
    }

    return status;
  }

  private Set<ARGState> computeRelevantStates(final ReachedSet pReachedSet) {
    NavigableSet<ARGState> uncoveredAncestors = new TreeSet<>();
    Deque<ARGState> toAdd = new ArrayDeque<>();

    for (AbstractState unexplored : pReachedSet.getWaitlist()) {
      toAdd.push((ARGState) unexplored);
    }

    for (AbstractState stop :
        from(pReachedSet).filter(ResidualProgramConstructionAfterAnalysisAlgorithm::isStop)) {
      toAdd.push((ARGState) stop);
    }

    while (!toAdd.isEmpty()) {
      ARGState current = toAdd.pop();
      assert !current.isCovered();

      if (uncoveredAncestors.add(current)) {
        // current was not yet contained in parentSet,
        // so we need to handle its parents

        toAdd.addAll(current.getParents());

        for (ARGState coveredByCurrent : current.getCoveredByThis()) {
          toAdd.addAll(coveredByCurrent.getParents());
        }
      }
    }
    return uncoveredAncestors;
  }

  private Set<ARGState> getAllTargetStatesNotFullyExplored(
      final ReachedSet pIncompleteExploration, final ReachedSet pNodesOfInlinedProg)
      throws CPAException, InterruptedException {
    if (AbstractStates.extractStateByType(
            pIncompleteExploration.getFirstState(), CallstackState.class)
        == null) {
      return getAllTargetStatesNotFullyExploredBasedOnLocations(
          pIncompleteExploration.getWaitlist(), pNodesOfInlinedProg);
    } else {
      return getAllTargetStatesNotFullyExploredFunctionsInlined(
          pIncompleteExploration.getWaitlist(), pNodesOfInlinedProg);
    }
  }

  private Set<ARGState> getAllTargetStatesNotFullyExploredBasedOnLocations(
      final Collection<AbstractState> pUnexploredStates, final ReachedSet pNodesOfInlinedProg)
      throws InterruptedException {
    // overapproximating this set, considering all syntactical paths

    Set<CFANode> seen = Sets.newHashSetWithExpectedSize(cfa.getAllNodes().size());
    Deque<CFANode> toProcess = new ArrayDeque<>();
    CFANode current;

    for (AbstractState state : pUnexploredStates) {
      current = AbstractStates.extractLocation(state);
      Preconditions.checkNotNull(current);
      if (seen.add(current)) {
        toProcess.add(current);
      }
    }

    while (!toProcess.isEmpty()) {
      shutdown.shutdownIfNecessary();
      current = toProcess.pop();

      for (CFAEdge leaving : CFAUtils.leavingEdges(current)) {
        if (seen.add(leaving.getSuccessor())) {
          toProcess.push(leaving.getSuccessor());
        }
      }
    }

    return Sets.newHashSet(
        Iterables.filter(
            Iterables.filter(pNodesOfInlinedProg, ARGState.class),
            state -> state.isTarget() && seen.contains(AbstractStates.extractLocation(state))));
  }

  private Set<ARGState> getAllTargetStatesNotFullyExploredFunctionsInlined(
      final Collection<AbstractState> pUnexploredStates, final ReachedSet pNodesOfInlinedProg)
      throws CPAException, InterruptedException {
    // overapproximating this set, considering all syntactical paths

    Multimap<CFANode, CallstackStateEqualsWrapper> seen =
        HashMultimap.create(cfa.getAllNodes().size(), cfa.getNumberOfFunctions());
    Deque<Pair<CFANode, CallstackState>> toProcess = new ArrayDeque<>();
    Pair<CFANode, CallstackState> current, explored;

    for (AbstractState state : pUnexploredStates) {
      current =
          Pair.of(
              AbstractStates.extractLocation(state),
              AbstractStates.extractStateByType(state, CallstackState.class));
      if (seen.put(current.getFirst(), new CallstackStateEqualsWrapper(current.getSecond()))) {
        toProcess.add(current);
      }
    }

    CallstackCPA callstackCpa;
    try {
      callstackCpa = new CallstackCPA(Configuration.defaultConfiguration(), logger);
    } catch (InvalidConfigurationException e) {
      logger.log(
          Level.INFO,
          "Cannot use inlined representation to detect unexplored target states. ",
          "Use fall-back solution (less precise) and only consider locations.");
      return getAllTargetStatesNotFullyExploredBasedOnLocations(
          pUnexploredStates, pNodesOfInlinedProg);
    }
    CallstackTransferRelation csTr = callstackCpa.getTransferRelation();
    Collection<? extends AbstractState> csSucc;

    while (!toProcess.isEmpty()) {
      shutdown.shutdownIfNecessary();
      current = toProcess.pop();

      for (CFAEdge leaving : CFAUtils.leavingEdges(current.getFirst())) {
        csSucc =
            csTr.getAbstractSuccessorsForEdge(
                current.getSecond(), SingletonPrecision.getInstance(), leaving);
        if (!csSucc.isEmpty()) {
          explored = Pair.of(leaving.getSuccessor(), (CallstackState) csSucc.iterator().next());
          if (seen.put(
              explored.getFirst(), new CallstackStateEqualsWrapper(explored.getSecond()))) {
            toProcess.add(explored);
          }
        }
      }
    }

    return Sets.newHashSet(
        Iterables.filter(
            Iterables.filter(pNodesOfInlinedProg, ARGState.class),
            state ->
                seen.get(AbstractStates.extractLocation(state))
                    .contains(
                        new CallstackStateEqualsWrapper(
                            AbstractStates.extractStateByType(state, CallstackState.class)))));
  }

  private @Nullable Pair<ARGState, ReachedSet> prepareARGToConstructResidualProgram(
      final CFANode mainFunction, final @Nullable Path assumptionAutomaton) {
    try {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption(
          "CompositeCPA.cpas", "cpa.location.LocationCPA,cpa.callstack.CallstackCPA");
      configBuilder.setOption("cpa.automaton.breakOnTargetState", "-1");
      Configuration config = configBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(config, logger, shutdown, AggregatedReachedSets.empty());

      Specification spec = getSpecification();
      if (usesParallelCompositionOfProgramAndCondition()) {
        assert assumptionAutomaton != null;
        spec =
            spec.withAdditionalSpecificationFile(
                ImmutableSet.of(getAssumptionGuider(), assumptionAutomaton),
                cfa,
                config,
                logger,
                shutdown);
      }
      ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, spec);

      ReachedSet reached = coreComponents.createReachedSet(cpa);
      reached.add(
          cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition()));

      Algorithm algo = CPAAlgorithm.create(cpa, logger, config, shutdown);

      try {
        statistic.modelBuildTimer.start();
        algo.run(reached);
      } finally {
        statistic.modelBuildTimer.stop();
      }

      if (reached.hasWaitingState()) {
        logger.log(Level.SEVERE, "Analysis run to get structure of residual program is incomplete");
        return null;
      }

      return Pair.of((ARGState) reached.getFirstState(), reached);
    } catch (InvalidConfigurationException
        | CPAException
        | IllegalArgumentException
        | InterruptedException e1) {
      logger.log(Level.SEVERE, "Analysis to build structure of residual program failed", e1);
      return null;
    }
  }

  @Override
  protected void checkConfiguration() throws InvalidConfigurationException {
    if (usesParallelCompositionOfProgramAndCondition()) {
      if (getAssumptionGuider() == null) {
        throw new InvalidConfigurationException(
            "For current strategy "
                + getStrategy()
                + ", the control automaton guiding the exploration based on the condition is"
                + " needed. Please set the option residualprogram.assumptionGuider.");
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.addAll(stats);
  }
}
