// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.Collections;
import java.util.Iterator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

// TODO: this class can be removed when `CfaNetwork` is finished
public final class DominanceUtils {

  private DominanceUtils() {}

  private static Iterable<CFANode> createNodeIterable(
      CFANode pNode, boolean pForward, Predicate<CFANode> pStop, Predicate<CFANode> pFilter) {

    if (pStop.test(pNode)) {
      return Collections::emptyIterator;
    }

    Iterator<CFANode> iterator =
        (pForward ? CFAUtils.allSuccessorsOf(pNode) : CFAUtils.allPredecessorsOf(pNode)).iterator();

    return () -> Iterators.filter(iterator, pFilter);
  }

  private static Iterable<CFANode> createPredecessorIterable(
      CFANode pNode, ImmutableSet<CFANode> pIgnoreSet) {

    return createNodeIterable(
        pNode,
        false,
        node -> node instanceof FunctionEntryNode,
        node -> !(node instanceof FunctionExitNode) && !pIgnoreSet.contains(node));
  }

  private static Iterable<CFANode> createSuccessorIterable(
      CFANode pNode, ImmutableSet<CFANode> pIgnoreSet) {

    return createNodeIterable(
        pNode,
        true,
        node -> node instanceof FunctionExitNode,
        node -> !(node instanceof FunctionEntryNode) && !pIgnoreSet.contains(node));
  }

  public static DomTree<CFANode> createFunctionDomTree(FunctionEntryNode pEntryNode) {

    return DomTree.forGraph(
        node -> createPredecessorIterable(node, ImmutableSet.of()),
        node -> createSuccessorIterable(node, ImmutableSet.of()),
        pEntryNode);
  }

  public static DomTree<CFANode> createFunctionPostDomTree(FunctionEntryNode pEntryNode) {

    return DomTree.forGraph(
        node -> createSuccessorIterable(node, ImmutableSet.of()),
        node -> createPredecessorIterable(node, ImmutableSet.of()),
        pEntryNode.getExitNode());
  }

  public static DomTree<CFANode> createFunctionDomTree(
      CFANode pStartNode, ImmutableSet<CFANode> pIgnoreSet) {

    return DomTree.forGraph(
        node -> createPredecessorIterable(node, pIgnoreSet),
        node -> createSuccessorIterable(node, pIgnoreSet),
        pStartNode);
  }

  public static DomTree<CFANode> createFunctionPostDomTree(
      CFANode pStartNode, ImmutableSet<CFANode> pIgnoreSet) {

    return DomTree.forGraph(
        node -> createSuccessorIterable(node, pIgnoreSet),
        node -> createPredecessorIterable(node, pIgnoreSet),
        pStartNode);
  }
}
