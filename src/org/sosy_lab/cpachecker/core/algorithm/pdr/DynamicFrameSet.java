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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.ProverEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DynamicFrameSet implements FrameSet {

  private static final int DEFAULT_LOWEST_SSA_INDEX = 1;

  /** The frames per location. */
  private final Map<CFANode, List<ApproximationFrame>> frames;

  /** The current number of frames. */
  private int currentMaxLevel;

  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final BackwardTransition backwardTransition;

  public DynamicFrameSet(
      CFANode pStartLocation, FormulaManagerView pFmgr, BackwardTransition pBackwardTransition) {
    currentMaxLevel = 0;
    frames = Maps.newHashMap();
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    backwardTransition = pBackwardTransition;
    initFrameSetForLocation(pStartLocation, true);
  }

  private void initFrameSetForLocation(CFANode pLocation, boolean isStartLocation) {
    List<ApproximationFrame> initial = new ArrayList<>(currentMaxLevel + 2);
    initial.add(newDefaultFrame(isStartLocation));
    for (int level = 0; level <= currentMaxLevel; ++level) {
      initial.add(newDefaultFrame(true));
    }
    frames.put(pLocation, initial);
  }

  private ApproximationFrame newDefaultFrame(boolean pInitialValue) {
    ApproximationFrame f = new ApproximationFrame();
    f.addState(bfmgr.makeBoolean(pInitialValue));
    return f;
  }

  @Override
  public void openNextFrameSet() {
    for (List<ApproximationFrame> frameList : frames.values()) {
      frameList.add(newDefaultFrame(true));
    }
    currentMaxLevel++;
  }

  @Override
  public int getMaxLevel() {
    return currentMaxLevel;
  }

  @Override
  public Set<BooleanFormula> getStatesForLocation(CFANode pLocation, int pLevel) {
    Preconditions.checkPositionIndex(pLevel, currentMaxLevel);
    if (!frames.containsKey(pLocation)) {
      return Collections.emptySet();
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
    Preconditions.checkPositionIndex(pLevel, currentMaxLevel);

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
      initFrameSetForLocation(pLocation, false);
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
     * for all states s in F(i,l), check if s is inductive an add/subsume if it is
     * the case. Inductivity means: F(i,l) & T(l->l') & not(s_prime) is unsatisfiable.
     */
    for (int level = 1; level <= currentMaxLevel - 1; ++level) { // TODO bounds ok ?
      for (Map.Entry<CFANode, List<ApproximationFrame>> mapEntry : frames.entrySet()) {
        CFANode location = mapEntry.getKey();
        Set<BooleanFormula> frameStates = getStatesForLocation(location, level);
        int numberFrameStates = frameStates.size();

        for (BooleanFormula state : frameStates) { // Push F(i,l) [unprimed]
          pProver.push(
              fmgr.instantiate(state, SSAMap.emptySSAMap().withDefault(DEFAULT_LOWEST_SSA_INDEX)));
        }

        FluentIterable<Block> blocksToLocation = backwardTransition.getBlocksTo(location);

        // Invert blocks so that the SSA indices for the predecessors
        // ("unprimed" variables) match
        blocksToLocation = blocksToLocation.transform(block -> block.invertDirection());

        for (Block block : blocksToLocation) {
          CFANode predecessorLocation = block.getPredecessorLocation();
          pProver.push(block.getFormula()); // Push transition

          for (BooleanFormula state : frameStates) {

            // Push state [primed]
            pProver.push(bfmgr.not(fmgr.instantiate(state, block.getPrimedContext().getSsa())));

            if (pProver.isUnsat()) {
              if (location.equals(predecessorLocation)) {
                mapEntry.getValue().get(level).removeState(state);
              }
              addWithSubsumption(
                  state, frames.get(predecessorLocation).get(level + 1), pSubsumptionProver);
            }
            pProver.pop(); // Pop state [primed]
          }
          pProver.pop(); // Pop transition
        }
        for (int i = 0; i < numberFrameStates; ++i) { // Pop F(i,l) [unprimed]
          pProver.pop();
        }
      }
    }
  }

  private void addWithSubsumption(
      BooleanFormula pState, ApproximationFrame pTargetFrame, ProverEnvironment pProver)
      throws SolverException, InterruptedException {

    boolean addState = true;
    for (BooleanFormula stateInTargetFrame : pTargetFrame.getStates()) {

      /*
       * Check if other state is stronger than pState. If implication holds :
       * Do not add.
       */
      pProver.push(bfmgr.implication(stateInTargetFrame, pState));
      if (!pProver.isUnsat()) {
        addState = false;
      }
      pProver.pop();

      /*
       * Check if pState state is stronger than other one. If implication holds :
       * Remove other one and add pState later.
       */
      pProver.push(bfmgr.implication(pState, stateInTargetFrame));
      if (!pProver.isUnsat()) {
        pTargetFrame.removeState(stateInTargetFrame);
      }
      pProver.pop();
    }

    if (addState) {
      pTargetFrame.addState(pState);
    }
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
  }
}
