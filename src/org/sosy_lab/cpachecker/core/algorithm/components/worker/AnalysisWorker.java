// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.BackwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.block_analysis.BlockAnalysis.ForwardAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class AnalysisWorker extends Worker {

  protected final BlockNode block;

  protected final BlockAnalysis forwardAnalysis;
  protected final BlockAnalysis backwardAnalysis;

  AnalysisWorker(
      String pId,
      AnalysisOptions pOptions,
      BlockNode pBlock,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      UpdatedTypeMap pTypeMap)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    super("analysis-worker-" + pBlock.getId(), pLogger, pOptions);
    block = pBlock;

    String withAbstraction =
        analysisOptions.doAbstractAtTargetLocations() ? "-with-abstraction" : "";

    Configuration fileConfig =
        Configuration.builder().loadFromFile(
                "config/predicateAnalysis-block-backward" + withAbstraction + ".properties")
            .build();
    ConfigString backward = new ConfigString();
    fileConfig.inject(backward);

    Configuration backwardConfiguration = Configuration.builder()
        .copyFrom(fileConfig)
        .setOption("CompositeCPA.cpas", "cpa.block.BlockCPABackward, " + backward.cpas)
        .build();

    Specification backwardSpecification =
        Specification.fromFiles(ImmutableSet.of(Path.of("config/specification/MainEntry.spc"),
                Path.of("config/specification/TerminatingFunctions.spc")),
            pCFA, backwardConfiguration, logger, pShutdownManager.getNotifier());

    ConfigString forward = new ConfigString();
    pConfiguration.inject(forward);
    Configuration forwardConfiguration =
        Configuration.builder()
            .copyFrom(pConfiguration)
            .loadFromFile(
                "config/predicateAnalysis-block-forward" + withAbstraction + ".properties")
            .setOption("CompositeCPA.cpas", forward.cpas + ", cpa.block.BlockCPA")
            .build();

    forwardAnalysis = new ForwardAnalysis(pId, pLogger, pBlock, pCFA, pTypeMap, pSpecification,
        forwardConfiguration,
        pShutdownManager, pOptions);

    backwardAnalysis = new BackwardAnalysis(pId, pLogger, pBlock, pCFA,
        pTypeMap, backwardSpecification,
        backwardConfiguration, pShutdownManager, pOptions);
  }

  @Override
  public Collection<Message> processMessage(Message message)
      throws InterruptedException, CPAException, IOException, SolverException {
    switch (message.getType()) {
      case ERROR_CONDITION:
        return processErrorCondition(message);
      case BLOCK_POSTCONDITION:
        return processBlockPostCondition(message);
      case ERROR:
        // fall through
      case FOUND_RESULT:
        shutdown();
        // fall through
      case ERROR_CONDITION_UNREACHABLE:
        return ImmutableSet.of();
      default:
        throw new AssertionError("MessageType " + message.getType() + " does not exist");
    }
  }

  private Collection<Message> processBlockPostCondition(Message message)
      throws CPAException, InterruptedException, SolverException {
    MessageProcessing processing = forwardAnalysis.getDistributedCPA().proceed(message);
    if (processing.end()) {
      return processing;
    }
    return forwardAnalysis(processing);
  }

  private Collection<Message> processErrorCondition(Message message)
      throws SolverException, InterruptedException, CPAException {
    DistributedCompositeCPA distributed = backwardAnalysis.getDistributedCPA();
    MessageProcessing processing = distributed.proceed(message);
    if (processing.end()) {
      return processing;
    }
    return backwardAnalysis(processing);
  }

  // return post condition
  private Collection<Message> forwardAnalysis(Collection<Message> pPostConditionMessages)
      throws CPAException, InterruptedException, SolverException {
    forwardAnalysis.getDistributedCPA().synchronizeKnowledge(backwardAnalysis.getDistributedCPA());
    stats.forwardAnalysis.inc();
    return forwardAnalysis.analyze(pPostConditionMessages);
  }

  // return pre-condition
  protected Collection<Message> backwardAnalysis(MessageProcessing pMessageProcessing)
      throws CPAException, InterruptedException, SolverException {
    assert pMessageProcessing.size() == 1 : "BackwardAnalysis can only be based on one message";
    backwardAnalysis.getDistributedCPA().synchronizeKnowledge(forwardAnalysis.getDistributedCPA());
    stats.backwardAnalysis.inc();
    return backwardAnalysis.analyze(pMessageProcessing);
  }

  @Override
  public void run() {
    try {
      if (!block.isSelfCircular() && !block.getPredecessors().isEmpty()) {
        List<Message> initialMessages = ImmutableList.copyOf(forwardAnalysis(ImmutableSet.of(
            Message.newBlockPostCondition("", block.getStartNode().getNodeNumber(), Payload.empty(),
                false, true, ImmutableSet.of()))));
        Optional<Message> optionalMessage =
            initialMessages.stream().filter(m -> m.getType() == MessageType.BLOCK_POSTCONDITION)
                .findAny();
        if (optionalMessage.isPresent()) {
          Message message = optionalMessage.orElseThrow();
          if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
            forwardAnalysis.getDistributedCPA().setFirstMessage(message);
            backwardAnalysis.getDistributedCPA().setFirstMessage(message);
          }
        }
        broadcast(initialMessages);
      }
      super.run();
    } catch (CPAException | InterruptedException | IOException | SolverException pE) {
      logger.log(Level.SEVERE, "Worker run into an error: %s", pE);
      logger.log(Level.SEVERE, "Stopping analysis...");
    }
  }

  public String getBlockId() {
    return block.getId();
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

  @Options(prefix = "CompositeCPA")
  private static class ConfigString {
    @Option(description = "Read config")
    private String cpas = "";
  }
}
