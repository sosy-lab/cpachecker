// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.executors;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssAllWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness.DssWitnessArgStateCollector;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The single worker executor decomposed the CFA in multiple blocks and then only runs the block
 * analysis on one given block. For this, all{@link SingleWorkerDssExecutor#knownConditions known
 * conditions} need to be set in advance and all messages yet to be processed need to be set as
 * {@link SingleWorkerDssExecutor#newConditions}. The results will be written to a given output
 * directory.
 */
@Options(prefix = "distributedSummaries.singleWorker")
public class SingleWorkerDssExecutor implements DssExecutor {

  private record OldAndNewMessages(List<DssMessage> oldMessages, List<DssMessage> newMessages) {}

  private final Specification specification;
  private final DssAnalysisOptions options;
  private final DssMessageFactory messageFactory;
  private final ShutdownManager shutdownManager;

  @FileOption(Type.OUTPUT_DIRECTORY)
  @Option(description = "Where to write responses", secure = true)
  private Path outputMessages = Path.of("messages/");

  @FileOption(Type.OPTIONAL_INPUT_FILE)
  @Option(
      description =
          "List of input files that contain preconditions and verification conditions that should"
              + " be assumed as 'known' by block-summary analysis."
              + " Each file must contain a single, valid JSON DssMessage."
              + " If at least one file is provided, the block-summary analysis assumes"
              + " these pre- and verification-conditions."
              + " If no file is provided, the block-summary analysis assumes"
              + " the precondition 'true' and the verification condition 'false'.",
      secure = true)
  private List<Path> knownConditions = ImmutableList.of();

  @FileOption(Type.OPTIONAL_INPUT_FILE)
  @Option(
      description =
          "List of input files that contain preconditions and verification conditions that should"
              + " be assumed as 'new' by block-summary analysis."
              + " For each message in this list, block-summary analysis will perform a new analysis"
              + " run in the order of occurrence."
              + " Each file must contain a single, valid JSON DssMessage."
              + " If at least one file is provided, the block-summary analysis assumes"
              + " these pre- and verification-conditions."
              + " If no file is provided, the block-summary analysis assumes"
              + " the precondition 'true' and the verification condition 'false'.",
      secure = true)
  private List<Path> newConditions = ImmutableList.of();

  @Option(description = "Whether to spawn a worker for only one block id", secure = true)
  private String spawnWorkerForId = "";

  public SingleWorkerDssExecutor(
      Configuration pConfiguration, Specification pSpecification, ShutdownManager pShutdownManager)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    options = new DssAnalysisOptions(pConfiguration);
    messageFactory = new DssMessageFactory(options);
    specification = pSpecification;
    shutdownManager = pShutdownManager;
    if (Stream.concat(knownConditions.stream(), newConditions.stream())
        .anyMatch(f -> !Files.isRegularFile(f))) {
      throw new InvalidConfigurationException(
          "All input messages must be files that exist: " + knownConditions + ", " + newConditions);
    }
  }

  private void writeAllMessages(List<DssMessage> response) throws IOException {
    int messageCount = 0;
    for (DssMessage dssMessage : response) {
      Files.createDirectories(outputMessages);
      final String outputFileNamePrefix = dssMessage.getType().name();
      final String outputFileName = outputFileNamePrefix + messageCount + ".json";
      Path outputPath = outputMessages.resolve(outputFileName);
      JSON.writeJSONString(dssMessage.asJson(), outputPath);
      messageCount++;
    }
  }

  private static List<DssMessage> readMessages(List<Path> pPaths) throws IOException {
    List<DssMessage> messages = new ArrayList<>(pPaths.size());
    for (Path path : pPaths) {
      messages.add(DssMessage.fromJson(path));
    }
    return messages;
  }

  private OldAndNewMessages prepareOldAndNewMessages(
      List<Path> pKnownConditions, List<Path> pNewConditions) throws IOException {
    return new OldAndNewMessages(readMessages(pKnownConditions), readMessages(pNewConditions));
  }

  @Override
  public StatusAndResult execute(
      CFA cfa,
      BlockGraph blockGraph,
      DssWitnessArgStateCollector stateCollector,
      DssAllWorkerStatistics workerStatistics)
      throws CPAException,
          SolverException,
          InterruptedException,
          InvalidConfigurationException,
          IOException {
    BlockNode blockNode =
        blockGraph.getNodes().stream()
            .filter(b -> b.getId().equals(spawnWorkerForId))
            .findAny()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No block with id '" + spawnWorkerForId + "' found in the block graph."));
    try (DssActors actors =
        new DssWorkerBuilder(
                cfa,
                specification,
                () -> new DssDefaultQueue(),
                messageFactory,
                workerStatistics,
                shutdownManager)
            .addAnalysisWorker(blockNode, options)
            .build()) {

      DssAnalysisWorker actor = (DssAnalysisWorker) Objects.requireNonNull(actors.getOnlyActor());
      // use list instead of set. Each message has a unique timestamp,
      // so there will be no duplicates that a set can remove.
      // But the equality checks are unnecessarily expensive
      List<DssMessage> response = new ArrayList<>();
      if (knownConditions.isEmpty() && newConditions.isEmpty()) {
        response.addAll(actor.runInitialAnalysis());
      } else {
        OldAndNewMessages preparedBatches =
            prepareOldAndNewMessages(knownConditions, newConditions);
        for (DssMessage message : preparedBatches.oldMessages()) {
          actor.storeMessage(message);
        }
        for (DssMessage message : preparedBatches.newMessages()) {
          response.addAll(actor.processMessage(message));
        }
      }
      writeAllMessages(response);
    }
    return new StatusAndResult(AlgorithmStatus.NO_PROPERTY_CHECKED, Result.UNKNOWN);
  }
}
