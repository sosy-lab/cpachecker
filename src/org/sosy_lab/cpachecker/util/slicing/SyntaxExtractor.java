// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
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
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;

@Options(prefix = "slicing")
public class SyntaxExtractor implements SlicingCriteriaExtractor {

  @Option(secure = true, name = "conditionFile", description = "path to condition file")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path conditionFile = Path.of("output/AssumptionAutomaton.txt");

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
    try {
      condition = Iterables.getOnlyElement(automata);
    } catch (NoSuchElementException e) {
      throw new InvalidConfigurationException("Require exactly one condition automaton.", e);
    }
  }

  @Override
  public Set<CFAEdge> getSlicingCriteria(
      final CFA pCfa,
      final Specification pError,
      final ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger)
      throws InterruptedException {

    Multimap<CFANode, CFAEdge> nodesReachingTargetEdges =
        computeReachableTargetsPerLocation(
            getTargetNodes(pCfa, pError, pShutdownNotifier, pLogger), pShutdownNotifier);

    Set<CFAEdge> notFoundTargets = new HashSet<>(nodesReachingTargetEdges.values());
    ImmutableSet.Builder<CFAEdge> relevantTargets = ImmutableSet.builder();
    Collection<CFAEdge> allEdges = extractAllCFAEdges(pCfa);

    // currently we assume that
    // (1) we goto dedicated state __FALSE when successors are not explored
    // (2) __TRUE -> ... edge never necessary
    for (AutomatonInternalState state : condition.getStates()) {
      if (state.isTarget()) {
        continue;
      }
      for (CFAEdge edge : allEdges) {
        if (notFoundTargets.contains(edge) && state.nontriviallyMatches(edge, pLogger)) {
          notFoundTargets.remove(edge);
          relevantTargets.add(edge);
        } else if (state.nontriviallyMatchesAndEndsIn(edge, "__FALSE", pLogger)) {
          relevantTargets.addAll(nodesReachingTargetEdges.get(edge.getSuccessor()));
          notFoundTargets.removeAll(nodesReachingTargetEdges.get(edge.getSuccessor()));
        }

        if (notFoundTargets.isEmpty()) {
          return relevantTargets.build();
        }
      }
    }

    return relevantTargets.build();
  }

  private Collection<CFAEdge> extractAllCFAEdges(final CFA pCfa) {
    Collection<CFAEdge> edges = new ArrayList<>(2 * pCfa.getAllNodes().size());

    for (CFANode node : pCfa.getAllNodes()) {
      CFAUtils.allLeavingEdges(node).copyInto(edges);
    }
    return edges;
  }

  private Iterable<CFANode> getTargetNodes(
      final CFA pCfa,
      final Specification targetSpec,
      final ShutdownNotifier pShutdown,
      final LogManager pLogger) {

    final TargetLocationProvider targetProvider =
        new CachingTargetLocationProvider(pShutdown, pLogger, pCfa);
    return targetProvider.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), targetSpec);
  }

  /**
   * Returns a map of {@link CFANode CFANodes} to the target edges they can reach. A target edge is
   * an edge directly leading to a target node. If a CFANode can not reach any target, it may be
   * omitted.
   *
   * @param targets target nodes. these are used to compute the target edges
   * @param pShutdown {@link ShutdownNotifier} for stopping early
   * @return map of CFANodes to target edges reachable from them
   * @throws InterruptedException if ShutdownNotifier is triggered
   */
  private Multimap<CFANode, CFAEdge> computeReachableTargetsPerLocation(
      final Iterable<CFANode> targets, final ShutdownNotifier pShutdown)
      throws InterruptedException {
    Multimap<CFANode, CFAEdge> locToTargets = HashMultimap.create();
    for (CFANode target : targets) {
      pShutdown.shutdownIfNecessary();
      List<CFAEdge> allEdgesOnPathsToTarget = getAllEdgesOnPathToTarget(target);
      Iterable<CFAEdge> targetEdges = CFAUtils.allEnteringEdges(target);
      for (CFAEdge e : targetEdges) {
        putAllLocationsOnPathWithTarget(allEdgesOnPathsToTarget, e, locToTargets);
      }
    }
    return locToTargets;
  }

  private List<CFAEdge> getAllEdgesOnPathToTarget(CFANode target) {
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().backwards().traverseOnce(target, edgeCollector);
    return edgeCollector.getVisitedEdges();
  }

  private void putAllLocationsOnPathWithTarget(
      final List<CFAEdge> edges,
      final CFAEdge pTargetEdge,
      final Multimap<CFANode, CFAEdge> locToTargets) {
    for (CFAEdge edge : edges) {
      locToTargets.put(edge.getPredecessor(), pTargetEdge);
    }
  }
}
