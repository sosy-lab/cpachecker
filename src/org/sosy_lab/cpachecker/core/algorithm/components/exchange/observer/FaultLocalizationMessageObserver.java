// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Connection;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class FaultLocalizationMessageObserver implements MessageObserver {

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
    while (!mainConnection.isEmpty()) {
      process(mainConnection.read());
    }
    Set<String> visitedBlocks = new HashSet<>(Splitter.on(",")
        .splitToList(resultMessage.getPayload().getOrDefault(Payload.VISITED, "")));
    faults.removeIf(m -> !(visitedBlocks.contains(m.getUniqueBlockId())));
    if (!faults.isEmpty()) {
      logger.log(Level.INFO, "Found faults:\n" + Joiner.on("\n").join(
          faults.stream().map(m -> m.getPayload().getOrDefault(Payload.FAULT_LOCALIZATION, ""))
              .collect(
                  Collectors.toSet())));
    }
  }
}
