// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;


import com.google.common.collect.TreeMultimap;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.ForwardingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class AutomaticProgramRepair
    implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Specification specification;
  private final ShutdownNotifier shutdownNotifier;


  private final StatTimer totalTime = new StatTimer("Total time for bug repair");

  public AutomaticProgramRepair(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final Specification pSpecification,
      final ShutdownNotifier pShutdownNotifier
  )
      throws InvalidConfigurationException {

    config = pConfig;
    algorithm = pStoreAlgorithm;
    cfa = pCfa;
    logger = pLogger;
    specification = pSpecification;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);

    totalTime.start();

    try {
      logger.log(Level.INFO, "Starting bug repair...");
      logger.log(Level.INFO, "STATUS before: " + status.toString());

      runAlgorithm();

      logger.log(Level.INFO, "Stopping bug repair...");
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
    } finally{
      totalTime.stop();
    }
    return status;
  }

  private void runAlgorithm()
      throws InvalidConfigurationException, CPAException, InterruptedException {
    final MutableCFA clonedCFA = cloneCFA();
    final RepairCandidateCollector repairCandidatesCollector = new RepairCandidateCollector();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(clonedCFA.getMainFunction(), repairCandidatesCollector);

    final List<CFANode> repairCandidateNodes =  repairCandidatesCollector.getRepairCandidateNodes();

    logger.log(Level.INFO,  clonedCFA.getAllNodes().size());

    final MutableCFA mutatedCFA = mutateCFA(clonedCFA, repairCandidateNodes);
    final AlgorithmStatus newStatus = rerun(mutatedCFA);

    logger.log(Level.INFO, "STATUS after: " + newStatus.toString());
    logger.log(Level.INFO,  mutatedCFA.getAllNodes().size());
  }

  private AlgorithmStatus rerun(MutableCFA mutatedCFA)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    final ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
    final CPABuilder builder = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
    final ConfigurableProgramAnalysis mutatedCPA = builder.buildCPAs(mutatedCFA, specification, new AggregatedReachedSets());
    final CPAAlgorithm algo = CPAAlgorithm.create(mutatedCPA, logger, config, shutdownNotifier);

    return algo.run(reachedSetFactory.create());
  }

  private MutableCFA cloneCFA(){
    final TreeMultimap<String, CFANode>  nodes = TreeMultimap.create();

    for (final String function : cfa.getAllFunctionNames()) {
      nodes.putAll(function, CFATraversal.dfs().collectNodesReachableFrom(cfa.getFunctionHead(function)));
    }

    return new MutableCFA(cfa.getMachineModel(),
        cfa.getAllFunctions(),
        nodes,
        cfa.getMainFunction(),
        cfa.getFileNames(),
        cfa.getLanguage());
  }

  private MutableCFA mutateCFA(MutableCFA currentCFA, List<CFANode> repairCandidateNodes)  {
    for (CFANode node : repairCandidateNodes) {
      if(shouldDelete()){
        currentCFA.removeNode(node);
      }
    }

    return currentCFA;
  }


  private static class RepairCandidateCollector extends ForwardingCFAVisitor {
    private final List<CFANode> repairCandidateNodes = new ArrayList<>();

    public RepairCandidateCollector() {
      super(new EdgeCollectingCFAVisitor());
    }


    @Override
    public TraversalProcess visitNode(CFANode node) {
      if (isCandidate()) {
        repairCandidateNodes.add(node);
      }
      return super.visitNode(node);
    }

    private boolean isCandidate() {
      return Math.random() >= 0.7;
    }

    private List<CFANode> getRepairCandidateNodes() {
      return repairCandidateNodes;
    }
  }

  private boolean shouldDelete() {
    return Math.random() >= 0.7;
  }


  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add((Statistics) algorithm);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }

}
