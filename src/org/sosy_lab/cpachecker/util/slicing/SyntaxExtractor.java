/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.slicing;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "slicing")
public class SyntaxExtractor implements SlicingCriteriaExtractor {

  @Option(secure = true, name = "conditionFile", description = "path to condition file")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path conditionFile = Paths.get("output/AssumptionAutomaton.txt");

  private final Automaton condition;

  public SyntaxExtractor(
      final Configuration pConfig,
      final CFA pCfa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    List<Automaton> automata =
        AutomatonParser.parseAutomatonFile(
            conditionFile,
            pConfig,
            pLogger,
            pCfa.getMachineModel(),
            pCfa.getLanguage() == Language.C
                ? new CProgramScope(pCfa, pLogger)
                : DummyScope.getInstance(),
            pCfa.getLanguage(),
            pShutdownNotifier);
    if (automata.size() != 1) {
      throw new InvalidConfigurationException("Require exactly one condition automaton.");
    }
    condition = automata.get(0);
  }

  @Override
  public Set<CFAEdge> getSlicingCriteria(
      final CFA pCfa,
      final Specification pError,
      final ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger)
      throws InterruptedException {

    Multimap<CFANode, CFAEdge> targetsReachableFrom =
        computeReachableTargetsPerLocation(
            getTargetStates(pCfa, pError, pShutdownNotifier, pLogger), pShutdownNotifier);

    Set<CFAEdge> notFoundTargets = new HashSet<>(targetsReachableFrom.values());
    List<CFAEdge> relevantTargets = new ArrayList<>(notFoundTargets.size());
    Collection<CFAEdge> allEdges = extractAllCFAEdges(pCfa);

    // currently we assume that
    // (1) we goto dedicated state FALSE when successors are not explored
    // (2) TRUE -> ... edge never necessary
    for (AutomatonInternalState state : condition.getStates()) {
      if (state.isTarget()) {
        continue;
      }
      for (CFAEdge edge : allEdges) {
        if (notFoundTargets.contains(edge) && state.nontriviallyMatches(edge, pLogger)) {
          notFoundTargets.remove(edge);
          relevantTargets.add(edge);
        } else if (state.nontriviallyMatchesAndEndsIn(edge, "FALSE", pLogger)) {
          relevantTargets.addAll(targetsReachableFrom.get(edge.getSuccessor()));
          notFoundTargets.removeAll(targetsReachableFrom.get(edge.getSuccessor()));
        }

        if (notFoundTargets.isEmpty()) {
          return new HashSet<>(relevantTargets);
        }
      }
    }

    return new HashSet<>(relevantTargets);
  }

  private Collection<CFAEdge> extractAllCFAEdges(final CFA pCfa) {
    Collection<CFAEdge> edges = new ArrayList<>(2 * pCfa.getAllNodes().size());

    for (CFANode node : pCfa.getAllNodes()) {
      CFAUtils.allLeavingEdges(node).copyInto(edges);
    }
    return edges;
  }

  private Iterable<ARGState> getTargetStates(
      final CFA pCfa,
      final Specification targetSpec,
      final ShutdownNotifier pShutdown,
      final LogManager pLogger)
      throws InterruptedException {
    try {
      // Create new configuration with default set of CPAs
      // might not work correctly in the presence of recursion and function pointers
      ConfigurationBuilder configurationBuilder = Configuration.builder();
      configurationBuilder.loadFromResource(getClass(), "find-target-locations.properties");
      configurationBuilder.setOption("cpa.automaton.breakOnTargetState", "0");
      configurationBuilder.setOption("ARGCPA.cpa", "");
      Configuration configuration = configurationBuilder.build();

      ReachedSetFactory reachedSetFactory = new ReachedSetFactory(configuration, pLogger);
      CPABuilder cpaBuilder = new CPABuilder(configuration, pLogger, pShutdown, reachedSetFactory);
      final ConfigurableProgramAnalysis cpa =
          cpaBuilder.buildCPAs(pCfa, targetSpec, new AggregatedReachedSets());

      ReachedSet reached = reachedSetFactory.create();
      reached.add(
          cpa.getInitialState(pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(
              pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));
      CPAAlgorithm targetFindingAlgorithm =
          CPAAlgorithm.create(cpa, pLogger, configuration, pShutdown);

      pShutdown.shutdownIfNecessary();
      try {

        targetFindingAlgorithm.run(reached);

        Preconditions.checkState(!reached.hasWaitingState());

        return from(reached)
            .filter(AbstractStates.IS_TARGET_STATE)
            .transform(state -> AbstractStates.extractStateByType(state, ARGState.class));

      } finally {
        CPAs.closeCpaIfPossible(cpa, pLogger);
        CPAs.closeIfPossible(targetFindingAlgorithm, pLogger);
      }
    } catch (InvalidConfigurationException | CPAException | IllegalArgumentException e) {
      // Supplied configuration should not fail.
      throw new AssertionError(
          "Computation of target states that are syntactically reachable failed unexpectedly.", e);
    }
  }

  private Multimap<CFANode, CFAEdge> computeReachableTargetsPerLocation(
      final Iterable<ARGState> targets, final ShutdownNotifier pShutdown)
      throws InterruptedException {
    Multimap<CFANode, CFAEdge> locToTargets = HashMultimap.create();
    CFAEdge targetEdge;
    List<CFAEdge> pathSeq;
    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    ARGState succ;
    for (ARGState target : targets) {
      pShutdown.shutdownIfNecessary();

      for (ARGState predTarget : target.getParents()) {
        pShutdown.shutdownIfNecessary();

        pathSeq = predTarget.getEdgesToChild(target);
        targetEdge = pathSeq.get(pathSeq.size() - 1);
        addToMultiMap(targetEdge, pathSeq, locToTargets);

        seen.clear();
        seen.add(predTarget);
        waitlist.add(predTarget);

        while (!waitlist.isEmpty()) {
          pShutdown.shutdownIfNecessary();

          succ = waitlist.pop();
          for (ARGState pred : succ.getParents()) {
            pathSeq = pred.getEdgesToChild(succ);
            addToMultiMap(targetEdge, pathSeq, locToTargets);
            if (seen.add(pred)) {
              waitlist.push(pred);
            }
          }
        }
      }
    }
    return locToTargets;
  }

  private void addToMultiMap(
      final CFAEdge pTargetEdge,
      final List<CFAEdge> edges,
      final Multimap<CFANode, CFAEdge> locToTargets) {
    for (CFAEdge edge : edges) {
      locToTargets.put(edge.getPredecessor(), pTargetEdge);
    }
  }
}
