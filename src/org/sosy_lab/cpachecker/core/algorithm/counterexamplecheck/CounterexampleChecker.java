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
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import java.util.Set;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for components that can verify the feasibility of a counterexample.
 *
 * A counterexample is a finite set of loop-free paths in the ARG that form a
 * DAG with a single source (the root state of the ARG) and a single sink
 * (the target state).
 */
public interface CounterexampleChecker {

  /**
   * Check feasibility of counterexample.
   * @param rootState The source of the counterexample paths.
   * @param errorState The sink of the counterexample paths.
   * @param errorPathStates All state that belong to the counterexample paths.
   * @return True if the counterexample is feasible.
   * @throws CPAException If something goes wrong.
   * @throws InterruptedException If the thread was interrupted.
   */
  boolean checkCounterexample(ARGState rootState, ARGState errorState,
            Set<ARGState> errorPathStates)
            throws CPAException, InterruptedException;

}
