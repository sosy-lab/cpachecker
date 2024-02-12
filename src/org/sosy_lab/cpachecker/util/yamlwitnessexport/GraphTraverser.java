// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class provides a generic implementation of a graph traverser. It is used to traverse a graph
 * in a breadth-first manner. The graph is represented by a set of nodes and a function that returns
 * the successors of a given node. The graph is traversed by calling the method {@link #traverse()}.
 *
 * @param <NodeType> the type of the nodes in the graph
 * @param <E> the type of the exception that can be thrown during the traversal
 */
public abstract class GraphTraverser<NodeType, E extends Throwable> {

  private List<NodeType> waitlist;
  private Set<NodeType> reached;

  public GraphTraverser(NodeType startNode) {
    waitlist = new ArrayList<>(ImmutableList.of(startNode));
    reached = new HashSet<>(waitlist);
  }

  protected List<NodeType> getWaitlist() {
    return waitlist;
  }

  protected Set<NodeType> getReached() {
    return reached;
  }

  public void traverse() throws E {
    while (!waitlist.isEmpty()) {
      NodeType current = waitlist.remove(0);
      for (NodeType successor : getSuccessors(current)) {
        successor = visit(successor);
        if (!stop(successor)) {
          reached.add(successor);
          waitlist.add(successor);
        }
      }
    }
  }

  /**
   * This method returns true whenever a node's successors shall not be explored any further. The
   * default behavior is to return true if the successor is already in the set of reached nodes.
   *
   * @throws E in an exceptional state needs to be communicated to the caller
   */
  protected boolean stop(NodeType successor) throws E {
    return reached.contains(successor);
  }

  protected abstract NodeType visit(NodeType pSuccessor) throws E;

  protected abstract Iterable<NodeType> getSuccessors(NodeType pCurrent) throws E;
}
