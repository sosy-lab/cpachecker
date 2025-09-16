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
    AbstractState state =
        cpa.getInitialState(lastLocation, StateSpacePartition.getDefaultPartition());
    if (pPreviousCondition.isPresent()) {
      state =
          Objects.requireNonNull(
              AbstractStates.extractStateByType(
                  pPreviousCondition.orElseThrow(), state.getClass()));
    }
    for (int i = counterexample.size() - 1; i >= 0; i--) {
      CFAEdge currentEdge = counterexample.get(i);
      if (currentEdge instanceof BlankEdge blank
          && blank.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
        continue;
      }
      Collection<? extends AbstractState> successors =
          transferRelation.getAbstractSuccessorsForEdge(
              state,
              cpa.getInitialPrecision(
                  currentEdge.getSuccessor(), StateSpacePartition.getDefaultPartition()),
              currentEdge);
      if (successors.isEmpty()) {
        return Optional.empty();
      }
      state = Objects.requireNonNull(Iterables.getOnlyElement(successors));
    }
    return Optional.of(state);
  }
}
