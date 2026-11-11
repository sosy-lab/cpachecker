// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Computes violation conditions with the backward transfer relation of the CPA. The conditions of
 * two paths are merged only if they are equal.
 */
public class BackwardTransferViolationConditionOperator
    implements MergeableViolationConditionOperator<AbstractState> {

  private final TransferRelation transferRelation;
  private final ConfigurableProgramAnalysis cpa;

  /** Decides equality, because the states do not implement it. */
  private final SerializeOperator serialize;

  public BackwardTransferViolationConditionOperator(
      TransferRelation pTransferRelation,
      ConfigurableProgramAnalysis pCpa,
      SerializeOperator pSerialize) {
    transferRelation = pTransferRelation;
    cpa = pCpa;
    serialize = pSerialize;
  }

  @Override
  public AbstractState initialCondition(ARGState pTarget, Optional<ARGState> pPreviousCondition)
      throws InterruptedException {
    CFANode location = Objects.requireNonNull(AbstractStates.extractLocation(pTarget));
    AbstractState state = cpa.getInitialState(location, StateSpacePartition.getDefaultPartition());
    return pPreviousCondition.isEmpty()
        ? state
        : Objects.requireNonNull(
            AbstractStates.extractStateByType(pPreviousCondition.orElseThrow(), state.getClass()));
  }

  @Override
  public Optional<AbstractState> prepend(AbstractState pCondition, List<CFAEdge> pEdges)
      throws InterruptedException, CPATransferException {
    AbstractState state = pCondition;
    for (CFAEdge edge : pEdges.reversed()) {
      if (edge instanceof BlankEdge
          && edge.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
        continue;
      }
      Collection<? extends AbstractState> successors =
          transferRelation.getAbstractSuccessorsForEdge(
              state,
              cpa.getInitialPrecision(
                  edge.getSuccessor(), StateSpacePartition.getDefaultPartition()),
              edge);
      if (successors.isEmpty()) {
        return Optional.empty();
      }
      state = Iterables.getOnlyElement(successors);
    }
    return Optional.of(state);
  }

  @Override
  public Optional<AbstractState> merge(AbstractState pFirst, AbstractState pSecond) {
    return serialize.serialize(pFirst).equals(serialize.serialize(pSecond))
        ? Optional.of(pFirst)
        : Optional.empty();
  }

  @Override
  public Optional<AbstractState> finish(
      ARGPath pPath, Optional<ARGState> pPreviousCondition, AbstractState pCondition) {
    return Optional.of(pCondition);
  }
}
