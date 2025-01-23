// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics.ThreadCPUTimer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg.DistributedARGCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssStatisticsMessage.DssStatisticType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DssAnalysisWorker extends DssWorker {

  private final BlockNode block;

  private final DssBlockAnalysis dssBlockAnalysis;
  private final LogManager logger;
  private final DssMessageFactory messageFactory;
  private boolean shutdown;

  private final DssConnection connection;

  private final ThreadCPUTimer forwardAnalysisTime = new ThreadCPUTimer("Forward Analysis");
  private final ThreadCPUTimer backwardAnalysisTime = new ThreadCPUTimer("Backward Analysis");

  /**
   * {@link DssAnalysisWorker}s trigger forward and backward analyses to find a verification
   * verdict.
   *
   * @param pId unique id of worker that will be prefixed with 'analysis-worker-'
   * @param pOptions analysis options for distributed analysis
   * @param pConnection unique connection to other actors
   * @param pBlock block where this analysis works on
   * @param pCFA complete CFA of which pBlock is a subgraph
   * @param pSpecification specification that should not be violated
   * @param pShutdownManager handler for unexpected shutdowns
   * @throws CPAException exceptions that are logged
   * @throws InterruptedException thrown if user exits program
   * @throws InvalidConfigurationException thrown if configuration contains unexpected values
   * @throws IOException thrown if socket and/or files are not readable
   */
  DssAnalysisWorker(
      String pId,
      DssAnalysisOptions pOptions,
      DssConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager,
      LogManager pLogger)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("analysis-worker-" + pId, pMessageFactory, pLogger);
    block = pBlock;
    connection = pConnection;

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();

    messageFactory = pMessageFactory;
    logger = pLogger;
    dssBlockAnalysis =
        new DssBlockAnalysis(
            logger,
            pBlock,
            pCFA,
            pSpecification,
            forwardConfiguration,
            pOptions,
            pMessageFactory,
            pShutdownManager);
  }

  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, SolverException, InterruptedException {
    return dssBlockAnalysis.runInitialAnalysis();
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage message) {
    switch (message.getType()) {
      case ERROR_CONDITION -> {
        try {
          backwardAnalysisTime.start();
          return dssBlockAnalysis.runAnalysisUnderCondition(
              (DssErrorConditionMessage) message, true);
        } catch (Exception | Error e) {
          return ImmutableSet.of(messageFactory.newErrorMessage(getBlockId(), e));
        } finally {
          backwardAnalysisTime.stop();
        }
      }
      case BLOCK_POSTCONDITION -> {
        try {
          forwardAnalysisTime.start();
          return dssBlockAnalysis.runAnalysis((DssPostConditionMessage) message);
        } catch (Exception | Error e) {
          return ImmutableSet.of(messageFactory.newErrorMessage(getBlockId(), e));
        } finally {
          forwardAnalysisTime.stop();
        }
      }
      case ERROR, FOUND_RESULT -> {
        shutdown = true;
        return ImmutableSet.of(messageFactory.newStatisticsMessage(getBlockId(), getStats()));
      }
      case STATISTICS -> {
        return ImmutableSet.of();
      }
      default -> throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  public void storeMessage(DssMessage message) throws SolverException, InterruptedException {
    switch (message.getType()) {
      case STATISTICS, FOUND_RESULT, ERROR -> {}
      case ERROR_CONDITION -> {
        DssErrorConditionMessage errorCond = (DssErrorConditionMessage) message;
        dssBlockAnalysis.updateErrorCondition(errorCond);
        dssBlockAnalysis.updateSeenPrefixes(errorCond);
      }
      case BLOCK_POSTCONDITION -> {
        //noinspection ResultOfMethodCallIgnored
        dssBlockAnalysis.shouldRepeatAnalysis((DssPostConditionMessage) message);
      }
      default -> throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void run() {
    try {
      broadcast(dssBlockAnalysis.runInitialAnalysis());
      super.run();
    } catch (Exception | Error e) {
      logger.logException(Level.SEVERE, e, "Worker stopped working due to an error...");
      broadcastOrLogException(ImmutableSet.of(messageFactory.newErrorMessage(getBlockId(), e)));
      shutdown = true;
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{block=" + block + ", finished=" + shutdownRequested() + '}';
  }

  private Map<String, Object> getStats() {
    DistributedCompositeCPA forwardDCPA = null;
    if (dssBlockAnalysis.getDCPA() instanceof DistributedARGCPA arg) {
      if (arg.getWrappedCPA() instanceof DistributedCompositeCPA composite) {
        forwardDCPA = composite;
      }
    }

    return ImmutableMap.<String, Object>builder()
        .put(DssStatisticType.FORWARD_TIME.name(), forwardAnalysisTime.nanos())
        .put(DssStatisticType.BACKWARD_TIME.name(), backwardAnalysisTime.nanos())
        .put(DssStatisticType.MESSAGES_SENT.name(), Integer.toString(getSentMessages()))
        .put(DssStatisticType.MESSAGES_RECEIVED.name(), Integer.toString(getReceivedMessages()))
        .put(
            DssStatisticType.FORWARD_ANALYSIS_STATS.name(),
            forwardDCPA == null ? ImmutableMap.of() : forwardDCPA.getStatistics().getStatistics())
        .buildOrThrow();
  }
}
