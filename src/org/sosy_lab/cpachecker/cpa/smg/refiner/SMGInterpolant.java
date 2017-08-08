/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGIntersectStates.SMGIntersectionResult;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class SMGInterpolant {

  private static final SMGInterpolant FALSE = new SMGInterpolant();

  private final ImmutableSet<SMGAbstractionBlock> abstractionBlock;
  private final ImmutableSet<SMGMemoryPath> trackedMemoryPaths;
  private final ImmutableSet<MemoryLocation> trackedStackVariables;
  private final ImmutableSet<SMGState> smgStates;

  private SMGInterpolant() {
    abstractionBlock = ImmutableSet.of();
    trackedMemoryPaths = ImmutableSet.of();
    trackedStackVariables = ImmutableSet.of();
    smgStates = ImmutableSet.of();
  }

  public SMGInterpolant(Collection<SMGState> pStates) {
    this(pStates, Collections.emptySet());
  }

  public SMGInterpolant(
      Collection<SMGState> pStates, Collection<SMGAbstractionBlock> pAbstractionBlock) {
    smgStates = ImmutableSet.copyOf(pStates);
    abstractionBlock = ImmutableSet.copyOf(pAbstractionBlock);

    Builder<SMGMemoryPath> memoryPaths = ImmutableSet.builder();
    Builder<MemoryLocation> stackVariables = ImmutableSet.builder();
    for (SMGState state : smgStates) {
      memoryPaths.addAll(state.getMemoryPaths());
      stackVariables.addAll(state.getStackVariables().keySet());
    }

    trackedMemoryPaths = memoryPaths.build();
    trackedStackVariables = stackVariables.build();
  }

  /** return a new instance of each state from the interpolant, if possible. */
  public Set<SMGState> reconstructStates() {
    if (isFalse()) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      return new HashSet<>(Collections2.transform(smgStates, s -> new SMGState(s)));
    }
  }

  public Set<SMGMemoryPath> getMemoryLocations() {
    return trackedMemoryPaths;
  }

  public boolean isTrue() {
    /* No heap abstraction can be performed without hv-edges, thats
     * why every interpolant without hv-edges and stack variables is true.
     */
    return !isFalse() && trackedMemoryPaths.isEmpty() && trackedStackVariables.isEmpty();
  }

  public boolean isFalse() {
    return this == FALSE;
  }

  public boolean isTrivial() {
    return isTrue() || isFalse();
  }

  public SMGInterpolant join(SMGInterpolant pOtherInterpolant) {
    if (isFalse() || pOtherInterpolant.isFalse()) {
      return SMGInterpolant.FALSE;
    }

    Set<SMGState> joinResult = new HashSet<>();
    Set<SMGState> originalStatesNotJoint = new HashSet<>(smgStates);

    for (SMGState otherState : pOtherInterpolant.smgStates) {
      SMGIntersectionResult result = SMGIntersectionResult.getNotDefinedInstance();

      for (SMGState state : originalStatesNotJoint) {
        result = state.intersectStates(otherState);

        if (result.isDefined()) {
          break;
        }
      }

      if (result.isDefined()) {
        originalStatesNotJoint.remove(result.getSmg1());
        joinResult.add(result.getCombinationResult());
      } else {
        joinResult.add(otherState);
      }
    }

    joinResult.addAll(originalStatesNotJoint);

    Set<SMGAbstractionBlock> jointAbstractionBlock =
        Sets.union(abstractionBlock, pOtherInterpolant.abstractionBlock);
    return new SMGInterpolant(joinResult, jointAbstractionBlock);
  }

  public static SMGInterpolant createInitial(LogManager logger, MachineModel model,
      FunctionEntryNode pMainFunctionNode, SMGOptions options) {
    SMGState initState = new SMGState(logger, model, options);
    CFunctionEntryNode functionNode = (CFunctionEntryNode) pMainFunctionNode;
    try {
      initState.addStackFrame(functionNode.getFunctionDefinition());
    } catch (SMGInconsistentException exc) {
      logger.log(Level.SEVERE, exc.getMessage());
    }

    return new SMGInterpolant(ImmutableSet.of(initState));
  }

  public static SMGInterpolant getFalseInterpolant() {
    return FALSE;
  }

  @Override
  public String toString() {
    if (isFalse()) {
      return "FALSE";
    } else {
      return "Tracked memory paths: " + trackedMemoryPaths
          + "\nAbstraction blocks: " + abstractionBlock
          + "\nTracked stack variables: " + trackedStackVariables
          + "\nBasic SMG states: " + Collections2.transform(smgStates, SMGState::getId);
    }
  }

  public static SMGInterpolant getTrueInterpolant(SMGInterpolant template) {

    if (template.isFalse()) {
      throw new IllegalArgumentException(
        "Can't create true interpolant from a false interpolant template.");
    }

    SMGState templateState = template.smgStates.iterator().next();
    SMGState newState = new SMGState(templateState);
    newState.clearValues();
    newState.clearObjects();

    return new SMGInterpolant(ImmutableSet.of(newState));
  }

  public SMGPrecisionIncrement getPrecisionIncrement() {
    return new SMGPrecisionIncrement(trackedMemoryPaths, abstractionBlock, trackedStackVariables);
  }

  public static class SMGPrecisionIncrement {

    private final ImmutableSet<SMGMemoryPath> pathsToTrack;
    private final ImmutableSet<SMGAbstractionBlock> abstractionBlock;
    private final ImmutableSet<MemoryLocation> stackVariablesToTrack;

    private SMGPrecisionIncrement(
        Collection<SMGMemoryPath> pPathsToTrack,
        Collection<SMGAbstractionBlock> pAbstractionBlock,
        Collection<MemoryLocation> pStackVariablesToTrack) {
      pathsToTrack = ImmutableSet.copyOf(pPathsToTrack);
      abstractionBlock = ImmutableSet.copyOf(pAbstractionBlock);
      stackVariablesToTrack = ImmutableSet.copyOf(pStackVariablesToTrack);
    }

    public Set<SMGMemoryPath> getPathsToTrack() {
      return pathsToTrack;
    }

    public Set<SMGAbstractionBlock> getAbstractionBlock() {
      return abstractionBlock;
    }

    public Set<MemoryLocation> getStackVariablesToTrack() {
      return stackVariablesToTrack;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(abstractionBlock, pathsToTrack, stackVariablesToTrack);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof SMGPrecisionIncrement)) {
        return false;
      }
      SMGPrecisionIncrement other = (SMGPrecisionIncrement) obj;
      return Objects.equal(abstractionBlock, other.abstractionBlock)
          && Objects.equal(pathsToTrack, other.pathsToTrack)
          && Objects.equal(stackVariablesToTrack, other.stackVariablesToTrack);
    }

    @Override
    public String toString() {
      return "SMGPrecisionIncrement [pathsToTrack=" + pathsToTrack + ", abstractionBlock="
          + abstractionBlock + ", stackVariablesToTrack=" + stackVariablesToTrack + "]";
    }

    public SMGPrecisionIncrement join(SMGPrecisionIncrement pInc2) {
      Builder<SMGMemoryPath> pathsToTrack = ImmutableSet.builder();
      pathsToTrack.addAll(this.pathsToTrack).addAll(pInc2.pathsToTrack);
      Builder<SMGAbstractionBlock> abstractionBlock = ImmutableSet.builder();
      abstractionBlock.addAll(this.abstractionBlock).addAll(pInc2.abstractionBlock);
      Builder<MemoryLocation> stackVariablesToTrack = ImmutableSet.builder();
      stackVariablesToTrack.addAll(this.stackVariablesToTrack).addAll(pInc2.stackVariablesToTrack);

      return new SMGPrecisionIncrement(
          pathsToTrack.build(), abstractionBlock.build(), stackVariablesToTrack.build());
    }
  }
}