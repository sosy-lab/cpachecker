// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;

public class SummaryPostProcessor {

  private static CFA originalCFA;
  private Map<StrategiesEnum, Set<CFAEdge>> strategiesToEdges;
  private Set<StrategiesEnum> strategies;
  private boolean useCompilerForSummary;
  private int maxUnrollingsStrategy;
  private Set<StrategyInterface> strategiesClasses = new HashSet<>();
  private StrategyFactory strategyFactory;
  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private int maxIterationsSummaries;
  private StrategyDependencyInterface strategyDependencies;

  public SummaryPostProcessor(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, MutableCFA pCfa,
      Set<StrategiesEnum> pStrategies,
      boolean pUseCompilerForSummary,
      int pMaxUnrollingsStrategy, int pMaxIterationsSummaries, StrategyDependencyInterface pStrategyDependencies) {
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    maxIterationsSummaries = pMaxIterationsSummaries;
    strategyDependencies = pStrategyDependencies;
    setStrategies(pStrategies);
    useCompilerForSummary = pUseCompilerForSummary;
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
    setOriginalCFA(pCfa);
    strategyFactory =
        new StrategyFactory(
            pLogger, pShutdownNotifier, maxUnrollingsStrategy, useCompilerForSummary, strategyDependencies);

    SummaryInformation.getSummaryInformation().setFactory(strategyFactory);
    for (StrategiesEnum s : strategies) {
      StrategyInterface strategyClass = strategyFactory.buildStrategy(s);
      strategiesClasses.add(strategyClass);
      SummaryInformation.getSummaryInformation().addStrategy(strategyClass);
    }
  }

  public MutableCFA process(MutableCFA pCfa) {

    List<GhostCFA> ghostCfaToBeAdded = new ArrayList<>();
    CFANode startNode = pCfa.getMainFunction();
    boolean fixpoint = false;
    Integer iterations = 0;
    List<CFANode> currentNodes = new ArrayList<>();
    List<CFANode> newNodes = new ArrayList<>();
    currentNodes.add(startNode);
    while (!fixpoint) {
      fixpoint = true;

      for (CFANode node : currentNodes) {
        for (StrategyInterface s : strategiesClasses) {
          if (strategyDependencies.apply(s, iterations)) {
            Optional<GhostCFA> maybeGhostCFA = s.summarize(node);
            if (maybeGhostCFA.isPresent()) {
              ghostCfaToBeAdded.add(maybeGhostCFA.get());
            }
          }

        }

        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          newNodes.add(node.getLeavingEdge(i).getSuccessor());
        }
      }

      currentNodes = newNodes;
      newNodes = new ArrayList<>();

      if (iterations > maxIterationsSummaries) {
        fixpoint = true;
      }
      iterations += 1;
      for (GhostCFA gCFA : ghostCfaToBeAdded) {
        gCFA.connectOriginalAndGhostCFA();
      }
    }

    return null;
  }

  public static CFA getOriginalCFA() {
    return originalCFA;
  }

  public static void setOriginalCFA(CFA pOriginalCFA) {
    originalCFA = pOriginalCFA;
  }

  public Map<StrategiesEnum, Set<CFAEdge>> getStrategiesToEdges() {
    return strategiesToEdges;
  }

  public void setStrategiesToEdges(Map<StrategiesEnum, Set<CFAEdge>> pStrategiesToEdges) {
    strategiesToEdges = pStrategiesToEdges;
  }

  public Set<StrategiesEnum> getStrategies() {
    return strategies;
  }

  public void setStrategies(Set<StrategiesEnum> pStrategies) {
    strategies = pStrategies;
  }
}
