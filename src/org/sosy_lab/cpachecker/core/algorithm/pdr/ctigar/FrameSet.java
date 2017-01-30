/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A list of stepwise over-approximations of reachable states called frames. More specifically, the
 * frame at level 'i' represents the states reachable in at most 'i' steps from the program start
 * location. Frames are sets of clauses representing constraints. The current frontier can be
 * queried with {@link #getMaxLevel()}. Note that there is always a frame '0' containing the initial
 * states. New frames have to be explicitly created by {@link #openNextFrame()}. Due to the stepwise
 * interpretation, clauses added to frame 'i' are also added to all frames below level 'i'.
 */
public interface FrameSet {

  /**
   * Adds a new frame to the list and increases the current maximum level. The frame is initialized
   * with the safety property, thereby assuming it holds.
   *
   * @see #getMaxLevel()
   */
  void openNextFrame();

  /**
   * Gets the current maximum frame level. A frontier level of 'n' means there are 'n + 1' frames
   * F(0) to F(n).
   *
   * @return The currently highest frame level in this FrameSet.
   */
  int getMaxLevel();

  /**
   * Gets all reachable states of the frame at the specified level.
   *
   * <p>The returned formulas are instantiated as unprimed.
   *
   * @param pLevel The frame level.
   * @return A set of formulas representing an over-approximation of states reachable in at most
   *     {@code pLevel} steps from the initial states.
   */
  Set<BooleanFormula> getStates(int pLevel);

  /**
   * Blocks states, i.e. adds their negation to all frames at and below the specified level.
   *
   * <p>The provided states should be instantiated as unprimed.
   *
   * @param pStates The formula representing the blockable states.
   * @param pMaxLevel The highest level the states should be blocked at.
   */
  void blockStates(BooleanFormula pStates, int pMaxLevel);

  /**
   * Tries to push states forward based on whether they are inductive relative to the frame they are
   * in. Subsumes redundant states during the process. Returns whether 2 adjacent frames became
   * equal.
   *
   * @param pShutdownNotifier The notifier that checks if propagation takes too long and should be
   *     interrupted.
   * @return True, if there exists any level i so that the states in frame F(i) are equal to the
   *     states in frame F(i+1)
   * @throws SolverException If one of the SAT checks performed during propagation throws an
   *     exception.
   * @throws InterruptedException If propagation was interrupted.
   */
  boolean propagate(ShutdownNotifier pShutdownNotifier)
      throws SolverException, InterruptedException;
}
