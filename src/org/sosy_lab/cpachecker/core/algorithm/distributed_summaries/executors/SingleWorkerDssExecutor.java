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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssDefaultQueue;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisWorker;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusAndResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssWorkerBuilder;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "distributedSummaries.singleWorker")
public class SingleWorkerDssExecutor implements DssExecutor {

  private record OldAndNewMessages(List<DssMessage> oldMessages, List<DssMessage> newMessages) {}

  private final Specification specification;
  private final DssAnalysisOptions options;
  private final DssMessageFactory messageFactory;

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

  public SingleWorkerDssExecutor(Configuration pConfiguration, Specification pSpecification)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    options = new DssAnalysisOptions(pConfiguration);
    messageFactory = new DssMessageFactory(options);
    specification = pSpecification;
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

  private OldAndNewMessages prepareOldAndNewMessages(
      List<Path> pKnownConditions, List<Path> pNewConditions) throws IOException {
    List<DssMessage> toBeConsideredOld = new ArrayList<>();
    List<DssMessage> toBeConsideredNew = new ArrayList<>();
    // known conditions always stay 'old' and never become 'true'
    for (Path knownMessageFile : pKnownConditions) {
      DssMessage message = DssMessage.fromJson(knownMessageFile);
      toBeConsideredOld.add(message);
    }

    // new conditions can be considered 'new' (the default), but under certain conditions
    // we can avoid unnecessary analysis when we know that considering them 'old' is semantically
    // equivalent.
    // effect of a new postcondition: starts for each verification condition a new analysis run that
    // considers all known + the new postcondition
    // Multiple new postconditions have the same effect as only taking one of them as 'new' and the
    // others as 'old'.
    boolean isFirstPostcondition = true;
    for (Path newMessageFile : pNewConditions) {
      DssMessage message = DssMessage.fromJson(newMessageFile);
      if (message.getType() == DssMessageType.POST_CONDITION) {
        if (isFirstPostcondition) {
          // Do postconditions first, so that information is known before error conditions are
          // checked
          toBeConsideredNew.addFirst(message);
          isFirstPostcondition = false;
        } else {
          toBeConsideredOld.add(message);
        }
      } else {
        toBeConsideredNew.add(message);
      }
    }
    return new OldAndNewMessages(toBeConsideredOld, toBeConsideredNew);
  }

  @Override
  public StatusAndResult execute(CFA cfa, BlockGraph blockGraph)
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
    DssActors actors =
        new DssWorkerBuilder(cfa, specification, () -> new DssDefaultQueue(), messageFactory)
            .addAnalysisWorker(blockNode, options)
            .build();

    try (DssAnalysisWorker actor =
        (DssAnalysisWorker) Objects.requireNonNull(actors.getOnlyActor())) {
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

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {}
}
