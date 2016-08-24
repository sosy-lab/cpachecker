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
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;

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
   */
  int getMaxLevel();

  /**
   * Gets all reached states belonging to the specified location and frame level.
   */
  Set<BooleanFormula> getStatesForLocation(CFANode pLocation, int pLevel);

  /**
   * Gets all reached states of <strong>every</strong> location at the specified level.
   * This is the same as calling {@link #getStatesForLocation(CFANode, int)} for every
   * location.
   */
  Map<CFANode, Set<BooleanFormula>> getStatesForAllLocations(int pLevel);

  /**
   * Blocks a state, i.e. adds its negation to the frames belonging to the specified
   * location at all levels up till the provided maximum.
   */
  void blockState(BooleanFormula pState, int pMaxLevel, CFANode pLocation);

  /**
   * Tries to push states forward along the local transition between locations if they
   * are inductive relative to frame states. Subsumes redundant states during the process.
   * @throws SolverException if one of the SAT checks performed during
   * propagation throws an exception.
   * @throws InterruptedException if propagation was interrupted.
   * @throws CPAException if the analysis creating the transition formula
   * encounters an exception.
   */
  void propagate(ProverEnvironment pProver, ProverEnvironment pSubsumptionProver)
      throws SolverException, InterruptedException, CPAException;
}
