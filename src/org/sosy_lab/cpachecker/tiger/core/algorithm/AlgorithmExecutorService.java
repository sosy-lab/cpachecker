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
package org.sosy_lab.cpachecker.tiger.core.algorithm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Executes an algorithm with an optional timeout.
 */
public class AlgorithmExecutorService {

  private static AlgorithmExecutorService instance;
  private ExecutorService pool;
  private ReachedSet reachedSet;

  public static AlgorithmExecutorService getInstance(){
    if (instance == null){
      instance = new AlgorithmExecutorService();
    }

    return instance;
  }

  private AlgorithmExecutorService(){
    pool = Executors.newSingleThreadExecutor();
  }


  public void shutdownNow() {
    pool.shutdownNow();
  }

  /**
   * Executes the algorithm. If timeout is 0, then the algorithm has no time limit.
   * @param pAlgorithm
   * @param pReachedSet
   * @param pNotifier
   * @param timeout
   * @param units
   * @return
   */
  public boolean execute(Algorithm pAlgorithm, ReachedSet pReachedSet, ShutdownNotifier pNotifier, long timeout, TimeUnit units){
    CallableAlgorithm thr = new CallableAlgorithm(pAlgorithm, pReachedSet);

    boolean isSound = false;
    boolean forceShutdown = false;

    if (timeout <= 0){
      return runNoTimeout(pAlgorithm, pReachedSet);
    }

    Future<Boolean> task = pool.submit(thr);

    try {
      isSound = task.get(timeout, units);
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    catch (TimeoutException e) {
      forceShutdown = true;
    }
    catch (InterruptedException e) {
      isSound = false;
    }

    if (forceShutdown){
      // timeout occured, let the analysis end in a clean state
      pNotifier.requestShutdown("timeout");

      try {
        isSound = pAlgorithm.run(pReachedSet);
      }
      catch (InterruptedException e1) {
        isSound = false;
      } catch (CPAException e) {
        throw new RuntimeException(e);
      }

      task.cancel(true);
    }

    reachedSet = pReachedSet;
    return isSound;
  }

  private boolean runNoTimeout(Algorithm pAlgorithm, ReachedSet pReachedSet) {
    boolean isSound;
    try {
      isSound = pAlgorithm.run(pReachedSet);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      isSound = false;
    }

    return isSound;
  }


  public ReachedSet getReachedSet() {
    return reachedSet;
  }


}