// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ComponentsBuilder {

  private final LogManager logger;
  private final CFA cfa;
  private final Configuration configuration;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final List<Worker> workers;
  private final ConnectionProvider<?> connectionProvider;
  private int additionalConnections;

  public ComponentsBuilder(
      LogManager pLogger,
      CFA pCFA,
      ConnectionProvider<?> pConnectionProvider,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager) throws InvalidConfigurationException {
    logger = pLogger;
    cfa = pCFA;
    configuration = pConfiguration;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
    connectionProvider = pConnectionProvider;
    workers = new ArrayList<>();
  }

  private String nextId(String pAdditionalIdentifier) {
    return "W" + workers.size() + pAdditionalIdentifier;
  }

  public ComponentsBuilder createAdditionalConnections(int numberConnections) {
    additionalConnections = numberConnections;
    return this;
  }

  public ComponentsBuilder addAnalysisWorker(
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      AnalysisOptions pOptions)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    workers.add(
        new AnalysisWorker(nextId(pNode.getId()), pOptions, pNode, logger, cfa, specification,
            shutdownManager, pTypeMap));
    return this;
  }

  public ComponentsBuilder addSmartAnalysisWorker(
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      AnalysisOptions pOptions)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    workers.add(
        new SmartAnalysisWorker(nextId(pNode.getId()), pOptions, pNode, logger, cfa, specification,
            shutdownManager, pTypeMap));
    return this;
  }

  public ComponentsBuilder addFaultLocalizationWorker(
      BlockNode pNode,
      UpdatedTypeMap pTypeMap,
      AnalysisOptions pOptions)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    workers.add(
        new FaultLocalizationWorker(nextId(pNode.getId()), pOptions, pNode, logger, cfa,
            specification,
            configuration, shutdownManager, pTypeMap));
    return this;
  }

  public ComponentsBuilder addResultCollectorWorker(
      Collection<BlockNode> nodes,
      AnalysisOptions pOptions) {
    workers.add(new ResultWorker(logger, nodes, pOptions));
    return this;
  }

  public ComponentsBuilder addTimeoutWorker(TimeSpan pTimeout, AnalysisOptions pOptions) {
    workers.add(new TimeoutWorker(logger, pTimeout, pOptions));
    return this;
  }

  public ComponentsBuilder addVisualizationWorker(BlockTree pBlockTree, AnalysisOptions pOptions, Configuration pConfiguration)
      throws InvalidConfigurationException {
    workers.add(new VisualizationWorker(logger, pBlockTree, pOptions, pConfiguration));
    return this;
  }

  public Components build()
      throws InvocationTargetException, InstantiationException, IllegalAccessException,
          NoSuchMethodException, IOException {
    List<? extends Connection> connections =
        connectionProvider.createConnections(workers.size() + additionalConnections);
    List<Connection> excessConnections = new ArrayList<>();
    for (int i = 0; i < workers.size() + additionalConnections; i++) {
      if (i >= workers.size()) {
        excessConnections.add(connections.get(i));
      } else {
        workers.get(i).setConnection(connections.get(i));
      }
    }
    return new Components(workers, excessConnections);
  }

  public ComponentsBuilder addRootWorker(BlockNode pNode, AnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    workers.add(
        new RootWorker(nextId(pNode.getId()), pOptions, pNode, logger, cfa, specification,
            configuration,
            shutdownManager));
    return this;
  }

  public static class Components {

    private final List<Worker> workers;
    private final List<Connection> additionalConnections;

    private Components(
        List<Worker> pWorkers,
        List<Connection> pAdditionalConnections) {
      workers = pWorkers;
      additionalConnections = pAdditionalConnections;
    }

    public List<Worker> getWorkers() {
      return workers;
    }

    public List<Connection> getAdditionalConnections() {
      return additionalConnections;
    }
  }

}
