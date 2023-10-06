// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockSummaryHubAnalysisWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;

  private final BlockingQueue<BlockSummaryAnalysisWorker> inactiveWorkers;
  private final Set<BlockSummaryAnalysisWorker> activeWorkers;
  private final int maxThreads;
  private final BlockNode block;
  private final BlockSummaryAnalysisOptions options;
  private final CFA cfa;
  private final Specification specification;
  private final ShutdownManager manager;
  private final ConcurrentHashMap<String, BlockSummaryErrorConditionMessage> errorConditions;
  private final ConcurrentHashMap<String, BlockSummaryPostConditionMessage> preConditions;
  private final Set<Thread> threads;
  private int counter;

  private boolean shutdownRequested;

  /**
   * Abstract definition of a Worker. All workers enter the same routine of receiving and producing
   * messages.
   *
   * @param pId the id of the worker
   */
  BlockSummaryHubAnalysisWorker(
      String pId,
      BlockSummaryConnection pConnection,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager,
      int pMaxThreads,
      BlockNode pBlockNode,
      BlockSummaryAnalysisOptions pOptions) {
    super(pId, pOptions);
    connection = pConnection;
    inactiveWorkers = new LinkedBlockingDeque<>();
    activeWorkers = Sets.newConcurrentHashSet();
    block = pBlockNode;
    maxThreads = pMaxThreads;
    options = pOptions;
    cfa = pCFA;
    specification = pSpecification;
    manager = pShutdownManager;
    errorConditions = new ConcurrentHashMap<>();
    preConditions = new ConcurrentHashMap<>();
    threads = Sets.newConcurrentHashSet();
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage) {
    return switch (pMessage.getType()) {
      case ERROR, FOUND_RESULT, STATISTICS -> {
        shutdownRequested = true;
        threads.forEach(Thread::interrupt);
        yield ImmutableSet.of();
      }
      case ERROR_CONDITION_UNREACHABLE -> ImmutableSet.of();
      case ERROR_CONDITION -> {
        errorConditions.put(pMessage.getBlockId(), (BlockSummaryErrorConditionMessage) pMessage);
        threads.add(spawnWorker(pMessage));
        yield ImmutableSet.of();
      }
      case BLOCK_POSTCONDITION -> {
        preConditions.put(pMessage.getBlockId(), (BlockSummaryPostConditionMessage) pMessage);
        for (BlockSummaryErrorConditionMessage value : errorConditions.values()) {
          threads.add(spawnWorker(value));
        }
        yield ImmutableSet.of();
      }
    };
  }

  private synchronized Thread spawnWorker(BlockSummaryMessage pMessage) {
    Thread thread =
        new Thread(
            () -> {
              ImmutableSet.Builder<BlockSummaryMessage> responses = ImmutableSet.builder();
              try {
                BlockSummaryAnalysisWorker worker = getWorker();
                for (BlockSummaryPostConditionMessage msg : preConditions.values()) {
                  responses.addAll(worker.processMessage(msg));
                }
                responses.addAll(worker.processMessage(pMessage));
                inactiveWorkers.put(worker);
              } catch (Exception e) {
                responses.add(BlockSummaryMessage.newErrorMessage(block.getId(), e));
              }
              broadcastOrLogException(responses.build());
              threads.remove(Thread.currentThread());
            });
    thread.setDaemon(true);
    threads.add(thread);
    thread.start();
    return thread;
  }

  private BlockSummaryAnalysisWorker getWorker()
      throws InterruptedException, CPAException, IOException, InvalidConfigurationException {
    if (!inactiveWorkers.isEmpty() || activeWorkers.size() == maxThreads) {
      BlockSummaryAnalysisWorker worker = inactiveWorkers.take();
      activeWorkers.add(worker);
      return worker;
    }
    BlockSummaryAnalysisWorker analysisWorker =
        new BlockSummaryAnalysisWorker(
            getId() + "-" + counter++, options, null, block, cfa, specification, manager);
    inactiveWorkers.put(analysisWorker);
    return getWorker();
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdownRequested;
  }
}
