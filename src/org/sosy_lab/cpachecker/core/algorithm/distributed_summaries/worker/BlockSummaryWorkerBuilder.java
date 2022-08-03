// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnectionProvider;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockSummaryWorkerBuilder {

  private final CFA cfa;
  private final Configuration configuration;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final List<WorkerGenerator> workerGenerators;
  private final BlockSummaryConnectionProvider<?> connectionProvider;
  private int additionalConnections;

  public BlockSummaryWorkerBuilder(
      CFA pCFA,
      BlockSummaryConnectionProvider<?> pConnectionProvider,
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

  public BlockSummaryWorkerBuilder createAdditionalConnections(int numberConnections) {
    additionalConnections = numberConnections;
    return this;
  }

  public BlockSummaryWorkerBuilder addAnalysisWorker(
      BlockNode pNode, BlockSummaryAnalysisOptions pOptions) {
    workerGenerators.add(
        connection ->
            new BlockSummaryAnalysisWorker(
                nextId(pNode.getId()),
                pOptions,
                connection,
                pNode,
                cfa,
                specification,
                shutdownManager));
    return this;
  }

  public BlockSummaryWorkerBuilder addSmartAnalysisWorker(
      BlockNode pNode, BlockSummaryAnalysisOptions pOptions) {
    workerGenerators.add(
        connection ->
            new BlockSummarySmartAnalysisWorker(
                nextId(pNode.getId()),
                pOptions,
                connection,
                pNode,
                cfa,
                specification,
                shutdownManager));
    return this;
  }

  public BlockSummaryWorkerBuilder addResultCollectorWorker(
      Collection<BlockNode> nodes, BlockSummaryAnalysisOptions pOptions) {
    workerGenerators.add(connection -> new BlockSummaryResultWorker(nodes, connection, pOptions));
    return this;
  }

  public BlockSummaryWorkerBuilder addVisualizationWorker(
      BlockGraph pBlockTree, BlockSummaryAnalysisOptions pOptions) {
    workerGenerators.add(
        connection -> new BlockSummaryVisualizationWorker(pBlockTree, connection, pOptions));
    return this;
  }

  public BlockSummaryWorkerBuilder addRootWorker(
      BlockNode pNode, BlockSummaryAnalysisOptions pOptions) {
    workerGenerators.add(
        connection ->
            new BlockSummaryRootWorker(
                nextId(pNode.getId()),
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
    List<? extends BlockSummaryConnection> connections =
        connectionProvider.createConnections(workerGenerators.size() + additionalConnections);
    List<BlockSummaryWorker> worker = new ArrayList<>(workerGenerators.size());
    for (int i = 0; i < workerGenerators.size(); i++) {
      worker.add(workerGenerators.get(i).apply(connections.get(i)));
    }
    List<? extends BlockSummaryConnection> excessConnections =
        connections.subList(
            workerGenerators.size(), workerGenerators.size() + additionalConnections);
    return new Components(worker, excessConnections);
  }

  public static class Components {

    private final List<BlockSummaryWorker> workers;
    private final List<? extends BlockSummaryConnection> additionalConnections;

    private Components(
        List<BlockSummaryWorker> pWorkers,
        List<? extends BlockSummaryConnection> pAdditionalConnections) {
      workers = pWorkers;
      additionalConnections = pAdditionalConnections;
    }

    public List<BlockSummaryWorker> getWorkers() {
      return workers;
    }

    public List<? extends BlockSummaryConnection> getAdditionalConnections() {
      return additionalConnections;
    }
  }

  // Needed to forward exception handling to actual method and not this function.
  @FunctionalInterface
  private interface WorkerGenerator {
    BlockSummaryWorker apply(BlockSummaryConnection t)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException;
  }
}
