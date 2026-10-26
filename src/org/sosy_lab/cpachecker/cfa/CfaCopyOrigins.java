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
 * The element of the program that each node and edge a post-processing created stands for.
 *
 * <p>Everything that looks up a node or an edge of the program, for example the relation between
 * the CFA and the AST that witnesses are written with, only knows the ones the parser created. This
 * is what tells those lookups what a copy stands for.
 *
 * <p>Several post-processings copy nodes and edges, and they run one after the other, so a copy can
 * be copied again. Every copy is therefore resolved while it is remembered, which keeps every value
 * in here an element that the parser created.
 */
public final class CfaCopyOrigins {

  private final Map<CFANode, CFANode> nodes = new LinkedHashMap<>();
  private final Map<CFAEdge, CFAEdge> edges = new LinkedHashMap<>();
  private boolean copiesOfLoopHeadsEndABlock = false;

  /** Remembers that the given node was copied from the given one. */
  public void rememberNode(CFANode pCopy, CFANode pCopiedFrom) {
    CFANode original = originalOf(pCopiedFrom);
    assert !nodes.containsKey(original) : "a copy is not an element of the program: " + original;
    nodes.put(pCopy, original);
  }

  /** Remembers that the given edge was copied from the given one. */
  public void rememberEdge(CFAEdge pCopy, CFAEdge pCopiedFrom) {
    CFAEdge original = originalOf(pCopiedFrom);
    assert !edges.containsKey(original) : "a copy is not an element of the program: " + original;
    edges.put(pCopy, original);
  }

  /**
   * Asks for the copies of a loop head to end a block, cf. {@link #endsABlock}. The post-processing
   * that copies them decides this, because it is the one that knows whether anything still reports
   * about the loops of the program afterwards.
   */
  public void letCopiesOfLoopHeadsEndABlock() {
    copiesOfLoopHeadsEndABlock = true;
  }

  /**
   * The node of the program that the given node stands for, which is the node itself unless a
   * post-processing copied it.
   */
  public CFANode originalOf(CFANode pNode) {
    return nodes.getOrDefault(pNode, pNode);
  }

  /**
   * The edge of the program that the given edge stands for, which is the edge itself unless a
   * post-processing copied it.
   */
  public CFAEdge originalOf(CFAEdge pEdge) {
    return edges.getOrDefault(pEdge, pEdge);
  }

  /**
   * Whether a block of a basic-block aggregation has to end at the given node, although the shape
   * of the CFA does not say so.
   */
  public boolean endsABlock(CFANode pNode) {
    // A loop head of the program is entered from more than one place and ends a block on its own,
    // while a copy of it is entered from one place only.
    return copiesOfLoopHeadsEndABlock && originalOf(pNode).isLoopStart();
  }
}
