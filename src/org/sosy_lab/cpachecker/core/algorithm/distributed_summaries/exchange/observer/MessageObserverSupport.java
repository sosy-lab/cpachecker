// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MessageObserverSupport implements MessageObserver {

  private final List<MessageObserver> observerList;

  public MessageObserverSupport() {
    observerList = new ArrayList<>();
  }

  public void register(MessageObserver pObserver) {
    observerList.add(pObserver);
  }

  /**
   * Publishes a message to all registered observers and returns true if a listener wants to stop.
   * Listeners stop whenever they identified a verification result or an exception was reported.
   *
   * @param pMessage message to be processed
   * @return true, if listener should stop listening
   * @throws CPAException wrapper exception
   */
  @Override
  public boolean process(ActorMessage pMessage) throws CPAException {
    boolean finish = false;
    for (MessageObserver messageObserver : observerList) {
      finish |= messageObserver.process(pMessage);
    }
    return finish;
  }

  @Override
  public void finish() throws CPAException, InterruptedException {
    for (MessageObserver messageObserver : observerList) {
      messageObserver.finish();
    }
  }

  public <T extends MessageObserver> T getObserver(Class<T> clazz) {
    for (MessageObserver messageObserver : observerList) {
      if (clazz.isInstance(messageObserver)) {
        return clazz.cast(messageObserver);
      }
    }
    throw new NoSuchElementException("No observer found with class " + clazz);
  }
}
