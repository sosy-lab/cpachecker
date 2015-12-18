/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.refinement;

import java.util.Deque;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for checking the feasibility of error paths.
 */
public interface FeasibilityChecker<S extends AbstractState> {

  /**
   * Returns whether the given path is feasible, starting at the initial state.
   * This method's precision depends on the implementation.
   *
   * @param path the path to investigate
   * @return <code>true</code> if the given path is feasible,
   *    <code>false</code> otherwise
   */
  boolean isFeasible(final ARGPath path) throws CPAException, InterruptedException;

  /**
   * Returns whether the given path is feasible, starting at the given state.
   * This method's precision depends on the implementation.
   *
   * @param path the path to investigate
   * @param startingPoint the state to start at
   * @return <code>true</code> if the given path is feasible,
   *    <code>false</code> otherwise
   */
  boolean isFeasible(final ARGPath path, S startingPoint) throws CPAException, InterruptedException;

  boolean isFeasible(final ARGPath path, S startingPoint, Deque<S> callstack)
      throws CPAException, InterruptedException;

  /**
   * Returns whether the given path is feasible, starting at the initial state.
   *
   * For this method, a given path is feasible, if the end is reachable,
   * and it is a target of a given control automaton. This way, a more precise
   * set of concrete path can be specified to be tested.
   *
   * This method's precision depends on the implementation.
   *
   * @param path the path to investigate
   * @param automatons these automatons specify the concrete target states of the given path
   * @return <code>true</code> if the given path is feasible,
   *    <code>false</code> otherwise
   */
  boolean isFeasible(final ARGPath path, Set<ControlAutomatonCPA> automatons) throws CPAException, InterruptedException;

  /**
   * Returns whether the given path is feasible, starting at the given state.
   * This method's precision depends on the implementation.
   *
   * For this method, a given path is feasible, if the end is reachable,
   * and it is a target of a given control automaton. This way, a more precise
   * set of concrete path can be specified to be tested.
   *
   * @param path the path to investigate
   * @param startingPoint the state to start at
   * @param automatons these automatons specify the concrete target states of the given path
   * @return <code>true</code> if the given path is feasible,
   *    <code>false</code> otherwise
   */
  boolean isFeasible(final ARGPath path, S startingPoint, Set<ControlAutomatonCPA> automatons) throws CPAException, InterruptedException;

}