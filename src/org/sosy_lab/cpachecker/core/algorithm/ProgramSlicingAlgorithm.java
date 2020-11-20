// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraphBuilder;

@Options(prefix = "programSlicing")
public final class ProgramSlicingAlgorithm extends NestingAlgorithm {

  @Option(
      secure = true,
      name = "delegateAnalysis",
      description = "Configuration of the program slicing delegate analysis")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path delegateAnalysisConfig;

  private final DependenceGraphBuilder dependenceGraphBuilder;
  private final Collection<Statistics> stats;

  public ProgramSlicingAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);

    pConfig.inject(this);

    dependenceGraphBuilder = DependenceGraph.builder(cfa, globalConfig, logger, shutdownNotifier);

    if (!cfa.getVarClassification().isPresent()) {
      logger.log(
          Level.WARNING,
          "Variable Classification not present. Consider turning this on "
              + "to improve dependence graph construction.");
    }

    stats = new ArrayList<>();
  }

  private static CFA createCfaWithDependenceGraph(CFA pCfa, DependenceGraph pDependenceGraph) {

    TreeMultimap<String, CFANode> allNodes = TreeMultimap.create();

    for (CFANode node : pCfa.getAllNodes()) {
      allNodes.put(node.getFunction().getQualifiedName(), node);
    }

    MutableCFA newCfa =
        new MutableCFA(
            pCfa.getMachineModel(),
            pCfa.getAllFunctions(),
            allNodes,
            pCfa.getMainFunction(),
            pCfa.getFileNames(),
            pCfa.getLanguage());

    pCfa.getLoopStructure().ifPresent(newCfa::setLoopStructure);
    pCfa.getLiveVariables().ifPresent(newCfa::setLiveVariables);

    return newCfa.makeImmutableCFA(pCfa.getVarClassification(), Optional.of(pDependenceGraph));
  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createAlgorithm(
      CFA pCfa, AggregatedReachedSets pAggregatedReached)
      throws CPAException, InterruptedException {

    try {

      return createAlgorithm(
          delegateAnalysisConfig,
          pCfa,
          pCfa.getMainFunction(),
          ShutdownManager.createWithParent(shutdownNotifier),
          pAggregatedReached,
          ImmutableSet.of("analysis.useProgramSlicing", "programSlicing.delegateAnalysis"),
          stats);

    } catch (IOException ex) {

      String message =
          String.format(
              Locale.ENGLISH,
              "Skipping analysis because the configuration file could not be read: %s",
              delegateAnalysisConfig);

      if (shutdownNotifier.shouldShutdown() && ex instanceof ClosedByInterruptException) {
        logger.log(Level.WARNING, message);
      } else {
        logger.logUserException(Level.WARNING, ex, message);
      }

      return null;

    } catch (InvalidConfigurationException ex) {
      throw new CPAException("Configuration file is invalid: " + delegateAnalysisConfig, ex);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    AggregatedReachedSets aggregatedReached =
        new AggregatedReachedSets(Collections.singleton(pReachedSet));

    DependenceGraph dependenceGraph = dependenceGraphBuilder.build();
    CFA cfaWithDependenceGraph = createCfaWithDependenceGraph(cfa, dependenceGraph);

    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> delegateAnalysis =
        createAlgorithm(cfaWithDependenceGraph, aggregatedReached);

    if (delegateAnalysis == null) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;
    ReachedSet currentReached = delegateAnalysis.getThird();
    forwardingReachedSet.setDelegate(currentReached);

    Algorithm delegateAlgorithm = delegateAnalysis.getFirst();
    AlgorithmStatus status = delegateAlgorithm.run(currentReached);

    while (currentReached.hasWaitingState()) {
      status = delegateAlgorithm.run(currentReached);
    }

    return status;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    dependenceGraphBuilder.collectStatistics(pStatsCollection);
    pStatsCollection.addAll(stats);
  }
}
