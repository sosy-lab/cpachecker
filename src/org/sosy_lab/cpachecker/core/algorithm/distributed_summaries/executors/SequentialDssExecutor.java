// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssResultMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The sequential mode spawns one worker for each block and runs them one after the other for every
 * message until there are either no messages left to process (proof) or a violation condition is
 * broadcasted by a worker operating on a block node without predecessors.
 */
public class SequentialDssExecutor implements DssExecutor, AutoCloseable {

  private final DssMessageFactory messageFactory;
  private final DssAnalysisOptions options;
  private final Specification specification;
  private final List<Statistics> stats;

  public SequentialDssExecutor(Configuration pConfiguration, Specification pSpecification)
      throws InvalidConfigurationException {
    specification = pSpecification;
    options = new DssAnalysisOptions(pConfiguration);
    messageFactory = new DssMessageFactory(options);
    stats = new ArrayList<>();
  }

  private DssActors createDssActors(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    ImmutableSet<BlockNode> blocks = blockGraph.getNodes();
    DssWorkerBuilder builder =
        new DssWorkerBuilder(cfa, specification, () -> new DssDefaultQueue(), messageFactory);
    for (BlockNode distinctNode : blocks) {
      builder = builder.addAnalysisWorker(distinctNode, options);
    }
    if (options.isDebugModeEnabled()) {
      builder = builder.addVisualizationWorker(blockGraph, options);
    }
    return builder.build();
  }

  @Override
  public StatusAndResult execute(CFA cfa, BlockGraph blockGraph)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    DssActors actors = createDssActors(cfa, blockGraph);
    stats.addAll(actors.getWorkersWithStats());

    Set<String> finished = new LinkedHashSet<>();

    try {
      for (DssAnalysisWorker actor : actors.getAnalysisWorkers()) {
        actor.broadcastInitialMessages();
      }

      while (finished.size() < actors.getActors().size()) {
        for (DssActor actor : actors.getActors()) {
          if (actor.getConnection().hasPendingMessages()) {
            finished.remove(actor.getId());
            DssMessage next = actor.nextMessage();
            if (next instanceof DssResultMessage resultMessage) {
              return new StatusAndResult(
                  AlgorithmStatus.SOUND_AND_PRECISE, resultMessage.getResult());
            }
            if (next instanceof DssExceptionMessage exceptionMessage) {
              throw new CPAException(exceptionMessage.getExceptionMessage());
            }
            Collection<DssMessage> results = actor.processMessage(next);
            actor.broadcast(results);
          } else {
            finished.add(actor.getId());
          }
        }
      }
    } catch (SolverException e) {
      throw new CPAException("Solver exception", e);
    }

    // blocks the thread until the result message is received
    return new StatusAndResult(AlgorithmStatus.SOUND_AND_PRECISE, Result.TRUE);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.addAll(stats);
  }

  @Override
  public void close() {
    // TODO
  }
}
