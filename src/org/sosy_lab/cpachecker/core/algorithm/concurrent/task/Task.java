// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;

/**
 * {@link Task} provides a common base for all classes which implement subtasks of concurrent
 * analysis.
 */
public abstract class Task implements Runnable {
  protected final MessageFactory messageFactory;
  protected final LogManager logManager;
  protected final ShutdownNotifier shutdownNotifier;
  
  protected final ARGCPA cpa;
  protected final Algorithm algorithm;
  protected final ReachedSet reached;
  
  protected Task(
      final ARGCPA pCPA,
      final Algorithm pAlgorithm,
      final ReachedSet pReachedSet,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    cpa = pCPA;
    algorithm = pAlgorithm;
    reached = pReachedSet;
    
    messageFactory = pMessageFactory;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }
  
  @Override
  public final void run() {
    try {
      execute();
    } catch (final InterruptedException exception) {
      logManager.log(Level.INFO, "Task aborted due to shutdown request:", this);
      messageFactory.sendTaskAbortedMessage(this);
    } catch (final Throwable object) {
      logManager.log(Level.WARNING, "Unexpected throwable:", object);
      messageFactory.sendTaskAbortedMessage(this);
    } 
  }

  protected abstract void execute() throws Exception;
  
  @Override
  public abstract String toString();
  
  public ARGCPA getCPA() {
    return cpa;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }
  
  public ReachedSet getReachedSet() {
    return reached;
  }
}
