/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.mutation.strategy.AbstractCFAMutationStrategy;
import org.sosy_lab.cpachecker.cfa.mutation.strategy.CompositeStrategy;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options
public class CFAMutator extends CFACreator {
  @Option(
      secure = true,
      name = "mutations.count",
      description =
          "With this option set to positive integer, count of runs will be limited by given number.")
  private int runMutationsCount = 0;

  @Option(
      secure = true,
      name = "mutations.exportRounds",
      description =
          "With this option set to positive integer, CFA will be exported every round divisible by given number.")
  private int exportRounds = 0;

  @Option(
      secure = true,
      name = "mutations.exportOriginal",
      description = "With this option set to true, original CFA will be exported as well.")
  private boolean exportOriginal = false;

  private ParseResult parseResult = null;
  private Set<CFANode> originalNodes = null;
  private Set<CFAEdge> originalEdges = null;

  private CFA lastCFA = null;
  private boolean doLastRun = false;
  private boolean wasRollback = false;
  private final AbstractCFAMutationStrategy strategy;

  private static class CFAMutatorStatistics extends CFACreatorStatistics {
    private final StatTimer mutationTimer = new StatTimer("Time for mutations");
    private final StatTimer clearingTimer = new StatTimer("Time for clearing postprocessings");
    private final StatCounter mutationRound = new StatCounter("Count of mutation rounds");
    private final StatCounter mutationsDone = new StatCounter("Successful mutation rounds");
    private final StatCounter rollbacksDone =
        new StatCounter("Unsuccessful rounds (count of rollbacks)");
    private long originalNodesCount;
    private long originalEdgesCount;
    private long remainedNodesCount;
    private long remainedEdgesCount;
    private final Collection<Statistics> strategyStats = new ArrayList<>();

    private CFAMutatorStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      super.printStatistics(out, pResult, pReached);
      StatisticsWriter.writingStatisticsTo(out)
          .beginLevel()
          .put(mutationTimer)
          .put(clearingTimer)
          .put("Initial nodes count", originalNodesCount)
          .put("Initial edges count", originalEdgesCount)
          .put(mutationRound)
          .put(mutationsDone)
          .put(rollbacksDone)
          .put("Nodes remained", remainedNodesCount)
          .put("Edges remained", remainedEdgesCount)
          .endLevel();
      for (Statistics st : strategyStats) {
        st.printStatistics(out, pResult, pReached);
      }
    }

    @Override
    public @Nullable String getName() {
      return "CFA mutation";
    }
  }

  public CFAMutator(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier);

    pConfig.inject(this, CFAMutator.class);

    strategy = new CompositeStrategy(pConfig, pLogger);
  }


  // kind of main function
  @Override
  protected ParseResult parseToCFAs(final List<String> sourceFiles)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    if (parseResult == null) { // do zero-th run, init
      parseResult = super.parseToCFAs(sourceFiles);
      saveBeforePostproccessings();

      ((CFAMutatorStatistics) stats).originalNodesCount = originalNodes.size();
      ((CFAMutatorStatistics) stats).originalEdgesCount = originalEdges.size();
      return parseResult;
    }

    doMutationAftermath();

    if (doLastRun) {
      exportCFAAsync(lastCFA);
      return null;
    }

    if (runMutationsCount > 0
        && ((CFAMutatorStatistics) stats).mutationRound.getValue() == runMutationsCount) {
      doLastRun = true;
    } else {
      ((CFAMutatorStatistics) stats).mutationTimer.start();
      doLastRun = !mutate();
      ((CFAMutatorStatistics) stats).mutationTimer.stop();
    }

    if (doLastRun) {
      ((CFAMutatorStatistics) stats).remainedNodesCount = originalNodes.size();
      ((CFAMutatorStatistics) stats).remainedEdgesCount = originalEdges.size();
      strategy.makeAftermath(parseResult);
      strategy.collectStatistics(((CFAMutatorStatistics) stats).strategyStats);
    }

    // need to return after possible rollback
    return parseResult;
  }

  private void saveBeforePostproccessings() {
    originalNodes = new HashSet<>(parseResult.getCFANodes().values());

    final EdgeCollectingCFAVisitor visitor = new EdgeCollectingCFAVisitor();
    parseResult.getFunctions().forEach((k, v) -> CFATraversal.dfs().traverseOnce(v, visitor));

    originalEdges = Sets.newIdentityHashSet();
    originalEdges.addAll(visitor.getVisitedEdges());
  }


  private void doMutationAftermath() {
    if (((CFAMutatorStatistics) stats).mutationRound.getValue() == 0) {
      clearAfterPostprocessings();
    } else if (wasRollback) {
      ((CFAMutatorStatistics) stats).rollbacksDone.inc();
      wasRollback = false;
    } else if (!doLastRun) {
      clearAfterPostprocessings();
      ((CFAMutatorStatistics) stats).mutationsDone.inc();
    }
  }

  private boolean mutate() {
    ((CFAMutatorStatistics) stats).mutationRound.inc();
    if (strategy.mutate(parseResult)) {
      saveBeforePostproccessings();
      return true;
    } else {
      return false;
    }
  }

  // undo last mutation
  public void rollback() {
    assert !wasRollback;
    assert !doLastRun;
    wasRollback = true;
    clearAfterPostprocessings();
    strategy.rollback(parseResult);
    saveBeforePostproccessings();
  }

  private void clearAfterPostprocessings() {
    exportIfNeeded();

    ((CFAMutatorStatistics) stats).clearingTimer.start();
    final EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    final NodeCollectingCFAVisitor nodeCollector = new NodeCollectingCFAVisitor();
    final CFATraversal.CompositeCFAVisitor visitor =
        new CompositeCFAVisitor(edgeCollector, nodeCollector);

    for (final FunctionEntryNode entryNode : parseResult.getFunctions().values()) {
      CFATraversal.dfs().traverse(entryNode, visitor);
    }

    Set<CFAEdge> tmpSet = new IdentityHashSet<>();
    tmpSet.addAll(edgeCollector.getVisitedEdges());
    final Set<CFAEdge> edgesToRemove =
        Sets.difference(tmpSet, originalEdges);
    final Set<CFAEdge> edgesToAdd = Sets.difference(originalEdges, tmpSet);
    final Set<CFANode> nodesToRemove =
        Sets.difference(new HashSet<>(nodeCollector.getVisitedNodes()), originalNodes);
    final Set<CFANode> nodesToAdd =
        Sets.difference(originalNodes, new HashSet<>(nodeCollector.getVisitedNodes()));

    // finally remove nodes and edges added as global decl. and interprocedural
    SortedSetMultimap<String, CFANode> nodes = parseResult.getCFANodes();
    for (CFANode n : nodesToRemove) {
      logger.logf(
          Level.FINEST,
          "clearing: removing node %s (was present: %s)",
          n,
          nodes.remove(n.getFunctionName(), n));
    }
    for (CFANode n : nodesToAdd) {
      logger.logf(
          Level.FINEST,
          "clearing: returning node %s:%s (inserted: %s)",
          n.getFunctionName(),
          n,
          nodes.put(n.getFunctionName(), n));
    }
    for (CFAEdge e : edgesToRemove) {
      logger.logf(Level.FINEST, "clearing: removing edge %s", e);
      CFACreationUtils.removeEdgeFromNodes(e);
    }
    for (CFAEdge e : edgesToAdd) {
      logger.logf(Level.FINEST, "clearing: returning edge %s", e);
      // have to check because chain removing does not remove edges from originalEdges right away
      if (!e.getPredecessor().hasEdgeTo(e.getSuccessor())) {
        CFACreationUtils.addEdgeUnconditionallyToCFA(e);
      }
    }
    ((CFAMutatorStatistics) stats).clearingTimer.stop();
  }

  private void exportIfNeeded() {
    // TODO export with suffix or to different subdirs
    if (exportOriginal && ((CFAMutatorStatistics) stats).mutationRound.getValue() == 0) {
      exportCFA(lastCFA);
    } else if (exportRounds > 0) {
      long suf = ((CFAMutatorStatistics) stats).mutationRound.getValue() / exportRounds;
      if (suf * exportRounds == ((CFAMutatorStatistics) stats).mutationRound.getValue()) {
        exportCFA(lastCFA);
      }
    }
  }

  @Override
  protected void exportCFAAsync(final CFA cfa) {
    if (cfa == null) {
      return;
    }

    logger.logf(Level.FINE, "Count of CFA nodes: %d", cfa.getAllNodes().size());

    if (cfa == lastCFA) {
      super.exportCFAAsync(lastCFA);
    } else {
      lastCFA = cfa;
    }
  }

  @Override
  protected CFAMutatorStatistics createStatistics(LogManager pLogger) {
    return new CFAMutatorStatistics(pLogger);
  }
}
