// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnectionProvider;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockSummaryWorkerBuilder {

  public record Components(
      ImmutableList<BlockSummaryActor> actors,
      ImmutableList<? extends BlockSummaryConnection> connections) {}

  private final CFA cfa;
  private final Specification specification;
  private final List<WorkerGenerator> workerGenerators;
  private final BlockSummaryConnectionProvider<?> connectionProvider;
  private int additionalConnections;

  public BlockSummaryWorkerBuilder(
      CFA pCFA,
      BlockSummaryConnectionProvider<?> pConnectionProvider,
      Specification pSpecification) {
    cfa = pCFA;
    specification = pSpecification;
    // only one available for now
    connectionProvider = pConnectionProvider;
    workerGenerators = new ArrayList<>();
  }

  public Components build()
      throws IOException, CPAException, InterruptedException, InvalidConfigurationException {
    List<? extends BlockSummaryConnection> connections =
        connectionProvider.createConnections(workerGenerators.size() + additionalConnections);
    List<BlockSummaryWorker> worker = new ArrayList<>();
    for (int i = 0; i < workerGenerators.size(); i++) {
      worker.add(workerGenerators.get(i).apply(connections.get(i)));
    }
    List<? extends BlockSummaryConnection> excessConnections =
        connections.subList(
            workerGenerators.size(), workerGenerators.size() + additionalConnections);
    return new Components(ImmutableList.copyOf(worker), ImmutableList.copyOf(excessConnections));
  }

  @CanIgnoreReturnValue
  public BlockSummaryWorkerBuilder createAdditionalConnections(int numberConnections) {
    additionalConnections = numberConnections;
    return this;
  }

  @CanIgnoreReturnValue
  public BlockSummaryWorkerBuilder addAnalysisWorker(
      BlockNode pNode, BlockSummaryAnalysisOptions pOptions) {
    String workerId = nextId(pNode.getId());
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection ->
            new BlockSummaryAnalysisWorker(
                nextId(pNode.getId()),
                pOptions,
                connection,
                pNode,
                cfa,
                specification,
                ShutdownManager.create(),
                logger));
    return this;
  }

  private LogManager getLogger(BlockSummaryAnalysisOptions pOptions, String workerId) {
    try {
      Path logDirectory = pOptions.getLogDirectory();
      if (logDirectory != null) {
        logDirectory.toFile().mkdirs();
        return BasicLogManager.createWithHandler(
            new FileHandler(pOptions.getLogDirectory().toString() + "/" + workerId + ".log"));
      }
    } catch (IOException e) {
      // fall-through to return null-log manager
    }
    return LogManager.createNullLogManager();
  }

  @CanIgnoreReturnValue
  public BlockSummaryWorkerBuilder addVisualizationWorker(
      BlockGraph pBlockTree, BlockSummaryAnalysisOptions pOptions) {
    String workerId = "visualization-worker";
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection ->
            new BlockSummaryVisualizationWorker(
                workerId, pBlockTree, connection, pOptions, logger));
    return this;
  }

  @CanIgnoreReturnValue
  public BlockSummaryWorkerBuilder addRootWorker(
      BlockNode pNode, BlockSummaryAnalysisOptions pOptions) {
    String workerId = "root-worker-" + nextId(pNode.getId());
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection -> new BlockSummaryRootWorker(workerId, connection, pNode, logger));
    return this;
  }

  @CanIgnoreReturnValue
  public BlockSummaryWorkerBuilder addHubWorker(
      BlockNode pNode,
      BlockSummaryAnalysisOptions pOptions,
      ShutdownManager pShutdownManager,
      int pMaxThreads) {
    String workerId = nextId(pNode.getId());
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection ->
            new BlockSummaryHubAnalysisWorker(
                workerId,
                connection,
                cfa,
                specification,
                pShutdownManager,
                pMaxThreads,
                pNode,
                pOptions,
                logger));
    return this;
  }

  private String nextId(String pAdditionalIdentifier) {
    return "W" + workerGenerators.size() + pAdditionalIdentifier;
  }

  // Needed to forward exception handling to actual method and not the add* functions.
  @FunctionalInterface
  private interface WorkerGenerator {
    BlockSummaryWorker apply(BlockSummaryConnection connection)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException;
  }
}
