// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ErrorMessageObserver implements MessageObserver {

  @Override
  public boolean process(ActorMessage pMessage) throws CPAException {
    if (pMessage.getType() == MessageType.ERROR) {
      throw new CPAException(
          pMessage
              .getPayload()
              .getOrDefault(
                  Payload.EXCEPTION, "Error message received without exception message."));
    }
    return false;
  }

  @Override
  public void finish() {}
}
