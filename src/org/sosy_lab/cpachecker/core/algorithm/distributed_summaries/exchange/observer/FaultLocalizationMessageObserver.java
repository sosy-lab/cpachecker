// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "distributedSummaries.observer")
public class FaultLocalizationMessageObserver implements MessageObserver {

  @Option(
      description =
          "path for the file storing the results of the distributed fault localization algorithm")
  @FileOption(Type.OUTPUT_FILE)
  private Path resultFile = Path.of("faults.txt");

  private final Set<Message> faults;
  private final Connection mainConnection;
  private final LogManager logger;
  private Message resultMessage;
  private Result result;

  public FaultLocalizationMessageObserver(LogManager pLogManager, Connection pConnection, Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    faults = new HashSet<>();
    mainConnection = pConnection;
    logger = pLogManager;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean process(Message pMessage) throws CPAException {
    if (pMessage.getType() == MessageType.ERROR_CONDITION
        && pMessage.getPayload().containsKey(Payload.FAULT_LOCALIZATION)) {
      faults.add(pMessage);
    }
    if (pMessage.getType() == MessageType.FOUND_RESULT) {
      resultMessage = pMessage;
      result = Result.valueOf(pMessage.getPayload().get(Payload.RESULT));
    }
    return false;
  }

  @Override
  public void finish() throws InterruptedException, CPAException {
    if (result != Result.FALSE) {
      return;
    }
    // read all remaining faults
    // since a violation has already been found, we know that every possible fault is present
    // but the message might not have been processed by now
    while (!mainConnection.isEmpty()) {
      process(mainConnection.read());
    }
    Set<String> visitedBlocks =
        ImmutableSet.copyOf(
            Splitter.on(",").split(resultMessage.getPayload().getOrDefault(Payload.VISITED, "")));
    faults.removeIf(m -> !visitedBlocks.contains(m.getUniqueBlockId()));
    if (!faults.isEmpty()) {
      logger.logf(
          Level.INFO,
          "Fault localization found %d faults. See %s for more information.",
          faults.size(),
          resultFile);
      try (Writer outputFile = IO.openOutputFile(resultFile, StandardCharsets.UTF_8)) {
        Joiner.on("\n")
            .appendTo(
                outputFile,
                FluentIterable.from(faults)
                    .transform(m -> m.getPayload().getOrDefault(Payload.FAULT_LOCALIZATION, "")));
      } catch (IOException pE) {
        logger.logUserException(
            Level.WARNING, pE, "Unable to write results of fault localization to a file");
      }
    } else {
      logger.log(
          Level.INFO,
          "It seems like no block changes the value of the variables in the pre-condition. Most likely"
              + " the variables in your post-condition never change their value.");
    }
  }
}
