// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer.InferDCPAAlgorithm.ConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryWorker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class InferWorker extends BlockSummaryWorker {

  private final BlockNode block;

  private final InferDCPAAlgorithm dcpaAlgorithm;

  private boolean shutdown;

  private final BlockSummaryConnection connection;

  private final String workerFunction;

  static final String TOTAL_BLOCK_MESSAGES = "total_block_messages";

  private Map<String, Optional<Integer>> expectedMessages;
  private Map<String, Integer> messagesReceived;
  private Map<String, Integer> runCounts;
  private int messagesSent;

  /**
   * {@link InferWorker}s trigger CEGAR refinement using forward and backward analyses to find a
   * verification verdict.
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
  public InferWorker(
      String pId,
      InferOptions pOptions,
      BlockSummaryConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("infer-worker-" + pId, new BlockSummaryAnalysisOptions(pOptions.getParentConfig()));
    block = pBlock;
    connection = pConnection;

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();

    dcpaAlgorithm =
        new InferDCPAAlgorithm(
            getLogger(), pBlock, pCFA, pSpecification, forwardConfiguration, pShutdownManager);
    workerFunction = block.getFirst().getFunctionName();
    expectedMessages = initialOptionalIntMap();
    messagesReceived = initialIntMap();
    runCounts = initialIntMap();

    messagesSent = 0;
  }

  /*
   * In the context of Infer, processMessage is a strengthening operation on a block
   */
  @Override
  @SuppressWarnings("unchecked")
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage message)
      throws InterruptedException, CPAException, SolverException {

    if (!isCalled(message)) {
      return ImmutableSet.of();
    }

    return switch (message.getType()) {
      case ERROR_CONDITION, BLOCK_POSTCONDITION -> {
        String messageFunction =
            (String) message.getPayload().get(InferDCPAAlgorithm.MESSAGE_FUNCTION);
        int incrementedReceivedCount = messagesReceived.get(messageFunction) + 1;
        messagesReceived.put(messageFunction, incrementedReceivedCount);

        int messageRunCount = (int) message.getPayload().get(InferDCPAAlgorithm.RUN_ORDER);
        int workerRunCount = runCounts.get(messageFunction);

        // If we get a message with a run count lower than the current max run count, then we can
        // just ignore it since it is less precise than what we already have
        if (workerRunCount > messageRunCount) {
          yield ImmutableSet.of();
        }

        AbstractState state = dcpaAlgorithm.getDCPA().getDeserializeOperator().deserialize(message);

        Builder<BlockSummaryMessage> messagesBuilder = ImmutableList.builder();

        Collection<BlockSummaryMessage> messages;
        if (workerRunCount < messageRunCount) {
          messages =
              dcpaAlgorithm.runAnalysisUnderCondition(
                  Optional.of(state),
                  message.getType(),
                  messageFunction,
                  ConditionOperator.REPLACE);
        } else {
          messages =
              dcpaAlgorithm.runAnalysisUnderCondition(
                  Optional.of(state), message.getType(), messageFunction, ConditionOperator.ADD);
        }

        messagesSent += messages.size();
        messagesBuilder.addAll(messages);

        if (allAcknowledged() && allReceived()) {
          BlockSummaryMessage ackMessage = createAcknowledgementMessage(messagesSent);
          messagesBuilder.add(ackMessage);
        }

        yield messagesBuilder.build();
      }
      case INFER_ACKNOWLEDGMENT -> {
        String messageFunction =
            (String) message.getPayload().get(InferDCPAAlgorithm.MESSAGE_FUNCTION);
        Integer ackCount = (Integer) message.getPayload().get(TOTAL_BLOCK_MESSAGES);
        expectedMessages.put(messageFunction, Optional.of(ackCount));

        if (allAcknowledged() && allReceived()) {
          yield ImmutableSet.of(createAcknowledgementMessage(messagesSent));
        }
        yield ImmutableSet.of();
      }
      default -> {
        yield ImmutableSet.of();
      }
    };
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }

  @Override
  public void run() {
    try {
      Collection<BlockSummaryMessage> messages = dcpaAlgorithm.runInitialAnalysis();
      int initialSize = messages.size();
      messagesSent += initialSize;
      if (isLeafWorker()) {
        BlockSummaryMessage acknowledgement = createAcknowledgementMessage(initialSize);
        List<BlockSummaryMessage> messagesWithAck = listAndElement(messages, acknowledgement);
        broadcast(messagesWithAck);
      } else {
        broadcast(messages);
      }
      super.run();
    } catch (CPAException e) {
      getLogger().logException(Level.SEVERE, e, "Worker stopped working...");
      broadcastOrLogException(
          ImmutableSet.of(BlockSummaryMessage.newErrorMessage(getBlockId(), e)));
    } catch (InterruptedException e) {
      getLogger().logException(Level.SEVERE, e, "Thread interrupted unexpectedly.");
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{block=" + block + ", finished=" + shutdownRequested() + '}';
  }

  private boolean isCalled(BlockSummaryMessage message) {
    String messageFunction =
        (String) message.getPayload().getOrDefault(InferDCPAAlgorithm.MESSAGE_FUNCTION, "");
    return calledFunctions().contains(messageFunction);
  }

  private ImmutableSet<String> calledFunctions() {
    return FluentIterable.from(block.getEdges())
        .filter(FunctionSummaryEdge.class)
        .transform(edge -> edge.getFunctionEntry().getFunctionName())
        .filter(name -> !name.equals("reach_error"))
        .toSet();
  }

  private boolean allAcknowledged() {
    return !expectedMessages.containsValue(Optional.empty());
  }

  private boolean allReceived() {
    for (Map.Entry<String, Integer> entry : messagesReceived.entrySet()) {
      Optional<Integer> count = expectedMessages.get(entry.getKey());
      if (count.isEmpty() || !count.orElseThrow().equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  private BlockSummaryMessage createAcknowledgementMessage(int pSize) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(InferDCPAAlgorithm.MESSAGE_FUNCTION, workerFunction)
            .addEntry(TOTAL_BLOCK_MESSAGES, pSize)
            .buildPayload();
    return InferAcknowledgementMessage.newAcknowledgement(
        block.getId(), block.getLast().getNodeNumber(), payload);
  }

  private Map<String, Optional<Integer>> initialOptionalIntMap() {
    Map<String, Optional<Integer>> initial = new HashMap<>();
    for (String fn : calledFunctions()) {
      initial.put(fn, Optional.empty());
    }
    return initial;
  }

  private Map<String, Integer> initialIntMap() {
    Map<String, Integer> initial = new HashMap<>();
    for (String fn : calledFunctions()) {
      initial.put(fn, 0);
    }
    return initial;
  }

  private boolean isLeafWorker() {
    return calledFunctions().isEmpty();
  }
}
