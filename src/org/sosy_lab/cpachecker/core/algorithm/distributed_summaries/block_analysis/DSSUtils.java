// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DSSObserverWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DSSObserverWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DSSObserverWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.ExtendedSpecificationViolationTargetInformation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DSSUtils {

  private DSSUtils() {}

  static Optional<CFANode> abstractStateToLocation(AbstractState state) {
    return Optional.ofNullable(AbstractStates.extractLocation(state))
        .or(
            () ->
                Optional.ofNullable(AbstractStates.extractStateByType(state, BlockState.class))
                    .map(BlockState::getLocationNode));
  }

  static DSSMessagePayload appendStatus(
      AlgorithmStatus pStatus, DSSMessagePayload pCurrentPayload) {
    return DSSMessagePayload.builder()
        .addAllEntries(pCurrentPayload)
        .addEntry(
            DSSMessagePayload.PROPERTY,
            pStatus.wasPropertyChecked()
                ? StatusPropertyChecked.CHECKED.name()
                : StatusPropertyChecked.UNCHECKED.name())
        .addEntry(
            DSSMessagePayload.SOUND,
            pStatus.isSound() ? StatusSoundness.SOUND.name() : StatusSoundness.UNSOUND.name())
        .addEntry(
            DSSMessagePayload.PRECISE,
            pStatus.isPrecise() ? StatusPrecise.PRECISE.name() : StatusPrecise.IMPRECISE.name())
        .buildPayload();
  }

  /**
   * Analyze the code block until all target states in this block are found. Block entry points
   * (initial and final location) are target states, too.
   *
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  static BlockAnalysisIntermediateResult findReachableTargetStatesInBlock(
      Algorithm pAlgorithm, ReachedSet pReachedSet, BlockNode pBlockNode)
      throws CPAException, InterruptedException {
    AbstractState startState = pReachedSet.getFirstState();
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    status = status.update(pAlgorithm.run(pReachedSet));
    return new BlockAnalysisIntermediateResult(pReachedSet, startState, pBlockNode, status);
  }

  static class BlockAnalysisIntermediateResult {

    private final ImmutableSet<ARGState> specificationViolations;
    private final ImmutableSet<ARGState> extendedSpecificationViolations;
    private final ImmutableSet<ARGState> abstractions;
    private final AlgorithmStatus status;

    private BlockAnalysisIntermediateResult(
        ReachedSet pReachedSet,
        AbstractState pStartState,
        BlockNode pBlockNode,
        AlgorithmStatus pStatus) {
      ImmutableSet.Builder<ARGState> feasibleAbstractionLocations = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> realViolations = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> abstractionLocations = ImmutableSet.builder();
      for (AbstractState abstractState : pReachedSet) {
        if (abstractState.equals(pStartState)) {
          continue;
        }
        ARGState argState = (ARGState) abstractState;
        boolean isAdded = false;
        if (argState.isTarget()) {
          for (TargetInformation targetInformation : argState.getTargetInformation()) {
            if (targetInformation instanceof ExtendedSpecificationViolationTargetInformation ext) {
              if (ext.isViolation()) {
                feasibleAbstractionLocations.add(argState);
              } else {
                abstractionLocations.add(argState);
              }
              isAdded = true;
            }
          }
          if (!isAdded) {
            realViolations.add(argState);
            isAdded = true;
          }
        }
        if (!isAdded
            && abstractStateToLocation(abstractState)
                .map(location -> location.equals(pBlockNode.getLast()))
                .orElse(false)) {
          abstractionLocations.add(argState);
        }
      }
      specificationViolations = realViolations.build();
      extendedSpecificationViolations = feasibleAbstractionLocations.build();
      abstractions = abstractionLocations.build();
      status = pStatus;
    }

    public AlgorithmStatus getStatus() {
      return status;
    }

    public ImmutableSet<ARGState> getAbstractions() {
      return abstractions;
    }

    public ImmutableSet<ARGState> getSpecificationViolations() {
      return specificationViolations;
    }

    public ImmutableSet<ARGState> getViolations() {
      return ImmutableSet.<ARGState>builder()
          .addAll(getSpecificationViolations())
          .addAll(getExtendedSpecificationViolations())
          .build();
    }

    public ImmutableSet<ARGState> getExtendedSpecificationViolations() {
      return extendedSpecificationViolations;
    }

    @Override
    public String toString() {
      return "BlockAnalysisIntermediateResult{"
          + ", violationStates="
          + specificationViolations
          + ", extendedViolationStates="
          + extendedSpecificationViolations
          + ", blockEndStates="
          + abstractions
          + ", status="
          + status
          + '}';
    }
  }
}
