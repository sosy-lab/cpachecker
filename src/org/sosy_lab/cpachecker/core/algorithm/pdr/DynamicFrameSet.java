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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.BackwardTransition;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

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

  /** The frames per location. */
  private final Map<CFANode, List<ApproximationFrame>> frames;

  /** The current number of frames. */
  private int currentMaxLevel;

  private final BooleanFormulaManager bfmgr;
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
  public void blockStates(BooleanFormula pState, int pMaxLevel, CFANode pLocation) {
    Preconditions.checkPositionIndex(pMaxLevel, currentMaxLevel + 1);
    if (!frames.containsKey(pLocation)) {
      initFrameSetForLocation(pLocation);
    }

    ApproximationFrame targetFrame = frames.get(pLocation).get(pMaxLevel);

    // Only need to add to highest level (delta encoding)
    targetFrame.addState(bfmgr.not(pState));
  }

  @Override
  public boolean propagate(
      PDRSat pPDRSat,
      ShutdownNotifier pShutdownNotifier)
      throws InterruptedException, CPAException, SolverException {

    /*
     * For all levels i till max, for all locations l', for all predecessors l of l',
     * for all clauses s in all l', if s is inductive relative to all l' :
     * Take s from F(i,l') and also add it to F(i+1,l)
     */
    for (int level = 0; level <= currentMaxLevel; ++level) {
      final int lvl = level;

      // For each location to propagate TO
      for (Map.Entry<CFANode, List<ApproximationFrame>> mapEntry : frames.entrySet()) {
        CFANode propTarget = mapEntry.getKey();
        FluentIterable<Block> blocksToPropTarget = backwardTransition.getBlocksTo(propTarget);
        FluentIterable<BooleanFormula> allClausesInPropTargetPreds = blocksToPropTarget
            .transform(Block::getPredecessorLocation)
            .transformAndConcat(loc -> getStatesForLocation(loc, lvl));

        for (BooleanFormula clause : allClausesInPropTargetPreds) {
          boolean canPropFromAllPreds = true;

          // Check if propagation possible FROM ALL predecessors
          for (Block predBlock : blocksToPropTarget) {
            if (!pPDRSat.canPropagate(clause, lvl, predBlock)) {
              canPropFromAllPreds = false;
              break;
            }
            pShutdownNotifier.shutdownIfNecessary();
          }

          // Add TO successor location if all predecessors allowed propagation
          if (canPropFromAllPreds) {
            blockStates(clause, lvl + 1, propTarget);
          }
        }
      }

      /*
       *  Early termination if layer at current iteration became empty for all locations.
       *  Propagation not continued beyond that iteration.
       */
      if (clearRedundantClauses(lvl + 1, pPDRSat, pShutdownNotifier)) {
        return true;
      }
    }

    /*
     *  Usual termination if no layer became empty for all locations.
     *  Full cycle of propagation was executed.
     */
    return false;
  }

  /**
   * For each location, goes over all frame clauses for levels <= pMaxCheckedLevel
   * and removes redundant ones. If some frame below pMaxCheckedLevel became empty
   * for all locations, returns true, else false.
   */
  private boolean clearRedundantClauses(int pMaxCheckedLevel, PDRSat pPDRSat, ShutdownNotifier pShutdownNotifier)
      throws SolverException, InterruptedException {

    // For all levels till provided maximum
    for (int frameLevel = 0; frameLevel <= pMaxCheckedLevel; ++frameLevel) {

      // For all frames per location
      for (Map.Entry<CFANode, List<ApproximationFrame>> mapEntry : frames.entrySet()) {
        List<ApproximationFrame> framesForCurrentLoc = mapEntry.getValue();

        // TODO deep copy like below necessary?
        Set<BooleanFormula> potentiallySubsumingClauses = Sets.newHashSet();
        for (BooleanFormula clause : framesForCurrentLoc.get(frameLevel).getStates()) {
          potentiallySubsumingClauses.add(bfmgr.and(clause, bfmgr.makeTrue()));
        }

        // For all clauses in frame at current level at current location
        for (BooleanFormula maybeSubsumingClause : potentiallySubsumingClauses) {

          // For all levels up to current
          for (int searchedLevel = frameLevel; searchedLevel >= 0; --searchedLevel) {
            ApproximationFrame checkedLayer = framesForCurrentLoc.get(searchedLevel);
            Set<BooleanFormula> checkedClauses = checkedLayer.getStates();

            // Be careful to not subsume itself
            if (searchedLevel == frameLevel) {
              checkedClauses.remove(maybeSubsumingClause);
            }

            // For all clauses to be checked against
            Iterator<BooleanFormula> it = checkedClauses.iterator();
            while (it.hasNext()) {

              // Finally check redundancy
              if (pPDRSat.subsumes(maybeSubsumingClause, it.next())) {
                it.remove();
                pShutdownNotifier.shutdownIfNecessary();
              }
            }
          }
        }
      }

      // Check if layer at current iteration became empty for all locations.
      int lvl = frameLevel;
      if (frames.entrySet()
          .stream()
          .map(entry -> entry.getValue().get(lvl))
          .allMatch(ApproximationFrame::isEmpty)) {
        return true;
      }
    }
    return false;
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

    private Set<BooleanFormula> getStates() {
      return states;
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
