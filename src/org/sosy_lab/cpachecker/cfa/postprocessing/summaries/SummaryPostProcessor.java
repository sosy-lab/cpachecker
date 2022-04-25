// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public class SummaryPostProcessor implements StatisticsProvider {

  private static CFA originalCFA;
  private Set<StrategiesEnum> strategies;
  private int maxUnrollingsStrategy;
  private Set<StrategyInterface> strategiesClasses = new HashSet<>();
  private StrategyFactory strategyFactory;
  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private int maxIterationsSummaries;
  private StrategyDependencyInterface strategyDependencies;
  private SummaryInformation summaryInformation;
  private SummaryCFAStatistics stats;

  public SummaryPostProcessor(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Set<StrategiesEnum> pStrategies,
      int pMaxUnrollingsStrategy,
      int pMaxIterationsSummaries,
      StrategyDependencyInterface pStrategyDependencies) {
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    maxIterationsSummaries = pMaxIterationsSummaries;
    strategyDependencies = pStrategyDependencies;
    setStrategies(pStrategies);
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
    setOriginalCFA(pCfa);
    summaryInformation = pCfa.getSummaryInformation().orElseThrow();
    strategyFactory =
        new StrategyFactory(
            pLogger,
            pShutdownNotifier,
            maxUnrollingsStrategy,
            strategyDependencies,
            pCfa);

    stats = new SummaryCFAStatistics(summaryInformation, strategies);

    summaryInformation.setFactory(strategyFactory);
    for (StrategiesEnum s : strategies) {
      StrategyInterface strategyClass = strategyFactory.buildStrategy(s);
      strategiesClasses.add(strategyClass);
      summaryInformation.addStrategy(strategyClass);
    }
  }

  public MutableCFA process(MutableCFA pCfa) {

    List<GhostCFA> ghostCfaToBeAdded;
    CFANode startNode = pCfa.getMainFunction();
    boolean fixpoint = false;
    boolean nodesAdded = true;
    Integer iterations = 0;
    List<CFANode> currentNodes = new ArrayList<>();
    List<CFANode> newNodes = new ArrayList<>();
    Set<CFANode> visitedNodes = new HashSet<>();
    while (!fixpoint) {
      ghostCfaToBeAdded = new ArrayList<>();

      currentNodes.add(startNode);

      while (nodesAdded) {
        nodesAdded = false;
        visitedNodes.addAll(currentNodes);
        for (CFANode node : currentNodes) {
          for (StrategyInterface s : strategiesClasses) {
            if (strategyDependencies.apply(s, iterations)) {
              Optional<GhostCFA> maybeGhostCFA = s.summarize(node);
              if (maybeGhostCFA.isPresent()) {
                ghostCfaToBeAdded.add(maybeGhostCFA.orElseThrow());
                fixpoint = false;
              }
            }
          }

          for (int i = 0; i < node.getNumLeavingEdges(); i++) {
            CFANode newNode = node.getLeavingEdge(i).getSuccessor();
            if (!visitedNodes.contains(newNode) && !newNodes.contains(newNode)) {
              newNodes.add(newNode);
              nodesAdded = true;
            }
          }
        }

        currentNodes = newNodes;
        newNodes = new ArrayList<>();
      }

      currentNodes = new ArrayList<>();
      newNodes = new ArrayList<>();
      visitedNodes = new HashSet<>();
      nodesAdded = true;

      if (iterations > maxIterationsSummaries) {
        fixpoint = true;
      }

      iterations += 1;
      for (GhostCFA gCFA : ghostCfaToBeAdded) {
        stats.addStrategy(gCFA.getStrategy());
        gCFA.connectOriginalAndGhostCFA();
        for (CFANode n : gCFA.getAllNodes()) {
          pCfa.addNode(startNode.getFunctionName(), n);
        }
        summaryInformation.addGhostCFA(gCFA);
      }
      // TODO: update CFA structure to incorporate new summaries. Specifically, create a new
      // LoopStructure if a Loop inside a Loop was summarized using the summarized loop as an inner
      // edge.

      fixpoint = this.strategyDependencies.stopPostProcessing(iterations, !fixpoint);
    }

    return pCfa;
  }

  public static CFA getOriginalCFA() {
    return originalCFA;
  }

  public static void setOriginalCFA(CFA pOriginalCFA) {
    originalCFA = pOriginalCFA;
  }

  public Set<StrategiesEnum> getStrategies() {
    return strategies;
  }

  public void setStrategies(Set<StrategiesEnum> pStrategies) {
    strategies = pStrategies;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
