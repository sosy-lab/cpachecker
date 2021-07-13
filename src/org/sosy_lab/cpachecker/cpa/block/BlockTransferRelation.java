// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockTransferRelation extends LocationTransferRelation {

  private final ImmutableSet<CFAEdge> edges;
  private final ImmutableSet<CFANode> nodes;

  /**
   * This transfer relation produces successors iff an edge between two nodes exists in the CFA
   * and it is part of the block
   * @param pFactory factory for location states
   * @param pBlockNode a sub graph of the CFA
   */
  public BlockTransferRelation(LocationStateFactory pFactory, BlockNode pBlockNode) {
    super(pFactory);
    edges = validEdgesIn(pBlockNode);
    nodes = ImmutableSet.copyOf(pBlockNode.getNodesInBlock());
  }

  private ImmutableSet<CFAEdge> validEdgesIn(BlockNode pBlockNode) {
    Builder<CFAEdge> setBuilder = ImmutableSet.builder();
    for(CFANode node: pBlockNode.getNodesInBlock()) {
      for(CFAEdge edge: CFAUtils.allLeavingEdges(node)) {
        if (pBlockNode.getNodesInBlock().contains(edge.getSuccessor())) {
          setBuilder.add(edge);
        }
      }
    }
    return setBuilder.build();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    return Sets.intersection(ImmutableSet.copyOf(super.getAbstractSuccessorsForEdge(element, prec, cfaEdge)), edges);
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element, Precision prec) throws CPATransferException {
    return Sets.intersection(ImmutableSet.copyOf(super.getAbstractSuccessors(element, prec)), nodes);
  }

}
