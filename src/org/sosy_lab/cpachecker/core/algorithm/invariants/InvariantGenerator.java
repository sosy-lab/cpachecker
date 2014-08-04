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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;


/**
 * Interface for methods to generate invariants about the program.
 *
 * First {@link #start(CFANode)} needs to be called with the entry point
 * of the CFA, and then {@link #get()} can be called to retrieve the reached
 * set with the invariants.
 *
 * It is a good idea to call {@link #start(CFANode)} as soon as possible
 * and {@link #get()} as late as possible to minimize waiting times
 * if the generator is configured for asynchronous execution.
 *
 * It is also a good idea to call {@link #get()} only if really necessary
 * (in synchronous case, it is expensive).
 */
public interface InvariantGenerator {

  /**
   * Prepare invariant generation, and optionally start the algorithm.
   * May be called only once.
   */
  void start(CFANode initialLocation);

  /**
   * Cancel the invariant generation algorithm, if running.
   * Can be called only after {@link #start(CFANode)} was called.
   */
  void cancel();

  /**
   * Retrieve the generated ReachedSet with the invariants.
   * Can be called only after {@link #start(CFANode)} was called.
   * May be called more than once and returns the same result.
   * @return
   * @throws CPAException If the invariant generation failed.
   * @throws InterruptedException If the invariant generation was interrupted.
   */
  UnmodifiableReachedSet get() throws CPAException, InterruptedException;

  /*
   * Returns a Timer from which the time that was necessary to generate
   * the invariants can be read.
   * For correct measurements, the caller should not modify the Timer.
   */
  Timer getTimeOfExecution();
}