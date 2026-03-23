// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Edge cloner for POR that delegates to {@link PorCfaCloner} which clones the entire CFA (nodes
 * and edges) per thread ID.
 */
final class PorEdgeCloner {

  private PorEdgeCloner() {}

  /**
   * Returns the cloned edge for the given original CFA edge and thread ID. The first call for a
   * given thread ID triggers a full CFA clone. If the edge is already a cloned edge, it is returned
   * as-is.
   */
  static CFAEdge clone(final CFAEdge pCFAEdge, final int pid, final CFA pCfa) {
    PorCfaCloner cfaCloner = PorCfaCloner.getOrCreate(pid, pCfa);
    return cfaCloner.getClonedEdge(pCFAEdge);
  }

  /**
   * Returns the cloned CFA node corresponding to the given node for the given thread ID. If the
   * node is already a cloned node, it is returned as-is.
   */
  static CFANode getClonedNode(final CFANode pNode, final int pid, final CFA pCfa) {
    PorCfaCloner cfaCloner = PorCfaCloner.getOrCreate(pid, pCfa);
    return cfaCloner.getClonedNode(pNode);
  }

  /**
   * Returns the original CFA node corresponding to the given (potentially cloned) node. If the
   * node is not a cloned node, it is returned as-is.
   */
  static CFANode getOriginalNode(final CFANode pNode) {
    return PorCfaCloner.getOriginalNode(pNode);
  }

  /**
   * Returns the original CFA edge corresponding to the given (potentially cloned) edge. If the
   * edge is not a cloned edge, it is returned as-is.
   */
  static CFAEdge getOriginalEdge(final CFAEdge pEdge) {
    return PorCfaCloner.getOriginalEdge(pEdge);
  }
}
