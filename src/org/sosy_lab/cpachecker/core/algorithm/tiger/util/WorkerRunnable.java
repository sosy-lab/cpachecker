/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.LinkedList;
import java.util.List;

import javax.management.JMException;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;


public class WorkerRunnable implements Runnable, ShutdownRequestListener {

  private Algorithm algorithm;
  private ReachedSet localReachedSet;
  private boolean soundAnalysis = true;
  private Throwable caughtThrowable = null;
  private ResourceLimitChecker limitChecker;
  private boolean timeoutOccured = false;

  public WorkerRunnable(Algorithm pAlgorithm, ReachedSet pReachedSet, long pTimelimitInSeconds, ShutdownManager pShutdownManager) {
    algorithm = pAlgorithm;
    localReachedSet = pReachedSet;

    List<ResourceLimit> limits = new LinkedList<>();
    ProcessCpuTimeLimit limit;
    try {
      limit = ProcessCpuTimeLimit.fromNowOn(pTimelimitInSeconds, java.util.concurrent.TimeUnit.SECONDS);
    } catch (JMException e) {
      throw new RuntimeException(e);
    }
    limits.add(limit);

    pShutdownManager.getNotifier().register(this);

    limitChecker = new ResourceLimitChecker(pShutdownManager, limits);
  }

  @Override
  public void run() {
    try {
      limitChecker.start();
      soundAnalysis = algorithm.run(localReachedSet).isSound();
      limitChecker.cancel();
    } catch (InterruptedException e) {
      soundAnalysis = false;
      timeoutOccured = true;
    } catch (Throwable t) {
      caughtThrowable = t;
    }
  }

  public boolean throwableWasCaught() {
    return (caughtThrowable != null);
  }

  public Throwable getCaughtThrowable() {
    return caughtThrowable;
  }

  public boolean analysisWasSound() {
    return soundAnalysis;
  }

  public boolean hasTimeout() {
    return timeoutOccured;
  }

  @Override
  public void shutdownRequested(String pReason) {
    Thread.currentThread().interrupt();
  }

}

