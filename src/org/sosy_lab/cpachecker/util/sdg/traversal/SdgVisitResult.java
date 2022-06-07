// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

/**
 * Result of a node or edge visit that guides system dependence graph (SDG) traversals and is
 * returned by a {@link SdgVisitor}.
 */
public enum SdgVisitResult {

  /**
   * Continue traversal.
   *
   * <p>Meaning for a visited node: follow all edges leaving (for forwards traversals) or entering
   * (for backwards traversals) this node.
   *
   * <p>Meaning for a visited edge: follow the edge to its successor (for forwards traversals) or
   * predecessor (for backwards traversals).
   */
  CONTINUE,

  /**
   * Terminate traversal immediately.
   *
   * <p>No more nodes and edges are visited after returning this result during a SDG traversal. The
   * traversal ends immediately after returning this visit result.
   */
  TERMINATE,

  /**
   * Skip this node or edge.
   *
   * <p>Meaning for a visited node: do not follow any edges leaving (for forwards traversals) or
   * entering (for backward traversals) this node. Other nodes connected to the visited nodes may
   * still be visited during the traversal when adjacent nodes are reached.
   *
   * <p>Meaning for a visited edge: do not follow the edge to its successor (for forwards
   * traversals) or predecessor (for backward traversals).
   *
   * <p>The traversal still continues for other edges and nodes that were not skipped.
   */
  SKIP;
}
