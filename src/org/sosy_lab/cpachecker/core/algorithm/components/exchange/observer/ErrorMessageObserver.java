// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer;

import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ErrorMessageObserver implements MessageObserver {

  @Override
  public boolean process(Message pMessage) throws CPAException {
    if (pMessage.getType() == MessageType.ERROR) {
      throw new CPAException(pMessage.getPayload().get(Payload.EXCEPTION));
    }
    return false;
  }

  @Override
  public void finish() {

  }
}
