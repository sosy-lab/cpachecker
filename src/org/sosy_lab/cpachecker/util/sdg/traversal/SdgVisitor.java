// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

import java.util.Collection;
import org.sosy_lab.cpachecker.util.sdg.SdgEdgeType;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;

/**
 * Instances of {@link SdgVisitor} are informed about node and edge visits during system dependence
 * graph (SDG) traversals. They also guide SDG traversals by returning specific {@link
 * SdgVisitResult} constants.
 *
 * <p>All visitor implementations should implement either {@link ForwardsSdgVisitor} or {@link
 * BackwardsSdgVisitor} instead of implementing this interface directly. This enforces the correct
 * direction for SDG traversal.
 *
 * @param <N> the node type of the SDG
 * @see SdgVisitResult
 * @see SystemDependenceGraph#traverse(Collection, ForwardsSdgVisitor)
 * @see SystemDependenceGraph#traverse(Collection, BackwardsSdgVisitor)
 */
public interface SdgVisitor<N extends SystemDependenceGraph.Node<?, ?, ?>> {

  /**
   * Called during SDG traversals when a node is visited and returns a {@link SdgVisitResult}
   * constant that guides the SDG traversal.
   *
   * @param pNode the visited node
   * @return a {@link SdgVisitResult} constant to guide the SDG traversal
   */
  SdgVisitResult visitNode(N pNode);

  /**
   * Called during SDG traversals when an edge is visited and returns a {@link SdgVisitResult}
   * constant that guides the SDG traversal.
   *
   * @param pType the {@link SdgEdgeType} of the visited edge
   * @param pPredecessor the predecessor of the visited edge
   * @param pSuccessor the successor of the visited edge
   * @return a {@link SdgVisitResult} constant to guide the SDG traversal
   */
  SdgVisitResult visitEdge(SdgEdgeType pType, N pPredecessor, N pSuccessor);
}
