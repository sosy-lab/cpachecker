/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for components that can verify the feasibility of a counterexample.
 *
 * A counterexample is a finite set of loop-free paths in the ART that form a
 * DAG with a single source (the root element of the ART) and a single sink
 * (the target element).
 */
public interface CounterexampleChecker {

  /**
   * Check feasibility of counterexample.
   * @param rootElement The source of the counterexample paths.
   * @param errorElement The sink of the counterexample paths.
   * @param errorPathElements All elements that belong to the counterexample paths.
   * @return True if the counterexample is feasible.
   * @throws CPAException If something goes wrong.
   * @throws InterruptedException If the thread was interrupted.
   */
  boolean checkCounterexample(ARTElement rootElement, ARTElement errorElement,
            Set<ARTElement> errorPathElements)
            throws CPAException, InterruptedException;

}