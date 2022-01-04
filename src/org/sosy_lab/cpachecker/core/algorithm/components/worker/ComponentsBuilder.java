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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.network.NetworkConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.MonitoredAnalysisWorker.Monitor;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

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

  public ComponentsBuilder addAnalysisWorker(BlockNode pNode, SSAMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    String id = "W" + workers.size() + pNode.getId();
    workers.add(
        new SmartAnalysisWorker(id, pNode, logger, cfa, specification, configuration, shutdownManager, pTypeMap));
    return this;
  }

  public ComponentsBuilder addMonitoredAnalysisWorker(Monitor pMonitor, BlockNode pNode, SSAMap pMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    String id = "W" + workers.size() + pNode.getId();
    workers.add(new MonitoredAnalysisWorker(id, pNode, logger, cfa, specification, configuration,
        shutdownManager, pMonitor, pMap));
    return this;
  }

  public ComponentsBuilder addResultCollectorWorker(Collection<BlockNode> nodes) {
    workers.add(new ResultWorker(logger, nodes));
    return this;
  }

  public ComponentsBuilder addTimeoutWorker(long pTimeout) {
    workers.add(new TimeoutWorker(logger, pTimeout));
    return this;
  }

  public ComponentsBuilder addVisualizationWorker(BlockTree pBlockTree, Solver pSolver) {
    workers.add(new VisualizationWorker(logger, pBlockTree, pSolver));
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
