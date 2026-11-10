// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Remembers the element of the program (created by the parser) that each node and edge created by a
 * transformation of the CFA stands for, e.g., by a copy of the whole CFA. This is unrelated to
 * {@link CFANode#getOriginal()} and {@link CFAEdge#getOriginal()}, which lead from a per-thread
 * clone back to the CFA.
 *
 * <p>Everything that looks up a node or an edge of the program, for example the relation between
 * the CFA and the AST that witnesses are written with, only knows the ones the parser created. This
 * class tells those lookups what a copy stands for.
 */
public final class CfaCopyOrigins {

  private final Map<CFANode, CFANode> nodes = new LinkedHashMap<>();
  private final Map<CFAEdge, CFAEdge> edges = new LinkedHashMap<>();

  /** Remembers that the given node was copied from the given one. */
  public void rememberNode(CFANode pCopy, CFANode pCopiedFrom) {
    CFANode programNode = programNodeOf(pCopiedFrom);
    assert !nodes.containsKey(programNode)
        : "a copy is not an element of the program: " + programNode;
    nodes.put(pCopy, programNode);
  }

  /** Remembers that the given edge was copied from the given one. */
  public void rememberEdge(CFAEdge pCopy, CFAEdge pCopiedFrom) {
    CFAEdge programEdge = programEdgeOf(pCopiedFrom);
    assert !edges.containsKey(programEdge)
        : "a copy is not an element of the program: " + programEdge;
    edges.put(pCopy, programEdge);
  }

  /**
   * The node of the program that the given node stands for, which is the node itself unless a
   * transformation copied it.
   */
  public CFANode programNodeOf(CFANode pNode) {
    return nodes.getOrDefault(pNode, pNode);
  }

  /**
   * The edge of the program that the given edge stands for, which is the edge itself unless a
   * transformation copied it.
   */
  public CFAEdge programEdgeOf(CFAEdge pEdge) {
    return edges.getOrDefault(pEdge, pEdge);
  }
}
