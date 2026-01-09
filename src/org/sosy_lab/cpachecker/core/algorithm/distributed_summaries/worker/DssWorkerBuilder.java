// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.CommunicationId;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssCommunicationEntity;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssMessageBroadcaster;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssSchedulerConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DssWorkerBuilder {

  private final CFA cfa;
  private final Specification specification;

  private final DssMessageFactory messageFactory;
  private final ImmutableMap.Builder<CommunicationId, WorkerGenerator> workerGenerators;
  private final Supplier<BlockingQueue<DssMessage>> queueFactory;

  public DssWorkerBuilder(
      CFA pCFA,
      Specification pSpecification,
      Supplier<BlockingQueue<DssMessage>> pQueueFactory,
      DssMessageFactory pMessageFactory) {
    cfa = pCFA;
    specification = pSpecification;
    queueFactory = pQueueFactory;
    messageFactory = pMessageFactory;
    workerGenerators = ImmutableMap.builder();
  }

  public List<DssActor> build()
      throws IOException, CPAException, InterruptedException, InvalidConfigurationException {

    // create a queue for each worker
    ImmutableMap<CommunicationId, WorkerGenerator> futureWorkers = workerGenerators.buildOrThrow();
    ImmutableMap.Builder<CommunicationId, BlockingQueue<DssMessage>> queues =
        ImmutableMap.builderWithExpectedSize(futureWorkers.size());
    for (CommunicationId id : futureWorkers.keySet()) {
      queues.put(id, queueFactory.get());
    }
    ImmutableMap<CommunicationId, BlockingQueue<DssMessage>> allQueues = queues.buildOrThrow();

    // create a broadcaster for all queues
    DssMessageBroadcaster broadcaster = new DssMessageBroadcaster(allQueues);

    // create connections for each worker
    ImmutableList.Builder<DssActor> workers =
        ImmutableList.builderWithExpectedSize(futureWorkers.size());
    for (Entry<CommunicationId, WorkerGenerator> generatorEntry : futureWorkers.entrySet()) {
      DssSchedulerConnection connection =
          new DssSchedulerConnection(allQueues.get(generatorEntry.getKey()), broadcaster);
      workers.add(generatorEntry.getValue().apply(connection));
    }

    return workers.build();
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder addAnalysisWorker(BlockNode pNode, DssAnalysisOptions pOptions) {
    String workerId = pNode.getId();
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.put(
        new CommunicationId(workerId, DssCommunicationEntity.BLOCK),
        connection ->
            new DssAnalysisWorker(
                workerId,
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

  @CanIgnoreReturnValue
  public DssWorkerBuilder addVisualizationWorker(
      BlockGraph pBlockGraph, DssAnalysisOptions pOptions) {
    // ensures that exactly one visualization worker is created
    // as later, we call buildOrThrow() on the map
    String workerId = "visualization-worker";
    final LogManager logger = getLogger(pOptions, workerId);
    workerGenerators.put(
        new CommunicationId(workerId, DssCommunicationEntity.OBSERVER),
        connection ->
            new DssVisualizationWorker(
                workerId, pBlockGraph, connection, pOptions, messageFactory, logger));
    return this;
  }

  @CanIgnoreReturnValue
  public DssWorkerBuilder addObserverWorker(
      String pId, int pNumberOfBlocks, DssAnalysisOptions pOptions) {
    final LogManager logger = getLogger(pOptions, pId);
    workerGenerators.put(
        new CommunicationId(pId, DssCommunicationEntity.OBSERVER),
        connection ->
            new DssObserverWorker(pId, connection, pNumberOfBlocks, messageFactory, logger));
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

  // Needed to forward exception handling to actual method and not the add* functions.
  @FunctionalInterface
  private interface WorkerGenerator {
    DssWorker apply(DssConnection connection)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException;
  }
}
