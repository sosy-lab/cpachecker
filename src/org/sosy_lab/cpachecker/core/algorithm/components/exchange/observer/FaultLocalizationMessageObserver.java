// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class FaultLocalizationMessageObserver implements MessageObserver {

  @FileOption(Type.OUTPUT_FILE)
  private static final Path FL_RESULT = Path.of("faults.txt");

  private final Set<Message> faults;
  private final Connection mainConnection;
  private final LogManager logger;
  private Message resultMessage;
  private Result result;

  public FaultLocalizationMessageObserver(LogManager pLogManager, Connection pConnection) {
    faults = new HashSet<>();
    mainConnection = pConnection;
    logger = pLogManager;
  }

  @Override
  @CanIgnoreReturnValue
  public boolean process(Message pMessage) throws CPAException {
    if (pMessage.getType() == MessageType.ERROR_CONDITION && pMessage.getPayload()
        .containsKey(Payload.FAULT_LOCALIZATION)) {
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
            Splitter.on(",")
                .split(resultMessage.getPayload().getOrDefault(Payload.VISITED, "")));
    faults.removeIf(m -> !visitedBlocks.contains(m.getUniqueBlockId()));
    if (!faults.isEmpty()) {
      logger.logf(
          Level.INFO,
          "Fault localization found %d faults. See %s for more information.",
          faults.size(),
          FL_RESULT);
      try {
        Files.writeString(
            FL_RESULT,
            Joiner.on("\n")
                .join(
                    transformedImmutableListCopy(
                        faults, m -> m.getPayload().getOrDefault(Payload.FAULT_LOCALIZATION, ""))));
      } catch (IOException pE) {
        throw new CPAException("Unable to write faults to file: " + FL_RESULT, pE);
      }
    } else {
      logger.log(
          Level.INFO,
          "It seems like no block changes the value of the variables in the pre-condition. Most likely"
              + " the variables in your post-condition never change their value.");
    }
  }
}
