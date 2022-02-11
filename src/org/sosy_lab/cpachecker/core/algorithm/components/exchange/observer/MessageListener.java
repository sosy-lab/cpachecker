// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MessageListener {

  private final List<MessageObserver> observerList;

  public MessageListener() {
    observerList = new ArrayList<>();
  }

  public void register(MessageObserver pObserver) {
    observerList.add(pObserver);
  }

  /**
   * Publishes a message to all registered observers and returns true if a listener wants to stop.
   *
   * @param pMessage message to be processed
   * @return true, if listener should stop listening
   * @throws CPAException wrapper exception
   */
  public boolean publish(Message pMessage) throws CPAException {
    boolean finish = false;
    for (MessageObserver messageObserver : observerList) {
      finish |= messageObserver.process(pMessage);
    }
    return finish;
  }

  public void finish() throws CPAException, InterruptedException {
    for (MessageObserver messageObserver : observerList) {
      messageObserver.finish();
    }
  }

  public <T extends MessageObserver> T getObserver(Class<T> clazz) {
    for (MessageObserver messageObserver : observerList) {
      if (messageObserver.getClass().equals(clazz)) {
        return clazz.cast(messageObserver);
      }
    }
    throw new NoSuchElementException("No observer found with class " + clazz);
  }
}
