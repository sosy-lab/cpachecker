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
package org.sosy_lab.cpachecker.core.algorithm.pdr;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.Map;
import java.util.Set;

/**
 * A list of sets of reachable states for locations. More specifically, for any location, the
 * set at position <i>i</i> represents the states reachable in at most <i>i</i> steps from the
 * program start location.
 */
public interface FrameSet {

  /**
   * Adds a new empty set of frames for all locations and increases the current maximum level.
   * @see #getMaxLevel()
   */
  void openNextFrameSet();

  /**
   * Gets the current maximum frame level.
   * @return the currently highest frame level in the FrameSet
   */
  int getMaxLevel();

  /**
   * Gets all reached states belonging to the specified location and frame level.
   * @param pLocation the program location
   * @param pLevel the frame level
   * @return a set of formulas representing an over-approximation of states reachable
   * at {@pLocation} in at most {@code pLevel} steps from the start location
   */
  Set<BooleanFormula> getStatesForLocation(CFANode pLocation, int pLevel);

  /**
   * Gets all reached states of <strong>every</strong> location at the specified level.
   * This is the same as calling {@link #getStatesForLocation(CFANode, int)} for every
   * location.
   * @param pLevel the frame level
   * @return a map containing sets of formulas representing an over-approximation of states
   * reachable at the respective location in at most {@code pLevel} steps from the start
   * location
   */
  Map<CFANode, Set<BooleanFormula>> getStatesForAllLocations(int pLevel);

  /**
   * Blocks a state, i.e. adds its negation to the frames belonging to the specified
   * location at <strong>all</strong> levels up till the provided maximum.
   * @param pState the state to be blocked
   * @param pMaxLevel the highest level the state should be blocked at
   * @param pLocation the program location the state should be blocked at
   */
  void blockState(BooleanFormula pState, int pMaxLevel, CFANode pLocation);

  /**
   * Checks if there exists any level <i>i</i> so that two neighboring frames F(i,l)
   * and F(i+1,l) are equal for all locations <i>l</i>.
   * @return {@code true} if a level as described above exists, {@code false} otherwise
   */
  boolean isConvergent();

  /**
   * Tries to push states forward along the local transition between locations/blocks if they
   * are inductive relative to frame states. Subsumes redundant states during the process.
   * @param pProver the prover environment used to make more complicated sat checks
   * @param pSubsumptionProver the prover environment used to make simple implication checks
   * for subsumption
   * @throws SolverException if one of the SAT checks performed during
   * propagation throws an exception.
   * @throws InterruptedException if propagation was interrupted.
   * @throws CPAException if the analysis creating the transition formula
   * encounters an exception.
   */
  void propagate(ProverEnvironment pProver, ProverEnvironment pSubsumptionProver)
      throws SolverException, InterruptedException, CPAException;
}
