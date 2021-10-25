// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class BlockAwareARGTransferRelation implements TransferRelation {
  private final ARGTransferRelation transferRelation;

  private final Block block;

  private final AnalysisDirection direction;

  BlockAwareARGTransferRelation(
      final Block pBlock,
      final ARGTransferRelation pTransferRelation,
      final AnalysisDirection pDirection) {
    block = pBlock;
    transferRelation = pTransferRelation;
    direction = pDirection;
  }

  @Override
  public Collection<BlockAwareARGState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {
    assert state instanceof BlockAwareARGState;

    Collection<AbstractState> successors;

    LocationState locState = extractStateByType(state, LocationState.class);
    successors = new ArrayList<>(2);
    if (locState == null) {
      final String message =
          "BlockAwareARGTransferRelation requires a composite CPA with LocationCPA";
      throw new CPATransferException(message);
    }

    for (CFAEdge edge : locState.getOutgoingEdges()) {
      if (remainsWithinBlock(edge)) {
        successors.addAll(transferRelation.getAbstractSuccessorsForEdge(state, precision, edge));
      }
    }

    return successors.stream()
        .map(
            successor ->
                new BlockAwareARGState((ARGState) successor, block, direction))
        .collect(Collectors.toList());
  }

  private boolean remainsWithinBlock(final CFAEdge edge) {
    if (direction == AnalysisDirection.FORWARD) {
      return block.contains(edge.getSuccessor());
    } else {
      return block.contains(edge.getPredecessor());
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    final String message =
        "BlockAwareARGTransferRelation does not implement getAbstractSuccessorsForEdge";
    throw new CPATransferException(message);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    return transferRelation.strengthen(state, otherStates, cfaEdge, precision);
  }
}
