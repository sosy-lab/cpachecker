// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BackwardTransferViolationConditionOperator implements ViolationConditionOperator {

  private final TransferRelation transferRelation;
  private final ConfigurableProgramAnalysis cpa;

  public BackwardTransferViolationConditionOperator(
      TransferRelation pTransferRelation, ConfigurableProgramAnalysis pCpa) {
    transferRelation = pTransferRelation;
    cpa = pCpa;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException {
    List<CFAEdge> counterexample = pARGPath.getFullPath();
    CFANode lastLocation = Objects.requireNonNull(counterexample.getLast()).getSuccessor();
    return prepend(initialState(lastLocation, pPreviousCondition), counterexample);
  }

  public AbstractState initialState(CFANode location, Optional<ARGState> previous)
      throws InterruptedException {
    AbstractState state = cpa.getInitialState(location, StateSpacePartition.getDefaultPartition());
    return previous.isEmpty()
        ? state
        : Objects.requireNonNull(
            AbstractStates.extractStateByType(previous.orElseThrow(), state.getClass()));
  }

  public Optional<AbstractState> prepend(AbstractState state, List<CFAEdge> edges)
      throws InterruptedException, CPATransferException {
    for (CFAEdge edge : edges.reversed()) {
      if (edge instanceof BlankEdge
          && edge.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
        continue;
      }
      var successors =
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
}
