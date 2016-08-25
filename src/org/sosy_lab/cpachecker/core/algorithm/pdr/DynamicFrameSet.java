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

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.BackwardTransition;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Blocks;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A structure that contains an over-approximation of reachable states for any number of steps
 * at any location.
 */
public class DynamicFrameSet implements FrameSet {
  /*
   * This implementation uses the so called "delta encoding" of frames as described
   * in "Efficient Implementation of Property Directed Reachability" by Niklas Een,
   * Alan Mishchenko and Robert Brayton. Since each frame at level 'i' is a subset
   * of frame 'i - 1', each state is only stored in the highest frame where it holds.
   * Thus, the set of states in a frame at level 'i' is computed by adding all states
   * at level 'j' for j from i to current maximum level.
   */

  private static final int DEFAULT_LOWEST_SSA_INDEX = 1;

  /** The frames per location. */
  private final Map<CFANode, List<ApproximationFrame>> frames;

  /** The current number of frames. */
  private int currentMaxLevel;

  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final BackwardTransition backwardTransition;

  /**
   * Creates a new DynamicFrameSet.
   * @param pStartLocation the program start location
   * @param pFmgr the formula manager used as basis for most internal operations
   * @param pBackwardTransition the backward transition used to calculate predecessor blocks and
   * path formulas
   */
  public DynamicFrameSet(
      CFANode pStartLocation, FormulaManagerView pFmgr, BackwardTransition pBackwardTransition) {
    currentMaxLevel = 0;
    frames = Maps.newHashMap();
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    backwardTransition = pBackwardTransition;

    // Initialize frames for start location
    List<ApproximationFrame> initial = new ArrayList<>(2);
    initial.add(new ApproximationFrame());
    initial.add(new ApproximationFrame());
    frames.put(pStartLocation, initial);
  }

  /**
   * Creates a new map entry in {@code frames} for pLocation and initializes the list of
   * approximation frames to the currently expected number of frames.
   */
  private void initFrameSetForLocation(CFANode pLocation) {
    List<ApproximationFrame> initial = new ArrayList<>(currentMaxLevel + 2);
    ApproximationFrame frameAtLevel0 = new ApproximationFrame();
    frameAtLevel0.addState(bfmgr.makeFalse());
    initial.add(frameAtLevel0);

    for (int level = 1; level <= currentMaxLevel + 1; ++level) {
      initial.add(new ApproximationFrame());
    }
    frames.put(pLocation, initial);
  }

  @Override
  public void openNextFrameSet() {
    for (List<ApproximationFrame> frameList : frames.values()) {
      frameList.add(new ApproximationFrame());
    }
    currentMaxLevel++;
  }

  @Override
  public int getMaxLevel() {
    return currentMaxLevel;
  }

  @Override
  public Set<BooleanFormula> getStatesForLocation(CFANode pLocation, int pLevel) {
    Preconditions.checkPositionIndex(pLevel, currentMaxLevel + 1);
    if (!frames.containsKey(pLocation)) {
      initFrameSetForLocation(pLocation);
    }

    /*
     *  Initially start with states at the specified level
     *  and add all states of higher levels afterwards (delta encoding)
     */
    Set<BooleanFormula> states = frames.get(pLocation).get(pLevel).getStates();
    for (int i = pLevel + 1; i <= currentMaxLevel; ++i) {
      states.addAll(frames.get(pLocation).get(i).getStates());
    }

    return states;
  }

  @Override
  public Map<CFANode, Set<BooleanFormula>> getStatesForAllLocations(int pLevel) {
    Preconditions.checkPositionIndex(pLevel, currentMaxLevel + 1);

    Map<CFANode, Set<BooleanFormula>> statesPerLocation = Maps.newHashMap();
    for (CFANode location : frames.keySet()) {
      statesPerLocation.put(location, getStatesForLocation(location, pLevel));
    }

    return statesPerLocation;
  }

  @Override
  public void blockState(BooleanFormula pState, int pMaxLevel, CFANode pLocation) {
    Preconditions.checkPositionIndex(pMaxLevel, currentMaxLevel);
    if (!frames.containsKey(pLocation)) {
      initFrameSetForLocation(pLocation);
    }

    // TODO subsume here too?
    // Only need to add to highest level (delta encoding)
    frames.get(pLocation).get(pMaxLevel).addState(bfmgr.not(pState));
  }

  @Override
  public void propagate(ProverEnvironment pProver, ProverEnvironment pSubsumptionProver)
      throws InterruptedException, CPAException, SolverException {

    /*
     * For all levels i till max, for all locations l', for all predecessors l of l'
     * for all states s in F(i,l), check if s is inductive an add to F(i+1,l') if it is
     * the case. Inductivity means: F(i,l) & T(l->l') & not(s_prime) is unsatisfiable.
     */
    for (int level = 1; level <= currentMaxLevel - 1; ++level) { // TODO bounds ok ?

      // For each location
      for (Map.Entry<CFANode, List<ApproximationFrame>> mapEntry : frames.entrySet()) {
        CFANode location = mapEntry.getKey();
        FluentIterable<Block> blocksToLocation = backwardTransition.getBlocksTo(location);

        // For each predecessor location
        for (Block predBlock : blocksToLocation) {
          CFANode predLocation = predBlock.getPredecessorLocation();
          Set<BooleanFormula> predFrameStates = getStatesForLocation(predLocation, level);
          int numberFrameStates = predFrameStates.size();

          for (BooleanFormula state : predFrameStates) { // Push F(i,l) [unprimed]
            pProver.push(
                fmgr.instantiate(
                    state, SSAMap.emptySSAMap().withDefault(DEFAULT_LOWEST_SSA_INDEX)));
          }

          // Invert blocks so that the SSA indices for the predecessors
          // ("unprimed" variables) match
          BooleanFormula transitionFormula = Blocks.formulaWithInvertedIndices(predBlock, fmgr);
          pProver.push(transitionFormula); // Push transition

          for (BooleanFormula state : predFrameStates) {

            // Push state [primed] // TODO SSA correct ? must be highest ones
            pProver.push(bfmgr.not(fmgr.instantiate(state, predBlock.getPrimedContext().getSsa())));

            if (pProver.isUnsat()) {
              if (location.equals(predLocation)) {
                removeStateFromDeltaLayers(state, predLocation, level);
              }

              // Add state to location at level + 1
              addWithSubsumption(state, location, level + 1, pSubsumptionProver);
            }
            pProver.pop(); // Pop state [primed]
          }
          pProver.pop(); // Pop transition
          for (int i = 0; i < numberFrameStates; ++i) { // Pop F(i,l) [unprimed]
            pProver.pop();
          }
        }
      }
    }
  }

  /** Searches the frames of pLocation starting at pLevel for pState and removes it. */
  private void removeStateFromDeltaLayers(BooleanFormula pState, CFANode pLocation, int pLevel) {
    frames
        .get(pLocation)
        .stream()
        .skip(pLevel) // pState is at pLevel or higher
        .filter(frame -> frame.contains(pState))
        .findFirst()
        .get()
        .removeState(pState);
  }

  @Override
  public boolean isConvergent() { // TODO Use only when subsumption is fully implemented

    // Check if one delta layer is empty for all locations at the same level
    Iterator<CFANode> it = frames.keySet().iterator();
    for (int currentLevel = 1; currentLevel <= currentMaxLevel; ++currentLevel) {
      boolean isLayerEmpty = true;

      while (isLayerEmpty && it.hasNext()) {
        isLayerEmpty = frames.get(it.next()).get(currentLevel).isEmpty();
      }
      if (isLayerEmpty) {
        return true;
      }
    }
    return false;
  }

  /** Adds a state to a frame while also removing all redundant states. */
  private void addWithSubsumption(
      BooleanFormula pState, CFANode pLocation, int pLevel, ProverEnvironment pProver)
      throws SolverException, InterruptedException {

    boolean addState = true;

    for (int level = 0; level <= currentMaxLevel; ++level) {
      ApproximationFrame currentFrame = frames.get(pLocation).get(level);

      for (BooleanFormula stateInFrame :
          currentFrame
              .getStates()
              .stream()
              .filter(bf -> !bfmgr.isFalse(bf))
              .collect(Collectors.toList())) {

        /*
         * Check if other state is stronger than pState. If implication holds :
         * Do not add.
         */
        pProver.push(bfmgr.implication(stateInFrame, pState));
        if (!pProver.isUnsat()) {
          addState = false;
        }
        pProver.pop();

        /*
         * Check if pState state is stronger than other one. If implication holds :
         * Remove other one and add pState later.
         */
        pProver.push(bfmgr.implication(pState, stateInFrame));
        if (!pProver.isUnsat()) {
          currentFrame.removeState(stateInFrame);
        }
        pProver.pop();
      }
    }

    if (addState) {
      frames.get(pLocation).get(pLevel).addState(pState);
    }
  }

  @Override
  public String toString() {
    StringBuilder stringRepresentation = new StringBuilder("Frame set : ");

    for (CFANode location : frames.keySet()) {
      stringRepresentation.append("\n").append(location).append(" ->");

      for (int level = 0; level <= currentMaxLevel + 1; ++level) {
        Set<BooleanFormula> states = getStatesForLocation(location, level);
        stringRepresentation
            .append("\n   Level : ")
            .append(level)
            .append("\n      ")
            .append(states);
      }
    }
    return stringRepresentation.toString();
  }

  /** Holds an over-approximation of reached states for a frame. */
  private static final class ApproximationFrame {

    private final Set<BooleanFormula> states;

    private ApproximationFrame() {
      states = new HashSet<>();
    }

    private void addState(BooleanFormula pState) {
      states.add(pState);
    }

    private void removeState(BooleanFormula pState) {
      states.remove(pState);
    }

    private Set<BooleanFormula> getStates() {
      return states;
    }

    private boolean contains(BooleanFormula pState) {
      return states.contains(pState);
    }

    private boolean isEmpty() {
      return states.isEmpty();
    }

    @Override
    public String toString() {
      return states.toString();
    }
  }
}
