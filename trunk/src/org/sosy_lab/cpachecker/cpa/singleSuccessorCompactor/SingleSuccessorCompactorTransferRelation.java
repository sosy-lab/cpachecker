// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatHist;

public class SingleSuccessorCompactorTransferRelation
    extends AbstractSingleWrapperTransferRelation {

  @Nullable private final BlockPartitioning partitioning;
  private final StatHist chainSizes;
  private final int maxChainLength;

  SingleSuccessorCompactorTransferRelation(
      TransferRelation pDelegate,
      BlockPartitioning pPartitioning,
      StatHist pChainSizes,
      int pMaxChainLength) {
    super(pDelegate);
    partitioning = pPartitioning;
    chainSizes = pChainSizes;
    maxChainLength = pMaxChainLength;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState state, final Precision precision)
      throws CPATransferException, InterruptedException {
    return getAbstractSuccessorsWithList(state, precision, null);
  }

  /**
   * Computes successors and optionally adds all intermediate states to the given list. The list
   * includes the first state (given as parameter) until (exclusive) the last states that are
   * returned directly.
   */
  Collection<? extends AbstractState> getAbstractSuccessorsWithList(
      AbstractState state, final Precision precision, final @Nullable List<AbstractState> lst)
      throws CPATransferException, InterruptedException {

    // this is the main core of this CPA:
    // iterate as long as there is only one successor, abort on target, zero or multiple successors.

    Collection<? extends AbstractState> states;
    int chainSize = 0;
    do {
      chainSize++;
      if (lst != null) {
        lst.add(state);
      }
      states = transferRelation.getAbstractSuccessors(state, precision);
      state = Iterables.getFirst(states, null);
    } while (canExpandChain(state, states, chainSize));
    chainSizes.insertValue(chainSize);
    return states;
  }

  private boolean canExpandChain(
      AbstractState state, Collection<? extends AbstractState> states, int chainSize) {
    return states.size() == 1
        && (maxChainLength == -1 || chainSize <= maxChainLength)
        && !AbstractStates.isTargetState(state)
        && !isAtBlockBorder(state);
  }

  private boolean isAtBlockBorder(AbstractState pState) {
    if (partitioning == null) {
      return false;
    }
    CFANode node = AbstractStates.extractLocation(pState);
    return partitioning.isCallNode(node) || partitioning.isReturnNode(node);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {
    throw new UnsupportedOperationException();
  }
}
