// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.nio_network.NetworkConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.MonitoredAnalysisWorker.Monitor;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ComponentsBuilder {

  private final LogManager logger;
  private final CFA cfa;
  private final Configuration configuration;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final Monitor monitor;
  private final List<Worker> workers;
  private Class<? extends ConnectionProvider<?>> connectionProviderClass;
  private int additionalConnections;

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
    monitor = new Monitor(logger, 1);
  }

  /**
   * @param clazz             ConnectionProvider that is responsible for creating the list
   * @param numberConnections number of Connections to generate
   * @param <C>               explicit type of the ConnectionProvider
   * @return list of connections of type C
   */
  private static <C extends ConnectionProvider<?>> List<? extends Connection> getConnections(
      Class<C> clazz,
      int numberConnections)
      throws InstantiationException, IllegalAccessException, NoSuchMethodException,
             InvocationTargetException, IOException {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    boolean hasEmptyConstructor = false;
    for (Constructor<?> constructor : constructors) {
      if (constructor.getParameterTypes().length == 0) {
        hasEmptyConstructor = true;
        break;
      }
    }
    if (!hasEmptyConstructor) {
      throw new AssertionError(ComponentsBuilder.class
          + " can only use classes without constructor parameters that are an instance of "
          + ConnectionProvider.class);
    }
    C connectionProvider = clazz.getDeclaredConstructor().newInstance();
    return connectionProvider.createConnections(numberConnections);
  }

  private String nextId(String pAdditionalIdentifier) {
    return "W" + workers.size() + pAdditionalIdentifier;
  }

  public ComponentsBuilder withConnectionType(Class<? extends ConnectionProvider<?>> pConnectionType) {
    connectionProviderClass = pConnectionType;
    return this;
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

  public ComponentsBuilder addMonitoredAnalysisWorker(
      BlockNode pNode,
      UpdatedTypeMap pMap,
      AnalysisOptions pOptions)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    workers.add(
        new MonitoredAnalysisWorker(nextId(pNode.getId()), pOptions, pNode, logger, cfa,
            specification,
            shutdownManager, monitor, pMap));
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
        getConnections(connectionProviderClass, workers.size() + additionalConnections);
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
