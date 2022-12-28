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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyFactory;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.LoopStructure;

public class SummaryPostProcessor implements StatisticsProvider {

  private final Set<Strategy> strategiesClasses = new HashSet<>();
  private final StrategyFactory strategyFactory;
  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private final StrategyDependency strategyDependencies;
  private final SummaryInformation summaryInformation;
  private final SummaryCFAStatistics stats;

  private final Set<StrategiesEnum> strategies;
  private final StrategyDependencyEnum cfaCreationStrategy;
  private final StrategyDependencyEnum transferStrategy;
  private final int maxUnrollingsStrategy;
  private final int maxIterationsSummaries;

  public SummaryPostProcessor(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      MutableCFA pCfa)
      throws InvalidConfigurationException {
    SummaryOptions options = new SummaryOptions(pConfig);
    strategies = options.getStrategies();
    cfaCreationStrategy = options.getCfaCreationStrategy();
    transferStrategy = options.getTransferStrategy();
    maxUnrollingsStrategy = options.getMaxUnrollingsStrategy();
    maxIterationsSummaries = options.getMaxIterationsSummaries();

    strategyDependencies = new StrategyDependencyFactory().createStrategy(this.cfaCreationStrategy);

    summaryInformation =
        new SummaryInformation(
            pCfa,
            strategyDependencies,
            new StrategyDependencyFactory().createStrategy(transferStrategy));
    // TODO: the fact that we need to modify pCfa here in this constructor is a code smell:
    pCfa.setSummaryInformations(summaryInformation);

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    strategyFactory =
        new StrategyFactory(
            pLogger, pShutdownNotifier, maxUnrollingsStrategy, strategyDependencies, pCfa);

    stats = new SummaryCFAStatistics(summaryInformation, strategies);

    summaryInformation.setFactory(strategyFactory);
    for (StrategiesEnum s : strategies) {
      Strategy strategyClass = strategyFactory.buildStrategy(s);
      strategiesClasses.add(strategyClass);
      summaryInformation.addStrategy(strategyClass);
    }
  }

  public MutableCFA process(MutableCFA pCfa) throws ParserException {

    List<GhostCFA> ghostCfaToBeAdded;
    CFANode startNode = pCfa.getMainFunction();
    boolean fixpoint = false;
    boolean nodesAdded = true;
    Integer iterations = 0;
    List<CFANode> currentNodes = new ArrayList<>();
    List<CFANode> newNodes = new ArrayList<>();
    Set<CFANode> visitedNodes = new HashSet<>();

    // This allows to control what startegies are applied inside of strategies
    Set<StrategiesEnum> strategiesWhichShouldNotBeFurtherAnalyzed = new HashSet<>();
    strategiesWhichShouldNotBeFurtherAnalyzed.add(StrategiesEnum.LOOPUNROLLING); // TODO: Make this variable, since it is currently hardcoded
    Set<CFANode> nodesToBeIgnored = new HashSet<>();

    while (!fixpoint) {
      fixpoint = true;
      ghostCfaToBeAdded = new ArrayList<>();

      currentNodes.add(startNode);

      while (nodesAdded) {
        nodesAdded = false;
        visitedNodes.addAll(currentNodes);
        for (CFANode node : currentNodes) {
          if (nodesToBeIgnored.contains(node)) {
            continue;
          }

          for (Strategy s : strategiesClasses) {
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
          if (strategiesWhichShouldNotBeFurtherAnalyzed.contains(gCFA.getStrategy())) {
            nodesToBeIgnored.add(n);
          }
        }
        summaryInformation.addGhostCFA(gCFA);
      }

      // # Update CFA structure to incorporate new summaries.
      // 1. reverse postorder for the new CFA nodes needs to be set
      for (FunctionEntryNode function : pCfa.getAllFunctionHeads()) {
        CFAReversePostorder sorter = new CFAReversePostorder();
        sorter.assignSorting(function);
      }

      // 2. recalculate the loop structure based on the current state of the CFA
      pCfa.setLoopStructure(LoopStructure.getLoopStructure(pCfa));

      fixpoint = this.strategyDependencies.stopPostProcessing(iterations, !fixpoint);
    }

    return pCfa;
  }

  public Set<StrategiesEnum> getStrategies() {
    return strategies;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
