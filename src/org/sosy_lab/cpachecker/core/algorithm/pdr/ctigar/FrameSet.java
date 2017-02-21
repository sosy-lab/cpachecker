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
 * A list of stepwise overapproximations of reachable states called frames. More specifically, the
 * frame at level <i>i</i> represents the states reachable in at most <i>i</i> steps from the
 * program's start-location. Frames are sets of clauses representing constraints. The current
 * frontier can be queried with {@link #getFrontierLevel()}. Note that there is always a frame F_0
 * containing the initial states. New frames have to be explicitly created by {@link
 * #openNextFrame()}. Due to the stepwise interpretation, states blocked at frame <i>i</i> are also
 * blocked at all frames below level <i>i</i>.
 */
public interface FrameSet {

  /**
   * Adds a new frame to the list and increases the current maximum level. The frame is initialized
   * with the safety property, thereby assuming it holds. This method should only be called when the
   * above assumption is justified.
   *
   * @see #getFrontierLevel()
   */
  void openNextFrame();

  /**
   * Gets the current frontier frame level. A frontier level of <i>k</i> means there are <i>k +
   * 1</i> frames <i>F</i>_0 to <i>F</i>_k.
   *
   * @return The currently highest frame level in this FrameSet.
   */
  int getFrontierLevel();

  /**
   * Gets all reachable states in the frame at the specified level.
   *
   * <p>The returned formulas are instantiated as unprimed.
   *
   * @param pLevel The frame level.
   * @return A set of formulas representing an overapproximation of states reachable in at most
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
   * in. Subsumes redundant states during the process. Returns whether two adjacent frames became
   * equal during the whole process.
   *
   * @param pShutdownNotifier The notifier that checks if propagation takes too long and should be
   *     interrupted.
   * @return True if there exists any level <i>i</i> so that the states in frame <i>F_i</i> are
   *     equal to the states in frame <i>F_(i+1)</i>, false otherwise.
   * @throws SolverException If one of the SAT checks performed during propagation throws an
   *     exception.
   * @throws InterruptedException If propagation was interrupted.
   */
  boolean propagate(ShutdownNotifier pShutdownNotifier)
      throws SolverException, InterruptedException;
}
