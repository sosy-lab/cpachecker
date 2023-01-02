// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGIntersectStates;
import org.sosy_lab.cpachecker.cpa.smg.SMGIntersectStates.SMGIntersectionResult;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGInterpolant implements Interpolant<Collection<SMGState>, SMGInterpolant> {

  private static final SMGInterpolant FALSE = new SMGInterpolant();

  private final ImmutableSet<SMGAbstractionBlock> abstractionBlock;
  private final ImmutableSet<SMGMemoryPath> trackedMemoryPaths;
  private final ImmutableSet<MemoryLocation> trackedStackVariables;
  private final ImmutableSet<UnmodifiableSMGState> smgStates;

  private SMGInterpolant() {
    abstractionBlock = ImmutableSet.of();
    trackedMemoryPaths = ImmutableSet.of();
    trackedStackVariables = ImmutableSet.of();
    smgStates = ImmutableSet.of();
  }

  public SMGInterpolant(Collection<? extends UnmodifiableSMGState> pStates) {
    this(pStates, ImmutableSet.of());
  }

  public SMGInterpolant(
      Collection<? extends UnmodifiableSMGState> pStates,
      Collection<SMGAbstractionBlock> pAbstractionBlock) {
    smgStates = ImmutableSet.copyOf(pStates);
    abstractionBlock = ImmutableSet.copyOf(pAbstractionBlock);

    ImmutableSet.Builder<SMGMemoryPath> memoryPaths = ImmutableSet.builder();
    ImmutableSet.Builder<MemoryLocation> stackVariables = ImmutableSet.builder();
    for (UnmodifiableSMGState state : smgStates) {
      memoryPaths.addAll(new SMGMemoryPathCollector(state.getHeap()).getMemoryPaths());
      stackVariables.addAll(state.getStackVariables().keySet());
    }

    trackedMemoryPaths = memoryPaths.build();
    trackedStackVariables = stackVariables.build();
  }

  /** return a new instance of each state from the interpolant, if possible. */
  @Override
  public Set<SMGState> reconstructState() {
    if (isFalse()) {
      throw new IllegalStateException("Can't reconstruct state from FALSE-interpolant");
    } else {
      return new HashSet<>(Collections2.transform(smgStates, s -> s.copyOf()));
    }
  }

  @Override
  public Set<MemoryLocation> getMemoryLocations() {
    return trackedStackVariables;
  }

  Set<SMGMemoryPath> getMemoryPaths() {
    return trackedMemoryPaths;
  }

  @Override
  public boolean isTrue() {
    /* No heap abstraction can be performed without hv-edges, thats
     * why every interpolant without hv-edges and stack variables is true.
     */
    return !isFalse() && trackedMemoryPaths.isEmpty() && trackedStackVariables.isEmpty();
  }

  @Override
  public boolean isFalse() {
    return this == FALSE;
  }

  @Override
  public boolean isTrivial() {
    return isTrue() || isFalse();
  }

  @Override
  public SMGInterpolant join(SMGInterpolant pOtherInterpolant) {
    if (isFalse() || pOtherInterpolant.isFalse()) {
      return SMGInterpolant.FALSE;
    }

    Set<UnmodifiableSMGState> joinResult = new HashSet<>();
    Set<UnmodifiableSMGState> originalStatesNotJoint = new HashSet<>(smgStates);

    for (UnmodifiableSMGState otherState : pOtherInterpolant.smgStates) {
      SMGIntersectionResult result = SMGIntersectionResult.getNotDefinedInstance();

      for (UnmodifiableSMGState state : originalStatesNotJoint) {
        result = new SMGIntersectStates(state, otherState).intersect();

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

  public static SMGInterpolant createInitial(
      LogManager logger,
      MachineModel model,
      FunctionEntryNode pMainFunctionNode,
      SMGOptions options)
      throws SMGInconsistentException {
    SMGState initState = new SMGState(logger, model, options);
    CFunctionEntryNode functionNode = (CFunctionEntryNode) pMainFunctionNode;
    initState.addStackFrame(functionNode.getFunctionDefinition());
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
      return "Tracked memory paths: "
          + trackedMemoryPaths
          + "\nAbstraction blocks: "
          + abstractionBlock
          + "\nTracked stack variables: "
          + trackedStackVariables
          + "\nBasic SMG states: "
          + Collections2.transform(smgStates, UnmodifiableSMGState::getId);
    }
  }

  public static SMGInterpolant getTrueInterpolant(SMGInterpolant template) {

    checkArgument(
        !template.isFalse(), "Can't create true interpolant from a false interpolant template.");

    UnmodifiableSMGState templateState = template.smgStates.iterator().next();
    SMGState newState = templateState.copyOf();
    newState.clearValues();
    newState.clearObjects();

    return new SMGInterpolant(ImmutableSet.of(newState));
  }

  public SMGPrecisionIncrement getPrecisionIncrement() {
    return new SMGPrecisionIncrement(trackedMemoryPaths, abstractionBlock, trackedStackVariables);
  }

  @Override
  public int getSize() {
    // just for statistics, not really useful
    return trackedStackVariables.size();
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
      return Objects.hash(abstractionBlock, pathsToTrack, stackVariablesToTrack);
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
      return Objects.equals(abstractionBlock, other.abstractionBlock)
          && Objects.equals(pathsToTrack, other.pathsToTrack)
          && Objects.equals(stackVariablesToTrack, other.stackVariablesToTrack);
    }

    @Override
    public String toString() {
      return "SMGPrecisionIncrement [pathsToTrack="
          + pathsToTrack
          + ", abstractionBlock="
          + abstractionBlock
          + ", stackVariablesToTrack="
          + stackVariablesToTrack
          + "]";
    }

    public SMGPrecisionIncrement join(SMGPrecisionIncrement pInc2) {
      return new SMGPrecisionIncrement(
          Sets.union(pathsToTrack, pInc2.pathsToTrack),
          Sets.union(abstractionBlock, pInc2.abstractionBlock),
          Sets.union(stackVariablesToTrack, pInc2.stackVariablesToTrack));
    }
  }
}
