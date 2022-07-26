// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

public class AlgorithmFactory {

  public static Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createAlgorithm(
      final LogManager logger,
      final Specification specification,
      final CFA cfa,
      final Configuration globalConfig,
      final ShutdownManager singleShutdownManager,
      final Collection<String> ignoreOptions,
      final BlockNode node)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    Configuration singleConfig = buildSubConfig(ignoreOptions, globalConfig);
    LogManager singleLogger = logger.withComponentName("Analysis " + node);

    ResourceLimitChecker singleLimits =
        ResourceLimitChecker.fromConfiguration(singleConfig, singleLogger, singleShutdownManager);
    singleLimits.start();

    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            singleConfig,
            singleLogger,
            singleShutdownManager.getNotifier(),
            AggregatedReachedSets.empty());

    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, specification);
    ARGCPA argcpa = (ARGCPA) cpa;
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<>(argcpa.getWrappedCPAs());
    while (!cpas.isEmpty()) {
      ConfigurableProgramAnalysis wrappedCPA = cpas.remove(0);
      if (wrappedCPA instanceof BlockCPA) {
        ((BlockCPA) wrappedCPA).init(node);
      } else if (wrappedCPA instanceof BlockCPABackward) {
        ((BlockCPABackward) wrappedCPA).init(node);
      }
      if (wrappedCPA instanceof CompositeCPA) {
        cpas.addAll(((CompositeCPA) wrappedCPA).getWrappedCPAs());
      }
    }
    GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
    Algorithm algorithm = coreComponents.createAlgorithm(cpa, cfa, specification);
    ReachedSet reached =
        createInitialReachedSet(cpa, node.getStartNode(), coreComponents, singleLogger);

    return Triple.of(algorithm, cpa, reached);
  }

  private static Configuration buildSubConfig(
      Collection<String> ignoreOptions, Configuration globalConfig)
      throws InvalidConfigurationException {

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    for (String ignore : ignoreOptions) {
      singleConfigBuilder.clearOption(ignore);
    }

    return singleConfigBuilder.build();
  }

  private static ReachedSet createInitialReachedSet(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      CoreComponentsFactory pFactory,
      LogManager singleLogger)
      throws InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pFactory.createReachedSet(cpa);
    reached.add(initialState, initialPrecision);
    return reached;
  }
}
