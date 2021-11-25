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
import java.util.List;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProviders;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.network.NetworkConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.MonitoredAnalysisWorker.Monitor;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ComponentsBuilder {

  private final LogManager logger;
  private final CFA cfa;
  private final Configuration configuration;
  private final ShutdownManager shutdownManager;
  private final Specification specification;

  private Class<? extends ConnectionProvider<?>> connectionProviderClass;
  private int additionalConnections;

  private final List<Worker> workers;

  public ComponentsBuilder(
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager) throws InvalidConfigurationException {
    logger = pLogger;
    cfa = pCFA;
    configuration = Configuration.builder().copyFrom(pConfiguration).build();
    shutdownManager = pShutdownManager;
    specification = pSpecification;
    connectionProviderClass = NetworkConnectionProvider.class;
    workers = new ArrayList<>();
  }

  public ComponentsBuilder withConnectionType(Class<? extends ConnectionProvider<?>> pConnectionType) {
    connectionProviderClass = pConnectionType;
    return this;
  }

  public ComponentsBuilder createAdditionalConnections(int numberConnections) {
    additionalConnections = numberConnections;
    return this;
  }

  public ComponentsBuilder addAnalysisWorker(BlockNode pNode)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    String id = "W" + workers.size() + pNode.getId();
    workers.add(new AnalysisWorker(id, pNode, logger, cfa, specification, configuration, shutdownManager));
    return this;
  }

  public ComponentsBuilder addMonitoredAnalysisWorker(Monitor pMonitor, BlockNode pNode)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    String id = "W" + workers.size() + pNode.getId();
    workers.add(new MonitoredAnalysisWorker(id, pNode, logger, cfa, specification, configuration, shutdownManager, pMonitor));
    return this;
  }

  public ComponentsBuilder createResultCollectorWorker(int numAnalysisWorker) {
    workers.add(new ResultWorker(logger, numAnalysisWorker));
    return this;
  }

  public Components build()
      throws InvocationTargetException, InstantiationException, IllegalAccessException,
             NoSuchMethodException, IOException {
    List<? extends Connection> connections = ConnectionProviders.getConnections(connectionProviderClass, workers.size() + additionalConnections);
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

  public static class Components {

    private final List<Worker> workers;
    private final List<Connection> additionalConnections;

    public Components(
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
