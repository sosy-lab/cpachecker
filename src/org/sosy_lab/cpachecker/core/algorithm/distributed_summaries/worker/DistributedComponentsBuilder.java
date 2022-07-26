// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DistributedComponentsBuilder {

  private final CFA cfa;
  private final Configuration configuration;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final List<WorkerGenerator> workerGenerators;
  private final ConnectionProvider<?> connectionProvider;
  private int additionalConnections;

  public DistributedComponentsBuilder(
      CFA pCFA,
      ConnectionProvider<?> pConnectionProvider,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager) {
    cfa = pCFA;
    configuration = pConfiguration;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
    // only one available for now
    connectionProvider = pConnectionProvider;
    workerGenerators = new ArrayList<>();
  }

  private String nextId(String pAdditionalIdentifier) {
    return "W" + workerGenerators.size() + pAdditionalIdentifier;
  }

  public DistributedComponentsBuilder createAdditionalConnections(int numberConnections) {
    additionalConnections = numberConnections;
    return this;
  }

  public DistributedComponentsBuilder addAnalysisWorker(
      BlockNode pNode, AnalysisOptions pOptions, UpdatedTypeMap pTypeMap) {
    workerGenerators.add(
        connection ->
            new AnalysisBlockSummaryWorker(
                nextId(pNode.getId()),
                pTypeMap,
                pOptions,
                connection,
                pNode,
                cfa,
                specification,
                shutdownManager));
    return this;
  }

  public DistributedComponentsBuilder addSmartAnalysisWorker(
      BlockNode pNode, AnalysisOptions pOptions, UpdatedTypeMap pTypeMap) {
    workerGenerators.add(
        connection ->
            new SmartAnalysisBlockSummaryWorker(
                nextId(pNode.getId()),
                pTypeMap,
                pOptions,
                connection,
                pNode,
                cfa,
                specification,
                shutdownManager));
    return this;
  }

  public DistributedComponentsBuilder addResultCollectorWorker(
      Collection<BlockNode> nodes, AnalysisOptions pOptions) {
    workerGenerators.add(connection -> new ResultBlockSummaryWorker(nodes, connection, pOptions));
    return this;
  }

  public DistributedComponentsBuilder addVisualizationWorker(
      BlockTree pBlockTree, AnalysisOptions pOptions, Configuration pConfiguration) {
    workerGenerators.add(
        connection ->
            new VisualizationBlockSummaryWorker(pBlockTree, connection, pOptions, pConfiguration));
    return this;
  }

  public DistributedComponentsBuilder addRootWorker(
      BlockNode pNode, AnalysisOptions pOptions, UpdatedTypeMap pTypeMap) {
    workerGenerators.add(
        connection ->
            new RootBlockSummaryWorker(
                nextId(pNode.getId()),
                pTypeMap,
                connection,
                pOptions,
                pNode,
                cfa,
                specification,
                configuration,
                shutdownManager));
    return this;
  }

  public Components build()
      throws IOException, CPAException, InterruptedException, InvalidConfigurationException {
    List<? extends Connection> connections =
        ImmutableList.copyOf(
            connectionProvider.createConnections(workerGenerators.size() + additionalConnections));
    List<BlockSummaryWorker> worker = new ArrayList<>(workerGenerators.size());
    for (int i = 0; i < workerGenerators.size(); i++) {
      worker.add(workerGenerators.get(i).apply(connections.get(i)));
    }
    List<? extends Connection> excessConnections =
        connections.subList(
            workerGenerators.size(), workerGenerators.size() + additionalConnections);
    return new Components(worker, excessConnections);
  }

  public static class Components {

    private final List<BlockSummaryWorker> workers;
    private final List<? extends Connection> additionalConnections;

    private Components(
        List<BlockSummaryWorker> pWorkers, List<? extends Connection> pAdditionalConnections) {
      workers = pWorkers;
      additionalConnections = pAdditionalConnections;
    }

    public List<BlockSummaryWorker> getWorkers() {
      return workers;
    }

    public List<? extends Connection> getAdditionalConnections() {
      return additionalConnections;
    }
  }

  // Needed to forward exception handling to actual method and not this function.
  @FunctionalInterface
  private interface WorkerGenerator {
    BlockSummaryWorker apply(Connection t)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException;
  }
}
