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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DssHubAnalysisWorker extends DssWorker {

  private final DssConnection connection;

  private final BlockingQueue<DssAnalysisWorker> inactiveWorkers;
  private final Set<DssAnalysisWorker> activeWorkers;
  private final int maxThreads;
  private final BlockNode block;
  private final DssAnalysisOptions options;
  private final CFA cfa;
  private final Specification specification;
  private final ShutdownManager manager;
  private final DssMessageFactory messageFactory;
  private final LogManager logger;
  private final ConcurrentHashMap<String, DssErrorConditionMessage> errorConditions;
  private final ConcurrentHashMap<String, DssPostConditionMessage> preConditions;
  private final Set<Thread> threads;
  private int counter;

  private boolean shutdownRequested;

  /**
   * Abstract definition of a Worker. All workers enter the same routine of receiving and producing
   * messages.
   *
   * @param pId the id of the worker
   */
  DssHubAnalysisWorker(
      String pId,
      DssConnection pConnection,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager,
      int pMaxThreads,
      BlockNode pBlockNode,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogger) {
    super(pId, pMessageFactory, pLogger);
    connection = pConnection;
    inactiveWorkers = new LinkedBlockingDeque<>();
    activeWorkers = Sets.newConcurrentHashSet();
    block = pBlockNode;
    maxThreads = pMaxThreads;
    options = pOptions;
    cfa = pCFA;
    specification = pSpecification;
    manager = pShutdownManager;
    messageFactory = pMessageFactory;
    logger = pLogger;
    errorConditions = new ConcurrentHashMap<>();
    preConditions = new ConcurrentHashMap<>();
    threads = Sets.newConcurrentHashSet();
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage) {
    return switch (pMessage.getType()) {
      case ERROR, FOUND_RESULT, STATISTICS -> {
        shutdownRequested = true;
        threads.forEach(Thread::interrupt);
        yield ImmutableSet.of();
      }
      case ERROR_CONDITION -> {
        errorConditions.put(pMessage.getBlockId(), (DssErrorConditionMessage) pMessage);
        threads.add(spawnWorker(pMessage));
        yield ImmutableSet.of();
      }
      case BLOCK_POSTCONDITION -> {
        preConditions.put(pMessage.getBlockId(), (DssPostConditionMessage) pMessage);
        for (DssErrorConditionMessage value : errorConditions.values()) {
          threads.add(spawnWorker(value));
        }
        yield ImmutableSet.of();
      }
    };
  }

  private synchronized Thread spawnWorker(DssMessage pMessage) {
    Thread thread =
        new Thread(
            () -> {
              ImmutableSet.Builder<DssMessage> responses = ImmutableSet.builder();
              try {
                DssAnalysisWorker worker = getWorker();
                for (DssPostConditionMessage msg : preConditions.values()) {
                  responses.addAll(worker.processMessage(msg));
                }
                responses.addAll(worker.processMessage(pMessage));
                inactiveWorkers.put(worker);
              } catch (Exception e) {
                responses.add(messageFactory.newErrorMessage(block.getId(), e));
              }
              broadcastOrLogException(responses.build());
              threads.remove(Thread.currentThread());
            });
    thread.setDaemon(true);
    threads.add(thread);
    thread.start();
    return thread;
  }

  private DssAnalysisWorker getWorker()
      throws InterruptedException, CPAException, IOException, InvalidConfigurationException {
    if (!inactiveWorkers.isEmpty() || activeWorkers.size() == maxThreads) {
      DssAnalysisWorker worker = inactiveWorkers.take();
      activeWorkers.add(worker);
      return worker;
    }
    DssAnalysisWorker analysisWorker =
        new DssAnalysisWorker(
            getId() + "-" + counter++,
            options,
            null,
            block,
            cfa,
            specification,
            messageFactory,
            manager,
            logger);
    inactiveWorkers.put(analysisWorker);
    return getWorker();
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdownRequested;
  }
}
