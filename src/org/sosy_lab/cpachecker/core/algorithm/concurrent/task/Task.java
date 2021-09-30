// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import static org.sosy_lab.cpachecker.core.CPAcheckerResult.Result.UNKNOWN;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.SubtaskResult;

/**
 * {@link Task} provides a common base for all classes which implement subtasks of concurrent
 * analysis.
 */
public abstract class Task implements Runnable {
  protected final MessageFactory messageFactory;
  protected final LogManager logManager;
  protected final ShutdownNotifier shutdownNotifier;

  protected Task(
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    messageFactory = pMessageFactory;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }
  
  @Override
  public final void run() {
    try {
      execute();
    } catch (final Throwable object) {
      logManager.log(Level.WARNING, "Unexpected throwable:", object);
      
      SubtaskResult result = SubtaskResult.create(UNKNOWN, AlgorithmStatus.UNSOUND_AND_IMPRECISE);
      messageFactory.sendTaskCompletionMessage(this, result);
    }
  }

  protected abstract void execute() throws Exception;
}
