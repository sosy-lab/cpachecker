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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnectionProvider;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DssWorkerBuilder {

  public record Components(
      ImmutableList<DssActor> actors, ImmutableList<? extends DssConnection> connections) {}

  private final CFA cfa;
  private final Specification specification;

  private final DssMessageFactory messageFactory;
  private final List<WorkerGenerator> workerGenerators;
  private final DssConnectionProvider<?> connectionProvider;
  private int additionalConnections;

  public DssWorkerBuilder(
      CFA pCFA,
      DssConnectionProvider<?> pConnectionProvider,
      Specification pSpecification,
      DssMessageFactory pMessageFactory) {
    cfa = pCFA;
    specification = pSpecification;
    messageFactory = pMessageFactory;
    // only one available for now
    connectionProvider = pConnectionProvider;
    workerGenerators = new ArrayList<>();
  }

  public Components build()
      throws IOException, CPAException, InterruptedException, InvalidConfigurationException {
    List<? extends DssConnection> connections =
        connectionProvider.createConnections(workerGenerators.size() + additionalConnections);
    List<DssWorker> worker = new ArrayList<>();
    for (int i = 0; i < workerGenerators.size(); i++) {
      worker.add(workerGenerators.get(i).apply(connections.get(i)));
    }
    List<? extends DssConnection> excessConnections =
        connections.subList(
            workerGenerators.size(), workerGenerators.size() + additionalConnections);
    return new Components(ImmutableList.copyOf(worker), ImmutableList.copyOf(excessConnections));
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder createAdditionalConnections(int numberConnections) {
    additionalConnections = numberConnections;
    return this;
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder addAnalysisWorker(BlockNode pNode, DssAnalysisOptions pOptions) {
    String workerId = nextId(pNode.getId());
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection ->
            new DssAnalysisWorker(
                nextId(pNode.getId()),
                pOptions,
                connection,
                pNode,
                cfa,
                specification,
                messageFactory,
                ShutdownManager.create(),
                logger));
    return this;
  }

  private LogManager getLogger(DssAnalysisOptions pOptions, String workerId) {
    try {
      Path logDirectory = pOptions.getLogDirectory();
      if (logDirectory != null) {
        boolean logDirectoryExists = logDirectory.toFile().mkdirs();
        if (!logDirectoryExists) {
          throw new IOException("Could not create log directory: " + logDirectory);
        }
        return BasicLogManager.createWithHandler(
            new FileHandler(logDirectory + "/" + workerId + ".log"));
      }
    } catch (IOException e) {
      // fall-through to return null-log manager
    }
    return LogManager.createNullLogManager();
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder addVisualizationWorker(
      BlockGraph pBlockTree, DssAnalysisOptions pOptions) {
    String workerId = "visualization-worker";
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection ->
            new DssVisualizationWorker(
                workerId, pBlockTree, connection, pOptions, messageFactory, logger));
    return this;
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder addRootWorker(BlockNode pNode, DssAnalysisOptions pOptions) {
    String workerId = "root-worker-" + nextId(pNode.getId());
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection -> new DssRootWorker(workerId, connection, pNode, messageFactory, logger));
    return this;
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder addHubWorker(
      BlockNode pNode,
      DssAnalysisOptions pOptions,
      ShutdownManager pShutdownManager,
      int pMaxThreads) {
    String workerId = nextId(pNode.getId());
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.add(
        connection ->
            new DssHubAnalysisWorker(
                workerId,
                connection,
                cfa,
                specification,
                pShutdownManager,
                pMaxThreads,
                pNode,
                pOptions,
                messageFactory,
                logger));
    return this;
  }

  private String nextId(String pAdditionalIdentifier) {
    return "W" + workerGenerators.size() + pAdditionalIdentifier;
  }

  // Needed to forward exception handling to actual method and not the add* functions.
  @FunctionalInterface
  private interface WorkerGenerator {
    DssWorker apply(DssConnection connection)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException;
  }
}
