// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This interface defines the methods that are needed to aggregate basic blocks into dynamic
 * MultiEdges.
 */
public interface BasicBlockAggregator {

  /**
   * This method checks if the given node is a valid start for a dynamic MultiEdge.
   */
  boolean isValidMultiEdgeStart(CFANode node);

  /**
   * This method checks if the given edge and its successor node are a valid component for a
   * continuing dynamic MultiEdge. The start node of the MultiEdge is given for reference.
   */
  boolean isValidMultiEdgeComponent(CFANode startNode, CFAEdge edge);
}
